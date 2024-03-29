package org.stianloader.softmap.tokens;

import java.util.NoSuchElementException;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class StringToken extends Token {

    @NotNull
    private final String stringSource;

    public StringToken(@NotNull String source, int start, int end, int row, int col) {
        super(start, end, row, col);
        this.stringSource = source;
    }

    @Contract(pure = true)
    public final boolean contentMatches(boolean ignoreCase, @NotNull String other) {
        return this.contentMatches(ignoreCase, other, 0, this.getEnd() - this.getStart());
    }

    @Contract(pure = true)
    public final boolean contentMatches(boolean ignoreCase, @NotNull String other, int ooffset) {
        return this.contentMatches(ignoreCase, other, ooffset, this.getEnd() - this.getStart());
    }

    @Contract(pure = true)
    public final boolean contentMatches(boolean ignoreCase, @NotNull String other, int ooffset, int length) {
        return this.stringSource.regionMatches(ignoreCase, this.getStart(), other, ooffset, length);
    }

    @Contract(pure = true)
    public final boolean contentMatches(boolean ignoreCase, int toffset, @NotNull String other, int ooffset, int length) {
        if (toffset < 0) {
            throw new IndexOutOfBoundsException("toffset < 0: " + toffset);
        }
        toffset += this.getStart();
        if (toffset + length > this.getEnd()) {
            throw new IndexOutOfBoundsException("toffset + this.getStart() + length > this.getEnd(): " + (toffset + length) + " > " + this.getEnd());
        }
        return this.stringSource.regionMatches(ignoreCase, toffset, other, ooffset, length);
    }

    @Contract(pure = true)
    public final boolean contentMatches(@NotNull String other) {
        return other.length() == this.getContentLength() && this.contentMatches(false, other, 0, this.getContentLength());
    }

    @Contract(pure = true)
    public final int indexOf(int codepoint, int fromIndex) {
        int index = this.stringSource.indexOf(codepoint, this.getStart() + fromIndex);
        return (index != -1 && index < this.getEnd()) ? index - this.getStart() : -1;
    }

    @Contract(pure = true)
    public final int indexOf(int codepoint) {
        return this.indexOf(codepoint, 0);
    }

    @NotNull
    @Contract(pure = true, value = "-> new")
    public final String getText() {
        return this.stringSource.substring(this.getStart(), this.getEnd());
    }

    @NotNull
    @Contract(pure = true, value = "param1, param2 -> new")
    public final StringToken subtoken(int from, int to) {
        if (to < from) {
            throw new IndexOutOfBoundsException("to < from: " + to + ", " + from);
        } else if (from < 0) {
            throw new IndexOutOfBoundsException("from < 0: " + from);
        } else if (to < 0) {
            throw new IndexOutOfBoundsException("to < 0: " + to);
        } else if (to + this.getStart() > this.getEnd()) {
            throw new IndexOutOfBoundsException("to + this.getStart() > this.getEnd(): " + to + " + " + this.getStart() + ", " + this.getEnd());
        }
        return new StringToken(this.stringSource, this.getStart() + from, this.getStart() + to, this.getRow(), this.getColumn() + from);
    }

    @Contract(pure = true)
    public final int getContentLength() {
        return this.getEnd() - this.getStart();
    }

    @Contract(pure = true)
    public final int codepointBefore(int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("index < 0: " + index);
        }
        index += this.getStart();
        if (index > this.getEnd()) {
            throw new IndexOutOfBoundsException("index + this.getStart() > this.getEnd(): " + index + ", " + this.getText());
        }

        return this.stringSource.codePointBefore(index);
    }

    @Contract(pure = true)
    public final int codepointAt(int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("index < 0: " + index);
        }
        index += this.getStart();
        if (index >= this.getEnd()) {
            throw new IndexOutOfBoundsException("index + this.getStart() >= this.getEnd(): " + index + ", " + this.getText());
        }

        return this.stringSource.codePointAt(index);
    }

    @Contract(pure = true)
    public final int lastCodepoint() {
        if (this.getStart() == this.getEnd()) {
            throw new NoSuchElementException("Empty token");
        }
        return this.stringSource.codePointBefore(this.getEnd());
    }

    @NotNull
    public final String subtext(int from, int to) {
        if (from < 0) {
            throw new IndexOutOfBoundsException("from < 0");
        } else if (from > to) {
            throw new IndexOutOfBoundsException("from > to: " + from + ", " + to);
        }
        to += this.getStart();
        if (to > this.getEnd()) {
            throw new IndexOutOfBoundsException("to + this.getEnd() > this.getEnd(): " + to + ", " + this.getEnd());
        }

        return this.stringSource.substring(from + this.getStart(), to);
    }

    @Override
    public String toString() {
        return "StringToken@" + this.describeLocation() + ": " + this.getText();
    }
}
