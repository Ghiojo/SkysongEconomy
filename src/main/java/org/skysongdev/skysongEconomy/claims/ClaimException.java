package org.skysongdev.skysongEconomy.claims;

import org.skysongdev.skysongEconomy.messaging.RefusedException;

import java.util.Map;

/** Something asked of a Claim was refused, for example selling a Free Claim. It names a {@code claims.*} message. */
public final class ClaimException extends RefusedException {
    private static final long serialVersionUID = 1L;

    public ClaimException(String messageKey, Map<String, String> placeholders) {
        super(messageKey, placeholders);
    }

    /** @return a refusal whose message only names the Claim, as {@code <claim>} */
    static ClaimException about(String messageKey, String claimId) {
        return new ClaimException(messageKey, Map.of("claim", claimId));
    }
}
