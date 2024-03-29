package org.stianloader.softmap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.stianloader.softmap.FramedRemapper.RemapperFrame;
import org.stianloader.softmap.SimpleFramedRemapper.MethodLoc;
import org.stianloader.softmap.insns.FieldInsn;
import org.stianloader.softmap.insns.InsnBlock;
import org.stianloader.softmap.insns.InsnParser;
import org.stianloader.softmap.insns.InvokeInsn;
import org.stianloader.softmap.insns.MatchResult;
import org.stianloader.softmap.insns.SimpleInsnBlock;
import org.stianloader.softmap.insns.VarInsn;
import org.stianloader.softmap.insns.WildcardInsnBlock;
import org.stianloader.softmap.tokens.BlockToken;
import org.stianloader.softmap.tokens.CommentToken;
import org.stianloader.softmap.tokens.StringToken;
import org.stianloader.softmap.tokens.Token;

public class SoftmapContext {

    public static class ApplicationResult {
        @NotNull
        @Unmodifiable
        private final List<@NotNull SoftmapApplicationError> errors;

        @NotNull
        @Unmodifiable
        private final List<@NotNull String> generatedTinyV1Mappings;

        public ApplicationResult(@NotNull @Unmodifiable List<@NotNull String> tinyV1, @NotNull @Unmodifiable List<@NotNull SoftmapApplicationError> errors) {
            this.generatedTinyV1Mappings = tinyV1;
            this.errors = errors;
        }

        @Contract(pure = true)
        @NotNull
        @Unmodifiable
        public List<@NotNull SoftmapApplicationError> getErrors() {
            return this.errors;
        }

        @Contract(pure = true)
        @NotNull
        @Unmodifiable
        public List<@NotNull String> getGeneratedTinyV1Mappings() {
            return this.generatedTinyV1Mappings;
        }
    }

    /**
     * The version identifier to use as a fallback when a version has not been explicitly
     * defined. Note that it is still an error to not define the version the parser should be used,
     * however this parser is made in a way that is rather lenient when it comes to such errors.
     */
    private static final int FALLBACK_VERSION = 1;

    @Unmodifiable
    @NotNull
    private static final Map<@NotNull String, @NotNull InsnParser<?>> INSTRUCTION_PARSERS;

