/**
 * What a picked module does besides being described: its {@code effects} list. Each effect type
 * is one record of {@link org.skysongdev.skysongEconomy.business.module.effect.ModuleEffect} and one parser
 * registered in {@link org.skysongdev.skysongEconomy.business.module.effect.ModuleEffectParsers}, so a new
 * effect is a new record, a new parser and one registry line. Nothing here touches server classes.
 */
package org.skysongdev.skysongEconomy.business.module.effect;
