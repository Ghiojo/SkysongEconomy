package org.skysongdev.skysongEconomy.gui;

import net.kyori.adventure.text.Component;
import org.skysongdev.skysongEconomy.gui.layout.BorderedLayout;

/**
 * A 6-row menu with a border (see {@link BorderedLayout}). Subclasses fill the header, the
 * content and the side slots; the border is drawn first so anything they set covers it.
 */
public abstract class BorderedMenu extends Menu {
    protected final MenuTheme theme;

    protected BorderedMenu(MenuTheme theme, Component title) {
        super(BorderedLayout.SIZE, title);
        this.theme = theme;
    }

    @Override
    protected final void draw() {
        MenuButton border = theme.border();
        for (int slot = 0; slot < BorderedLayout.SIZE; slot++) {
            if (BorderedLayout.isBorder(slot)) {
                set(slot, border);
            }
        }
        drawContent();
    }

    /** Puts this menu's own buttons in place, over the border. */
    protected abstract void drawContent();
}
