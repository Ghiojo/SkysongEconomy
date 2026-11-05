package org.skysongdev.skysongEconomy.commands.good;

import org.skysongdev.skysongEconomy.data.database.GoodTypeRepository;
import org.skysongdev.skysongEconomy.items.MaterialIdResolver;
import org.skysongdev.skysongEconomy.messaging.Messenger;

import java.sql.SQLException;
import java.util.logging.Logger;

/** /se good add [materialId] &lt;goodType&gt;: tags an item with a good type. */
final class AddGoodAssociation extends GoodAssociationCommand {
    AddGoodAssociation(GoodTypeRepository goodTypes, MaterialIdResolver materialIds, Messenger messenger, Logger logger) {
        super(goodTypes, materialIds, messenger, logger);
    }

    @Override
    public String name() {
        return "add";
    }

    @Override
    protected boolean apply(String materialId, String goodType) throws SQLException {
        return goodTypes.add(materialId, goodType);
    }

    @Override
    protected String successMessage() {
        return "Associated <yellow><material></yellow> with good type <yellow><good_type></yellow>.";
    }

    @Override
    protected String unchangedMessage() {
        return "<yellow><material></yellow> already has good type <yellow><good_type></yellow>.";
    }
}
