package org.skysongdev.skysongEconomy.commands.good;

import org.skysongdev.skysongEconomy.data.database.Database;
import org.skysongdev.skysongEconomy.items.MaterialIdResolver;
import org.skysongdev.skysongEconomy.messaging.Messenger;

import java.sql.SQLException;
import java.util.logging.Logger;

/** /se good add [materialId] &lt;goodType&gt;: tags an item with a good type. */
final class AddGoodAssociation extends GoodAssociationCommand {
    AddGoodAssociation(Database database, MaterialIdResolver materialIds, Messenger messenger, Logger logger) {
        super(database, materialIds, messenger, logger);
    }

    @Override
    public String name() {
        return "add";
    }

    @Override
    protected void apply(String materialId, String goodType) throws SQLException {
        database.createGoodTypeAssociation(goodType, materialId);
    }

    @Override
    protected String successMessage() {
        return "Associated <yellow><material></yellow> with good type <yellow><good_type></yellow>.";
    }
}