    static {
        Map<@NotNull String, @NotNull InsnParser<?>> insnParsersModifable = new HashMap<>();
        insnParsersModifable.put("*", WildcardInsnBlock.PARSER);

        insnParsersModifable.put("GETFIELD", FieldInsn.PARSER_GETFIELD);
        insnParsersModifable.put("GETSTATIC", FieldInsn.PARSER_GETSTATIC);
        insnParsersModifable.put("PUTFIELD", FieldInsn.PARSER_PUTFIELD);
        insnParsersModifable.put("PUTSTATIC", FieldInsn.PARSER_PUTSTATIC);

        insnParsersModifable.put("INVOKEINTERFACE", InvokeInsn.PARSER_INVOKEINTERFACE);
        insnParsersModifable.put("INVOKEVIRTUAL", InvokeInsn.PARSER_INVOKEVIRTUAL);
        insnParsersModifable.put("INVOKESTATIC", InvokeInsn.PARSER_INVOKESTATIC);
        insnParsersModifable.put("INVOKESPECIAL", InvokeInsn.PARSER_INVOKESPECIAL);

        insnParsersModifable.put("NOP", SimpleInsnBlock.NOP);
        insnParsersModifable.put("ACONST_NULL", SimpleInsnBlock.ACONST_NULL);
        insnParsersModifable.put("IALOAD", SimpleInsnBlock.IALOAD);
        insnParsersModifable.put("LALOAD", SimpleInsnBlock.LALOAD);
        insnParsersModifable.put("FALOAD", SimpleInsnBlock.FALOAD);
        insnParsersModifable.put("DALOAD", SimpleInsnBlock.DALOAD);
        insnParsersModifable.put("CALOAD", SimpleInsnBlock.CALOAD);
        insnParsersModifable.put("BALOAD", SimpleInsnBlock.BALOAD);
        insnParsersModifable.put("AALOAD", SimpleInsnBlock.AALOAD);
        insnParsersModifable.put("SALOAD", SimpleInsnBlock.SALOAD);
        insnParsersModifable.put("IASTORE", SimpleInsnBlock.IASTORE);
        insnParsersModifable.put("LASTORE", SimpleInsnBlock.LASTORE);
        insnParsersModifable.put("FASTORE", SimpleInsnBlock.FASTORE);
        insnParsersModifable.put("DASTORE", SimpleInsnBlock.DASTORE);
        insnParsersModifable.put("CASTORE", SimpleInsnBlock.CASTORE);
        insnParsersModifable.put("BASTORE", SimpleInsnBlock.BASTORE);
        insnParsersModifable.put("AASTORE", SimpleInsnBlock.AASTORE);
        insnParsersModifable.put("SASTORE", SimpleInsnBlock.SASTORE);
        insnParsersModifable.put("POP", SimpleInsnBlock.POP);
        insnParsersModifable.put("POP2", SimpleInsnBlock.POP2);
        insnParsersModifable.put("DUP", SimpleInsnBlock.DUP);
        insnParsersModifable.put("DUP2", SimpleInsnBlock.DUP2);
        insnParsersModifable.put("DUP2_X1", SimpleInsnBlock.DUP2_X1);
        insnParsersModifable.put("DUP2_X2", SimpleInsnBlock.DUP2_X2);
        insnParsersModifable.put("DUP_X1", SimpleInsnBlock.DUP_X1);
        insnParsersModifable.put("DUP_X2", SimpleInsnBlock.DUP_X2);
        insnParsersModifable.put("SWAP", SimpleInsnBlock.SWAP);
        insnParsersModifable.put("IADD", SimpleInsnBlock.IADD);
        insnParsersModifable.put("LADD", SimpleInsnBlock.LADD);
        insnParsersModifable.put("FADD", SimpleInsnBlock.FADD);
        insnParsersModifable.put("DADD", SimpleInsnBlock.DADD);
        insnParsersModifable.put("ISUB", SimpleInsnBlock.ISUB);
        insnParsersModifable.put("LSUB", SimpleInsnBlock.LSUB);
        insnParsersModifable.put("FSUB", SimpleInsnBlock.FSUB);
        insnParsersModifable.put("DSUB", SimpleInsnBlock.DSUB);
        insnParsersModifable.put("IMUL", SimpleInsnBlock.IMUL);
        insnParsersModifable.put("LMUL", SimpleInsnBlock.LMUL);
        insnParsersModifable.put("FMUL", SimpleInsnBlock.FMUL);
        insnParsersModifable.put("DMUL", SimpleInsnBlock.DMUL);
        insnParsersModifable.put("IDIV", SimpleInsnBlock.IDIV);
        insnParsersModifable.put("LDIV", SimpleInsnBlock.LDIV);
        insnParsersModifable.put("FDIV", SimpleInsnBlock.FDIV);
        insnParsersModifable.put("DDIV", SimpleInsnBlock.DDIV);
        insnParsersModifable.put("IREM", SimpleInsnBlock.IREM);
        insnParsersModifable.put("LREM", SimpleInsnBlock.LREM);
        insnParsersModifable.put("FREM", SimpleInsnBlock.FREM);
        insnParsersModifable.put("DREM", SimpleInsnBlock.DREM);
        insnParsersModifable.put("INEG", SimpleInsnBlock.INEG);
        insnParsersModifable.put("LNEG", SimpleInsnBlock.LNEG);
        insnParsersModifable.put("FNEG", SimpleInsnBlock.FNEG);
        insnParsersModifable.put("DNEG", SimpleInsnBlock.DNEG);
        insnParsersModifable.put("ISHL", SimpleInsnBlock.ISHL);
        insnParsersModifable.put("LSHL", SimpleInsnBlock.LSHL);
        insnParsersModifable.put("ISHR", SimpleInsnBlock.ISHR);
        insnParsersModifable.put("LSHR", SimpleInsnBlock.LSHR);
        insnParsersModifable.put("IUSHR", SimpleInsnBlock.IUSHR);
        insnParsersModifable.put("LUSHR", SimpleInsnBlock.LUSHR);
        insnParsersModifable.put("IAND", SimpleInsnBlock.IAND);
        insnParsersModifable.put("LAND", SimpleInsnBlock.LAND);
        insnParsersModifable.put("IOR", SimpleInsnBlock.IOR);
        insnParsersModifable.put("LOR", SimpleInsnBlock.LOR);
        insnParsersModifable.put("IXOR", SimpleInsnBlock.IXOR);
        insnParsersModifable.put("LXOR", SimpleInsnBlock.LXOR);
        insnParsersModifable.put("I2L", SimpleInsnBlock.I2L);
        insnParsersModifable.put("I2F", SimpleInsnBlock.I2F);
        insnParsersModifable.put("I2D", SimpleInsnBlock.I2D);
        insnParsersModifable.put("I2S", SimpleInsnBlock.I2S);
        insnParsersModifable.put("I2B", SimpleInsnBlock.I2B);
        insnParsersModifable.put("I2C", SimpleInsnBlock.I2C);
        insnParsersModifable.put("L2I", SimpleInsnBlock.L2I);
        insnParsersModifable.put("L2F", SimpleInsnBlock.L2F);
        insnParsersModifable.put("L2D", SimpleInsnBlock.L2D);
        insnParsersModifable.put("F2D", SimpleInsnBlock.F2D);
        insnParsersModifable.put("F2I", SimpleInsnBlock.F2I);
        insnParsersModifable.put("F2L", SimpleInsnBlock.F2L);
        insnParsersModifable.put("LCMP", SimpleInsnBlock.LCMP);
        insnParsersModifable.put("FCMPL", SimpleInsnBlock.FCMPL);
        insnParsersModifable.put("FCMPG", SimpleInsnBlock.FCMPG);
        insnParsersModifable.put("DCMPL", SimpleInsnBlock.DCMPL);
        insnParsersModifable.put("DCMPG", SimpleInsnBlock.DCMPG);
        insnParsersModifable.put("IRETURN", SimpleInsnBlock.IRETURN);
        insnParsersModifable.put("LRETURN", SimpleInsnBlock.LRETURN);
        insnParsersModifable.put("FRETURN", SimpleInsnBlock.FRETURN);
        insnParsersModifable.put("DRETURN", SimpleInsnBlock.DRETURN);
        insnParsersModifable.put("ARETURN", SimpleInsnBlock.ARETURN);
        insnParsersModifable.put("RETURN", SimpleInsnBlock.RETURN);
        insnParsersModifable.put("ARRAYLENGTH", SimpleInsnBlock.ARRAYLENGTH);
        insnParsersModifable.put("ATHROW", SimpleInsnBlock.ATHROW);
        insnParsersModifable.put("MONITOREXIT", SimpleInsnBlock.MONITOREXIT);
        insnParsersModifable.put("MONITORENTER", SimpleInsnBlock.MONITORENTER);

        insnParsersModifable.put("ALOAD", VarInsn.PARSER_ALOAD);
        insnParsersModifable.put("DLOAD", VarInsn.PARSER_DLOAD);
        insnParsersModifable.put("ILOAD", VarInsn.PARSER_ILOAD);
        insnParsersModifable.put("FLOAD", VarInsn.PARSER_FLOAD);
        insnParsersModifable.put("LLOAD", VarInsn.PARSER_LLOAD);
        insnParsersModifable.put("ASTORE", VarInsn.PARSER_ASTORE);
        insnParsersModifable.put("DSTORE", VarInsn.PARSER_DSTORE);
        insnParsersModifable.put("ISTORE", VarInsn.PARSER_ISTORE);
        insnParsersModifable.put("FSTORE", VarInsn.PARSER_FSTORE);
        insnParsersModifable.put("LSTORE", VarInsn.PARSER_LSTORE);

        // Note to self: No, don't try to use "SoftmapContext.INSTRUCTION_PARSERS" here, java (or at least the eclipse compiler) does not like it
        INSTRUCTION_PARSERS = Collections.unmodifiableMap(insnParsersModifable);
    }

