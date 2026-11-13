package org.skysongdev.skysongEconomy.gui.business;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.module.BusinessModules;
import org.skysongdev.skysongEconomy.business.module.Module;
import org.skysongdev.skysongEconomy.business.module.ModulePickGroup;
import org.skysongdev.skysongEconomy.character.SkillCatalog;
import org.skysongdev.skysongEconomy.messaging.Messenger;

import java.util.ArrayList;
import java.util.List;

/**
 * What the module menus share: the services they read and how a module is drawn.
 *
 * @param modules the modules each business picked
 * @param skills  skill names
 */
record ModuleScreens(BusinessMenus menus, BusinessModules modules, SkillCatalog skills, Messenger messenger) {
    /** @return the {@code skill} placeholder: the skill's name, with its formatting */
    TagResolver skill(String skillId) {
        return Placeholder.parsed("skill", skills.displayName(skillId));
    }

    /** @return the module's name, rendered */
    Component name(Module module) {
        return messenger.renderText(module.name());
    }

    /** @return the module's description, then the given lines */
    List<Component> lore(Module module, List<Component> after) {
        List<Component> lore = new ArrayList<>(module.description().stream().map(messenger::renderText).toList());
        lore.addAll(after);
        return lore;
    }

    /** @return the item named in a module file, or paper when it isn't an item */
    static Material icon(Module module) {
        Material material = Material.matchMaterial(module.icon());
        return material != null && material.isItem() ? material : Material.PAPER;
    }

    /** @return how many modules the business has */
    int picked(Business business) {
        return modules.of(business).size();
    }

    /** @return how many module picks the business has, counting modules kept over a skill's total */
    int total(Business business) {
        return modules.groups(business).stream().mapToInt(group -> Math.max(group.total(), group.picks().size())).sum();
    }

    /** @return how many module picks are still free */
    int free(Business business) {
        return modules.groups(business).stream().mapToInt(ModulePickGroup::free).sum();
    }

    void openList(Player player, Business business) {
        new ModulesMenu(this, menus.context(player, business)).open(player);
    }
}
