/**
 * What other plugins can compile against to follow Claim sales: {@link
 * org.skysongdev.skysongEconomy.api.claims.ClaimPurchaseEvent}, fired whenever a Claim is bought,
 * and the records it carries.
 * <p>
 * Add SkysongEconomy as {@code compileOnly 'com.github.Ghiojo:SkysongEconomy:<tag>'} and as a
 * {@code softdepend} (or {@code depend}) in plugin.yml. For example, a treasury keeping a district
 * tax:
 * <pre>{@code
 * @EventHandler
 * public void onClaimPurchase(ClaimPurchaseEvent event) {
 *     for (AppliedModifier modifier : event.getSale().modifiers()) {
 *         if (modifier.key().equals("district_tax")) {
 *             treasury.store(modifier.share());
 *         }
 *     }
 * }
 * }</pre>
 * These types only change in ways that keep such code compiling: new record components or
 * methods may be added, existing ones are not removed or renamed.
 */
package org.skysongdev.skysongEconomy.api.claims;
