package org.polyfrost.flashbacklegacy.patch;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;

public final class FlashbackPatcher {
    public static final String SHIM_PREFIX = "org/polyfrost/flashbacklegacy/shim/";
    public static final String PATCHED_JAR = "flashback-patched.jar";

    private static final String[] SHIMMED_PACKAGES = {
            "net/minecraft/",
            "com/mojang/blaze3d/",
            "com/mojang/math/",
            "com/mojang/authlib/",
            "com/mojang/logging/",
            "com/mojang/realmsclient/",
            "com/mojang/renderpearl/",
            "net/fabricmc/fabric/",
    };

    private static final Remapper RELOCATOR = new Remapper(Opcodes.ASM9) {
        @Override
        public String map(String internalName) {
            for (String pkg : SHIMMED_PACKAGES) {
                if (internalName.startsWith(pkg)) return SHIM_PREFIX + internalName;
            }
            return internalName;
        }
    };

    private static final String LIBRARY_COMPAT = "org/polyfrost/flashbacklegacy/compat/LibraryCompat";

    private FlashbackPatcher() {
    }

    public static void main(String[] args) throws IOException {
        patch(Path.of(args[0]), Path.of(args[1]));
    }

    public static List<Path> patch(Path flashbackJar, Path outDir) throws IOException {
        Files.createDirectories(outDir);
        Path patched = outDir.resolve(PATCHED_JAR);
        List<Path> classpath = new ArrayList<>();
        classpath.add(patched);
        try (InputStream in = Files.newInputStream(flashbackJar);
             ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(patched))) {
            copy(in, out, new HashSet<>(), outDir, classpath);
        }
        return classpath;
    }

    private static void copy(InputStream jar, ZipOutputStream out, Set<String> seen, Path outDir, List<Path> classpath) throws IOException {
        ZipInputStream in = new ZipInputStream(jar);
        for (ZipEntry entry; (entry = in.getNextEntry()) != null; ) {
            String name = entry.getName();
            if (entry.isDirectory() || isDropped(name)) continue;
            byte[] bytes = in.readAllBytes();

            if (name.startsWith("META-INF/jars/")) {
                String jarName = name.substring("META-INF/jars/".length());
                if (jarName.startsWith("lattice-")) {
                    copy(new ByteArrayInputStream(bytes), out, seen, outDir, classpath);
                } else if (jarName.startsWith("commons-math3-")) {
                    Path lib = outDir.resolve(jarName);
                    Files.write(lib, bytes);
                    classpath.add(lib);
                }
                continue;
            }

            if (!seen.add(name)) continue;
            out.putNextEntry(new ZipEntry(name));
            out.write(name.endsWith(".class") ? relocate(bytes) : bytes);
            out.closeEntry();
        }
    }

    private static boolean isDropped(String name) {
        return name.equals("fabric.mod.json")
                || name.endsWith(".mixins.json")
                || name.endsWith(".accesswidener")
                || name.equals("META-INF/MANIFEST.MF")
                || name.matches("META-INF/[^/]+\\.(SF|RSA|DSA|EC)")
                || name.matches("com/moulberry/[^/]+/mixin/.*")
                || name.matches("com/moulberry/flashback/playback/ReplayServer(\\$.*)?\\.class")
                || name.matches("com/moulberry/flashback/editor/ui/CustomImGuiImplB3D(\\$.*)?\\.class");
    }

    public static byte[] relocate(byte[] bytes) {
        ClassWriter writer = new ClassWriter(0);
        new ClassReader(bytes).accept(new ClassRemapper(new FieldAccessors(writer), RELOCATOR), 0);
        return writer.toByteArray();
    }

    private static final class FieldAccessors extends ClassVisitor {
        private static final Map<String, Optional<ClassNode>> SHIMS = new HashMap<>();

        FieldAccessors(ClassVisitor next) {
            super(Opcodes.ASM9, next);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            return new MethodVisitor(Opcodes.ASM9, super.visitMethod(access, name, desc, signature, exceptions)) {
                @Override
                public void visitMethodInsn(int opcode, String owner, String method, String type, boolean itf) {
                    String compat = opcode == Opcodes.INVOKESTATIC ? type : "(L" + owner + ";" + type.substring(1);
                    if (!owner.startsWith(SHIM_PREFIX) && opcode != Opcodes.INVOKESPECIAL && declaresMethod(LIBRARY_COMPAT, method + compat)) {
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, LIBRARY_COMPAT, method, compat, false);
                        return;
                    }
                    super.visitMethodInsn(opcode, owner, declaresMethod(owner, method + "$" + type) ? method + "$" : method, type, itf);
                }

                @Override
                public void visitFieldInsn(int opcode, String owner, String field, String type) {
                    if (!owner.startsWith(SHIM_PREFIX) || declaresField(owner, field)) {
                        super.visitFieldInsn(opcode, owner, field, type);
                        return;
                    }
                    boolean itf = shim(owner).map(node -> (node.access & Opcodes.ACC_INTERFACE) != 0).orElse(false);
                    switch (opcode) {
                        case Opcodes.GETFIELD -> super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, "get$" + field, "()" + type, false);
                        case Opcodes.PUTFIELD -> super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, "set$" + field, "(" + type + ")V", false);
                        case Opcodes.GETSTATIC -> super.visitMethodInsn(Opcodes.INVOKESTATIC, owner, "get$" + field, "()" + type, itf);
                        default -> super.visitMethodInsn(Opcodes.INVOKESTATIC, owner, "set$" + field, "(" + type + ")V", itf);
                    }
                }
            };
        }

        private static boolean declaresField(String owner, String field) {
            for (Optional<ClassNode> node = shim(owner); node.isPresent(); node = shim(node.get().superName)) {
                if (node.get().fields.stream().anyMatch(f -> f.name.equals(field) && (f.access & Opcodes.ACC_PUBLIC) != 0)) return true;
                for (String itf : node.get().interfaces) if (declaresField(itf, field)) return true;
            }
            return false;
        }

        private static boolean declaresMethod(String owner, String method) {
            for (Optional<ClassNode> node = shim(owner); node.isPresent(); node = shim(node.get().superName)) {
                if (node.get().methods.stream().anyMatch(m -> method.equals(m.name + m.desc))) return true;
            }
            return false;
        }

        private static Optional<ClassNode> shim(String name) {
            return SHIMS.computeIfAbsent(name, n -> {
                if (n == null || !n.startsWith(SHIM_PREFIX) && !n.equals(LIBRARY_COMPAT)) return Optional.empty();
                try (InputStream in = FlashbackPatcher.class.getClassLoader().getResourceAsStream(n + ".class")) {
                    if (in == null) return Optional.empty();
                    ClassNode node = new ClassNode();
                    new ClassReader(in).accept(node, ClassReader.SKIP_CODE);
                    return Optional.of(node);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }
}
