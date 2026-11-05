package org.skysongdev.skysongEconomy.commands.good;

import org.skysongdev.skysongEconomy.data.database.Database;
import org.skysongdev.skysongEconomy.items.MaterialIdResolver;
import org.skysongdev.skysongEconomy.messaging.Messenger;

import java.sql.SQLException;
import java.util.logging.Logger;

/** /se good remove [materialId] &lt;goodType&gt;: removes a good type tag from an item. */
final class RemoveGoodAssociation extends GoodAssociationCommand {
    RemoveGoodAssociation(Database database, MaterialIdResolver materialIds, Messenger messenger, Logger logger) {
        super(database, materialIds, messenger, logger);
    }

    @Override
    public String name() {
        return "remove";
    }

    @Override
    protected void apply(String materialId, String goodType) throws SQLException {
        database.deleteGoodTypeAssociation(goodType, materialId);
    }

    @Override
    protected String successMessage() {
        return "Removed good type <yellow><good_type></yellow> from <yellow><material></yellow>.";
    }
}
