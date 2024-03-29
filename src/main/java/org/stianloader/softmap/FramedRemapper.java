package org.stianloader.softmap;

import java.util.List;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

public interface FramedRemapper {
    public static interface RemapperFrame {
        /* This is a marker interface and doesn't do much on it's own */
    }

    @Contract(pure = false)
    void discardFrame();

    @Contract(pure = true, value = "-> new")
    @NotNull
    @Unmodifiable
    List<@NotNull String> exportToTinyV1();

    @Contract(pure = true)
    int getFrameCount();

    @Nullable
    @Contract(pure = true)
    String getMappedClass(@NotNull String srcName);

    @NotNull
    @Contract(pure = true)
    default String getMappedClassOpt(@NotNull String srcName) {
        return this.getMappedClassOpt(srcName, srcName);
    }

    @NotNull
    @Contract(pure = true)
    default String getMappedClassOpt(@NotNull String srcName, @NotNull String defaultName) {
        String dstName = this.getMappedClass(srcName);
        return dstName == null ? defaultName : dstName;
    }

    @Nullable
    @Contract(pure = true)
    String getMappedField(@NotNull String srcNameOwner, @NotNull String srcNameField, @NotNull String srcDescField);

    @NotNull
    @Contract(pure = true)
    default String getMappedFieldOpt(@NotNull String srcNameOwner, @NotNull String srcNameField, @NotNull String srcDescField) {
        return this.getMappedFieldOpt(srcNameOwner, srcNameField, srcDescField, srcNameField);
    }

    @NotNull
    @Contract(pure = true)
    default String getMappedFieldOpt(@NotNull String srcNameOwner, @NotNull String srcNameField, @NotNull String srcDescField, @NotNull String defaultName) {
        String dstName = this.getMappedField(srcNameOwner, srcNameField, srcDescField);
        return dstName == null ? defaultName : dstName;
    }

    @Nullable
    @Contract(pure = true)
    String getMappedMethod(@NotNull String srcNameOwner, @NotNull String srcNameMethod, @NotNull String srcDescMethod);

    @NotNull
    @Contract(pure = true)
    default String getMappedMethodOpt(@NotNull String srcNameOwner, @NotNull String srcNameMethod, @NotNull String srcDescMethod) {
        return this.getMappedMethodOpt(srcNameOwner, srcNameMethod, srcDescMethod, srcNameMethod);
    }

    @NotNull
    @Contract(pure = true)
    default String getMappedMethodOpt(@NotNull String srcNameOwner, @NotNull String srcNameMethod, @NotNull String srcDescMethod, @NotNull String defaultName) {
        String dstName = this.getMappedMethod(srcNameOwner, srcNameMethod, srcDescMethod);
        return dstName == null ? defaultName : dstName;
    }

    @Contract(pure = false)
    void mapClass(@NotNull String srcOwner, @NotNull String dstOwner);

    @Contract(pure = false)
    void mapField(@NotNull String owner, @NotNull String srcName, @NotNull String desc, @NotNull String dstName);

    @Contract(pure = false)
    void mapMethod(@NotNull String owner, @NotNull String srcName, @NotNull String desc, @NotNull String dstName);

    @Contract(pure = false)
    void mergeFrame();

    @Contract(pure = false)
    @NotNull RemapperFrame popFrame();

    @Contract(pure = false)
    void pushFrame();

    /**
     * Push a discarded frame back into the remapper as the uppermost frame.
     *
     * <p>The remapper is not required to accept any implementation of {@link RemapperFrame},
     * as that interface is more or less a marker interface; However this method
     * should accept any {@link RemapperFrame} instances emitted by {@link #popFrame()}.
     *
     * @param frame The frame to push into the stack.
     */
    @Contract(pure = false)
    void pushFrame(@NotNull RemapperFrame frame);
}
