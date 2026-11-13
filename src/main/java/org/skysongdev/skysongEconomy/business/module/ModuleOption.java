package org.skysongdev.skysongEconomy.business.module;

import org.jetbrains.annotations.Nullable;

/**
 * One module of a skill's pool, as a business sees it.
 *
 * @param module  the module
 * @param blocked why the business can't pick it now, {@code null} when it can
 */
public record ModuleOption(Module module, @Nullable Blocked blocked) {
    /** Why a business can't pick a module. */
    public enum Blocked {
        /** The business already picked it as often as {@code max-picks} allows. */
        MAX_PICKS("business.modules.blocked.max-picks"),
        /** The business's tier is below {@code requires.tier}. */
        TIER("business.modules.blocked.tier"),
        /** The business has fewer picks of the skill than {@code requires.level}. */
        LEVEL("business.modules.blocked.level"),
        /** The business lacks a module named in {@code requires.modules}. */
        MODULES("business.modules.blocked.modules");

        private final String messageKey;

        Blocked(String messageKey) {
            this.messageKey = messageKey;
        }

        /** @return the key in {@code messages.yml} that says so; placeholders {@code tier}, {@code level}, {@code modules}, {@code max} */
        public String messageKey() {
            return messageKey;
        }
    }

    /** @return whether the business can pick it now */
    public boolean available() {
        return blocked == null;
    }
}
