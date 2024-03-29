package org.stianloader.softmap.tokens;

import org.jetbrains.annotations.Contract;

public class BlockToken extends Token {
    private final boolean startOfBlock;

    public BlockToken(int start, int end, int row, int col, boolean startOfBlock) {
        super(start, end, row, col);
        this.startOfBlock = startOfBlock;
    }

    @Contract(pure = true)
    public final boolean isStartOfBlock() {
        return this.startOfBlock;
    }

    @Contract(pure = true)
    public final boolean isEndOfBlock() {
        return !this.startOfBlock;
    }
}
