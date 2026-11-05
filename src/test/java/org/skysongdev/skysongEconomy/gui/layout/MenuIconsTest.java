package org.skysongdev.skysongEconomy.gui.layout;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.config.YamlFiles;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MenuIconsTest {
    @Test
    void missingIconsKeepTheirDefaultsAndNamesAreUpperCased() {
        MenuIcons icons = MenuIcons.parse(YamlFiles.parse("menus.yml", new StringReader("icons: {back: oak_door}")));

        assertEquals("OAK_DOOR", icons.back());
        assertEquals("GRAY_STAINED_GLASS_PANE", icons.border());
        assertEquals("ARROW", icons.nextPage());
        assertEquals("LIME_CONCRETE", icons.confirm());
    }
}
