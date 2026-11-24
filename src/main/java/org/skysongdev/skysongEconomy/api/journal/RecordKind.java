package org.skysongdev.skysongEconomy.api.journal;

/**
 * What a journal record is about. New kinds may be added; code reading records should skip kinds it
 * doesn't know.
 */
public enum RecordKind {
    /** An import was ordered and paid: player to server. Items: the expected items, {@link RecordItem.Flow#IN}. */
    IMPORT_ORDERED,
    /** An import was handed over at collection (paid when ordered). Items: what was handed over, in. */
    IMPORT_DELIVERED,
    /** An import was seized at its pier: no refund. Items: what it carried, {@link RecordItem.Flow#LOST}. */
    IMPORT_SEIZED,
    /** A deleted business's import still on its way was refunded: server to the player who ordered it. */
    IMPORT_REFUNDED,
    /** A batch of an export was sold: server to the seller. Items: what was sold, {@link RecordItem.Flow#OUT}. */
    EXPORT_SOLD,
    /** A finished shift put its vault cut in the business's vault: server to vault. */
    SHIFT_VAULT_CUT,
    /** A worker collected their shift pay: server to player. */
    SHIFT_PAY,
    /** Someone paid for a training session: player to vault. */
    TRAINING_PAID,
    /** A vault was collected: vault to its owner (or the staff member collecting a server business's vault). */
    VAULT_COLLECTED,
    /** A deleted business's vault was emptied: vault to its owner, or vault to server for a server business. */
    VAULT_CLOSED,
    /** An owner took a ledger out of its slot and paid the removal fine: player to server. */
    LEDGER_FINE,
    /** Someone harvested at a node: player to server for any money it cost. Items: what they got, in. */
    HARVEST,
    /** A primer was planted in a farm node. Items: the primer, out. */
    FARM_PLANTED,
    /** A primer came back out of a farm node: taken out, or given back to its planter. Items: the primer, in. */
    FARM_UNPLANTED,
    /** An NPC's upkeep cycle was eaten from its stock. Items: what was eaten, out. */
    UPKEEP_PAID,
    /**
     * A Claim was bought: buyer to each seller (a resale), or buyer to server (a server sale), plus buyer
     * to server for every modifier's share.
     */
    CLAIM_BOUGHT,
    /**
     * A seller's share of a Claim sale that waited for them was paid. No money: it was already counted as
     * buyer to seller in {@link #CLAIM_BOUGHT}.
     */
    CLAIM_PAYOUT
}
