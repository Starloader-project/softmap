package org.stianloader.softmap.insns;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.stianloader.softmap.FramedRemapper;
import org.stianloader.softmap.SoftmapParseError;
import org.stianloader.softmap.tokens.StringToken;

public final class SimpleInsnBlock implements InsnBlock {

    private final int opcode;
    @NotNull
    private final StringToken token;

    public static final @NotNull InsnParser<SimpleInsnBlock> NOP = new SimpleInsnBlockParser(Opcodes.NOP);
    public static final @NotNull InsnParser<SimpleInsnBlock> ACONST_NULL = new SimpleInsnBlockParser(Opcodes.ACONST_NULL);
    public static final @NotNull InsnParser<SimpleInsnBlock> IALOAD = new SimpleInsnBlockParser(Opcodes.IALOAD);
    public static final @NotNull InsnParser<SimpleInsnBlock> LALOAD = new SimpleInsnBlockParser(Opcodes.LALOAD);
    public static final @NotNull InsnParser<SimpleInsnBlock> FALOAD = new SimpleInsnBlockParser(Opcodes.FALOAD);
    public static final @NotNull InsnParser<SimpleInsnBlock> DALOAD = new SimpleInsnBlockParser(Opcodes.DALOAD);
    public static final @NotNull InsnParser<SimpleInsnBlock> AALOAD = new SimpleInsnBlockParser(Opcodes.AALOAD);
    public static final @NotNull InsnParser<SimpleInsnBlock> BALOAD = new SimpleInsnBlockParser(Opcodes.BALOAD);
    public static final @NotNull InsnParser<SimpleInsnBlock> CALOAD = new SimpleInsnBlockParser(Opcodes.CALOAD);
    public static final @NotNull InsnParser<SimpleInsnBlock> SALOAD = new SimpleInsnBlockParser(Opcodes.SALOAD);
    public static final @NotNull InsnParser<SimpleInsnBlock> IASTORE = new SimpleInsnBlockParser(Opcodes.IASTORE);
    public static final @NotNull InsnParser<SimpleInsnBlock> LASTORE = new SimpleInsnBlockParser(Opcodes.LASTORE);
    public static final @NotNull InsnParser<SimpleInsnBlock> FASTORE = new SimpleInsnBlockParser(Opcodes.FASTORE);
    public static final @NotNull InsnParser<SimpleInsnBlock> DASTORE = new SimpleInsnBlockParser(Opcodes.DASTORE);
    public static final @NotNull InsnParser<SimpleInsnBlock> AASTORE = new SimpleInsnBlockParser(Opcodes.AASTORE);
    public static final @NotNull InsnParser<SimpleInsnBlock> BASTORE = new SimpleInsnBlockParser(Opcodes.BASTORE);
    public static final @NotNull InsnParser<SimpleInsnBlock> CASTORE = new SimpleInsnBlockParser(Opcodes.CASTORE);
    public static final @NotNull InsnParser<SimpleInsnBlock> SASTORE = new SimpleInsnBlockParser(Opcodes.SASTORE);
    public static final @NotNull InsnParser<SimpleInsnBlock> POP = new SimpleInsnBlockParser(Opcodes.POP);
    public static final @NotNull InsnParser<SimpleInsnBlock> POP2 = new SimpleInsnBlockParser(Opcodes.POP2);
    public static final @NotNull InsnParser<SimpleInsnBlock> DUP = new SimpleInsnBlockParser(Opcodes.DUP);
    public static final @NotNull InsnParser<SimpleInsnBlock> DUP2 = new SimpleInsnBlockParser(Opcodes.DUP2);
    public static final @NotNull InsnParser<SimpleInsnBlock> DUP_X1 = new SimpleInsnBlockParser(Opcodes.DUP_X1);
    public static final @NotNull InsnParser<SimpleInsnBlock> DUP2_X1 = new SimpleInsnBlockParser(Opcodes.DUP2_X1);
    public static final @NotNull InsnParser<SimpleInsnBlock> DUP_X2 = new SimpleInsnBlockParser(Opcodes.DUP_X2);
    public static final @NotNull InsnParser<SimpleInsnBlock> DUP2_X2 = new SimpleInsnBlockParser(Opcodes.DUP2_X2);
    public static final @NotNull InsnParser<SimpleInsnBlock> SWAP = new SimpleInsnBlockParser(Opcodes.SWAP);
    public static final @NotNull InsnParser<SimpleInsnBlock> IADD = new SimpleInsnBlockParser(Opcodes.IADD);
    public static final @NotNull InsnParser<SimpleInsnBlock> LADD = new SimpleInsnBlockParser(Opcodes.LADD);
    public static final @NotNull InsnParser<SimpleInsnBlock> FADD = new SimpleInsnBlockParser(Opcodes.FADD);
    public static final @NotNull InsnParser<SimpleInsnBlock> DADD = new SimpleInsnBlockParser(Opcodes.DADD);
    public static final @NotNull InsnParser<SimpleInsnBlock> ISUB = new SimpleInsnBlockParser(Opcodes.ISUB);
    public static final @NotNull InsnParser<SimpleInsnBlock> LSUB = new SimpleInsnBlockParser(Opcodes.LSUB);
    public static final @NotNull InsnParser<SimpleInsnBlock> FSUB = new SimpleInsnBlockParser(Opcodes.FSUB);
    public static final @NotNull InsnParser<SimpleInsnBlock> DSUB = new SimpleInsnBlockParser(Opcodes.DSUB);
    public static final @NotNull InsnParser<SimpleInsnBlock> IMUL = new SimpleInsnBlockParser(Opcodes.IMUL);
    public static final @NotNull InsnParser<SimpleInsnBlock> LMUL = new SimpleInsnBlockParser(Opcodes.LMUL);
    public static final @NotNull InsnParser<SimpleInsnBlock> FMUL = new SimpleInsnBlockParser(Opcodes.FMUL);
    public static final @NotNull InsnParser<SimpleInsnBlock> DMUL = new SimpleInsnBlockParser(Opcodes.DMUL);
    public static final @NotNull InsnParser<SimpleInsnBlock> IDIV = new SimpleInsnBlockParser(Opcodes.IDIV);
    public static final @NotNull InsnParser<SimpleInsnBlock> LDIV = new SimpleInsnBlockParser(Opcodes.LDIV);
    public static final @NotNull InsnParser<SimpleInsnBlock> FDIV = new SimpleInsnBlockParser(Opcodes.FDIV);
    public static final @NotNull InsnParser<SimpleInsnBlock> DDIV = new SimpleInsnBlockParser(Opcodes.DDIV);
    public static final @NotNull InsnParser<SimpleInsnBlock> IREM = new SimpleInsnBlockParser(Opcodes.IREM);
    public static final @NotNull InsnParser<SimpleInsnBlock> LREM = new SimpleInsnBlockParser(Opcodes.LREM);
    public static final @NotNull InsnParser<SimpleInsnBlock> FREM = new SimpleInsnBlockParser(Opcodes.FREM);
    public static final @NotNull InsnParser<SimpleInsnBlock> DREM = new SimpleInsnBlockParser(Opcodes.DREM);
    public static final @NotNull InsnParser<SimpleInsnBlock> INEG = new SimpleInsnBlockParser(Opcodes.INEG);
    public static final @NotNull InsnParser<SimpleInsnBlock> LNEG = new SimpleInsnBlockParser(Opcodes.LNEG);
    public static final @NotNull InsnParser<SimpleInsnBlock> FNEG = new SimpleInsnBlockParser(Opcodes.FNEG);
    public static final @NotNull InsnParser<SimpleInsnBlock> DNEG = new SimpleInsnBlockParser(Opcodes.DNEG);
    public static final @NotNull InsnParser<SimpleInsnBlock> ISHL = new SimpleInsnBlockParser(Opcodes.ISHL);
    public static final @NotNull InsnParser<SimpleInsnBlock> LSHL = new SimpleInsnBlockParser(Opcodes.LSHL);
    public static final @NotNull InsnParser<SimpleInsnBlock> ISHR = new SimpleInsnBlockParser(Opcodes.ISHR);
    public static final @NotNull InsnParser<SimpleInsnBlock> LSHR = new SimpleInsnBlockParser(Opcodes.LSHR);
    public static final @NotNull InsnParser<SimpleInsnBlock> IUSHR = new SimpleInsnBlockParser(Opcodes.IUSHR);
    public static final @NotNull InsnParser<SimpleInsnBlock> LUSHR = new SimpleInsnBlockParser(Opcodes.LUSHR);
    public static final @NotNull InsnParser<SimpleInsnBlock> IAND = new SimpleInsnBlockParser(Opcodes.IAND);
    public static final @NotNull InsnParser<SimpleInsnBlock> LAND = new SimpleInsnBlockParser(Opcodes.LAND);
    public static final @NotNull InsnParser<SimpleInsnBlock> IOR = new SimpleInsnBlockParser(Opcodes.IOR);
    public static final @NotNull InsnParser<SimpleInsnBlock> LOR = new SimpleInsnBlockParser(Opcodes.LOR);
    public static final @NotNull InsnParser<SimpleInsnBlock> IXOR = new SimpleInsnBlockParser(Opcodes.IXOR);
    public static final @NotNull InsnParser<SimpleInsnBlock> LXOR = new SimpleInsnBlockParser(Opcodes.LXOR);
    public static final @NotNull InsnParser<SimpleInsnBlock> I2L = new SimpleInsnBlockParser(Opcodes.I2L);
    public static final @NotNull InsnParser<SimpleInsnBlock> I2F = new SimpleInsnBlockParser(Opcodes.I2F);
    public static final @NotNull InsnParser<SimpleInsnBlock> I2D = new SimpleInsnBlockParser(Opcodes.I2D);
    public static final @NotNull InsnParser<SimpleInsnBlock> I2S = new SimpleInsnBlockParser(Opcodes.I2S);
    public static final @NotNull InsnParser<SimpleInsnBlock> I2B = new SimpleInsnBlockParser(Opcodes.I2B);
    public static final @NotNull InsnParser<SimpleInsnBlock> I2C = new SimpleInsnBlockParser(Opcodes.I2C);
    public static final @NotNull InsnParser<SimpleInsnBlock> L2I = new SimpleInsnBlockParser(Opcodes.L2I);
    public static final @NotNull InsnParser<SimpleInsnBlock> L2F = new SimpleInsnBlockParser(Opcodes.L2F);
    public static final @NotNull InsnParser<SimpleInsnBlock> L2D = new SimpleInsnBlockParser(Opcodes.L2D);
    public static final @NotNull InsnParser<SimpleInsnBlock> F2D = new SimpleInsnBlockParser(Opcodes.F2D);
    public static final @NotNull InsnParser<SimpleInsnBlock> F2I = new SimpleInsnBlockParser(Opcodes.F2I);
    public static final @NotNull InsnParser<SimpleInsnBlock> F2L = new SimpleInsnBlockParser(Opcodes.F2L);
    public static final @NotNull InsnParser<SimpleInsnBlock> LCMP = new SimpleInsnBlockParser(Opcodes.LCMP);
    public static final @NotNull InsnParser<SimpleInsnBlock> FCMPL = new SimpleInsnBlockParser(Opcodes.FCMPL);
    public static final @NotNull InsnParser<SimpleInsnBlock> FCMPG = new SimpleInsnBlockParser(Opcodes.FCMPG);
    public static final @NotNull InsnParser<SimpleInsnBlock> DCMPL = new SimpleInsnBlockParser(Opcodes.DCMPL);
    public static final @NotNull InsnParser<SimpleInsnBlock> DCMPG = new SimpleInsnBlockParser(Opcodes.DCMPG);
    public static final @NotNull InsnParser<SimpleInsnBlock> IRETURN = new SimpleInsnBlockParser(Opcodes.IRETURN);
    public static final @NotNull InsnParser<SimpleInsnBlock> LRETURN = new SimpleInsnBlockParser(Opcodes.LRETURN);
    public static final @NotNull InsnParser<SimpleInsnBlock> FRETURN = new SimpleInsnBlockParser(Opcodes.FRETURN);
    public static final @NotNull InsnParser<SimpleInsnBlock> DRETURN = new SimpleInsnBlockParser(Opcodes.DRETURN);
    public static final @NotNull InsnParser<SimpleInsnBlock> ARETURN = new SimpleInsnBlockParser(Opcodes.ARETURN);
    public static final @NotNull InsnParser<SimpleInsnBlock> RETURN = new SimpleInsnBlockParser(Opcodes.RETURN);
    public static final @NotNull InsnParser<SimpleInsnBlock> ARRAYLENGTH = new SimpleInsnBlockParser(Opcodes.ARRAYLENGTH);
    public static final @NotNull InsnParser<SimpleInsnBlock> ATHROW = new SimpleInsnBlockParser(Opcodes.ATHROW);
    public static final @NotNull InsnParser<SimpleInsnBlock> MONITOREXIT = new SimpleInsnBlockParser(Opcodes.MONITOREXIT);
    public static final @NotNull InsnParser<SimpleInsnBlock> MONITORENTER = new SimpleInsnBlockParser(Opcodes.MONITORENTER);

