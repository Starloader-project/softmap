package org.stianloader.softmap;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.stianloader.softmap.tokens.Token;

public class SoftmapApplicationError {
    @NotNull
    private final String description;
    @NotNull
    private final Token errorLocation;

    public SoftmapApplicationError(@NotNull Token errorLocation, @NotNull String description) {
        this.description = description;
        this.errorLocation = errorLocation;
    }

    @NotNull
    @Contract(pure = true)
    public String getDescription() {
        return this.description;
    }

    @NotNull
    @Contract(pure = true)
    public Token getErrorLocation() {
        return this.errorLocation;
    }
}
