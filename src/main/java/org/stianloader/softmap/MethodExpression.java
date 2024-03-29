package org.stianloader.softmap;

import java.util.List;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.stianloader.softmap.insns.InsnBlock;
import org.stianloader.softmap.tokens.BlockToken;
import org.stianloader.softmap.tokens.StringToken;
import org.stianloader.softmap.tokens.Token;

class MethodExpression {

    @NotNull
    private final StringToken declaringLocation;

    @Nullable
    private final BlockToken endOfBody;

    @NotNull
    @Unmodifiable
    private final List<@NotNull ? extends InsnBlock> insns;

    @Nullable
    private final StringToken methodDesc;

    @Nullable
    private final StringToken methodLocation;

    @Nullable
    private final StringToken methodName;

    @Nullable
    private final StringToken ownerName;

    @Nullable
    private final BlockToken startOfBody;

    @NotNull
    @Unmodifiable
    private final List<@NotNull Token> tokens;

    public MethodExpression(@NotNull StringToken declaringLocation, @Nullable StringToken methodLocation, @Nullable StringToken ownerName, @Nullable StringToken methodName, @Nullable StringToken methodDesc, @Nullable BlockToken startOfBody, @Nullable BlockToken endOfBody, @NotNull @Unmodifiable List<@NotNull Token> tokens, @NotNull @Unmodifiable List<@NotNull ? extends InsnBlock> insns) {
        this.declaringLocation = declaringLocation;
        this.methodLocation = methodLocation;
        this.ownerName = ownerName;
        this.methodName = methodName;
        this.methodDesc = methodDesc;
        this.startOfBody = startOfBody;
        this.endOfBody = endOfBody;
        this.tokens = tokens;
        this.insns = insns;
    }

    @NotNull
    @Contract(pure = true)
    public StringToken getDeclaringLocation() {
        return this.declaringLocation;
    }

    @Nullable
    @Contract(pure = true)
    public BlockToken getEndOfBody() {
        return this.endOfBody;
    }

    @NotNull
    @Contract(pure = true)
    public List<@NotNull ? extends InsnBlock> getInsns() {
        return this.insns;
    }

    @Nullable
    @Contract(pure = true)
    public StringToken getMethodDesc() {
        return this.methodDesc;
    }

    @Nullable
    @Contract(pure = true)
    public StringToken getMethodLocation() {
        return this.methodLocation;
    }

    @Nullable
    @Contract(pure = true)
    public StringToken getMethodName() {
        return this.methodName;
    }

    @Nullable
    @Contract(pure = true)
    public StringToken getOwnerName() {
        return this.ownerName;
    }

    @Nullable
    @Contract(pure = true)
    public BlockToken getStartOfBody() {
        return this.startOfBody;
    }

    @NotNull
    @Unmodifiable
    @Contract(pure = true)
    public List<@NotNull Token> getTokens() {
        return this.tokens;
    }
}