    @Nullable
    private static InsnBlock evaluateMethodBodyLine(@NotNull List<@NotNull SoftmapParseError> errors, @NotNull List<@NotNull Token> line) {
        List<@NotNull StringToken> dataTokens = new ArrayList<>();
        for (Token t : line) {
            if (t instanceof StringToken) {
                dataTokens.add((StringToken) t);
            } else if (!(t instanceof CommentToken)) {
                // Hint: comments are discarded / not of relevance
                // [comment-like structures are forbidden and shouldn't occur/be required in the wild, so this assumption is safe]
                errors.add(new SoftmapParseError(t, "Unknown/Unexpected token type: " + t.getClass().getName()));
            }
        }

        if (dataTokens.isEmpty()) {
            // This case can for example occur when making use of comments
            return null;
        }

        String opcode = dataTokens.get(0).getText();

        if (!SoftmapContext.INSTRUCTION_PARSERS.containsKey(opcode)) {
            opcode = opcode.toUpperCase(Locale.ROOT);
            if (!SoftmapContext.INSTRUCTION_PARSERS.containsKey(opcode)) {
                errors.add(new SoftmapParseError(dataTokens.get(0), "Cannot decode instruction line: Unknown/Unsupported opcode"));
                return null;
            }
        }

        InsnParser<? extends InsnBlock> parser = SoftmapContext.INSTRUCTION_PARSERS.get(opcode);
        InsnBlock insn = parser.parseInstruction(dataTokens, errors);
        if (Objects.isNull(insn)) {
            throw new NullPointerException("parser#parseInstruction may not return null for opcode " + opcode + " (parser resolves to instance of type " + parser.getClass().getName() + ")");
        }
        return insn;
    }

