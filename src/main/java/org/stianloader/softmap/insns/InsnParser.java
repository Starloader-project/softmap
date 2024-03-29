package org.stianloader.softmap.insns;

import java.util.List;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.stianloader.softmap.SoftmapParseError;
import org.stianloader.softmap.tokens.StringToken;

public interface InsnParser<T extends InsnBlock> {
    @NotNull
    @Contract(pure = true)
    T parseInstruction(@NotNull List<@NotNull StringToken> lineContents, @NotNull List<@NotNull SoftmapParseError> errorStream);
}
