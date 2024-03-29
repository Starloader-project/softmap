package org.stianloader.softmap.insns;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.stianloader.softmap.FramedRemapper;
import org.stianloader.softmap.SoftmapParseError;
import org.stianloader.softmap.tokens.StringToken;

public class VarInsn implements InsnBlock {

    private static class Parser implements InsnParser<VarInsn> {

        private final int opcode;

        public Parser(int opcode) {
            this.opcode = opcode;
        }

        @Override
        @NotNull
        public VarInsn parseInstruction(@NotNull List<@NotNull StringToken> lineContents, @NotNull List<@NotNull SoftmapParseError> errorStream) {
            StringToken opcodeToken = lineContents.get(0);
            StringToken varToken = null;
            if (lineContents.size() == 1) {
                errorStream.add(new SoftmapParseError(opcodeToken, "Var insn has too few arguments. Expected var index. Expression format: '<opcode> <index>'"));
            } else if (lineContents.size() != 2) {
                errorStream.add(new SoftmapParseError(lineContents.get(2), "Var insn has too many arguments. Expression format '<opcode> <index>'."));
                varToken = lineContents.get(1);
            } else {
                varToken = lineContents.get(1);
            }

            int varIndex = 0;
            if (varToken != null) {
                if (varToken.contentMatches(true, "this")) {
                    varIndex = 0;
                } else {
                    String varIndexString = varToken.getText();
                    try {
                        varIndex = Integer.parseUnsignedInt(varIndexString);
                    } catch (NumberFormatException nfe) {
                        errorStream.add(new SoftmapParseError(varToken, "Unattainable var index: index not a valid integer (named indices are not yet supported)."));
                        varToken = null;
                    }
                }
            }

            return new VarInsn(this.opcode, opcodeToken, varIndex, varToken);
        }
    }

    @NotNull
    public static final InsnParser<VarInsn> PARSER_ALOAD = new Parser(Opcodes.ALOAD);
    @NotNull
    public static final InsnParser<VarInsn> PARSER_ASTORE = new Parser(Opcodes.ASTORE);
    @NotNull
    public static final InsnParser<VarInsn> PARSER_DLOAD = new Parser(Opcodes.DLOAD);
    @NotNull
    public static final InsnParser<VarInsn> PARSER_DSTORE = new Parser(Opcodes.DSTORE);
    @NotNull
    public static final InsnParser<VarInsn> PARSER_FLOAD = new Parser(Opcodes.FLOAD);
    @NotNull
    public static final InsnParser<VarInsn> PARSER_FSTORE = new Parser(Opcodes.FSTORE);
    @NotNull
    public static final InsnParser<VarInsn> PARSER_ILOAD = new Parser(Opcodes.ILOAD);
    @NotNull
    public static final InsnParser<VarInsn> PARSER_ISTORE = new Parser(Opcodes.ISTORE);
    @NotNull
    public static final InsnParser<VarInsn> PARSER_LLOAD = new Parser(Opcodes.LLOAD);

    @NotNull
    public static final InsnParser<VarInsn> PARSER_LSTORE = new Parser(Opcodes.LSTORE);
    private final int matchOpcode;
    @NotNull
    private final StringToken opcodeToken;
    private final int var;

    @Nullable
    private final StringToken varToken;

    public VarInsn(int opcode, @NotNull StringToken opcodeToken, int var, @Nullable StringToken varToken) {
        this.matchOpcode = opcode;
        this.opcodeToken = opcodeToken;
        this.var = var;
        this.varToken = varToken;
    }

    @Override
    @NotNull
    public MatchResult matchesInstruction(@NotNull AbstractInsnNode insn, @NotNull FramedRemapper remapper) {
        if (insn.getOpcode() != this.matchOpcode) {
            return new MatchResult("Instruction opcode mismatch", this.opcodeToken);
        }

        StringToken varToken = this.varToken;
        if (varToken == null) {
            return MatchResult.RESULT_BREAK;
        }

        VarInsnNode vInsn = (VarInsnNode) insn;
        if (vInsn.var != this.var) {
            return new MatchResult("var insn var mismatch; got " + vInsn.var, varToken);
        }

        return MatchResult.RESULT_BREAK;
    }
}