    @NotNull
    @Contract(pure = true, value = "null, _, _, _, _ -> fail; !null, _, _, _, _ -> new")
    public static SoftmapContext parse(@NotNull String source, int start, int end, int rowStart, int columnStart) {
        List<Token> tokens = SoftmapContext.tokenize(source, start, end, rowStart, columnStart);
        List<@NotNull MethodExpression> methods = new ArrayList<>();
        List<@NotNull SoftmapParseError> parseErrors = new ArrayList<>();

        int currentVersion = -1;

        for (int readerIndex = 0; readerIndex < tokens.size(); readerIndex++) {
            Token token = tokens.get(readerIndex);
            if (token instanceof CommentToken) {
                continue;
            }

            if (!(token instanceof StringToken)) {
                parseErrors.add(new SoftmapParseError(token.getStart(), token.getEnd(), token.getRow(), token.getColumn(), "Unknown token type: " + token.getClass().getName() + ", expected any of 'softmap [...]', 'method [...]'; Failed to comprehend beginning of expression."));
                continue;
            }

            StringToken stringToken = (StringToken) token;
            if (stringToken.contentMatches(true, "method")) {
                int useVersion = currentVersion;
                if (currentVersion == -1) {
                    useVersion = SoftmapContext.FALLBACK_VERSION;
                    parseErrors.add(new SoftmapParseError(stringToken, "Start of 'method' expression without declaring the format version/header. Expected 'softmap v" + SoftmapContext.FALLBACK_VERSION + "' at this position."));
                }
                readerIndex += SoftmapContext.parseMethod(tokens, readerIndex, useVersion, methods, parseErrors);
            } else if (stringToken.contentMatches(true, "softmap")) {
                StringToken next = null;
                while (++readerIndex < tokens.size()) {
                    Token t = tokens.get(readerIndex);
                    if (t instanceof StringToken) {
                        next = (StringToken) t;
                        break;
                    } else if (!(t instanceof CommentToken)) {
                        parseErrors.add(new SoftmapParseError(t.getStart(), t.getEnd(), t.getRow(), t.getColumn(), "Unknown token type: " + t.getClass().getName() + ", expected expression 'softmap <version>'; failed to resolve '<version>'."));
                        continue;
                    }
                    // Discard comments
                }

                if (next == null) {
                    parseErrors.add(new SoftmapParseError(token.getStart(), token.getEnd(), token.getRow(), token.getColumn(), "Expected expression 'softmap <version>'; failed to resolve '<version>': End of parsing range. Premature end of file?"));
                    continue;
                }

                String versionName = next.getText();
                if (versionName.codePointAt(0) == 'v') {
                    versionName = versionName.substring(1);
                }

                int parsedVersion;
                try {
                    parsedVersion = Integer.parseInt(versionName);
                } catch (NumberFormatException e) {
                    parseErrors.add(new SoftmapParseError(next.getStart(), next.getEnd(), next.getRow(), next.getColumn(), "Incorrect expression 'softmap <version>'; Invalid format for '<version>': Expected any of '<number>', 'v<number>'."));
                    continue;
                }

                if (parsedVersion != 1) {
                    parseErrors.add(new SoftmapParseError(next.getStart(), next.getEnd(), next.getRow(), next.getColumn(), "Incorrect expression 'softmap <version>'; Unknown version. This parser only supports version 1."));
                    continue;
                }

                currentVersion = parsedVersion;
            }
        }

        return new SoftmapContext(Collections.unmodifiableList(methods), Collections.unmodifiableList(parseErrors));
    }

