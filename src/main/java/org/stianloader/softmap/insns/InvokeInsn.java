package org.stianloader.softmap.insns;

import java.util.List;

import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.stianloader.softmap.DescString;
import org.stianloader.softmap.FramedRemapper;
import org.stianloader.softmap.SoftmapParseError;
import org.stianloader.softmap.tokens.StringToken;
import org.stianloader.softmap.tokens.Token;

public class InvokeInsn implements InsnBlock {

    @NotNull
    public static final InsnParser<InvokeInsn> PARSER_INVOKESTATIC = new Parser(Opcodes.INVOKESTATIC);
    @NotNull
    public static final InsnParser<InvokeInsn> PARSER_INVOKEVIRTUAL = new Parser(Opcodes.INVOKEVIRTUAL);
    @NotNull
    public static final InsnParser<InvokeInsn> PARSER_INVOKEINTERFACE = new Parser(Opcodes.INVOKEINTERFACE);
    @NotNull
    public static final InsnParser<InvokeInsn> PARSER_INVOKESPECIAL = new Parser(Opcodes.INVOKESPECIAL);

    private final int opcode;
    @Nullable
    private final StringToken className;
    @Nullable
    private final StringToken methodName;
    @Nullable
    private final StringToken methodDescriptor;
    @NotNull
    private final StringToken opcodeToken;

    public InvokeInsn(int opcode, @NotNull StringToken opcodeToken, @Nullable StringToken className, @Nullable StringToken methodName, @Nullable StringToken methodDescriptor) {
        this.opcode = opcode;
        this.opcodeToken = opcodeToken;
        this.className = className;
        this.methodName = methodName;
        this.methodDescriptor = methodDescriptor;
    }

    private static final class Parser implements InsnParser<InvokeInsn> {

        private final int opcode;

        private Parser(int opcode) {
            this.opcode = opcode;
        }

        @Override
        @NotNull
        @Contract(pure = true)
        public InvokeInsn parseInstruction(@NotNull List<@NotNull StringToken> lineContents,
                @NotNull List<@NotNull SoftmapParseError> errorStream) {
            StringToken arg = null;
            if (lineContents.size() == 1) {
                errorStream.add(new SoftmapParseError(lineContents.get(0), "InvokeX instruction is missing the method that is invoked, it is formatted in the scheme of '<opcode> <class>.<name><desc>', e.g.: 'INVOKESTATIC java/lang/String.toString()Ljava/lang/String;'."));
            } else if (lineContents.size() == 2) {
                arg = lineContents.get(1);
            } else {
                arg = lineContents.get(1);
                Token firstSuperflous = lineContents.get(2);
                Token last = lineContents.get(lineContents.size() - 1);
                errorStream.add(new SoftmapParseError(firstSuperflous.getStart(), last.getEnd(), firstSuperflous.getRow(), firstSuperflous.getColumn(), "InvokeX instruction provided with superflous arguments, it is formatted in the scheme of '<opcode> <class>.<name><desc>', e.g.: 'INVOKESTATIC java/lang/String.toString()Ljava/lang/String;'. Are there any rouge whitespaces? (Note: The softmap format does not support whitespaces in member names, even though that is valid according to the JVMS)"));
            }

            StringToken ownerName = null;
            StringToken methodName = null;
            StringToken methodDesc = null;

            if (arg != null) {
                int endOfOwner = arg.indexOf('.');
                int startOfDesc = arg.indexOf('(', endOfOwner + 1);

                if (endOfOwner == -1) {
                    errorStream.add(new SoftmapParseError(arg, "InvokeX target method missing owner name, which are separated from the method name via '.'. The instruction is formatted in the scheme of '<opcode> <class>.<name><desc>', e.g.: 'INVOKESTATIC java/lang/String.toString()Ljava/lang/String;'."));
                } else if (startOfDesc == -1) {
                    errorStream.add(new SoftmapParseError(arg, "InvokeX target method missing descriptor, which starts with '('. The instruction is formatted in the scheme of '<opcode> <class>.<name><desc>', e.g.: 'INVOKESTATIC java/lang/String.toString()Ljava/lang/String;'."));
                } else if (arg.indexOf(')', startOfDesc) == -1) {
                    errorStream.add(new SoftmapParseError(arg, "InvokeX target method has an invalid descriptor: missing codepoint ')'. The instruction is formatted in the scheme of '<opcode> <class>.<name><desc>', e.g.: 'INVOKESTATIC java/lang/String.toString()Ljava/lang/String;'."));
                }

                if (endOfOwner != -1) {
                    ownerName = arg.subtoken(0, endOfOwner);
                }

                int endOfName;
                if (startOfDesc != -1) {
                    methodDesc = arg.subtoken(startOfDesc, arg.getContentLength());
                    endOfName = startOfDesc;
                } else {
                    endOfName = arg.getContentLength();
                }

                methodName = arg.subtoken(endOfOwner + 1, endOfName);
            }

            if (methodName != null && methodName.indexOf('.') != -1) {
                int startWith;
                int column;
                if (ownerName != null) {
                    startWith = ownerName.getStart();
                    column = ownerName.getColumn();
                } else {
                    startWith = methodName.getStart();
                    column = methodName.getColumn();
                }

                errorStream.add(new SoftmapParseError(startWith, methodName.getEnd(), methodName.getRow(), column, "The owner definition contains at least one dot ('.'), which is a character banned from usage in method names. Remember that package names should be separated out with forward slashes ('/'), not dots. This hints at an invalid owner name."));
            }

            if (methodDesc != null && methodDesc.indexOf('.') != -1) {
                errorStream.add(new SoftmapParseError(methodDesc, "The target method descriptor contains at least one dot ('.'), which is a character banned from usage in descriptors. Remember that package names should be separated out with forward slashes ('/'), not dots. This hints at an invalid descriptor type."));
            }

            return new InvokeInsn(this.opcode, lineContents.get(0), ownerName, methodName, methodDesc);
        }
    }

