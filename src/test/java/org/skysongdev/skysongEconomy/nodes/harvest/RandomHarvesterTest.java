package org.skysongdev.skysongEconomy.nodes.harvest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.business.BlockPosition;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessService;
import org.skysongdev.skysongEconomy.business.InMemoryBusinessRepository;
import org.skysongdev.skysongEconomy.business.display.NameplateSettings;
import org.skysongdev.skysongEconomy.business.tier.BusinessTiers;
import org.skysongdev.skysongEconomy.business.tier.TierSettings;
import org.skysongdev.skysongEconomy.business.type.BusinessType;
import org.skysongdev.skysongEconomy.business.type.BusinessTypeRegistry;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.character.DowntimeWallet;
import org.skysongdev.skysongEconomy.character.SkillLevels;
import org.skysongdev.skysongEconomy.loot.LootEntry;
import org.skysongdev.skysongEconomy.loot.LootOutput;
import org.skysongdev.skysongEconomy.loot.LootRoller;
import org.skysongdev.skysongEconomy.messaging.RefusedException;
import org.skysongdev.skysongEconomy.money.MapWallet;
import org.skysongdev.skysongEconomy.nodes.InMemoryNodeRepository;
import org.skysongdev.skysongEconomy.nodes.Node;
import org.skysongdev.skysongEconomy.nodes.NodeKind;
import org.skysongdev.skysongEconomy.nodes.NodeMenuIcons;
import org.skysongdev.skysongEconomy.nodes.NodeService;
import org.skysongdev.skysongEconomy.nodes.NodeSettings;
import org.skysongdev.skysongEconomy.nodes.NodeTables;
import org.skysongdev.skysongEconomy.nodes.random.RandomTable;
import org.skysongdev.skysongEconomy.nodes.random.RandomTableRegistry;
import org.skysongdev.skysongEconomy.nodes.random.RandomTier;
import org.skysongdev.skysongEconomy.tokens.GatheringTokens;
import org.skysongdev.skysongEconomy.tokens.TokenRepository;
import org.skysongdev.skysongEconomy.trade.IntRange;
import org.skysongdev.skysongEconomy.trade.ItemAmount;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RandomHarvesterTest {
    private static final List<String> LEVELS = List.of("unskilled", "novice", "competent", "journeyman", "expert");
    private static final NodeSettings SETTINGS = new NodeSettings(Set.of("gathering"),
            Map.of(1, "unskilled", 2, "novice", 3, "competent", 4, "journeyman", 5, "expert"),
            new NameplateSettings(true, 0.4, 16, true, false), NodeMenuIcons.DEFAULTS);
    private static final BlockPosition BLOCK = new BlockPosition("world", 1, 64, 1);
    private static final BlockPosition BUSINESS_BLOCK = new BlockPosition("world", 5, 64, 5);

    private final CharacterRef miner = character("Miner");
    private final CharacterRef stranger = character("Stranger");
    private final Map<CharacterRef, String> miningLevels = new HashMap<>();
    private final Map<UUID, Integer> downtime = new HashMap<>();
    private final MapWallet wallet = new MapWallet();
    private final List<Harvest> heard = new ArrayList<>();
    private GatheringTokens tokens;
    private BusinessService businesses;
    private NodeService nodes;
    private RandomHarvester harvester;

    private static CharacterRef character(String profile) {
        return new CharacterRef(UUID.nameUUIDFromBytes(profile.getBytes()), profile);
    }

    private static LootOutput coal(int amount) {
        return new LootOutput(List.of(new LootEntry.Item("coal", IntRange.exactly(amount))), List.of(), List.of());
    }

    /** Tier 1: 1 token, or 1 downtime and 5 money in public. Tier 2: 3 tokens, business only. Tier 4: 0 tokens. */
    private static RandomTable mine() {
        return new RandomTable("mine", "Mine", "mining", "mining", Map.of(
                1, new RandomTier(1, 1, Optional.of(new HarvestCost(1, 0, new BigDecimal("5"))), coal(2)),
                2, new RandomTier(2, 3, Optional.empty(), coal(5)),
                4, new RandomTier(4, 0, Optional.of(HarvestCost.FREE), coal(9))));
    }

    @BeforeEach
    void setUp() {
        tokens = new GatheringTokens(new TokenRepository() {
            @Override
            public Map<CharacterRef, Long> loadAll() {
                return Map.of();
            }

            @Override
            public void save(CharacterRef character, long balance) {
            }
        });
        BusinessTypeRegistry types = new BusinessTypeRegistry(List.of(new BusinessType("gathering", "Gathering", Set.of("mining"), false)));
        BusinessTiers tiers = new BusinessTiers(IntStream.rangeClosed(1, 5).mapToObj(tier -> new TierSettings(tier, tier)).toList());
        businesses = new BusinessService(types, tiers, new InMemoryBusinessRepository(), Clock.systemUTC());
        NodeTables tables = new NodeTables(new RandomTableRegistry(List.of(mine())));
        nodes = new NodeService(tables, SETTINGS, businesses, new InMemoryNodeRepository());
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
        SkillLevels levels = new SkillLevels() {
            @Override
            public List<String> levelIds() {
                return LEVELS;
            }

            @Override
            public Optional<String> levelOf(CharacterRef character, String skillId) {
                return skillId.equals("mining") ? Optional.of(miningLevels.getOrDefault(character, "unskilled")) : Optional.empty();
            }
        };
        harvester = new RandomHarvester(new RandomHarvester.Dependencies(tables, SETTINGS, businesses, tokens,
                downtimeWallet, wallet, levels, heard::add), new LootRoller(new Random(1)));
    }

    private Node businessNode(int tier) {
        Business miners = businesses.createServerOwned("miners", "gathering", BUSINESS_BLOCK);
        businesses.hire(miners, miner);
        Node node = nodes.create("quarry", NodeKind.RANDOM, "mine", BLOCK);
        nodes.setOwner(node, "miners");
        nodes.setTier(node, tier);
        return node;
    }

    private String refusal(Runnable action) {
        return assertThrows(RefusedException.class, action::run).messageKey();
    }

    @Test
    void workersPayHarvestTokensAndGetTheTiersOutput() {
        Node node = businessNode(3);
        tokens.add(miner, 5);
        miningLevels.put(miner, "novice");

        Harvest harvest = harvester.harvest(node, miner, 2);

        assertEquals(List.of(new ItemAmount("coal", 5)), harvest.items());
        assertEquals(HarvestCost.tokens(3), harvest.paid());
        assertEquals(2, tokens.balance(miner));
        assertEquals(List.of(harvest), heard, "the harvest event fires");
    }

    @Test
    void aNodeOffersTheTablesTiersUpToItsOwnTier() {
        Node node = businessNode(3);

        assertEquals(List.of(1, 2), harvester.offered(node).stream().map(RandomTier::tier).toList());
        assertEquals("node.harvest.no-tier", refusal(() -> harvester.harvest(node, miner, 4)), "above the node's tier");
        assertEquals("node.harvest.no-tier", refusal(() -> harvester.harvest(node, miner, 3)), "not in the table");
    }

    @Test
    void onlyMembersGatherOnBusinessNodesAndNobodyOnUnownedOnes() {
        Node node = businessNode(1);
        tokens.add(stranger, 5);

        assertEquals("node.harvest.not-member", refusal(() -> harvester.harvest(node, stranger, 1)));

        businesses.delete(businesses.require("miners"));
        assertEquals("node.harvest.unowned", refusal(() -> harvester.harvest(node, miner, 1)));
        assertEquals(5, tokens.balance(stranger), "nothing was charged");
    }

    @Test
    void aCharacterBelowTheTiersLevelIsRefused() {
        Node node = businessNode(3);
        tokens.add(miner, 5);

        assertEquals("node.harvest.low-level", refusal(() -> harvester.harvest(node, miner, 2)));
        assertEquals(5, tokens.balance(miner));
        assertTrue(heard.isEmpty());
    }

    @Test
    void notEnoughTokensChargesNothing() {
        Node node = businessNode(1);
        tokens.add(miner, 1);
        miningLevels.put(miner, "expert");
        harvester.harvest(node, miner, 1);

        assertEquals("node.harvest.no-tokens", refusal(() -> harvester.harvest(node, miner, 1)));
        assertEquals(0, tokens.balance(miner));
    }

    @Test
    void aFreeTierNeedsNoDowntimePool() {
        Node node = businessNode(4);
        miningLevels.put(miner, "journeyman");

        Harvest harvest = harvester.harvest(node, miner, 4);

        assertEquals(List.of(new ItemAmount("coal", 9)), harvest.items());
    }

    @Test
    void publicNodesChargeEveryPartOfThePublicCostTogether() {
        Node node = nodes.create("pit", NodeKind.RANDOM, "mine", BLOCK);
        downtime.put(stranger.player(), 2);
        wallet.balances.put(stranger.player(), new BigDecimal("7"));

        Harvest harvest = harvester.harvest(node, stranger, 1);

        assertEquals(new HarvestCost(1, 0, new BigDecimal("5")), harvest.paid());
        assertEquals(1, downtime.get(stranger.player()));
        assertEquals(0, new BigDecimal("2").compareTo(wallet.balance(stranger.player())));
    }

    @Test
    void aPublicHarvestThatCantBePaidInFullChargesNothing() {
        Node node = nodes.create("pit", NodeKind.RANDOM, "mine", BLOCK);
        wallet.balances.put(stranger.player(), new BigDecimal("7"));

        assertEquals("node.harvest.no-downtime", refusal(() -> harvester.harvest(node, stranger, 1)));
        assertEquals(0, new BigDecimal("7").compareTo(wallet.balance(stranger.player())), "money was not taken");

        downtime.put(stranger.player(), 2);
        wallet.balances.put(stranger.player(), new BigDecimal("4"));
        assertEquals("node.harvest.no-money", refusal(() -> harvester.harvest(node, stranger, 1)));
        assertEquals(2, downtime.get(stranger.player()), "downtime was not taken");
    }

    @Test
    void aTierWithoutAPublicCostIsBusinessOnly() {
        Node node = nodes.create("pit", NodeKind.RANDOM, "mine", BLOCK);
        nodes.setTier(node, 2);
        miningLevels.put(stranger, "expert");

        assertEquals("node.harvest.business-only", refusal(() -> harvester.harvest(node, stranger, 2)));
    }

    @Test
    void farmNodesAndNodesWhoseTableDidntLoadHaveNoTable() {
        Node farm = nodes.create("field", NodeKind.FARM, "crop", BUSINESS_BLOCK);

        assertEquals("node.harvest.unavailable", refusal(() -> harvester.harvest(farm, miner, 1)));
        assertEquals("node.harvest.unavailable", refusal(() -> harvester.table(Node.create("lost", NodeKind.RANDOM, "gone", null))));
    }
}
