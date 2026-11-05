package org.skysongdev.skysongEconomy.gui.business;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.skysongdev.skysongEconomy.business.shift.ShiftPay;
import org.skysongdev.skysongEconomy.messaging.Messenger;
import org.skysongdev.skysongEconomy.money.Wallet;

/** Writes shift pay for menus, e.g. "6 Pouches and 1 Gathering Token", from messages.yml. */
final class ShiftPayText {
    private ShiftPayText() {
    }

    /** @return a {@code <pay>} placeholder describing the pay */
    static TagResolver placeholder(Messenger messenger, Wallet wallet, ShiftPay pay) {
        return Placeholder.component("pay", describe(messenger, wallet, pay));
    }

    static Component describe(Messenger messenger, Wallet wallet, ShiftPay pay) {
        TagResolver money = Placeholder.unparsed("money", wallet.format(pay.money()));
        if (pay.tokens() == 0) {
            return messenger.render("business.shift.pay-money", money);
        }
        return messenger.render("business.shift.pay-with-tokens", money,
                Placeholder.unparsed("tokens", String.valueOf(pay.tokens())));
    }
}
