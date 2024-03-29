package org.stianloader.softmap.insns;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.stianloader.softmap.FramedRemapper;
import org.stianloader.softmap.SoftmapParseError;
import org.stianloader.softmap.tokens.StringToken;

public final class FieldInsn implements InsnBlock {

    private static class Parser implements InsnParser<FieldInsn> {
        private final int matchOpcode;
        private Parser(int matchOpcode) {
            this.matchOpcode = matchOpcode;
        }

        @Override
        @NotNull
        public FieldInsn parseInstruction(@NotNull List<@NotNull StringToken> lineContents, @NotNull List<@NotNull SoftmapParseError> errorStream) {
            StringToken opcodeToken = lineContents.get(0);
            if (lineContents.size() < 3) {
                errorStream.add(new SoftmapParseError(lineContents.get(lineContents.size() - 1), "Too few arguments to FieldInsn-type opcode. Expression expected '<opcode> <owner>.<name> <desc>'"));
            } else if (lineContents.size() > 3) {
                errorStream.add(new SoftmapParseError(lineContents.get(3), "Too many arguments to FieldInsn-type opcode. Expression expected '<opcode> <owner>.<name> <desc>'"));
            }

            StringToken owner = null;
            StringToken name = null;
            StringToken desc = null;

            if (lineContents.size() > 1) {
                StringToken tok = lineContents.get(1);
                int dotIndex = tok.indexOf('.');
                if (dotIndex == -1) {
                    if (lineContents.size() == 2) {
                        name = tok;
                    } else {
                        owner = tok;
                        name = lineContents.get(2);
                        if (lineContents.size() > 3) {
                            // It is hard to differ between desc and name for fields, so we go with the principle of "hopefully it works"
                            // After all, this is malformed anyways
                            desc = lineContents.get(3);
                        } else {
                            errorStream.add(new SoftmapParseError(tok, "Malformed field instruction: Expected expression format: '<opcode> <owner>.<name> <desc>'; missing codepoint '.'."));
                        }
                    }
                } else {
                    owner = tok.subtoken(0, dotIndex);
                    name = tok.subtoken(dotIndex + 1, tok.getContentLength());
                    if (lineContents.size() > 2) {
                        desc = lineContents.get(2);
                    }
                }
            }

            if (desc != null && desc.codepointBefore(desc.getContentLength()) == '[') {
                // Guard for edge case that would otherwise cause a crash
                errorStream.add(new SoftmapParseError(desc, "Malformed field instruction: Malformed descriptor: A descriptor may not end with the character '['."));
                desc = null;
            }

            if (desc != null) {
                // Guard for nonsense descriptors that might cause a crash if handled improperly - better be safe than sorry
                int srcArrayDepth = 0;
                while (desc.codepointAt(srcArrayDepth) == '[') srcArrayDepth++;
                if (desc.codepointAt(srcArrayDepth) == 'L') {
                    int semicolonIndex = desc.indexOf(';');
                    if (semicolonIndex == -1) {
                        errorStream.add(new SoftmapParseError(desc, "Malformed field instruction: Malformed descriptor: An L-type reference must be closed with ';', but no ';' could be found."));
                        desc = null;
                    } else if (desc.codepointBefore(desc.getContentLength()) == '?') {
                        if (semicolonIndex != desc.getContentLength() - 2) {
                            errorStream.add(new SoftmapParseError(desc, "Malformed field instruction: Malformed descriptor: ';' must be the last character before '?'."));
                            desc = null;
                        }
                    } else if (semicolonIndex != desc.getContentLength() - 1) {
                        errorStream.add(new SoftmapParseError(desc, "Malformed field instruction: Malformed descriptor: ';' must be the last character of a L-type reference."));
                        desc = null;
                    } else if (desc.indexOf('.') != -1) {
                        errorStream.add(new SoftmapParseError(desc, "Malformed field instruction: Malformed descriptor: Illegal codepoint '.', use forward slashes ('/') to separate packages."));
                    }
                } else if (desc.getContentLength() != (srcArrayDepth + 1)) {
                    if (desc.codepointAt(srcArrayDepth + 1) == '?') {
                        errorStream.add(new SoftmapParseError(desc, "Malformed field instruction: Malformed descriptor: Illegal attempt at mapping a primitive."));
                        desc = desc.subtoken(0, srcArrayDepth + 1);
                    } else {
                        errorStream.add(new SoftmapParseError(desc, "Malformed field instruction: Malformed descriptor: Unexpected character after primitive at column " + (desc.getColumn() + srcArrayDepth + 1) + "."));
                        desc = null;
                    }
                }
            }

            return new FieldInsn(this.matchOpcode, opcodeToken, owner, name, desc);
        }
    }
    @NotNull
    public static final InsnParser<FieldInsn> PARSER_GETFIELD = new Parser(Opcodes.GETFIELD);
    @NotNull
    public static final InsnParser<FieldInsn> PARSER_GETSTATIC = new Parser(Opcodes.GETSTATIC);
    @NotNull
    public static final InsnParser<FieldInsn> PARSER_PUTFIELD = new Parser(Opcodes.PUTFIELD);