    @Contract(pure = false, mutates = "param4,param5")
    @CheckReturnValue
    private static int parseMethod(@NotNull List<@NotNull Token> tokens, int readerIndex, int version, @NotNull List<@NotNull MethodExpression> out, @NotNull List<@NotNull SoftmapParseError> errors) {
        final int startIndex = readerIndex++;
        if (readerIndex == tokens.size()) {
            errors.add(new SoftmapParseError(tokens.get(startIndex), "Unable to parse method expression: Premature end of token stream. Expected at least the following structure: 'method <class>.<method><descriptor> {}'"));
            return 0;
        }
        Token t = tokens.get(readerIndex);
        while (t instanceof CommentToken) {
            if (readerIndex == tokens.size()) {
                errors.add(new SoftmapParseError(tokens.get(startIndex).getStart(), t.getEnd(), t.getRow(), t.getColumn(), "Unable to parse method expression: Premature end of token stream. Expected at least the following structure: 'method <class>.<method><descriptor> {}'"));
                return readerIndex - startIndex - 1;
            }
            t = tokens.get(++readerIndex);
        }

        StringToken methodLoc;
        StringToken ownerName = null;
        StringToken methodName = null;
        StringToken methodDesc = null;
        if (!(t instanceof StringToken)) {
            methodLoc = null;
            errors.add(new SoftmapParseError(tokens.get(startIndex), "Unable to parse method expression: Unexpected token type when attempting to extract '<class>.<method><descriptor>'. Expected at least the following structure: 'method <class>.<method><descriptor> {}'"));
        } else {
            methodLoc = (StringToken) t;

            int beginName = methodLoc.indexOf('.');
            int beginDesc = methodLoc.indexOf('(');
            int beginRet = methodLoc.indexOf(')') + 1;

            if (beginName == -1) {
                errors.add(new SoftmapParseError(methodLoc, "Unable to parse method expression: The method location definition is invalid, it should be in the format of '<class>.<method><descriptor>'. Missing codepoint '.' (which separates owner name and method name)"));
            } else if (beginDesc == -1) {
                errors.add(new SoftmapParseError(methodLoc, "Unable to parse method expression: The method location definition is invalid, it should be in the format of '<class>.<method><descriptor>'. The descriptor is malformed (missing codepoint '(')"));
            } else if (beginRet == 0) {
                errors.add(new SoftmapParseError(methodLoc.getStart() + beginDesc, methodLoc.getEnd(), methodLoc.getRow(), methodLoc.getColumn() + beginDesc, "Unable to parse method expression: The method location definition is invalid, it should be in the format of '<class>.<method><descriptor>'. The descriptor is malformed (missing codepoint ')')"));
            } else if (beginRet < beginDesc) {
                errors.add(new SoftmapParseError(methodLoc, "Unable to parse method expression: The method location definition is invalid, it should be in the format of '<class>.<method><descriptor>'. The descriptor is malformed (Codepoint ')' is before codepoint '(')"));
            } else if (methodLoc.indexOf('(', beginDesc + 1) != -1) {
                errors.add(new SoftmapParseError(methodLoc, "Unable to parse method expression: The method location definition is invalid, it should be in the format of '<class>.<method><descriptor>'. The descriptor is malformed (duplicate codepoint '(')"));
            } else if (methodLoc.indexOf(')', beginRet) != -1) {
                errors.add(new SoftmapParseError(methodLoc, "Unable to parse method expression: The method location definition is invalid, it should be in the format of '<class>.<method><descriptor>'. The descriptor is malformed (duplicate codepoint ')')"));
            }

            int endName = beginDesc;

            if (beginDesc == -1) {
                endName = methodLoc.getContentLength();
            }

            if (beginName != -1) {
                ownerName = methodLoc.subtoken(0, beginName);
                if (beginName + 1 != endName) {
                    methodName = methodLoc.subtoken(beginName + 1, endName);
                }
            } else if (endName != 0) {
                methodName = methodLoc.subtoken(0, endName);
            }

            if (beginDesc != -1 && beginRet != -1) {
                methodDesc = methodLoc.subtoken(beginDesc, methodLoc.getContentLength());
            }
        }

        t = tokens.get(++readerIndex);
        while (t instanceof CommentToken) {
            if (readerIndex == tokens.size()) {
                int startError;
                if (methodLoc != null) {
                    startError = methodLoc.getEnd() + 1;
                } else {
                    startError = tokens.get(startIndex).getStart();
                }
                errors.add(new SoftmapParseError(startError, t.getEnd(), t.getRow(), t.getColumn(), "Unable to parse method expression: Premature end of token stream. Expected at least the following structure: 'method <class>.<method><descriptor> {}'"));
                return readerIndex - startIndex - 1;
            }
            t = tokens.get(++readerIndex);
        }

        BlockToken startBlockToken = null;
        if (t instanceof StringToken) {
            errors.add(new SoftmapParseError(t, "Unable to parse method expression: Unexpected string token; Ensure that the method location definition does not contain newlines or whitespaces. Expected codepoint at this position is '{'. The following structure is required at minimum: 'method <class>.<method><descriptor> {}'"));
        } else if (!(t instanceof BlockToken)) {
            errors.add(new SoftmapParseError(t, "Unable to parse method expression: Unexpected token type; no further information. Expected codepoint at this position is '{'. The following structure is required at minimum: 'method <class>.<method><descriptor> {}'"));
        } else {
            startBlockToken = (BlockToken) t;
            if (startBlockToken.isEndOfBlock()) {
                errors.add(new SoftmapParseError(startBlockToken, "Unable to parse method expression: Unexpected end of block. Expected codepoint at this position is '{'. The following structure is required at minimum: 'method <class>.<method><descriptor> {}'"));
                return readerIndex - startIndex;
            }
        }

        BlockToken endBlockToken = null;
        final int beginBlockIndex = ++readerIndex;

        for (; readerIndex < tokens.size(); readerIndex++) {
            Token token = tokens.get(readerIndex);
            if (token instanceof BlockToken) {
                BlockToken bToken = (BlockToken) token;
                if (bToken.isStartOfBlock()) {
                    errors.add(new SoftmapParseError(bToken, "Unable to parse method expression body: Unexpected start of block. Was a '}' ommitted in previous lines?"));
                    continue;
                } else {
                    endBlockToken = bToken;
                    break;
                }
            }
        }

        List<@NotNull Token> bodyTokens = tokens.subList(beginBlockIndex, readerIndex);
        List<@NotNull ? extends InsnBlock> insns = SoftmapContext.parseMethodBody(bodyTokens, errors);

        out.add(new MethodExpression((StringToken) tokens.get(startIndex), methodLoc, ownerName, methodName, methodDesc, startBlockToken, endBlockToken, Collections.unmodifiableList(bodyTokens), Collections.unmodifiableList(insns)));

        if (endBlockToken == null) {
            Token last = tokens.get(readerIndex - 1);
            errors.add(new SoftmapParseError(last, "Unable to parse method expression: Premature end of token stream. Expected character at this position is '}'."));
            return readerIndex - startIndex - 1;
        }

        return readerIndex - startIndex;
    }

