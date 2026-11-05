package org.skysongdev.skysongEconomy.tokens;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongEconomy.business.BusinessException;
import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GatheringTokensTest {
    private final UUID player = UUID.randomUUID();
    private final CharacterRef alice = new CharacterRef(player, "Alice");
    private final CharacterRef bob = new CharacterRef(player, "Bob");
    private InMemoryTokenRepository repository;
    private GatheringTokens tokens;

    @BeforeEach
    void setUp() {
        repository = new InMemoryTokenRepository();
        tokens = new GatheringTokens(repository);
    }

    private static String refusal(Runnable action) {
        return assertThrows(BusinessException.class, action::run).messageKey();
    }

    @Test
    void charactersStartWithNoTokens() {
        assertEquals(0, tokens.balance(alice));
    }

    @Test
    void eachCharacterOfAPlayerHasItsOwnBalance() {
        tokens.add(alice, 5);

        assertEquals(5, tokens.balance(alice));
        assertEquals(0, tokens.balance(bob));
    }

    @Test
    void addRemoveAndSetReturnAndSaveTheNewBalance() {
        assertEquals(10, tokens.add(alice, 10));
        assertEquals(7, tokens.remove(alice, 3));
        assertEquals(7L, repository.saved.get(alice));
        assertEquals(0, tokens.set(alice, 0));
        assertEquals(0L, repository.saved.get(alice));
    }

    @Test
    void refusesRemovingMoreThanTheBalance() {
        tokens.add(alice, 2);

        assertEquals("tokens.not-enough", refusal(() -> tokens.remove(alice, 3)));
        assertEquals(2, tokens.balance(alice), "nothing is removed");
    }

    @Test
    void refusesAmountsBelowOneAndNegativeBalances() {
        assertEquals("tokens.invalid-amount", refusal(() -> tokens.add(alice, 0)));
        assertEquals("tokens.invalid-amount", refusal(() -> tokens.remove(alice, -1)));
        assertEquals("tokens.invalid-amount", refusal(() -> tokens.set(alice, -1)));
    }

    @Test
    void refusesBalancesTooLargeToStore() {
        tokens.set(alice, Long.MAX_VALUE);

        assertEquals("tokens.invalid-amount", refusal(() -> tokens.add(alice, 1)));
        assertEquals(Long.MAX_VALUE, tokens.balance(alice));
    }

    @Test
    void balancesSurviveALoad() {
        tokens.add(alice, 4);

        GatheringTokens reloaded = new GatheringTokens(repository);
        reloaded.loadAll();
        assertEquals(4, reloaded.balance(alice));
    }
}
