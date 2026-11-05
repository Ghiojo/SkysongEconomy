package org.skysongdev.skysongEconomy.trade;

import java.util.List;

/** An import or an export of a ledger: what their buttons share. */
public sealed interface TradeOffer permits ImportOffer, ExportOffer {
    /** @return its key in the ledger file */
    String id();

    /** @return MiniMessage name of its button */
    String displayName();

    /** @return Minecraft material of its button */
    String icon();

    /** @return MiniMessage lines under its name */
    List<String> lore();
}