    @NotNull
    private static List<@NotNull ? extends InsnBlock> parseMethodBody(@NotNull @Unmodifiable List<@NotNull Token> contents, @NotNull List<@NotNull SoftmapParseError> errors) {
        if (contents.isEmpty()) {
            return Collections.<@NotNull InsnBlock>emptyList();
        }

        // We assume that tokens are laid out from top to bottom, left to right. Is that not the case, an
        // exception will be thrown (there is no need in creating a parse error as this would be an error in the parser
        // rather than the contents of the parsed file)

        int currentRow = -1;
        int currentCol = -1;

        List<@NotNull Token> lineBuffer = new ArrayList<>();
        Iterator<Token> it = contents.iterator();
        List<@NotNull InsnBlock> insnBlocks = new ArrayList<>();

        while (it.hasNext()) {
            Token currentToken = it.next();
            boolean endOfLine = false;
            if (currentRow < currentToken.getRow()) {
                if (currentRow != -1) { // The first token shouldn't trigger the previous (non-existing) line to be evaluated
                    endOfLine = true;
                }
                currentRow = currentToken.getRow();
                currentCol = currentToken.getColumn();
            } else if (currentRow == currentToken.getRow()) {
                if (currentCol > currentToken.getColumn()) {
                    throw new IllegalStateException("currentCol = " + currentCol + " > currentToken.getColumn() = " + currentToken.getColumn());
                }
                currentCol = currentToken.getColumn();
            } else {
                throw new IllegalStateException("currentRow = " + currentRow + " > currentToken.getRow() = " + currentToken.getRow());
            }

            if (endOfLine) {
                InsnBlock insnBlock = SoftmapContext.evaluateMethodBodyLine(errors, lineBuffer);
                if (insnBlock != null) {
                    insnBlocks.add(insnBlock);
                }
                lineBuffer.clear();
            }

            lineBuffer.add(currentToken);
        }

        // Flush line buffer (for the last row)
        InsnBlock finalBlock = SoftmapContext.evaluateMethodBodyLine(errors, lineBuffer);
        if (finalBlock != null) {
            insnBlocks.add(finalBlock);
        }

        return insnBlocks;
    }

    @NotNull
    @Contract(pure = true, value = "null, _, _, _, _ -> fail; !null, _, _, _, _ -> new")
    private static List<@NotNull Token> tokenize(@NotNull String source, int codepointStart, int codepointEnd, int row, int col) {
        TokenizeReader reader = new TokenizeReader(Objects.requireNonNull(source, "source may not be null"), codepointStart, codepointEnd, row, col);
        List<@NotNull Token> expressions = new ArrayList<>();

        while (!reader.isExhausted()) {
            reader.consumeWhitespace(expressions);
            if (reader.isExhausted()) {
                break;
            }
            Token token = reader.consumeToken();
            if (token != null) {
                expressions.add(token);
            }
        }

        return expressions;
    }

    @NotNull
    @Unmodifiable
    private final List<@NotNull MethodExpression> methodExpressions;

    @NotNull
    @Unmodifiable
    private final List<@NotNull SoftmapParseError> parseErrors;

    protected SoftmapContext(@NotNull @Unmodifiable List<@NotNull MethodExpression> expressions, @NotNull @Unmodifiable List<@NotNull SoftmapParseError> parseErrors) {
        this.methodExpressions = expressions;
        this.parseErrors = parseErrors;
    }

    @NotNull
    @Unmodifiable
    @Contract(pure = true)
    public List<@NotNull SoftmapParseError> getParseErrors() {
        return this.parseErrors;
    }

