package org.stianloader.softmap.insns;

import java.util.List;

import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.stianloader.softmap.FramedRemapper;
import org.stianloader.softmap.SoftmapParseError;
import org.stianloader.softmap.tokens.StringToken;

public class WildcardInsnBlock implements InsnBlock {

    @NotNull
    public static final InsnParser<WildcardInsnBlock> PARSER = new Parser();
    @NotNull
    public static final WildcardInsnBlock INSTANCE = new WildcardInsnBlock();

    private WildcardInsnBlock() {
        // No-args constructor with reduced visibility
    }

    private static final class Parser implements InsnParser<WildcardInsnBlock> {
        private Parser() {
        }

        @Override
        @NotNull
        @Contract(pure = true)
        public WildcardInsnBlock parseInstruction(@NotNull List<@NotNull StringToken> lineContents,
                @NotNull List<@NotNull SoftmapParseError> errorStream) {
            if (lineContents.size() != 1) {
                StringToken startToken = lineContents.get(1);
                StringToken endToken = lineContents.get(lineContents.size() - 1);
                errorStream.add(new SoftmapParseError(startToken.getStart(), endToken.getEnd(), startToken.getRow(), startToken.getColumn(), "Unexpected arguments for wildcard expression."));
            }
            return WildcardInsnBlock.INSTANCE;
        }
    }

    @Override
    @Contract(pure = false, mutates = "param2")
    @CheckReturnValue
    @NotNull
    public MatchResult matchesInstruction(@NotNull AbstractInsnNode insn, @NotNull FramedRemapper remapper) {
        return MatchResult.RESULT_GREEDY;
    }
}
