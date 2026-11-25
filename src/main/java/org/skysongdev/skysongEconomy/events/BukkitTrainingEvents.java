package org.skysongdev.skysongEconomy.events;

import org.bukkit.plugin.PluginManager;
import org.skysongdev.skysongEconomy.api.business.TrainingFinishEvent;
import org.skysongdev.skysongEconomy.api.business.TrainingStartEvent;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessDirectory;
import org.skysongdev.skysongEconomy.business.training.TrainingEvents;
import org.skysongdev.skysongEconomy.business.training.TrainingResult;
import org.skysongdev.skysongEconomy.business.training.TrainingSession;

import java.util.Optional;

/** Fires {@link TrainingStartEvent} and {@link TrainingFinishEvent}. */
public final class BukkitTrainingEvents implements TrainingEvents {
    private final PluginManager plugins;

    public BukkitTrainingEvents(PluginManager plugins) {
        this.plugins = plugins;
    }

    @Override
    public void started(Business business, TrainingSession session) {
        plugins.callEvent(new TrainingStartEvent(BusinessDirectory.info(business), PublicTypes.training(session)));
    }

    @Override
    public void finished(Optional<Business> business, TrainingResult result) {
        plugins.callEvent(new TrainingFinishEvent(business.map(BusinessDirectory::info).orElse(null),
                PublicTypes.training(result.session()), PublicTypes.skills(result.skills())));
    }
}
