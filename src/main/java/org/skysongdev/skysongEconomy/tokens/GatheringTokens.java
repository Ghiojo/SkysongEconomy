package org.skysongdev.skysongEconomy.tokens;

import org.skysongdev.skysongEconomy.business.BusinessException;
import org.skysongdev.skysongEconomy.character.CharacterRef;

import java.util.HashMap;
import java.util.Map;

/**
 * Gathering Tokens: a whole-number balance per character, earned by working at Gathering
 * businesses and changed by staff with {@code /se tokens}.
 * <p>
 * Refused changes throw a {@link BusinessException} naming the {@code tokens.*} message to show.
 * Not thread-safe: use it on the main thread.
 */
public final class GatheringTokens {
    private final TokenRepository repository;
    private final Map<CharacterRef, Long> balances = new HashMap<>();

    public GatheringTokens(TokenRepository repository) {
        this.repository = repository;
    }

    /** Loads every saved balance. */
    public void loadAll() {
        balances.clear();
        balances.putAll(repository.loadAll());
    }

    public long balance(CharacterRef character) {
        return balances.getOrDefault(character, 0L);
    }

    /**
     * @return the new balance
     * @throws BusinessException {@code tokens.invalid-amount} unless the amount is above 0, or
     *                           when the balance would become too large to store
     */
    public long add(CharacterRef character, long amount) {
        requirePositive(amount);
        try {
            return set(character, Math.addExact(balance(character), amount));
        } catch (ArithmeticException e) {
            throw invalid(amount);
        }
    }

    /**
     * @return the new balance
     * @throws BusinessException {@code tokens.invalid-amount} unless the amount is above 0, or
     *                           {@code tokens.not-enough} when the character has fewer tokens
     */
    public long remove(CharacterRef character, long amount) {
        requirePositive(amount);
        long balance = balance(character);
        if (balance < amount) {
            throw new BusinessException("tokens.not-enough", Map.of(
                    "character", character.profile(), "balance", String.valueOf(balance), "amount", String.valueOf(amount)));
        }
        return set(character, balance - amount);
    }

    /**
     * @return the new balance
     * @throws BusinessException {@code tokens.invalid-amount} when the amount is negative
     */
    public long set(CharacterRef character, long balance) {
        if (balance < 0) {
            throw invalid(balance);
        }
        balances.put(character, balance);
        repository.save(character, balance);
        return balance;
    }

    private static void requirePositive(long amount) {
        if (amount <= 0) {
            throw invalid(amount);
        }
    }

    private static BusinessException invalid(long amount) {
        return new BusinessException("tokens.invalid-amount", Map.of("amount", String.valueOf(amount)));
    }
}