    public SimpleInsnBlock(int opcode, @NotNull StringToken token) {
        this.opcode = opcode;
        this.token = token;
    }

    @Override
    @NotNull
    public MatchResult matchesInstruction(@NotNull AbstractInsnNode insn, @NotNull FramedRemapper remapper) {
        if (insn.getOpcode() != this.opcode) {
            return new MatchResult("Opcode mismatch", this.token);
        } else {
            return MatchResult.RESULT_BREAK;
        }
    }

    private static final class SimpleInsnBlockParser implements InsnParser<SimpleInsnBlock> {
        private final int opcode;

        public SimpleInsnBlockParser(int opcode) {
            this.opcode = opcode;
        }

        @Override
        @NotNull
        public SimpleInsnBlock parseInstruction(@NotNull List<@NotNull StringToken> lineContents,
                @NotNull List<@NotNull SoftmapParseError> errorStream) {
            if (lineContents.size() != 1) {
                StringToken startToken = lineContents.get(1);
                StringToken endToken = lineContents.get(lineContents.size() - 1);
                errorStream.add(new SoftmapParseError(startToken.getStart(), endToken.getEnd(), startToken.getRow(), startToken.getColumn(), "Unexpected arguments for expression."));
            }

            return new SimpleInsnBlock(this.opcode, lineContents.get(0));
        }
    }
}
