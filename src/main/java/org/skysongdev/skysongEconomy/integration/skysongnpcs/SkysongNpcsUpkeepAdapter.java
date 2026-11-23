package org.skysongdev.skysongEconomy.integration.skysongnpcs;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.skysongdev.skysongEconomy.integration.ApiVersion;
import org.skysongdev.skysongEconomy.upkeep.UpkeepNpcs;
import org.skysongdev.skysongnpcs.api.SkysongNpcsApi;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** {@link UpkeepNpcs} over the SkysongNPCs API. Must be used on the server's main thread. */
public final class SkysongNpcsUpkeepAdapter implements UpkeepNpcs {
    private final SkysongNpcsApi npcs;

    public SkysongNpcsUpkeepAdapter(SkysongNpcsApi npcs) {
        this.npcs = Objects.requireNonNull(npcs, "npcs");
    }

    /**
     * Connects to the running SkysongNPCs.
     *
     * @throws IllegalStateException when its API is older than {@link SkysongNpcsAdapter#REQUIRED_API}
     */
    public static SkysongNpcsUpkeepAdapter connect() {
        SkysongNpcsApi api = SkysongNpcsApi.get();
        if (!ApiVersion.parse(api.apiVersion()).isAtLeast(SkysongNpcsAdapter.REQUIRED_API)) {
            throw new IllegalStateException("SkysongNPCs has API " + api.apiVersion() + ", but API "
                    + SkysongNpcsAdapter.REQUIRED_API + " or newer is needed: update SkysongNPCs");
        }
        return new SkysongNpcsUpkeepAdapter(api);
    }

    @Override
    public List<Known> all() {
        return npcs.npcs().all().stream()
                .map(npc -> new Known(npc.id(), npc.template().map(id -> id.toLowerCase(Locale.ROOT))))
                .toList();
    }

    @Override
    public boolean exists(UUID npc) {
        return npcs.npcs().isManaged(npc);
    }

    @Override
    public Optional<String> name(UUID npc) {
        // SkysongNPCs keeps names as MiniMessage text.
        return exists(npc) ? Optional.of(MiniMessage.miniMessage().stripTags(npcs.appearance().name(npc))) : Optional.empty();
    }
}
