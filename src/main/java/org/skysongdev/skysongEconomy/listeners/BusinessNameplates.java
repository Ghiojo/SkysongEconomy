package org.skysongdev.skysongEconomy.listeners;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.business.BlockPosition;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessChangeListener;
import org.skysongdev.skysongEconomy.messaging.Messenger;

/** Shows each business's name floating above its block, through {@link FloatingNames}. */
public final class BusinessNameplates implements BusinessChangeListener {
    private final FloatingNames names;
    private final Messenger messenger;

    /** @param names the floating names, set up with {@code businesses/block.yml} */
    public BusinessNameplates(FloatingNames names, Messenger messenger) {
        this.names = names;
        this.messenger = messenger;
    }

    @Override
    public void loaded(Iterable<Business> loaded) {
        names.close();
        loaded.forEach(this::show);
    }

    @Override
    public void changed(Business business, @Nullable BlockPosition previousPosition) {
        show(business);
    }

    @Override
    public void deleted(Business business) {
        names.hide(business.id());
    }

    private void show(Business business) {
        business.position().ifPresentOrElse(
                position -> names.show(business.id(), position,
                        messenger.render("business.nameplate", Placeholder.unparsed("business", business.displayName()))),
                () -> names.hide(business.id()));
    }
}
