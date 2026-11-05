package org.skysongdev.skysongEconomy.bootstrap;

import org.skysongdev.skysongEconomy.business.BusinessService;
import org.skysongdev.skysongEconomy.business.access.AccessService;
import org.skysongdev.skysongEconomy.business.activity.ActivityLock;
import org.skysongdev.skysongEconomy.business.shift.ShiftService;
import org.skysongdev.skysongEconomy.business.training.TrainingService;
import org.skysongdev.skysongEconomy.business.type.BusinessTypeRegistry;
import org.skysongdev.skysongEconomy.business.vault.BusinessVaults;
import org.skysongdev.skysongEconomy.gui.business.BusinessMenus;
import org.skysongdev.skysongEconomy.tokens.GatheringTokens;
import org.skysongdev.skysongEconomy.trade.EconomyLogisticsSettings;
import org.skysongdev.skysongEconomy.trade.LedgerRegistry;

/**
 * The running parts of the business system, for features and other plugins that build on it.
 *
 * @param businesses every business
 * @param types      the business types
 * @param access     who may train, order and sell at each business
 * @param vaults     the owners' earnings
 * @param tokens     every character's Gathering Tokens
 * @param activities which players are busy with a shift or training
 * @param shifts     Work a Shift and Retrieve Pay
 * @param training   Train/Learn
 * @param economyLogistics the Economy and Logistics rules: who has it, slots, delivery times
 * @param ledgers    the trade ledgers businesses can select
 * @param menus      the business menus, to add main menu buttons to
 */
public record BusinessSystem(BusinessService businesses, BusinessTypeRegistry types, AccessService access,
                             BusinessVaults vaults, GatheringTokens tokens, ActivityLock activities,
                             ShiftService shifts, TrainingService training,
                             EconomyLogisticsSettings economyLogistics, LedgerRegistry ledgers, BusinessMenus menus) {
}
