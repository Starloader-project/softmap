package org.stianloader.softmap;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.stianloader.softmap.tokens.Token;

public class SoftmapParseError {
    public final int row;
    public final int column;
    public final int startCodepoint;
    public final int endCodepoint;
    @NotNull
    private final String description;

    public SoftmapParseError(int startCodepoint, int endCodepoint, int row, int column, @NotNull String description) {
        this.row = row;
        this.column = column;
        this.description = description;
        this.startCodepoint = startCodepoint;
        this.endCodepoint = endCodepoint;
    }

    public SoftmapParseError(@NotNull Token location, @NotNull String description) {
        this(location.getStart(), location.getEnd(), location.getRow(), location.getColumn(), description);
    }

    @NotNull
    @Contract(pure = true)
    public String getDescription() {
        return this.description;
    }
}
