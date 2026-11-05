package org.skysongdev.skysongEconomy.nodes;

import java.util.Objects;

/** Who a node belongs to, which decides who may gather from it and what they pay. */
public sealed interface NodeOwner {
    /** Everyone may gather, paying the table's public cost. */
    record Public() implements NodeOwner {
    }

    /** Nobody may gather until staff give it an owner; what a node becomes when its business is deleted. */
    record Unowned() implements NodeOwner {
    }

    /** The business's owner and workers may gather, paying Harvest Tokens. */
    record Business(String businessId) implements NodeOwner {
        public Business {
            Objects.requireNonNull(businessId, "businessId");
        }
    }

    NodeOwner PUBLIC = new Public();
    NodeOwner UNOWNED = new Unowned();
}
