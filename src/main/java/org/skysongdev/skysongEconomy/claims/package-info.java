/**
 * Selling SkysongClaims Claims: prices, listings, quotes and sales.
 * <p>
 * SkysongClaims is optional. Its Claims are groups of WorldGuard regions owned by SkysongStats
 * characters; SkysongClaims keeps who owns them and whether they are for sale, and leaves the
 * price, the money and the fourth line of each Claim sign to SkysongEconomy.
 * <p>
 * The rules in this package have no server or SkysongClaims classes. They reach Claims through
 * {@link org.skysongdev.skysongEconomy.claims.ClaimGateway}, which
 * {@code integration/skysongclaims/SkysongClaimsAdapter} implements over the SkysongClaims API, so
 * they are unit tested with a fake.
 */
package org.skysongdev.skysongEconomy.claims;