    @NotNull
    @Contract(pure = true)
    public ApplicationResult tryApply(@NotNull List<@NotNull ClassNode> obfuscatedNodes) {
        FramedRemapper remapper = new SimpleFramedRemapper(SimpleFramedRemapper.realmsOf(obfuscatedNodes));
        remapper.pushFrame(); // Create the initial frame (this is not done by the remapper on creation as discarding the initial frame allows to )

        Map<String, ClassNode> nodeLookup = new HashMap<>();
        for (ClassNode node : obfuscatedNodes) {
            nodeLookup.put(node.name, node);
        }

        List<@NotNull SoftmapApplicationError> applicationErrors = new ArrayList<>();

        exprLoop:
        for (MethodExpression expr : this.methodExpressions) {
            boolean mapOwnerName = false;
            String mappedOwnerName = null;
            StringToken ownerName = expr.getOwnerName();
            if (ownerName != null) {
                if (ownerName.codepointBefore(ownerName.getContentLength()) == '?') {
                    mapOwnerName = true;
                    mappedOwnerName = ownerName.subtext(0, ownerName.getContentLength() - 1);
                } else {
                    mappedOwnerName = ownerName.getText();
                }
            }

            boolean mapMethodName = false;
            String mappedMethodName = null;
            StringToken methodName = expr.getMethodName();
            if (methodName != null) {
                if (methodName.codepointBefore(methodName.getContentLength()) == '?') {
                    mapMethodName = true;
                    mappedMethodName = methodName.subtext(0, methodName.getContentLength() - 1);
                } else {
                    mappedMethodName = methodName.getText();
                }
            }

            boolean mapMethodDesc = false;
            String mappedMethodDesc = null;
            StringToken methodDesc = expr.getMethodDesc();
            if (methodDesc != null) {
                if (methodDesc.indexOf('?') != -1) {
                    mapMethodDesc = true;
                }
                mappedMethodDesc = methodDesc.getText();
            }

            Iterable<ClassNode> candidateNodes = obfuscatedNodes;
            if (!mapOwnerName && mappedOwnerName != null) {
                ClassNode foundNode = nodeLookup.get(mappedOwnerName);
                if (foundNode == null) {
                    applicationErrors.add(new SoftmapApplicationError(Objects.requireNonNull(ownerName), "No class exists with this name"));
                    continue;
                }
                candidateNodes = Collections.singleton(foundNode);
            }

            RemapperFrame completeFrameFrame = null;
            MethodLoc completeFrameLoc = null;
            int furthestInsns = -1;
            MatchResult furthestError = null;
            boolean furthestExhaustedInstructions = false;
            List<MethodLoc> visitedMethods = new ArrayList<>();

            for (ClassNode node : candidateNodes) {
                methodLoop:
                for (MethodNode method : node.methods) {
                    if (!mapMethodName && mappedMethodName != null && !method.name.equals(mappedMethodName)) {
                        continue;
                    }
                    if (!mapMethodDesc && mappedMethodDesc != null && !method.desc.equals(mappedMethodDesc)) {
                        continue;
                    }
                    if (remapper.getFrameCount() != 1) {
                        throw new IllegalStateException("Unexpected frame count: " + remapper.getFrameCount());
                    }
                    remapper.pushFrame();
                    if (mapOwnerName) {
                        String nameSrc = node.name;
                        String nameDst = remapper.getMappedClassOpt(nameSrc);
                        if (!nameSrc.equals(nameDst)) {
                            if (!Objects.requireNonNull(mappedOwnerName).equals(nameDst)) {
                                remapper.discardFrame();
                                continue;
                            }
                        } else {
                            remapper.mapClass(nameSrc, Objects.requireNonNull(mappedOwnerName));
                        }
                    }
                    if (mapMethodName) {
                        String nameSrc = method.name;
                        String nameDst = remapper.getMappedMethodOpt(node.name, nameSrc, method.desc);
                        if (!nameSrc.equals(nameDst)) {
                            if (!Objects.requireNonNull(mappedMethodName).equals(nameDst)) {
                                remapper.discardFrame();
                                continue;
                            }
                        } else {
                            remapper.mapMethod(node.name, nameSrc, method.desc, Objects.requireNonNull(mappedMethodName));
                        }
                    }
                    if (mapMethodDesc) {
                        // Using InvokeInsn's mapDescriptor method isn't too ideal, but writing very similar code
                        // pretty much twice to thrice is not what I have in mind, so reusing an implementation
                        // is better in the short term and is better for those that wish to maintain this software.
                        if (InvokeInsn.mapDescriptor(Objects.requireNonNull(methodDesc), method.desc, remapper) != null) {
                            remapper.discardFrame();
                            continue;
                        }
                    }

                    if (remapper.getFrameCount() != 2) {
                        throw new IllegalStateException("Unexpected frame count: " + remapper.getFrameCount());
                    }
                    visitedMethods.add(new MethodLoc(node.name, method.name, method.desc));

                    List<? extends @NotNull InsnBlock> insnBlocks = expr.getInsns();
                    int i = 0;
                    AbstractInsnNode currentInsn = method.instructions.getFirst();
                    MatchResult lastResult = null;
                    while (i != insnBlocks.size() && currentInsn != null) {
                        InsnBlock currentBlock = insnBlocks.get(i);
                        remapper.pushFrame();
                        if (remapper.getFrameCount() != 3) {
                            throw new IllegalStateException("Unexpected frame count: " + remapper.getFrameCount());
                        }
                        MatchResult result = currentBlock.matchesInstruction(currentInsn, remapper);
                        if (result.isBreakingMatching()) {
                            i++;
                            remapper.mergeFrame();
                        } else if (result.isGreedyMatch() && insnBlocks.size() != i + 1) {
                            result = insnBlocks.get(i + 1).matchesInstruction(currentInsn, remapper);
                            if (result.isBreakingMatching()) {
                                remapper.mergeFrame();
                                i += 2;
                            } else if (result.isContinuingMatching() || result.isGreedyMatch()) {
                                i++;
                                remapper.mergeFrame();
                            } else if (result.isAnyMatch()) {
                                throw new IllegalStateException("Unexpected positive-match type (problem in the softmap implementation code - please report this bug):" + result.toString());
                            } else {
                                lastResult = result;
                                remapper.discardFrame();
                            }
                        } else if (result.isGreedyMatch() /* Final greedy matcher */ || result.isContinuingMatching()) {
                            // NOP
                            remapper.mergeFrame();
                            lastResult = result;
                        } else if (result.isAnyMatch()) {
                            throw new IllegalStateException("Unexpected positive-match type (problem in the softmap implementation code - please report this bug):" + result.toString());
                        } else {
                            lastResult = result;
                            remapper.discardFrame();
                            break;
                        }
                        currentInsn = currentInsn.getNext();
                    }

                    // Hint: i != insnBlocks.size()  would have issues when matching the final RETURN for example
                    // People are expected to use '*' if the instructions after some point don't matter.
                    if (lastResult != null && !lastResult.isGreedyMatch()) {
                        if (i > furthestInsns) {
                            furthestInsns = i;
                            furthestError = lastResult;
                            furthestExhaustedInstructions = currentInsn == null;
                        }
                        remapper.discardFrame();
                        continue methodLoop; // We only use continue with labels for clarity, but this isn't actually necessary
                    }

                    MethodLoc newLoc = new MethodLoc(node.name, method.name, method.desc);

                    if (completeFrameLoc != null) {
                        Token errorSource = expr.getMethodLocation();
                        if (errorSource == null) {
                            errorSource = expr.getDeclaringLocation();
                        }
                        String sourceA = node.name + '.' + method.name + method.desc;
                        String sourceB = node.name.equals(completeFrameLoc.getOwner()) ? "*" : completeFrameLoc.getOwner();
                        sourceB += method.name.equals(completeFrameLoc.getName()) ? "*" : completeFrameLoc.getName();
                        sourceB += method.desc.equals(completeFrameLoc.getDesc()) ? "*" : completeFrameLoc.getDesc();
                        applicationErrors.add(new SoftmapApplicationError(errorSource, "Multiple methods match the expression. Two of potentially multiple matches: '" + sourceA + "' and '" + sourceB + "'."));
                        remapper.discardFrame();
                        continue exprLoop;
                    }

                    completeFrameLoc = newLoc;
                    completeFrameFrame = remapper.popFrame();
                }
            }

            if (completeFrameFrame != null) {
                remapper.pushFrame(completeFrameFrame);
                remapper.mergeFrame();
            } else if (furthestError != null) {
                Token errorSource = furthestError.getErrorLocation();
                if (errorSource == null) {
                    errorSource = expr.getDeclaringLocation();
                }
                if (furthestExhaustedInstructions) {
                    applicationErrors.add(new SoftmapApplicationError(errorSource, "Instructions exhausted after evaluating " + furthestInsns + " insn blocks. Beware that the supplied error message and error location may not be the ultimate cause of the issue. Provided error message: " + furthestError.getErrorDescription()));
                } else {
                    applicationErrors.add(new SoftmapApplicationError(errorSource, "InsnBlock failed match after evaluating " + furthestInsns + " insn blocks. Beware that the supplied error message and error location may not be the ultimate cause of the issue. Provided error message: " + furthestError.getErrorDescription()));
                }
            } else {
                Token errorSource = expr.getMethodLocation();
                if (errorSource == null) {
                    errorSource = expr.getDeclaringLocation();
                }
                applicationErrors.add(new SoftmapApplicationError(errorSource, "No methods match the expression. Consider double-checking for typos and cross-reference the supplied method owner, name and descriptor with the bytecode owner, name and descriptor. Visited methods: " + visitedMethods));
            }
        }

        List<@NotNull String> tiny = remapper.exportToTinyV1();
        remapper.discardFrame(); // Destroy the initial frame
        return new ApplicationResult(tiny, Collections.unmodifiableList(applicationErrors));
    }
}
