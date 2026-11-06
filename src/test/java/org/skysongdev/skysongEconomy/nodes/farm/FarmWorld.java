package org.skysongdev.skysongEconomy.nodes.farm;

import org.skysongdev.skysongEconomy.business.BlockPosition;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessService;
import org.skysongdev.skysongEconomy.business.InMemoryBusinessRepository;
import org.skysongdev.skysongEconomy.business.access.AccessDefaults;
import org.skysongdev.skysongEconomy.business.access.AccessGroup;
import org.skysongdev.skysongEconomy.business.access.AccessService;
import org.skysongdev.skysongEconomy.business.access.InMemoryAccessRepository;
import org.skysongdev.skysongEconomy.business.access.Permission;
import org.skysongdev.skysongEconomy.business.display.NameplateSettings;
import org.skysongdev.skysongEconomy.business.tier.BusinessTiers;
import org.skysongdev.skysongEconomy.business.tier.TierSettings;
import org.skysongdev.skysongEconomy.business.type.BusinessType;
import org.skysongdev.skysongEconomy.business.type.BusinessTypeRegistry;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.character.DowntimeWallet;
import org.skysongdev.skysongEconomy.character.SkillLevels;
import org.skysongdev.skysongEconomy.config.FileRegistry;
import org.skysongdev.skysongEconomy.items.StoredItems;
import org.skysongdev.skysongEconomy.loot.LootEntry;
import org.skysongdev.skysongEconomy.loot.LootOutput;
import org.skysongdev.skysongEconomy.loot.LootRoller;
import org.skysongdev.skysongEconomy.money.MapWallet;
import org.skysongdev.skysongEconomy.nodes.InMemoryNodeRepository;
import org.skysongdev.skysongEconomy.nodes.Node;
import org.skysongdev.skysongEconomy.nodes.NodeKind;
import org.skysongdev.skysongEconomy.nodes.NodeMenuIcons;
import org.skysongdev.skysongEconomy.nodes.NodeService;
import org.skysongdev.skysongEconomy.nodes.NodeSettings;
import org.skysongdev.skysongEconomy.nodes.NodeTables;
import org.skysongdev.skysongEconomy.nodes.harvest.Harvest;
import org.skysongdev.skysongEconomy.nodes.harvest.HarvestPayer;
import org.skysongdev.skysongEconomy.nodes.harvest.NodeAccess;
import org.skysongdev.skysongEconomy.tokens.GatheringTokens;
import org.skysongdev.skysongEconomy.tokens.TokenRepository;
import org.skysongdev.skysongEconomy.trade.IntRange;
import org.skysongdev.skysongEconomy.trade.ItemAmount;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * A small world for farm tests: the gathering business "farmers" owned by {@link #owner}, with
 * {@link #worker} hired; a "crop" farm type taking wheat (tiers 1 and 2) and barley (tier 3
 * only), and a "pen" type taking sheep; and the farm node "field", tier 2, owned by farmers.
 * Online players get returned primers at once; the rest wait.
 */
final class FarmWorld {
    static final List<String> LEVELS = List.of("unskilled", "novice", "competent", "journeyman", "expert");
    static final NodeSettings SETTINGS = new NodeSettings(Set.of("gathering"),
            Map.of(1, "unskilled", 2, "novice", 3, "competent", 4, "journeyman", 5, "expert"),
            new NameplateSettings(true, 0.4, 16, true, false), NodeMenuIcons.DEFAULTS, 1);
    static final StoredItems SEEDS = StoredItems.of(List.of(new byte[] {1}));
    static final StoredItems SHEEP = StoredItems.of(List.of(new byte[] {2}, new byte[] {3}));

    final CharacterRef owner = character("Owner");
    final CharacterRef worker = character("Worker");
    final CharacterRef stranger = character("Stranger");
    final Map<CharacterRef, String> farmingLevels = new HashMap<>();
    final Map<UUID, Integer> downtime = new HashMap<>();
    final Set<UUID> online = new HashSet<>();
    /** Players with the staff bypass permission. */
    final Set<UUID> bypassing = new HashSet<>();
    final Map<UUID, List<StoredItems>> received = new HashMap<>();
    final List<Harvest> heard = new ArrayList<>();
    final Map<String, Planting> savedPlantings = new LinkedHashMap<>();
    final Map<UUID, PrimerReturn> savedReturns = new LinkedHashMap<>();
    final List<String> plantingChanges = new ArrayList<>();
    final GatheringTokens tokens;
    final BusinessService businesses;
    final AccessService access;
    final NodeTables tables;
    final NodeService nodes;
    final PrimerReturns returns;
    final FarmPlots plots;
    final FarmHarvester harvester;
    final Business farmers;
    final Node field;

    FarmWorld() {
        tokens = new GatheringTokens(new TokenRepository() {
            @Override
            public Map<CharacterRef, Long> loadAll() {
                return Map.of();
            }

            @Override
            public void save(CharacterRef character, long balance) {
            }
        });
        businesses = new BusinessService(
                new BusinessTypeRegistry(List.of(new BusinessType("gathering", "Gathering", Set.of("farming"), false))),
                new BusinessTiers(IntStream.rangeClosed(1, 5).mapToObj(tier -> new TierSettings(tier, tier)).toList()),
                new InMemoryBusinessRepository(), Clock.systemUTC());
        access = new AccessService(new AccessDefaults(
                new AccessGroup(AccessGroup.WORKERS, EnumSet.of(Permission.TRAIN), BigDecimal.ZERO, Set.of()),
                new AccessGroup(AccessGroup.EVERYONE, EnumSet.noneOf(Permission.class), BigDecimal.TEN, Set.of()), 2),
                new InMemoryAccessRepository());
        tables = new NodeTables(FileRegistry.empty(),
                new FileRegistry<>(List.of(new FarmType("crop", "Crop plot", "farming", List.of("wheat", "barley")),
                        new FarmType("pen", "Animal pen", "farming", List.of("sheep")))),
                new FileRegistry<>(List.of(
                        resource("wheat", "wheat_seeds", 1, Map.of(1, new FarmTier(1, 1, wheat(2)), 2, new FarmTier(2, 3, wheat(5)))),
                        resource("barley", "barley_seeds", 1, Map.of(3, new FarmTier(3, 2, wheat(9)))),
                        resource("sheep", "sheep_token", 2, Map.of(1, new FarmTier(1, 1, wheat(1)))))));
        nodes = new NodeService(tables, SETTINGS, businesses, new InMemoryNodeRepository());
        returns = new PrimerReturns(new PrimerReturnRepository() {
            @Override
            public List<PrimerReturn> loadAll() {
                return List.copyOf(savedReturns.values());
            }

            @Override
            public void save(PrimerReturn waiting) {
                savedReturns.put(waiting.id(), waiting);
            }

            @Override
            public void delete(UUID id) {
                savedReturns.remove(id);
            }
        }, (player, items) -> {
            if (!online.contains(player)) {
                return false;
            }
            received.computeIfAbsent(player, ignored -> new ArrayList<>()).add(items);
            return true;
        });
        DowntimeWallet downtimeWallet = new DowntimeWallet() {
            @Override
            public boolean trySpend(UUID player, int amount) {
                int left = available(player);
                if (left < amount) {
                    return false;
                }
                downtime.put(player, left - amount);
                return true;
            }

            @Override
            public int available(UUID player) {
                return downtime.getOrDefault(player, 0);
            }
        };
        NodeAccess nodeAccess = new NodeAccess(businesses);
        plots = new FarmPlots(new FarmPlots.Dependencies(tables, SETTINGS, nodeAccess, access, downtimeWallet,
                new PlantingRepository() {
                    @Override
                    public List<Planting> loadAll() {
                        return List.copyOf(savedPlantings.values());
                    }

                    @Override
                    public void save(Planting planting) {
                        savedPlantings.put(planting.nodeId(), planting);
                    }

                    @Override
                    public void delete(String nodeId) {
                        savedPlantings.remove(nodeId);
                    }
                }, returns, bypassing::contains));
        plots.addListener(plantingChanges::add);
        nodes.addListener(plots);
        businesses.addListener(nodes);
        SkillLevels levels = new SkillLevels() {
            @Override
            public List<String> levelIds() {
                return LEVELS;
            }

            @Override
            public Optional<String> levelOf(CharacterRef character, String skillId) {
                return skillId.equals("farming") ? Optional.of(farmingLevels.getOrDefault(character, "unskilled"))
                        : Optional.empty();
            }
        };
        harvester = new FarmHarvester(new FarmHarvester.Dependencies(plots, SETTINGS, nodeAccess,
                new HarvestPayer(tokens, downtimeWallet, new MapWallet()), levels, heard::add),
                new LootRoller(new Random(1)));
        farmers = businesses.create("farmers", "gathering", owner, new BlockPosition("world", 5, 64, 5));
        businesses.hire(farmers, worker);
        field = nodes.create("field", NodeKind.FARM, "crop", new BlockPosition("world", 1, 64, 1));
        nodes.setOwner(field, "farmers");
        nodes.setTier(field, 2);
    }

    static CharacterRef character(String profile) {
        return new CharacterRef(UUID.nameUUIDFromBytes(profile.getBytes()), profile);
    }

    static LootOutput wheat(int amount) {
        return new LootOutput(List.of(new LootEntry.Item("wheat", IntRange.exactly(amount))), List.of(), List.of());
    }

    private static FarmResource resource(String id, String primer, int amount, Map<Integer, FarmTier> tiers) {
        return new FarmResource(id, "<green>" + id, new ItemAmount(primer, amount), tiers);
    }

    FarmResource resource(String id) {
        return tables.farmResources().find(id).orElseThrow();
    }

    Planting plantWheat() {
        return plots.plant(field, owner, resource("wheat"), SEEDS);
    }

    /** Lets the workers group plant and take out primers. */
    void letWorkersTend() {
        access.setPermission(farmers, AccessGroup.WORKERS, Permission.TEND_FARMS, true);
    }
}
