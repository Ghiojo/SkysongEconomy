package org.skysongdev.skysongEconomy.claims;

import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Which Claims SkysongEconomy sells for their owners rather than for the server, kept in memory and
 * saved. A Claim on sale without a listing is a server sale. {@link ClaimSales} keeps it in step
 * with what is on sale. Main thread only.
 */
public final class ClaimListings {
    private final ClaimListingRepository repository;
    private final Clock clock;
    private final Map<String, ClaimListing> byClaim = new HashMap<>();

    public ClaimListings(ClaimListingRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** Loads every saved listing. Call once at start-up. */
    public void loadAll() {
        byClaim.clear();
        repository.loadAll().forEach(listing -> byClaim.put(listing.claimId(), listing));
    }

    /** @return the owner listing of the Claim, empty when it isn't on sale by an owner */
    public Optional<ClaimListing> find(String claimId) {
        return Optional.ofNullable(byClaim.get(claimId));
    }

    /** @return every owner listing */
    public List<ClaimListing> all() {
        return List.copyOf(byClaim.values());
    }

    /** Records that the owner put the Claim on sale now. */
    ClaimListing put(String claimId, CharacterRef listedBy) {
        ClaimListing listing = new ClaimListing(claimId, listedBy, clock.instant());
        byClaim.put(claimId, listing);
        repository.save(listing);
        return listing;
    }

    /** @return whether there was a listing to remove */
    boolean remove(String claimId) {
        if (byClaim.remove(claimId) == null) {
            return false;
        }
        repository.delete(claimId);
        return true;
    }
}