    @Override
    @Contract(pure = false, mutates = "param2")
    @CheckReturnValue
    @NotNull
    public MatchResult matchesInstruction(@NotNull AbstractInsnNode insn, @NotNull FramedRemapper remapper) {
        if (insn.getOpcode() != this.opcode) {
            return new MatchResult("Instruction opcode mismatch", this.opcodeToken);
        }

        MethodInsnNode mInsn = (MethodInsnNode) insn;

        StringToken classNameToken = this.className; // Hack to get eclipse's null analysis working correctly. Though technically speaking it is right given that even final fields can be modified via unsafe
        if (classNameToken != null) {
            String srcOwner = mInsn.owner;
            String dstOwner = remapper.getMappedClassOpt(srcOwner);
            if (classNameToken.lastCodepoint() != '?') {
                if (!classNameToken.contentMatches(dstOwner)) {
                    if (!srcOwner.equals(dstOwner)) {
                        return new MatchResult("Owner mismatch (explicit match, srcName: '" + srcOwner + "', dstName: '" + dstOwner + "')", classNameToken);
                    } else {
                        return new MatchResult("Owner mismatch (explicit match)", classNameToken);
                    }
                }
            } else {
                if (srcOwner.equals(dstOwner)) {
                    remapper.mapClass(srcOwner, classNameToken.getText());
                } else if (!classNameToken.contentMatches(false, dstOwner, 0, dstOwner.length())) {
                    return new MatchResult("Owner mismatch (mapping match; mapping collision. srcName: '" + srcOwner + "', dstName: '" + dstOwner + "')", classNameToken);
                }
            }
        }

        StringToken methodNameToken = this.methodName;
        if (methodNameToken != null) {
            String srcName = mInsn.name;
            String dstName = remapper.getMappedMethodOpt(mInsn.owner, srcName, mInsn.desc);

            if (methodNameToken.lastCodepoint() != '?') {
                // Explicit match
                if (!methodNameToken.contentMatches(dstName)) {
                    return new MatchResult("Method name mismatch (explicit match, srcName: '" + srcName + "', dstName: '" + dstName + "', srcOwner: '" + mInsn.owner + "', srcDesc: '" + mInsn.desc + "')", methodNameToken);
                }
            } else {
                // Mapping mismatch
                if (srcName.equals(dstName)) {
                    remapper.mapMethod(mInsn.owner, srcName, mInsn.desc, methodNameToken.getText());
                }
            }
        }

        StringToken methodDescToken = this.methodDescriptor;
        if (methodDescToken != null) {
            MatchResult result = InvokeInsn.mapDescriptor(methodDescToken, mInsn.desc, remapper);
            if (result != null) {
                return result; // Error occurred during descriptor mapping/reading
            }
        }

        return MatchResult.RESULT_BREAK;
    }

