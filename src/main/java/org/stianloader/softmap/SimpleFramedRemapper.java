package org.stianloader.softmap;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class SimpleFramedRemapper implements FramedRemapper {

    public static class MethodLoc {
        @NotNull
        private final String desc;
        @NotNull
        private final String name;
        @NotNull
        private final String owner;

        public MethodLoc(@NotNull String owner, @NotNull String name, @NotNull String desc) {
            this.owner = owner;
            this.name = name;
            this.desc = desc;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof MethodLoc)) {
                return false;
            }
            MethodLoc other = (MethodLoc) obj;
            return other.owner.equals(this.owner) && other.name.equals(this.name) && other.desc.equals(this.desc);
        }

        @NotNull
        public String getDesc() {
            return this.desc;
        }

        @NotNull
        public String getName() {
            return this.name;
        }

        @NotNull
        public String getOwner() {
            return this.owner;
        }

        @Override
        public int hashCode() {
            return this.owner.hashCode() ^ this.name.hashCode() ^ this.desc.hashCode();
        }

        @Override
        public String toString() {
            return this.owner + '.' + this.name + ' ' + this.desc;
        }
    }

    public static class MethodRealm {
        @NotNull
        private final String declaringClass;
        @SuppressWarnings("unused") // API reserved for future use
        @NotNull
        private final String methodDesc;
        @SuppressWarnings("unused") // API reserved for future use
        @NotNull
        private final String methodName;
        @NotNull
        private final Set<@NotNull String> realmMembers;

        public MethodRealm(@NotNull String declaringClass, @NotNull String methodName, @NotNull String methodDesc, @NotNull Set<@NotNull String> realmMembers) {
            this.declaringClass = declaringClass;
            this.methodName = methodName;
            this.methodDesc = methodDesc;
            this.realmMembers = realmMembers;
        }
    }

    private static class RemappingFrame implements FramedRemapper.RemapperFrame {

        @NotNull
        private final Map<String, String> classNameMappings = new HashMap<>();

        /**
         * Storage of field and method mappings for the current remapping frame.
         *
         * <p>We are cheating here by abusing the fact that methods and fields have the same owner, name & desc structure
         *  - ultimately they differ only by desc
         *
         * <p>Warning: For methods only the method realm owner is stored.
         * This is to remove the need of duplicate entries and to avoid dangerous mapping anomalies.
         * As fields cannot be overridden, there is no such concept for fields.
         */
        @NotNull
        private final Map<@NotNull MethodLoc, String> methodFieldMappings = new HashMap<>();
    }

    /**
     * <p>A helper method that assembles a map where for a Map with a key of A with a Set as a value, a Map
     * is returned where the keys are the same but the Set also contains all values that would be addressable
     * by either A or a value of the Set represented by key A (even recursively).
     *
     * <p>A small generic method  I wrote since I am fed up writing such algorithms by hand all the time
     * (it is remarkable how often one needs to assemble dependency trees)
     *
     * <p> Not very performant, but should do the job as a temporary band-aid fix until I feel like improving the performance
     *
     * <p>Null set values are ignored and treated like an empty set (which in turn has the same behaviour as
     * there being no entry in the first place).
     *
     * @param <T> The type of the key as well as the values of the Set
     * @param input The input map to do the computation on. May not be modified while running the method
     * @return The output map which follows the semantics described above
     */
    @NotNull
    @Contract(pure = true, value = "null -> fail; !null -> new")
    @Unmodifiable
    private static <T> Map<T, @NotNull Set<T>> collect(@Unmodifiable @NotNull Map<T, ? extends @Nullable Set<T>> input) {
        Map<T, @NotNull Set<T>> mapOut = new HashMap<>();
        Queue<T> queue = new ArrayDeque<>();
        for (T key : input.keySet()) {
            Set<T> collected = new HashSet<>();
            queue.addAll(input.get(key));
            while (!queue.isEmpty()) {
                T queued = queue.remove();
                if (!collected.add(queued)) {
                    continue;
                }
                if (mapOut.containsKey(queued)) {
                    collected.addAll(mapOut.get(queued));
                    continue;
                }
                Set<T> set = input.get(queued);
                if (set != null) {
                    collected.addAll(set);
                    queue.addAll(set);
                }
            }
            mapOut.put(key, collected);
        }
        return Collections.unmodifiableMap(mapOut);
    }

    @NotNull
    @Unmodifiable
    public static Map<@NotNull MethodLoc, @NotNull MethodRealm> realmsOf(@Unmodifiable @NotNull List<@NotNull ClassNode> nodes) {
        Map<@NotNull String, Set<@NotNull String>> immediateChildren = new HashMap<>();
        Map<@NotNull String, ClassNode> nodeLookup = new HashMap<>();
        for (ClassNode node : nodes) {
            nodeLookup.put(node.name, node);
            BiFunction<String, Set<String>, Set<String>> combiner = (key, children) -> {
                if (children == null) {
                    children = new TreeSet<>();
                }
                children.add(node.name);
                return children;
            };
            immediateChildren.compute(node.superName, combiner);
            for (String interfaceName : node.interfaces) {
                immediateChildren.compute(interfaceName, combiner);
            }
        }

        Map<@NotNull String, @NotNull Set<@NotNull String>> allChildren = SimpleFramedRemapper.collect(immediateChildren);

        // Ensure that we go by parent classes first, then go to the respective children
        TreeSet<@NotNull String> applyOrder = new TreeSet<>((e1, e0) -> {
            int hiOrder = allChildren.getOrDefault(e0, Collections.emptySet()).size() - allChildren.getOrDefault(e1, Collections.emptySet()).size();
            return hiOrder == 0 ? e1.compareTo(e0) : hiOrder;
        });

        // Ensure only obfuscated nodes are applied (e.g. ignore java/lang/Object).
        // This assumes that users won't try to map deobfuscated names, but that could be handled separately in the future
        applyOrder.addAll(nodeLookup.keySet());

        Map<@NotNull MethodLoc, @NotNull MethodRealm> realms = new HashMap<>();
        for (String superType : applyOrder) {
            // Note: non-obfuscated classes won't be present here, nor will they be in the set of children classes
            ClassNode superNode = nodeLookup.get(superType);
            for (MethodNode superMethod : superNode.methods) {
                MethodLoc myLoc = new MethodLoc(superType, superMethod.name, superMethod.desc);
                if (realms.containsKey(myLoc)) {
                    // Someone (likely a supertype) already added the entry - safe to assume it is being overwritten by this class
                    continue;
                }
                if ((superMethod.access & Opcodes.ACC_STATIC) != 0 || (superMethod.access & Opcodes.ACC_PRIVATE) != 0) {
                    // ACC_STATIC or ACC_PRIVATE is set - no need for inheritance
                    // -> The list is as such immutable
                    realms.put(myLoc, new MethodRealm(superType, superMethod.name, superMethod.desc, Collections.singleton(superType)));
                } else if ((superMethod.access & Opcodes.ACC_PUBLIC) != 0 || (superMethod.access & Opcodes.ACC_PROTECTED) != 0) {
                    // ACC_PUBLIC or ACC_PROTECTED is set (both behave the same as far as overrides are concerned)
                    // -> Exposed to all children (ACC_FINAL is irrelevant as far as I know, nor are bridge methods)
                    Set<@NotNull String> children = allChildren.getOrDefault(superType, Collections.emptySet());
                    Set<@NotNull String> realmMembers;
                    if (children.isEmpty()) {
                        realmMembers = Collections.singleton(superType);
                    } else {
                        realmMembers = new HashSet<>(children);
                        realmMembers.add(superType);
                    }
                    MethodRealm realm = new MethodRealm(superType, superMethod.name, superMethod.desc, realmMembers);
                    realms.put(myLoc, realm);
                    for (String child : children) {
                        realms.put(new MethodLoc(child, superMethod.name, superMethod.desc), realm);
                    }
                } else {
                    // package-protected access (no explicit access flags set) - this is where it gets more complicated
                    // as children could expand the access to ACC_PUBLIC or ACC_PROTECTED
                    // However, one still needs to be aware that in order for ACC_PUBLIC or ACC_PROTECTED to work in that way,
                    // the class that widens the access must be in the same package as the defining class.
                    Set<@NotNull String> realmAccess = new TreeSet<>();
                    Set<@NotNull String> children = allChildren.getOrDefault(superType, Collections.emptySet());
                    int lastSlashSuper = superType.lastIndexOf('/');
                    realmAccess.add(superType);
                    for (String child : children) {
                        int lastSlashChild = child.lastIndexOf('/');
                        if (lastSlashChild != lastSlashSuper || !child.regionMatches(0, superType, 0, lastSlashChild)) {
                            continue;
                        }
                        realmAccess.add(child);
                        ClassNode childNode = nodeLookup.get(child);
                        if (childNode == null) {
                            // Won't happen, but doesn't hurt to have it in there regardless
                            continue;
                        }
                        for (MethodNode method : childNode.methods) {
                            if (!method.name.equals(superMethod.name) || !method.desc.equals(superMethod.desc)) {
                                continue;
                            }
                            if ((method.access & Opcodes.ACC_PUBLIC) != 0 || (method.access & Opcodes.ACC_PROTECTED) != 0) {
                                // Widened access
                                realmAccess.addAll(allChildren.getOrDefault(child, Collections.emptySet()));
                            }
                        }
                    }

                    MethodRealm realm = new MethodRealm(superType, superMethod.name, superMethod.desc, realmAccess);
                    for (String realmType : realmAccess) {
                        realms.put(new MethodLoc(realmType, superMethod.name, superMethod.desc), realm);
                    }
                }
                if (!realms.containsKey(myLoc)) {
                    System.err.println("???? " + myLoc);
                }
            }
        }

        return Collections.unmodifiableMap(realms);
    }

    @NotNull
    private final Queue<RemappingFrame> frames = Collections.asLifoQueue(new ArrayDeque<>());

    @NotNull
    @Unmodifiable
    private final Map<MethodLoc, MethodRealm> realms;

    public SimpleFramedRemapper(@NotNull @Unmodifiable Map<MethodLoc, MethodRealm> realms) {
        this.realms = realms;
    }

    @Override
    public void discardFrame() {
        this.frames.remove();
    }

    @Override
    @NotNull
    @Contract(pure = true, value = "-> new")
    public @Unmodifiable List<@NotNull String> exportToTinyV1() {
        List<@NotNull String> tiny = new ArrayList<>();
        for (RemappingFrame frame : this.frames) {
            for (String key : frame.classNameMappings.keySet()) {
                tiny.add("CLASS\t" + key + '\t' + frame.classNameMappings.get(key));
            }
            for (MethodLoc key : frame.methodFieldMappings.keySet()) {
                if (key.desc.codePointAt(0) == '(') {
                    // Method
                    // Large parts of the stianloader toolchain incorrectly used to use "<owner> <name> <desc> <name>",
                    // but that is not correct.
                    // Why exactly this mistake was introduced in the first place and why it was never really fixed is a bit beyond me.
                    // While some might advocate sticking to the bugged behaviour, it is ultimately the goal of completely reverting
                    // this bugged behaviour and returning to valid tinyv1 files.
                    // Incidentally recaf 3X does the same mistake but for fields instead of methods. Why two independent implementations
                    // made similar mistakes begs the question of how well tinyv1 is defined and how great the deviations are between
                    // implementation to implementation.

                    // Simpler/Dumber tools may completely discard the existence of inheritance, so we shall remap the entire
                    // method realm, even though that may be counterproductive in terms of performance and usefulness to more
                    // robust remappers or other well-written tools consuming tinyV1 files.
                    MethodRealm realm = this.realms.get(key);
                    if (!realm.realmMembers.contains(realm.declaringClass)) {
                        throw new AssertionError("Declaring class not in realm members: " + realm.declaringClass + " for " + key);
                    }
                    for (String realmMember : realm.realmMembers) {
                        tiny.add("METHOD\t" + realmMember + '\t' + key.desc + '\t' + key.name + '\t' + frame.methodFieldMappings.get(key));
                    }
                } else {
                    // Field
                    tiny.add("FIELD\t" + key.owner + '\t' + key.desc + '\t' + key.name + '\t' + frame.methodFieldMappings.get(key));
                }
            }
        }
        return Collections.unmodifiableList(tiny);
    }

    @Override
    public int getFrameCount() {
        return this.frames.size();
    }

    @Override
    @Nullable
    @Contract(pure = true)
    public String getMappedClass(@NotNull String srcName) {
        String mapping = srcName;
        for (RemappingFrame frame : this.frames) {
            mapping = frame.classNameMappings.getOrDefault(srcName, mapping);
        }
        if (mapping == srcName) { // Instance comparison intended
            return null;
        } else {
            return mapping;
        }
    }

    @Override
    @Nullable
    public String getMappedField(@NotNull String srcNameOwner, @NotNull String srcNameField, @NotNull String srcDescField) {
        String mapping = srcNameField;
        MethodLoc loc = new MethodLoc(srcNameOwner, srcNameField, srcDescField);
        for (RemappingFrame frame : this.frames) {
            mapping = frame.methodFieldMappings.getOrDefault(loc, mapping);
        }
        if (mapping == srcNameOwner) { // Instance comparison intended
            return null;
        } else {
            return mapping;
        }
    }

    @Override
    @Nullable
    @Contract(pure = true)
    public String getMappedMethod(@NotNull String srcNameOwner, @NotNull String srcNameMethod, @NotNull String srcDescMethod) {
        if (srcDescMethod.codePointAt(0) != '(') {
            throw new IllegalStateException("Method " + srcNameOwner + "." + srcNameMethod + " " + srcDescMethod + " is not a method. (illegal desc)");
        }
        String mapping = srcNameMethod;
        MethodRealm realm = this.realms.get(new MethodLoc(srcNameOwner, srcNameMethod, srcDescMethod));
        if (realm == null) {
            throw new IllegalStateException("Realm may not be null for methodLoc " + new MethodLoc(srcNameOwner, srcNameMethod, srcDescMethod));
        }
        srcNameOwner = realm.declaringClass;
        MethodLoc loc = new MethodLoc(srcNameOwner, srcNameMethod, srcDescMethod);
        for (RemappingFrame frame : this.frames) {
            mapping = frame.methodFieldMappings.getOrDefault(loc, mapping);
        }
        if (mapping == srcNameOwner) { // Instance comparison intended
            return null;
        } else {
            return mapping;
        }
    }

    @Override
    @Contract(pure = false)
    public void mapClass(@NotNull String srcOwner, @NotNull String dstOwner) {
        RemappingFrame frame = this.frames.peek();
        if (frame == null) {
            throw new NoSuchElementException("No frame to edit");
        }

        if (srcOwner.codePointBefore(srcOwner.length()) == ';' || srcOwner.codePointAt(0) == '[') {
            throw new IllegalArgumentException("Illegal owner for the source namespace: " + srcOwner);
        }

        if (dstOwner.codePointBefore(dstOwner.length()) == ';' || dstOwner.codePointAt(0) == '[') {
            throw new IllegalArgumentException("Illegal owner for the destination namespace: " + dstOwner);
        }

        frame.classNameMappings.put(srcOwner, dstOwner);
    }

    @Override
    public void mapField(@NotNull String owner, @NotNull String srcName, @NotNull String desc, @NotNull String dstName) {
        RemappingFrame frame = this.frames.peek();
        if (frame == null) {
            throw new NoSuchElementException("No frame to edit");
        }

        frame.methodFieldMappings.put(new MethodLoc(owner, srcName, desc), dstName);
    }

    @Override
    @Contract(pure = false)
    public void mapMethod(@NotNull String owner, @NotNull String srcName, @NotNull String desc, @NotNull String dstName) {
        RemappingFrame frame = this.frames.peek();
        if (frame == null) {
            throw new NoSuchElementException("No frame to edit");
        }

        owner = this.realms.get(new MethodLoc(owner, srcName, desc)).declaringClass;
        frame.methodFieldMappings.put(new MethodLoc(owner, srcName, desc), dstName);
    }

    @Override
    @Contract(pure = false)
    public void mergeFrame() {
        if (this.frames.size() < 2) {
            throw new IllegalStateException("In order to be able to merge frames, at least two frames have to exist");
        }

        RemappingFrame topFrame = this.frames.remove();
        RemappingFrame bottomFrame = this.frames.element();

        bottomFrame.classNameMappings.putAll(topFrame.classNameMappings);
        bottomFrame.methodFieldMappings.putAll(topFrame.methodFieldMappings);
    }

    @Override
    @Contract(pure = false)
    public FramedRemapper.@NotNull RemapperFrame popFrame() {
        return this.frames.remove();
    }

    @Override
    @Contract(pure = false)
    public void pushFrame() {
        this.frames.add(new RemappingFrame());
    }

    @Override
    @Contract(pure = false)
    public void pushFrame(FramedRemapper.@NotNull RemapperFrame frame) {
        this.frames.add(Objects.requireNonNull((RemappingFrame) frame));
    }
}
