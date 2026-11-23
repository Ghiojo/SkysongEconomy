package org.skysongdev.skysongEconomy.integration.skysongclaims;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.skysongdev.skysongClaims.api.ClaimInfo;
import org.skysongdev.skysongClaims.api.ClaimOwner;
import org.skysongdev.skysongClaims.api.MenuButton;
import org.skysongdev.skysongClaims.api.OwnershipCheck;
import org.skysongdev.skysongClaims.api.SkysongClaimsApi;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.claims.ClaimGateway;
import org.skysongdev.skysongEconomy.claims.ClaimView;
import org.skysongdev.skysongEconomy.claims.OwnershipAnswer;
import org.skysongdev.skysongEconomy.integration.ApiVersion;
import org.skysongdev.skysongstats.api.CharacterId;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * The only class that calls the SkysongClaims API. It turns SkysongClaims' Claims and answers into
 * the plugin's own {@link ClaimView} and {@link OwnershipAnswer}, so nothing else depends on
 * SkysongClaims. Main thread only, like the API it calls.
 */
public final class SkysongClaimsAdapter implements ClaimGateway {
    /** The oldest SkysongClaims API this plugin can use. */
    public static final ApiVersion REQUIRED_API = new ApiVersion(1, 0, 0);

    private final SkysongClaimsApi api;
    private final Plugin plugin;
    private final MiniMessage miniMessage;

    private SkysongClaimsAdapter(SkysongClaimsApi api, Plugin plugin, MiniMessage miniMessage) {
        this.api = api;
        this.plugin = plugin;
        this.miniMessage = miniMessage;
    }

    /**
     * Connects to the SkysongClaims API that the running SkysongClaims registered.
     *
     * @param plugin SkysongEconomy, which SkysongClaims names as the seller of what it puts on sale
     * @throws IllegalStateException when SkysongClaims is not enabled, or its API is older than {@link #REQUIRED_API}
     */
    public static SkysongClaimsAdapter connect(Plugin plugin) {
        SkysongClaimsApi api = SkysongClaimsApi.get();
        if (!ApiVersion.parse(api.apiVersion()).isAtLeast(REQUIRED_API)) {
            throw new IllegalStateException("SkysongClaims has API " + api.apiVersion() + ", but API " + REQUIRED_API
                    + " or newer is needed: update SkysongClaims");
        }
        return new SkysongClaimsAdapter(api, Objects.requireNonNull(plugin, "plugin"), MiniMessage.miniMessage());
    }

    /** @return the API version the installed SkysongClaims implements */
    public String apiVersion() {
        return api.apiVersion();
    }

    @Override
    public Optional<ClaimView> find(String claimId) {
        return api.claims().find(claimId.toLowerCase(Locale.ROOT)).map(SkysongClaimsAdapter::view);
    }

    @Override
    public List<ClaimView> all() {
        return api.claims().all().stream().map(SkysongClaimsAdapter::view).toList();
    }

    /** @return the Claim at that spot, if any */
    public Optional<ClaimView> at(Location location) {
        return api.claims().at(location).map(SkysongClaimsAdapter::view);
    }

    /** Adds a button to every Claim sign's menu, or replaces SkysongEconomy's button with that id. */
    public void addMenuButton(String id, MenuButton button) {
        api.signs().addMenuButton(plugin, id, button);
    }

    @Override
    public boolean typeExists(String typeId) {
        return api.types().find(typeId).isPresent();
    }

    @Override
    public OwnershipAnswer canOwn(CharacterRef character, String claimId) {
        return answer(api.ownership().canOwn(id(character), claimId));
    }

    @Override
    public OwnershipAnswer addOwner(String claimId, CharacterRef character) {
        return answer(api.ownership().addOwner(plugin, claimId, id(character)));
    }

    @Override
    public void clearOwners(String claimId) {
        api.ownership().clearOwners(plugin, claimId);
    }

    @Override
    public void setForSale(String claimId, boolean forSale) {
        api.market().setForSale(plugin, claimId, forSale);
    }

    @Override
    public void setPriceLine(String claimId, String miniMessageLine) {
        api.signs().setPriceLine(plugin, claimId, miniMessage.deserialize(miniMessageLine));
    }

    @Override
    public void clearPriceLine(String claimId) {
        api.signs().clearPriceLine(plugin, claimId);
    }

    /** @return the Claim as the plugin's rules see it */
    static ClaimView view(ClaimInfo claim) {
        List<CharacterRef> owners = claim.owners().stream().map(ClaimOwner::character).map(SkysongClaimsAdapter::ref)
                .toList();
        return new ClaimView(claim.id(), claim.displayName(), claim.typeId(), claim.free(), owners, claim.forSaleBy());
    }

    /** @return SkysongClaims' answer as the plugin's own; one SkysongClaims added later reads as unknown */
    static OwnershipAnswer answer(OwnershipCheck check) {
        return switch (check) {
            case OK -> OwnershipAnswer.OK;
            case TYPE_LIMIT -> OwnershipAnswer.TYPE_LIMIT;
            case FULL -> OwnershipAnswer.FULL;
            case ALREADY_OWNER -> OwnershipAnswer.ALREADY_OWNER;
            case UNKNOWN_CHARACTER -> OwnershipAnswer.UNKNOWN_CHARACTER;
        };
    }

    private static CharacterRef ref(CharacterId id) {
        return new CharacterRef(id.player(), id.profile());
    }

    private static CharacterId id(CharacterRef character) {
        return new CharacterId(character.player(), character.profile());
    }
}
