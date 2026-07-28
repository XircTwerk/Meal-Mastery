package com.xirc.mealmastery.challenge;

/**
 * Thrown while reading a datapack-supplied challenge, collection or milestone.
 *
 * <p>Always caught by the loader: one malformed file is reported and skipped so
 * the rest of the pack still works.</p>
 */
public final class ChallengeParseException extends RuntimeException {
    public ChallengeParseException(String message) {
        super(message);
    }
}
