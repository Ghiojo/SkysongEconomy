package org.skysongdev.skysongEconomy.business.npc;

import java.util.Objects;
import java.util.Optional;

/**
 * The name SkysongNPCs knows one business access group by: {@code economy:<business id>/<group>},
 * such as {@code economy:smithy/workers}.
 * <p>
 * Business ids and group names never contain a slash, so the two parts cannot be confused.
 *
 * @param businessId the business's id
 * @param groupId    the group's name within that business, such as {@code workers} or {@code customers}
 */
public record BusinessGroupRef(String businessId, String groupId) {
    /** The group type Economy registers with SkysongNPCs: the part before the colon. */
    public static final String TYPE = "economy";

    private static final char SEPARATOR = '/';

    public BusinessGroupRef {
        Objects.requireNonNull(businessId, "businessId");
        Objects.requireNonNull(groupId, "groupId");
        if (businessId.isEmpty() || groupId.isEmpty()
                || businessId.indexOf(SEPARATOR) >= 0 || groupId.indexOf(SEPARATOR) >= 0) {
            throw new IllegalArgumentException("Not a business group: " + businessId + SEPARATOR + groupId);
        }
    }

    /**
     * Reads the part after {@code economy:}.
     *
     * @param id such as {@code smithy/workers}
     * @return the group it names, empty when it is not written that way
     */
    public static Optional<BusinessGroupRef> parseId(String id) {
        int slash = id.indexOf(SEPARATOR);
        if (slash <= 0 || slash == id.length() - 1 || id.indexOf(SEPARATOR, slash + 1) >= 0) {
            return Optional.empty();
        }
        return Optional.of(new BusinessGroupRef(id.substring(0, slash), id.substring(slash + 1)));
    }

    /** @return the part after {@code economy:}, such as {@code smithy/workers} */
    public String id() {
        return businessId + SEPARATOR + groupId;
    }

    /** @return the whole name, such as {@code economy:smithy/workers} */
    @Override
    public String toString() {
        return TYPE + ":" + id();
    }
}
