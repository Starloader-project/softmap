package org.stianloader.softmap.insns;

import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.stianloader.softmap.FramedRemapper;

public interface InsnBlock {
    @Contract(pure = false, mutates = "param2")
    @CheckReturnValue
    @NotNull
    MatchResult matchesInstruction(@NotNull AbstractInsnNode insn, @NotNull FramedRemapper remapper);
}
