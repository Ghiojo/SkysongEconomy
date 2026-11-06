package org.skysongdev.skysongEconomy.nodes;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.business.BlockPosition;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessException;
import org.skysongdev.skysongEconomy.business.BusinessService;
import org.skysongdev.skysongEconomy.business.InMemoryBusinessRepository;
import org.skysongdev.skysongEconomy.business.display.NameplateSettings;
import org.skysongdev.skysongEconomy.business.tier.BusinessTiers;
import org.skysongdev.skysongEconomy.business.tier.TierSettings;
import org.skysongdev.skysongEconomy.business.type.BusinessType;
import org.skysongdev.skysongEconomy.business.type.BusinessTypeRegistry;
import org.skysongdev.skysongEconomy.messaging.RefusedException;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeServiceTest {
    private static final BusinessTypeRegistry TYPES = new BusinessTypeRegistry(List.of(
            new BusinessType("gathering", "Gathering", Set.of("mining"), false),
            new BusinessType("crafting", "Crafting", Set.of("cooking"), false)));
    private static final BusinessTiers TIERS = new BusinessTiers(
            IntStream.rangeClosed(1, 5).mapToObj(tier -> new TierSettings(tier, tier)).toList());
    private static final NodeSettings SETTINGS = new NodeSettings(Set.of("gathering"),
            new NameplateSettings(true, 0.4, 16, true, false));
    /** The random table "mine" and the farm type "crop" exist. */
    private static final NodeSources SOURCES = (kind, id) -> Optional.ofNullable(switch (kind) {
        case RANDOM -> id.equals("mine") ? "Mine" : null;
        case FARM -> id.equals("crop") ? "Crop farm" : null;
    });
    private static final BlockPosition BLOCK = new BlockPosition("world", 1, 64, 1);
    private static final BlockPosition OTHER_BLOCK = new BlockPosition("world", 2, 64, 2);
    private static final BlockPosition THIRD_BLOCK = new BlockPosition("world", 3, 64, 3);

    private InMemoryNodeRepository repository;
    private BusinessService businesses;
    private NodeService nodes;

    @BeforeEach
    void setUp() {
        repository = new InMemoryNodeRepository();
        businesses = new BusinessService(TYPES, TIERS, new InMemoryBusinessRepository(), Clock.systemUTC());
        nodes = new NodeService(SOURCES, SETTINGS, businesses, repository);
        businesses.addListener(nodes);
        businesses.addBlockGuard(new NodeBlockGuard(nodes));
    }

    private static String refusal(Runnable action) {
        return assertThrows(RefusedException.class, action::run).messageKey();
    }

    @Test
    void createsATierOnePublicRandomNode() {
        Node node = nodes.create("Quarry", NodeKind.RANDOM, "MINE", BLOCK);

        assertEquals("quarry", node.id(), "ids are lower case");
        assertEquals("mine", node.source());
        assertEquals(1, node.tier());
        assertEquals(NodeOwner.PUBLIC, node.owner());
        assertEquals(node, nodes.at(BLOCK).orElseThrow());
        assertEquals(node, nodes.find("QUARRY").orElseThrow());
        assertEquals(node.snapshot(), repository.saved.get("quarry"));
    }

    @Test
    void aFarmNodeStartsUnownedBecauseItCantBePublic() {
        Node node = nodes.create("field", NodeKind.FARM, "crop", BLOCK);

        assertEquals(NodeOwner.UNOWNED, node.owner());
        assertEquals("node.farm-not-public", refusal(() -> nodes.makePublic(node)));
    }

    @Test
    void refusesBadIdsTakenIdsUnknownSourcesAndUsedBlocks() {
        nodes.create("quarry", NodeKind.RANDOM, "mine", BLOCK);

        assertEquals("node.invalid-id", refusal(() -> nodes.create("bad id", NodeKind.RANDOM, "mine", OTHER_BLOCK)));
        assertEquals("node.id-taken", refusal(() -> nodes.create("quarry", NodeKind.RANDOM, "mine", OTHER_BLOCK)));
        assertEquals("node.unknown-source.random", refusal(() -> nodes.create("pit", NodeKind.RANDOM, "crop", OTHER_BLOCK)));
        assertEquals("node.unknown-source.farm", refusal(() -> nodes.create("pit", NodeKind.FARM, "mine", OTHER_BLOCK)));
        assertEquals("node.block-taken", refusal(() -> nodes.create("pit", NodeKind.RANDOM, "mine", BLOCK)));
    }

    @Test
    void nodesAndBusinessesCantShareABlock() {
        businesses.createServerOwned("smithy", "crafting", BLOCK);
        Node node = nodes.create("quarry", NodeKind.RANDOM, "mine", OTHER_BLOCK);

        assertEquals("node.block-taken", refusal(() -> nodes.create("pit", NodeKind.RANDOM, "mine", BLOCK)));
        assertEquals("node.block-taken", refusal(() -> nodes.move(node, BLOCK)));
        assertEquals("business.block-taken", assertThrows(BusinessException.class,
                () -> businesses.createServerOwned("bakery", "crafting", OTHER_BLOCK)).messageKey());
        assertEquals("business.block-taken", assertThrows(BusinessException.class,
                () -> businesses.move(businesses.require("smithy"), OTHER_BLOCK)).messageKey());
    }

    @Test
    void movingAndUnplacingFreeTheOldBlock() {
        Node node = nodes.create("quarry", NodeKind.RANDOM, "mine", BLOCK);

        nodes.move(node, OTHER_BLOCK);
        assertTrue(nodes.at(BLOCK).isEmpty());
        assertEquals(node, nodes.at(OTHER_BLOCK).orElseThrow());

        nodes.unplace(node);
        assertTrue(nodes.at(OTHER_BLOCK).isEmpty());
        assertTrue(node.position().isEmpty());
        assertTrue(repository.saved.get("quarry").position() == null, "the node is kept without a block");
    }

    @Test
    void tiersStayFromOneToFive() {
        Node node = nodes.create("quarry", NodeKind.RANDOM, "mine", BLOCK);

        nodes.setTier(node, 5);
        assertEquals(5, repository.saved.get("quarry").tier());
        assertEquals("node.invalid-tier", refusal(() -> nodes.setTier(node, 0)));
        assertEquals("node.invalid-tier", refusal(() -> nodes.setTier(node, 6)));
        assertEquals(5, node.tier());
    }

    @Test
    void onlyAllowedBusinessTypesOwnNodes() {
        businesses.createServerOwned("miners", "gathering", BLOCK);
        businesses.createServerOwned("smithy", "crafting", OTHER_BLOCK);
        Node node = nodes.create("quarry", NodeKind.RANDOM, "mine", THIRD_BLOCK);

        nodes.setOwner(node, "MINERS");
        assertEquals(new NodeOwner.Business("miners"), node.owner());
        assertEquals(List.of(node), nodes.ownedBy("miners"));
        assertEquals("node.owner-wrong-type", refusal(() -> nodes.setOwner(node, "smithy")));
        assertEquals("node.unknown-business", refusal(() -> nodes.setOwner(node, "nobody")));

        nodes.makePublic(node);
        assertEquals(NodeOwner.PUBLIC, repository.saved.get("quarry").owner());
    }

    @Test
    void aDeletedBusinessLeavesItsNodesUnowned() {
        Business miners = businesses.createServerOwned("miners", "gathering", BLOCK);
        Node node = nodes.create("quarry", NodeKind.RANDOM, "mine", OTHER_BLOCK);
        nodes.setOwner(node, "miners");

        businesses.delete(miners);

        assertEquals(NodeOwner.UNOWNED, node.owner());
        assertEquals(NodeOwner.UNOWNED, repository.saved.get("quarry").owner());
    }

    @Test
    void theNameIsTheOwnNameElseTheSourceNameElseTheId() {
        Node node = nodes.create("quarry", NodeKind.RANDOM, "mine", BLOCK);
        assertEquals("Mine", nodes.name(node));

        nodes.rename(node, "  Old Quarry ");
        assertEquals("Old Quarry", nodes.name(node));
        assertEquals("node.invalid-name", refusal(() -> nodes.rename(node, "x".repeat(49))));
        assertEquals("node.invalid-name", refusal(() -> nodes.rename(node, "   ")));

        nodes.rename(node, null);
        assertEquals("Mine", nodes.name(node));

        Node unknown = Node.create("lost", NodeKind.RANDOM, "gone", null);
        assertEquals("lost", nodes.name(unknown));
    }

    @Test
    void changingTheSourceKeepsTheKindsApart() {
        Node node = nodes.create("quarry", NodeKind.RANDOM, "mine", BLOCK);

        assertEquals("node.unknown-source.random", refusal(() -> nodes.setSource(node, "crop")));
        assertEquals("mine", node.source());
    }

    @Test
    void deletingForgetsTheNodeAndItsBlock() {
        Node node = nodes.create("quarry", NodeKind.RANDOM, "mine", BLOCK);

        nodes.delete(node);

        assertTrue(nodes.find("quarry").isEmpty());
        assertTrue(nodes.at(BLOCK).isEmpty());
        assertTrue(repository.saved.isEmpty());
        assertEquals("node.not-found", refusal(() -> nodes.require("quarry")));
    }

    @Test
    void loadingDropsTheBlockOfANodeOnAnotherNodeOrABusiness() {
        businesses.createServerOwned("smithy", "crafting", BLOCK);
        repository.save(new NodeSnapshot("first", NodeKind.RANDOM, "mine", 2, NodeOwner.PUBLIC, null, OTHER_BLOCK));
        repository.save(new NodeSnapshot("second", NodeKind.RANDOM, "mine", 1, NodeOwner.PUBLIC, null, OTHER_BLOCK));
        repository.save(new NodeSnapshot("third", NodeKind.FARM, "crop", 1, NodeOwner.UNOWNED, "Field", BLOCK));

        assertEquals(3, nodes.loadAll());

        assertEquals("first", nodes.at(OTHER_BLOCK).orElseThrow().id());
        assertTrue(nodes.require("second").position().isEmpty());
        assertTrue(nodes.require("third").position().isEmpty());
        assertTrue(repository.saved.get("third").position() == null, "the fix is saved");
        assertEquals(2, nodes.require("first").tier());
    }

    @Test
    void listenersHearEveryChange() {
        List<String> heard = new ArrayList<>();
        nodes.addListener(new NodeChangeListener() {
            @Override
            public void changed(Node node, @Nullable BlockPosition previousPosition) {
                heard.add("changed " + node.id() + " from " + previousPosition);
            }

            @Override
            public void deleted(Node node) {
                heard.add("deleted " + node.id());
            }

            @Override
            public void loaded(Iterable<Node> loaded) {
                heard.add("loaded");
            }
        });

        Node node = nodes.create("quarry", NodeKind.RANDOM, "mine", BLOCK);
        nodes.move(node, OTHER_BLOCK);
        nodes.setTier(node, 3);
        nodes.delete(node);
        nodes.loadAll();

        assertEquals(List.of(
                "changed quarry from null",
                "changed quarry from " + BLOCK,
                "changed quarry from " + OTHER_BLOCK,
                "deleted quarry",
                "loaded"), heard);
    }

    @Test
    void theBlockGuardNamesTheNodeAndUnplacesIt() {
        Node node = nodes.create("quarry", NodeKind.RANDOM, "mine", BLOCK);
        var guarded = new NodeBlockGuard(nodes).at(BLOCK).orElseThrow();

        assertEquals("skysongeconomy.nodes", guarded.breakPermission());
        assertEquals(Map.of("node", "Mine", "id", "quarry"), guarded.placeholders());
        guarded.unplace().run();
        assertTrue(node.position().isEmpty());
        assertTrue(new NodeBlockGuard(nodes).at(BLOCK).isEmpty());
    }
}
