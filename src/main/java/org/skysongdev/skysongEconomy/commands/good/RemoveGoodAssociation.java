package org.skysongdev.skysongEconomy.commands.good;

import org.skysongdev.skysongEconomy.data.database.GoodTypeRepository;
import org.skysongdev.skysongEconomy.items.MaterialIdResolver;
import org.skysongdev.skysongEconomy.messaging.Messenger;

import java.sql.SQLException;
import java.util.logging.Logger;

/** /se good remove [materialId] &lt;goodType&gt;: removes a good type tag from an item. */
final class RemoveGoodAssociation extends GoodAssociationCommand {
    RemoveGoodAssociation(GoodTypeRepository goodTypes, MaterialIdResolver materialIds, Messenger messenger, Logger logger) {
        super(goodTypes, materialIds, messenger, logger);
    }

    @Override
    public String name() {
        return "remove";
    }

    @Override
    protected boolean apply(String materialId, String goodType) throws SQLException {
        return goodTypes.remove(materialId, goodType);
    }

    @Override
    protected String successMessage() {
        return "Removed good type <yellow><good_type></yellow> from <yellow><material></yellow>.";
    }

    @Override
    protected String unchangedMessage() {
        return "<yellow><material></yellow> does not have good type <yellow><good_type></yellow>.";
    }
}
