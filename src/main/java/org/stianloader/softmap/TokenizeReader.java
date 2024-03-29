package org.stianloader.softmap;

import java.util.List;

import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.stianloader.softmap.tokens.BlockToken;
import org.stianloader.softmap.tokens.CommentToken;
import org.stianloader.softmap.tokens.StringToken;
import org.stianloader.softmap.tokens.Token;

class TokenizeReader {

    public static final int TABULATOR_SIZE = 4;

    @NotNull
    private final String source;
    private int line = 1;
    private int position = 0;
    private int column = 1;
    private final int startPosition;
    private final int endPosition;
    private final int startRow;
    private final int startColumn;

    public TokenizeReader(@NotNull String source, int from, int to, int row, int column) {
        this.source = source;
        this.position = from;
        this.line = row;
        this.column = column;
        this.startPosition = from;
        this.endPosition = to;
        this.startColumn = column;
        this.startRow = row;
    }

    public void reset() {
        this.line = this.startRow;
        this.position = this.startPosition;
        this.column = this.startColumn;
    }

    public int readCodepoint() {
        int codepoint = this.source.codePointAt(this.position++);
        if (codepoint == '\n') {
            this.line++;
            this.column = 1;
            return this.readCodepoint();
        }

        this.column++;

        return codepoint != '\r' ? codepoint : this.readCodepoint();
    }

    @Nullable
    @CheckReturnValue
    public BlockToken consumeBlockToken() {
        int codepoint = this.peekCodepoint();
        boolean startofBlock;
        if (codepoint == '{') {
            startofBlock = true;
        } else if (codepoint == '}') {
            startofBlock = false;
        } else {
            return null;
        }

        return new BlockToken(this.position++, this.position, this.line, this.column++, startofBlock);
    }

    @Nullable
    @CheckReturnValue
    public Token consumeToken() {
        if (this.isExhausted()) {
            return null;
        }

        int codepoint = this.peekCodepoint();
        if (codepoint == '{' || codepoint == '}') {
            return new BlockToken(this.position++, this.position, this.line, this.column++, codepoint == '{');
        } else {
            return this.consumeString();
        }
    }

    @Nullable
    @CheckReturnValue
    public StringToken consumeString() {
        int start = this.position;
        while (this.position < this.endPosition) {
            int codepoint = this.peekCodepoint();
            // TODO '{' and '}' in Strings or codepoint literals
            if (JavaInterop.isBlank(codepoint)
                    || codepoint == '\r'
                    || codepoint == '\n'
                    || codepoint == '{'
                    || codepoint == '}') {
                break;
            }
            // TODO comments without whitespace in between?
            this.position++;
        }

        if (start == this.position) {
            return null;
        }

        int startCol = this.column;
        this.column += (this.position - start);

        return new StringToken(this.source, start, this.position, this.line, startCol);
    }

    public int peekCodepoint() {
        if (this.isExhausted()) {
            throw new IllegalStateException("Reader is exhausted at " + this.getVerboseCurrentLocation());
        }

        return this.source.codePointAt(this.position);
    }

    public void consumeWhitespace(List<? super CommentToken> stream) {
        while (this.position < this.endPosition) {
            int codepoint = this.source.codePointAt(this.position);
            if (codepoint == '\n') {
                this.position++;
                this.line++;
                this.column = 1;
                continue;
            } else if (JavaInterop.isBlank(codepoint) || codepoint == '\r') {
                this.position++;
                this.column += codepoint == '\t' ? TABULATOR_SIZE : 1;
                continue;
            }

            if (codepoint == '/') {
                // Potential start of comment
                int next = this.source.codePointAt(this.position + 1);
                if (next == '*') {
                    // C-style comment
                    int commentLine = this.line;
                    int commentCol = this.column;
                    int commentPos = this.position;
                    this.position += 2;
                    while (this.readCodepoint() != '*' && this.readCodepoint() != '/');
                    stream.add(new CommentToken(commentLine, commentCol, commentPos + 2, this.position - 2, true));
                } else if (next == '/') {
                    // Single-line comment
                    int commentPos = this.position;
                    this.position += 2;
                    // Exhaust the entire line
                    while (++this.position < this.endPosition && this.source.codePointAt(this.position) != '\n');
                    stream.add(new CommentToken(this.line, this.column, commentPos + 2, this.position, false));
                    this.line++;
                    this.column = 1;
                } else {
                    // not actually a comment (this could for example be a plain division sign)
                    break;
                }
            } else {
                break;
            }
        }
    }

    public boolean isExhausted() {
        return this.position >= this.endPosition;
    }

    @NotNull
    public String getVerboseCurrentLocation() {
        return "Character " + this.position + " of " + this.endPosition + " (0-indexed), line " + this.line + " column " + this.column + " (1-indexed)  [starting at character " + this.startPosition + ", line " + this.startRow + ", column " + this.startColumn + "]";
    }
}
