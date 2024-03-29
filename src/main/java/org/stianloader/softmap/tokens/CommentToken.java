package org.stianloader.softmap.tokens;

import org.jetbrains.annotations.Contract;

public  class CommentToken extends Token {
    private final int commentTextStart;
    private final int commentTextEnd;
    private final boolean cStyle;

    public CommentToken(int row, int col, int commentTextStart, int commentTextEnd, boolean cStyle) {
        super(commentTextStart - 2, cStyle ? commentTextEnd + 2 : commentTextEnd, row, col);
        this.commentTextStart = commentTextStart;
        this.commentTextEnd = commentTextEnd;
        this.cStyle = cStyle;
    }

    @Contract(pure = true)
    public final int getCommentTextStart() {
        return this.commentTextStart;
    }

    @Contract(pure = true)
    public final int getCommentTextEnd() {
        return this.commentTextEnd;
    }

    @Contract(pure = true)
    public final boolean isCStyleComment() {
        return this.cStyle;
    }
}
