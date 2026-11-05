package org.skysongdev.skysongEconomy.gui;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongEconomy.gui.layout.BorderedLayout;
import org.skysongdev.skysongEconomy.gui.layout.Page;

import java.util.List;

/**
 * A bordered menu that lists items across pages, with previous and next arrows on the bottom
 * row and a back button between them. Subclasses say what the items are and how each looks.
 */
public abstract class PagedMenu<T> extends BorderedMenu {
    private int page;

    protected PagedMenu(MenuTheme theme, Component title) {
        super(theme, title);
    }

    /** @return everything to list, in order; asked again on every redraw */
    protected abstract List<T> items();

    /** @return the button for one item */
    protected abstract MenuButton button(T item);

    /** @return the header button, or {@code null} to keep the border there */
    protected @Nullable MenuButton header() {
        return null;
    }

    /** Fills the right column, e.g. with delivery-time choices. Empty by default. */
    protected void drawSide() {
    }

    /** @return what the back button does, or {@code null} for a close button instead */
    protected abstract @Nullable MenuButton back();

    @Override
    protected final void drawContent() {
        Page<T> shown = Page.of(items(), BorderedLayout.content().size(), page);
        page = shown.number();
        List<Integer> slots = BorderedLayout.content();
        for (int i = 0; i < shown.items().size(); i++) {
            set(slots.get(i), button(shown.items().get(i)));
        }
        MenuButton header = header();
        if (header != null) {
            set(BorderedLayout.HEADER, header);
        }
        if (shown.hasPrevious()) {
            set(BorderedLayout.PREVIOUS_PAGE, theme.previousPage(click -> turnTo(page - 1)));
        }
        if (shown.hasNext()) {
            set(BorderedLayout.NEXT_PAGE, theme.nextPage(click -> turnTo(page + 1)));
        }
        MenuButton back = back();
        set(BorderedLayout.BACK, back != null ? back : theme.close());
        drawSide();
    }

    private void turnTo(int newPage) {
        page = newPage;
        redraw();
    }
}
