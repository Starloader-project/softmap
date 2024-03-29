package org.stianloader.softmap.insns;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.stianloader.softmap.tokens.Token;

public final class MatchResult {

    public static final int MATCH_GREDY_MATCH = 3;
    public static final int MATCH_MATCH_BREAK = 2;
    public static final int MATCH_MATCH_CONTINUE = 1;
    public static final int MATCH_NO_MATCH = 0;

    @NotNull
    public static final MatchResult RESULT_BREAK = new MatchResult(MatchResult.MATCH_MATCH_BREAK);
    @NotNull
    public static final MatchResult RESULT_CONTINUE = new MatchResult(MatchResult.MATCH_MATCH_CONTINUE);
    @NotNull
    public static final MatchResult RESULT_GREEDY = new MatchResult(MatchResult.MATCH_GREDY_MATCH);

    @Nullable
    private final String errorDescription;
    @Nullable
    private final Token errorLocation;
    private final int resultCode;

    private MatchResult(int result) {
        this.resultCode = result;
        this.errorDescription = null;
        this.errorLocation = null;
    }

    public MatchResult(@NotNull String errorDescription, @NotNull Token errorLocation) {
        this.resultCode = MatchResult.MATCH_NO_MATCH;
        this.errorDescription = errorDescription;
        this.errorLocation = errorLocation;
    }

    @Nullable
    @Contract(pure = true)
    public final String getErrorDescription() {
        return this.errorDescription;
    }

    @Nullable
    @Contract(pure = true)
    public final Token getErrorLocation() {
        return this.errorLocation;
    }

    @Contract(pure = true)
    public final boolean isAnyMatch() {
        return this.resultCode != MATCH_NO_MATCH;
    }

    @Contract(pure = true)
    public final boolean isBreakingMatching() {
        return this.resultCode == MATCH_MATCH_BREAK;
    }

    @Contract(pure = true)
    public final boolean isContinuingMatching() {
        return this.resultCode == MATCH_MATCH_CONTINUE;
    }

    @Contract(pure = true)
    public final boolean isGreedyMatch() {
        return this.resultCode == MATCH_GREDY_MATCH;
    }

    @Override
    public String toString() {
        Token t = this.errorLocation;
        return this.resultCode + "; " + this.errorDescription + " @ " + ((t == null) ? "null" : t.describeLocation());
    }
}
