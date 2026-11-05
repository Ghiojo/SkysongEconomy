package org.skysongdev.skysongEconomy.business.vault;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.business.Business;
import org.skysongdev.skysongEconomy.business.BusinessException;
import org.skysongdev.skysongEconomy.character.CharacterRef;
import org.skysongdev.skysongEconomy.money.MapWallet;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BusinessVaultsTest {
    private final CharacterRef owner = new CharacterRef(UUID.randomUUID(), "Owner");
    private final Business business = Business.create("smithy", "crafting", owner, null);
    private final Map<String, BigDecimal> saved = new HashMap<>();
    private final List<String> warnings = new ArrayList<>();
    private MapWallet wallet;
    private BusinessVaults vaults;

    @BeforeEach
    void setUp() {
        wallet = new MapWallet();
        vaults = new BusinessVaults(repository(), wallet, warnings::add);
    }

    private VaultRepository repository() {
        return new VaultRepository() {
            @Override
            public Map<String, BigDecimal> loadAll() {
                return new HashMap<>(saved);
            }

            @Override
            public void save(String businessId, BigDecimal balance) {
                saved.put(businessId, balance);
            }
        };
    }

    private static BigDecimal money(String amount) {
        return new BigDecimal(amount);
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, money(expected).compareTo(actual), "expected " + expected + " but was " + actual);
    }

    @Test
    void aNewVaultIsEmpty() {
        assertMoney("0", vaults.balance(business));
    }

    @Test
    void depositsAddUpAndAreSaved() {
        vaults.deposit(business, money("2.5"));
        vaults.deposit(business, money("10"));

        assertMoney("12.5", vaults.balance(business));
        assertMoney("12.5", saved.get("smithy"));
    }

    @Test
    void refusesNegativeDepositsAndIgnoresZero() {
        assertThrows(IllegalArgumentException.class, () -> vaults.deposit(business, money("-1")));
        vaults.deposit(business, BigDecimal.ZERO);
        assertTrue(saved.isEmpty(), "nothing to save");
    }

    @Test
    void collectingPaysTheOwnersPlayerAndEmptiesTheVault() {
        vaults.deposit(business, money("7"));

        assertMoney("7", vaults.collect(business));
        assertMoney("7", wallet.balance(owner.player()));
        assertMoney("0", vaults.balance(business));
        assertMoney("0", saved.get("smithy"));
    }

    @Test
    void anEmptyVaultCantBeCollected() {
        assertEquals("business.vault.empty",
                assertThrows(BusinessException.class, () -> vaults.collect(business)).messageKey());
    }

    @Test
    void aFailedPaymentKeepsTheMoneyInTheVault() {
        vaults.deposit(business, money("7"));
        wallet.refuseDeposits = true;

        assertEquals("business.vault.collect-failed",
                assertThrows(BusinessException.class, () -> vaults.collect(business)).messageKey());
        assertMoney("7", vaults.balance(business));
    }

    @Test
    void staffCollectTheVaultOfAServerOwnedBusinessIntoTheirOwnBalance() {
        Business market = Business.create("market", "crafting", null, null);
        UUID staff = UUID.randomUUID();
        vaults.deposit(market, money("5"));

        assertEquals("business.vault.server-owned",
                assertThrows(BusinessException.class, () -> vaults.collect(market)).messageKey());
        assertMoney("5", vaults.collect(market, staff));
        assertMoney("5", wallet.balance(staff));
        assertMoney("0", vaults.balance(market));
    }

    @Test
    void collectingAnOwnedBusinessAlwaysPaysTheOwner() {
        vaults.deposit(business, money("3"));

        vaults.collect(business, UUID.randomUUID());

        assertMoney("3", wallet.balance(owner.player()));
    }

    @Test
    void deletingAServerOwnedBusinessDropsItsVault() {
        Business market = Business.create("market", "crafting", null, null);
        vaults.deposit(market, money("5"));

        vaults.deleted(market);

        assertMoney("0", vaults.balance(market));
        assertTrue(wallet.balances.isEmpty());
        assertTrue(warnings.isEmpty());
    }

    @Test
    void balancesSurviveALoad() {
        vaults.deposit(business, money("3"));

        BusinessVaults reloaded = new BusinessVaults(repository(), wallet, warnings::add);
        reloaded.loadAll();
        assertMoney("3", reloaded.balance(business));
    }

    @Test
    void deletingABusinessPaysItsVaultToTheOwner() {
        vaults.deposit(business, money("4"));

        vaults.deleted(business);

        assertMoney("4", wallet.balance(owner.player()));
        assertMoney("0", vaults.balance(business));
        assertTrue(warnings.isEmpty());
    }

    @Test
    void aVaultThatCantBePaidOnDeletionIsReported() {
        vaults.deposit(business, money("4"));
        wallet.refuseDeposits = true;

        vaults.deleted(business);

        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("smithy"), warnings.get(0));
    }
}
