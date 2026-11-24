package org.skysongdev.skysongEconomy.claims;

import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.money.MapWallet;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.skysongdev.skysongEconomy.claims.ClaimFixture.ANNA;
import static org.skysongdev.skysongEconomy.claims.ClaimFixture.BRAM;
import static org.skysongdev.skysongEconomy.claims.ClaimFixture.NOW;
import static org.skysongdev.skysongEconomy.claims.ClaimFixture.money;

class ClaimPayoutsTest {
    private final MapWallet wallet = new MapWallet();
    private final InMemoryClaimPayoutRepository storage = new InMemoryClaimPayoutRepository();
    private final ClaimPayouts payouts = new ClaimPayouts(storage, wallet);
    private final PendingPayout rose = new PendingPayout(1, ANNA.player(), "rose_house", money("500"), NOW);
    private final PendingPayout oak = new PendingPayout(2, ANNA.player(), "oak_house", money("300"), NOW);

    @Test
    void paysEverythingOwedAndForgetsIt() {
        payouts.owe(rose);
        payouts.owe(oak);
        payouts.owe(new PendingPayout(2, BRAM.player(), "oak_house", money("300"), NOW));

        assertEquals(List.of(rose, oak), payouts.payOut(ANNA.player()));

        assertEquals(money("800"), wallet.balance(ANNA.player()));
        assertTrue(payouts.owed(ANNA.player()).isEmpty());
        assertEquals(1, storage.saved.size(), "Bram's is still owed");
    }

    @Test
    void whatTheWalletRefusesKeepsWaiting() {
        payouts.owe(rose);
        wallet.refuseDeposits = true;

        assertTrue(payouts.payOut(ANNA.player()).isEmpty());

        assertEquals(List.of(rose), payouts.owed(ANNA.player()));
        assertEquals(List.of(rose), storage.saved);
    }

    @Test
    void eachShareActuallyPaidIsReported() {
        List<PendingPayout> heard = new ArrayList<>();
        ClaimPayouts told = new ClaimPayouts(storage, wallet, heard::add);
        told.owe(rose);
        told.owe(oak);
        wallet.refuseDeposits = true;
        told.payOut(ANNA.player());
        assertTrue(heard.isEmpty(), "nothing is reported while the wallet refuses");

        wallet.refuseDeposits = false;
        told.payOut(ANNA.player());
        assertEquals(List.of(rose, oak), heard);
    }

    @Test
    void loadsWhatWasSaved() {
        storage.saved.add(rose);

        payouts.loadAll();

        assertEquals(List.of(rose), payouts.owed(ANNA.player()));
    }
}
