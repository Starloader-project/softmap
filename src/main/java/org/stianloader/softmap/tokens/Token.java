package org.stianloader.softmap.tokens;

import org.jetbrains.annotations.Contract;

public  abstract class Token {
    private final int col;
    private final int end;
    private final int row;
    private final int start;

    public Token(int start, int end, int row, int col) {
        this.start = start;
        this.end = end;
        this.row = row;
        this.col = col;
    }

    @Contract(pure = true)
    public String describeLocation() {
        return "Character " + this.start + " to " + this.end + ", beginning at row " + this.row + ", column " + this.col;
    }

    @Contract(pure = true)
    public final int getColumn() {
        return this.col;
    }

    @Contract(pure = true)
    public final int getEnd() {
        return this.end;
    }

    @Contract(pure = true)
    public final int getRow() {
        return this.row;
    }

    @Contract(pure = true)
    public final int getStart() {
        return this.start;
    }
}