    @NotNull
    public static final InsnParser<FieldInsn> PARSER_PUTSTATIC = new Parser(Opcodes.PUTSTATIC);
    @Nullable
    private final StringToken fieldDesc;
    @Nullable
    private final StringToken fieldName;
    @Nullable
    private final StringToken fieldOwner;
    private final int matchOpcode;

    @NotNull
    private final StringToken opcodeToken;

    private FieldInsn(int matchOpcode, @NotNull StringToken opcodeToken, @Nullable StringToken fieldOwner, @Nullable StringToken fieldName, @Nullable StringToken fieldDesc) {
        this.matchOpcode = matchOpcode;
        this.opcodeToken = opcodeToken;
        this.fieldOwner = fieldOwner;
        this.fieldName = fieldName;
        this.fieldDesc = fieldDesc;
    }

    @Override
    @NotNull
    public MatchResult matchesInstruction(@NotNull AbstractInsnNode insn, @NotNull FramedRemapper remapper) {
        if (insn.getOpcode() != this.matchOpcode) {
            return new MatchResult("Instruction opcode mismatch", this.opcodeToken);
        }

        FieldInsnNode fInsn = (FieldInsnNode) insn;

        StringToken fieldOwner = this.fieldOwner;
        if (fieldOwner != null
                && fieldOwner.codepointBefore(fieldOwner.getContentLength()) != '?'
                && !fieldOwner.contentMatches(fInsn.owner)) {
            return new MatchResult("Field owner mismatch (got " + fInsn.owner + ')', fieldOwner);
        }

        StringToken fieldName = this.fieldName;
        if (fieldName != null
                && fieldName.codepointBefore(fieldName.getContentLength()) != '?'
                && !fieldName.contentMatches(fInsn.name)) {
            return new MatchResult("Field name mismatch (got " + fInsn.name + ')', fieldName);
        }

        StringToken fieldDesc = this.fieldDesc;
        if (fieldDesc != null) {
            if (fieldDesc.codepointBefore(fieldDesc.getContentLength()) != '?') {
                if (!fieldDesc.contentMatches(fInsn.desc)) {
                    return new MatchResult("Field descriptor mismatch (got " + fInsn.desc + ')', fieldDesc);
                }
            } else {
                int srcArrayDepth = 0;
                while (fieldDesc.codepointAt(srcArrayDepth) == '[') srcArrayDepth++;
                int dstArrayDepth = 0;
                while (fInsn.desc.codePointAt(dstArrayDepth) == '[') dstArrayDepth++;
                if (srcArrayDepth != dstArrayDepth) {
                    return new MatchResult("Field descriptor array depth mismatch (got " + fInsn.desc + ')', fieldDesc);
                }
                if (fieldDesc.codepointAt(srcArrayDepth) != fInsn.desc.codePointAt(srcArrayDepth)) {
                    return new MatchResult("Field descriptor computation type mismatch (got " + fInsn.desc + ')', fieldDesc);
                }
                if (fieldDesc.codepointAt(srcArrayDepth) == 'L') {
                    String mappedName = fieldDesc.subtext(srcArrayDepth + 1, fieldDesc.getContentLength() - 2); // 1 for semicolon, 1 for ?
                    String srcName  = fInsn.desc.substring(srcArrayDepth + 1, fInsn.desc.length() - 1);
                    String dstName = remapper.getMappedClassOpt(srcName);
                    if (!srcName.equals(dstName)) {
                        if (!mappedName.equals(dstName)) {
                            return new MatchResult("Descriptor mismatch (mapping match; mapping collision. srcName: '" + srcName + "', dstName: '" + dstName + "')", fieldDesc);
                        }
                    } else {
                        remapper.mapClass(srcName, mappedName);
                    }
                }
            }
        }

        if (fieldOwner != null && fieldOwner.codepointBefore(fieldOwner.getContentLength()) == '?') {
            String srcName = fInsn.owner;
            String dstName = remapper.getMappedClassOpt(srcName);
            if (srcName.equals(dstName)) {
                remapper.mapClass(srcName, fieldOwner.subtext(0, fieldOwner.getContentLength() - 1));
            } else if (!dstName.equals(fieldOwner.subtext(0, fieldOwner.getContentLength() - 1))) {
                return new MatchResult("Owner mismatch (mapping match; mapping collision. srcName: '" + srcName + "', dstName: '" + dstName + "')", fieldOwner);
            }
        }

        if (fieldName != null && fieldName.codepointBefore(fieldName.getContentLength()) == '?') {
            String srcName = fInsn.name;
            String dstName = remapper.getMappedFieldOpt(fInsn.owner, srcName, fInsn.desc);

            if (srcName.equals(dstName)) {
                remapper.mapField(fInsn.owner, srcName, fInsn.desc, fieldName.subtext(0, fieldName.getContentLength() - 1));
            } else if (!dstName.equals(fieldName.subtext(0, fieldName.getContentLength() - 1))) {
                return new MatchResult("Field name mismatch (mapping match; mapping collision. srcName: '" + srcName + "', dstName: '" + dstName + "')", fieldName);
            }
        }

        return MatchResult.RESULT_BREAK;
    }
}