    @Nullable
    public static MatchResult mapDescriptor(@NotNull StringToken methodDescToken, @NotNull String methodDesc, @NotNull FramedRemapper remapper) {
        int head = 1;
        int headCodepoint = methodDescToken.codepointAt(head);
        DescString dString = new DescString(methodDesc);
        while (headCodepoint != ')') {
            // TODO validate `headCodepoint` codepoint (e.g. 'i' or ';' are not allowed)
            if (!dString.hasNext()) {
                return new MatchResult("Method descriptor mismatch (Argument count mismatch, descriptor of matched method: '" + methodDesc + "')", methodDescToken);
            }
            String cmpType = dString.nextType();
            if (cmpType.codePointAt(0) != headCodepoint) {
                return new MatchResult("Method descriptor mismatch (Argument type mismatch [different computational type]; descriptor of matched method: '" + methodDesc + "', discrepancy starting from column " + (methodDescToken.getStart() + head) + ")", methodDescToken);
            }

            headCodepoint = methodDescToken.codepointAt(++head);
            // verify array depth
            int arraydepth = 0;
            if (cmpType.codePointAt(0) == '[') {
                while (headCodepoint == '[') {
                    arraydepth++;
                    headCodepoint = methodDescToken.codepointAt(++head);
                }
                if (cmpType.length() < arraydepth + 2
                        || cmpType.codePointAt(arraydepth) != '['
                        || cmpType.codePointAt(arraydepth + 1) == '[') {
                    return new MatchResult("Method descriptor mismatch (Argument type mismatch [different array depth]; descriptor of matched method: '" + methodDesc + "', discrepancy is around column " + (methodDescToken.getStart() + head) + ")", methodDescToken);
                }
                arraydepth += 1;
            }

            if (headCodepoint != cmpType.codePointAt(arraydepth)) {
                return new MatchResult("Method descriptor mismatch (Argument type mismatch [different computational types once skipping arrays]; descriptor of matched method: '" + methodDesc + "', discrepancy starting from column " + (methodDescToken.getStart() + head) + ")", methodDescToken);
            }

            // Note: While 'head'/'headCodepoint2' is not offset by array depth, indices to cmpType is.
            if (headCodepoint != 'L') {
                // TODO here too, validate the `headCodepoint` - i.e. it should only be a primitive
                // primitive (array)

                if (cmpType.codePointAt(arraydepth) != headCodepoint) {
                    return new MatchResult("Method descriptor mismatch (Argument type mismatch [unrolled computational type mismatch]; descriptor of matched method: '" + methodDesc + "', discrepancy is around column " + (methodDescToken.getStart() + head) + ")", methodDescToken);
                }

                headCodepoint = methodDescToken.codepointAt(++head);
                if (headCodepoint == '?') {
                    // tried to map primitive (array) - an error condition that should honestly be caught at parse time so we shall silently discard this fact
                    // TODO At least catch that error at parsing time lol
                    headCodepoint = methodDescToken.codepointAt(++head);
                }
            } else {
                // object (array) - the real deal
                int lookaheadIndex = methodDescToken.indexOf(';', ++head);
                if (lookaheadIndex == -1 || methodDescToken.indexOf(')', lookaheadIndex) == -1) {
                    // Highly invalid descriptor
                    return new MatchResult("Invalidly parsed invoke instruction: Invalid method descriptor: Missing closing ';' after start of 'L'-type reference", methodDescToken);
                }

                if (cmpType.codePointAt(arraydepth) != 'L') {
                    return new MatchResult("Method descriptor mismatch (Argument type mismatch [unrolled computational type mismatch]; descriptor of matched method: '" + methodDesc + "', discrepancy is around column " + (methodDescToken.getStart() + head) + ")", methodDescToken);
                }

                String srcClass = cmpType.substring(arraydepth + 1, cmpType.length() - 1);
                String dstClass = remapper.getMappedClassOpt(srcClass);

                if (methodDescToken.codepointAt(lookaheadIndex + 1) == '?') {
                    // Mapping match
                    if (!srcClass.equals(dstClass)) {
                        if (lookaheadIndex - head != dstClass.length() || !methodDescToken.contentMatches(false, head, dstClass, 0, dstClass.length())) {
                            return new MatchResult("Method descriptor mismatch (mapping match; mapping collision. srcType: '" + srcClass + "', dstType: '" + dstClass + "', full (src) method descriptor of matched method: '" + methodDesc + "'. Discrepancy arises between column " + (head + methodDescToken.getColumn()) + " and " + (lookaheadIndex + methodDescToken.getColumn()) + ")", methodDescToken);
                        }
                    } else {
                        remapper.mapClass(srcClass, methodDescToken.subtext(head, lookaheadIndex));
                    }
                } else {
                    // Explicit match
                    if (lookaheadIndex - head != dstClass.length()
                           || !methodDescToken.contentMatches(false, head, dstClass, 0, dstClass.length())) {
                        return new MatchResult("Method descriptor mismatch (explicit match. srcType: '" + srcClass + "', dstType: '" + dstClass + "', full (src) method descriptor of matched method: '" + methodDesc + "'. Discrepancy arises between column " + (head + methodDescToken.getColumn()) + " and " + (lookaheadIndex + methodDescToken.getColumn()) + ")", methodDescToken);
                    }
                }
            }
        }

        return null;
    }
}
