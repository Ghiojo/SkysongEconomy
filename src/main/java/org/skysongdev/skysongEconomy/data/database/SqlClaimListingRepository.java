package org.skysongdev.skysongEconomy.data.database;

import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.claims.ClaimListing;
import org.skysongdev.skysongEconomy.claims.ClaimListingRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Keeps owners' Claim listings in the {@code claim_listings} table. */
public final class SqlClaimListingRepository implements ClaimListingRepository {
    private final ConnectionProvider connections;

    public SqlClaimListingRepository(ConnectionProvider connections) {
        this.connections = connections;
    }

    @Override
    public List<ClaimListing> loadAll() {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "SELECT claim_id, listed_by_uuid, listed_by_profile, listed_at FROM claim_listings");
             ResultSet rows = statement.executeQuery()) {
            List<ClaimListing> listings = new ArrayList<>();
            while (rows.next()) {
                listings.add(new ClaimListing(rows.getString("claim_id"),
                        new CharacterRef(UUID.fromString(rows.getString("listed_by_uuid")), rows.getString("listed_by_profile")),
                        Instant.ofEpochMilli(rows.getLong("listed_at"))));
            }
            return listings;
        } catch (SQLException e) {
            throw new StorageException("Failed to load Claim listings", e);
        }
    }

    @Override
    public void save(ClaimListing listing) {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "REPLACE INTO claim_listings (claim_id, listed_by_uuid, listed_by_profile, listed_at) VALUES (?, ?, ?, ?)")) {
            statement.setString(1, listing.claimId());
            statement.setString(2, listing.listedBy().player().toString());
            statement.setString(3, listing.listedBy().profile());
            statement.setLong(4, listing.listedAt().toEpochMilli());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Failed to save the listing of the Claim " + listing.claimId(), e);
        }
    }

    @Override
    public void delete(String claimId) {
        try (PreparedStatement statement = connections.getConnection().prepareStatement(
                "DELETE FROM claim_listings WHERE claim_id = ?")) {
            statement.setString(1, claimId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Failed to delete the listing of the Claim " + claimId, e);
        }
    }
}
