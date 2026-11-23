package org.skysongdev.skysongEconomy.claims;

import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * SkysongClaims in memory, answering the way its API documents: owners are checked in the same
 * order as SkysongClaims' own {@code canOwn} (already an owner, unknown character, no free slot,
 * type limit), and the price line is kept per Claim. Characters are known unless
 * {@link #forget forgotten}; every type allows {@link #setLimit one} Claim per character by default.
 */
public final class FakeClaims implements ClaimGateway {
    /** The plugin name sales are made in, as SkysongClaims records it. */
    public static final String PLUGIN = ClaimSales.SELLER;

    private final Map<String, Stored> claims = new LinkedHashMap<>();
    private final Map<String, Integer> limits = new HashMap<>();
    private final Set<CharacterRef> unknown = new HashSet<>();
    private final Map<String, String> priceLines = new HashMap<>();
    private int priceLineWrites;
    /** When set, the next {@link #addOwner} gives this answer and adds no one, as if things changed after {@link #canOwn}. */
    public OwnershipAnswer refuseNextOwner;
    /** Told the Claim's id after {@link #clearOwners}, as SkysongClaims fires its owner-removed events. */
    public Consumer<String> onOwnersCleared = claimId -> { };

    private static final class Stored {
        final String id;
        String displayName;
        String typeId;
        Integer slots;
        final List<CharacterRef> owners = new ArrayList<>();
        String forSaleBy;

        Stored(String id, String typeId, Integer slots) {
            this.id = id;
            this.displayName = id;
            this.typeId = typeId;
            this.slots = slots;
        }

        ClaimView view() {
            return new ClaimView(id, displayName, typeId, slots != null, owners, Optional.ofNullable(forSaleBy));
        }
    }

    /** Adds a Claim that is not free, unowned and not for sale, and its type if it is new. */
    public FakeClaims add(String claimId, String typeId) {
        claims.put(claimId, new Stored(claimId, typeId, null));
        limits.putIfAbsent(typeId, 1);
        return this;
    }

    /** Adds a Free Claim with that many slots, and its type if it is new. */
    public FakeClaims addFree(String claimId, String typeId, int slots) {
        claims.put(claimId, new Stored(claimId, typeId, slots));
        limits.putIfAbsent(typeId, 1);
        return this;
    }

    /** Adds a type with no Claims yet. */
    public FakeClaims addType(String typeId, int limit) {
        limits.put(typeId, limit);
        return this;
    }

    /** Sets how many Claims of a type one character may own. */
    public FakeClaims setLimit(String typeId, int limit) {
        limits.put(typeId, limit);
        return this;
    }

    /** Makes SkysongStats forget a character, as when its profile is deleted. */
    public FakeClaims forget(CharacterRef character) {
        unknown.add(character);
        return this;
    }

    /** Changes a Claim's type, as {@code /sclaims settype} does. */
    public void setType(String claimId, String typeId) {
        require(claimId).typeId = typeId;
    }

    /** Puts a Claim on sale in another plugin's name. */
    public void setForSaleBy(String claimId, String plugin) {
        require(claimId).forSaleBy = plugin;
    }

    /** Removes an owner, as {@code /sclaim leave} does. */
    public void removeOwner(String claimId, CharacterRef character) {
        require(claimId).owners.remove(character);
    }

    /** Deletes a Claim, as {@code /sclaims delete} does. */
    public void delete(String claimId) {
        claims.remove(claimId);
        priceLines.remove(claimId);
    }

    /** @return the fourth line of the Claim's signs, empty when it is blank */
    /** @return how many times a sign line was set or cleared */
    public int priceLineWrites() {
        return priceLineWrites;
    }

    public Optional<String> priceLine(String claimId) {
        return Optional.ofNullable(priceLines.get(claimId));
    }

    @Override
    public Optional<ClaimView> find(String claimId) {
        return Optional.ofNullable(claims.get(claimId.toLowerCase(Locale.ROOT))).map(Stored::view);
    }

    @Override
    public List<ClaimView> all() {
        return claims.values().stream().map(Stored::view).toList();
    }

    @Override
    public boolean typeExists(String typeId) {
        return limits.containsKey(typeId);
    }

    @Override
    public OwnershipAnswer canOwn(CharacterRef character, String claimId) {
        Stored claim = require(claimId);
        if (claim.owners.contains(character)) {
            return OwnershipAnswer.ALREADY_OWNER;
        }
        if (unknown.contains(character)) {
            return OwnershipAnswer.UNKNOWN_CHARACTER;
        }
        if (claim.slots != null && claim.owners.size() >= claim.slots) {
            return OwnershipAnswer.FULL;
        }
        long owned = claims.values().stream()
                .filter(other -> other.typeId.equals(claim.typeId) && other.owners.contains(character)).count();
        return owned >= limits.getOrDefault(claim.typeId, 0) ? OwnershipAnswer.TYPE_LIMIT : OwnershipAnswer.OK;
    }

    @Override
    public OwnershipAnswer addOwner(String claimId, CharacterRef character) {
        if (refuseNextOwner != null) {
            OwnershipAnswer refused = refuseNextOwner;
            refuseNextOwner = null;
            return refused;
        }
        OwnershipAnswer answer = canOwn(character, claimId);
        if (answer == OwnershipAnswer.OK) {
            require(claimId).owners.add(character);
        }
        return answer;
    }

    @Override
    public void clearOwners(String claimId) {
        require(claimId).owners.clear();
        onOwnersCleared.accept(claimId);
    }

    @Override
    public void setForSale(String claimId, boolean forSale) {
        require(claimId).forSaleBy = forSale ? PLUGIN : null;
    }

    @Override
    public void setPriceLine(String claimId, String miniMessage) {
        require(claimId);
        priceLines.put(claimId, miniMessage);
        priceLineWrites++;
    }

    @Override
    public void clearPriceLine(String claimId) {
        require(claimId);
        priceLines.remove(claimId);
        priceLineWrites++;
    }

    private Stored require(String claimId) {
        Stored claim = claims.get(claimId);
        if (claim == null) {
            throw new IllegalArgumentException("No Claim " + claimId);
        }
        return claim;
    }
}
