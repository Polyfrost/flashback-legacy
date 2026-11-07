package org.polyfrost.flashbacklegacy.patch;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

public final class ShimStubs {
    public static final String HIERARCHY = "flashback_legacy/shim-hierarchy.txt";
    public static final String STUBS_JAR = "flashback-stubs.jar";

    private ShimStubs() {
    }

    public static void main(String[] args) throws IOException {
        Path work = Files.createTempDirectory("flashback-stubs");
        Set<String> names = new HashSet<>();
        Set<String> interfaces = new HashSet<>();
        try (ZipFile patched = new ZipFile(FlashbackPatcher.patch(Path.of(args[0]), work).getFirst().toFile())) {
            for (ZipEntry entry : patched.stream().toList()) {
                if (entry.getName().endsWith(".class")) collect(patched.getInputStream(entry), names, interfaces);
            }
        }

        TreeMap<String, String> lines = new TreeMap<>();
        try (ZipFile minecraft = new ZipFile(args[1])) {
            List<String> queue = new ArrayList<>(names);
            while (!queue.isEmpty()) {
                String name = queue.removeLast();
                if (lines.containsKey(name)) continue;
                ZipEntry entry = minecraft.getEntry(name.substring(FlashbackPatcher.SHIM_PREFIX.length()) + ".class");
                if (entry == null) {
                    boolean itf = interfaces.contains(name);
                    lines.put(name, (itf ? Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT : 0) + " " + name + " java/lang/Object");
                    continue;
                }
                ClassReader reader = new ClassReader(FlashbackPatcher.relocate(minecraft.getInputStream(entry).readAllBytes()));
                StringBuilder line = new StringBuilder().append(reader.getAccess()).append(' ').append(name).append(' ').append(reader.getSuperName());
                for (String itf : reader.getInterfaces()) line.append(' ').append(itf);
                lines.put(name, line.toString());
                if (isShim(reader.getSuperName())) queue.add(reader.getSuperName());
                for (String itf : reader.getInterfaces()) if (isShim(itf)) queue.add(itf);
            }
        }
        Path out = Path.of(args[2]);
        Files.createDirectories(out.getParent());
        Files.write(out, lines.values());
    }

    private static void collect(InputStream in, Set<String> names, Set<String> interfaces) throws IOException {
        Remapper recorder = new Remapper(Opcodes.ASM9) {
            @Override
            public String map(String name) {
                if (isShim(name)) names.add(name);
                return name;
            }
        };
        ClassVisitor hints = new ClassVisitor(Opcodes.ASM9) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] itfs) {
                if (itfs != null) interfaces.addAll(List.of(itfs));
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                        if (itf) interfaces.add(owner);
                    }

                    @Override
                    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... args) {
                        interfaces.add(org.objectweb.asm.Type.getReturnType(desc).getInternalName());
                    }
                };
            }
        };
        try (in) {
            new ClassReader(in).accept(new ClassRemapper(hints, recorder), 0);
        }
    }

    public static Path generate(Path outDir, ClassLoader loader) throws IOException {
        List<String> lines;
        try (InputStream in = loader.getResourceAsStream(HIERARCHY)) {
            if (in == null) throw new IOException("Missing " + HIERARCHY);
            lines = new String(in.readAllBytes()).lines().toList();
        }
        Path jar = outDir.resolve(STUBS_JAR);
        write(jar, lines, name -> loader.getResource(name + ".class") != null);
        return jar;
    }

    private static void write(Path jar, List<String> lines, Predicate<String> implemented) throws IOException {
        Map<String, List<String>> nested = new HashMap<>();
        Map<String, Integer> accessOf = new HashMap<>();
        for (String line : lines) {
            String[] parts = line.split(" ");
            String name = parts[1];
            accessOf.put(name, Integer.parseInt(parts[0]) & ~Opcodes.ACC_FINAL | Opcodes.ACC_PUBLIC);
            int dollar = name.lastIndexOf('$');
            if (dollar > 0) nested.computeIfAbsent(name.substring(0, dollar), k -> new ArrayList<>()).add(name);
        }
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(jar))) {
            for (String line : lines) {
                String[] parts = line.split(" ");
                String name = parts[1];
                if (implemented.test(name)) continue;
                int access = Integer.parseInt(parts[0]) & ~Opcodes.ACC_FINAL | Opcodes.ACC_PUBLIC;
                ClassWriter writer = new ClassWriter(0);
                writer.visit(Opcodes.V1_8, access, name, null, parts[2], List.of(parts).subList(3, parts.length).toArray(String[]::new));
                int dollar = name.lastIndexOf('$');
                if (dollar > 0) writer.visitInnerClass(name, name.substring(0, dollar), name.substring(dollar + 1), access | Opcodes.ACC_STATIC);
                for (String child : nested.getOrDefault(name, List.of())) {
                    writer.visitInnerClass(child, name, child.substring(child.lastIndexOf('$') + 1), accessOf.get(child) | Opcodes.ACC_STATIC);
                }
                if ((access & (Opcodes.ACC_INTERFACE | Opcodes.ACC_ENUM)) == 0) {
                    MethodVisitor init = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
                    init.visitVarInsn(Opcodes.ALOAD, 0);
                    init.visitMethodInsn(Opcodes.INVOKESPECIAL, parts[2], "<init>", "()V", false);
                    init.visitInsn(Opcodes.RETURN);
                    init.visitMaxs(1, 1);
                    init.visitEnd();
                }
                if ((access & Opcodes.ACC_ENUM) != 0) {
                    MethodVisitor values = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "values", "()[L" + name + ";", null, null);
                    values.visitInsn(Opcodes.ICONST_0);
                    values.visitTypeInsn(Opcodes.ANEWARRAY, name);
                    values.visitInsn(Opcodes.ARETURN);
                    values.visitMaxs(1, 0);
                    values.visitEnd();
                }
                writer.visitEnd();
                out.putNextEntry(new ZipEntry(name + ".class"));
                out.write(writer.toByteArray());
                out.closeEntry();
            }
        }
    }

    public static final class Compile {
        public static void main(String[] args) throws IOException {
            Path sources = Path.of(args[2]);
            Path jar = Path.of(args[1]);
            Files.createDirectories(jar.getParent());
            write(jar, Files.readAllLines(Path.of(args[0])), name -> {
                String outer = name.contains("$") ? name.substring(0, name.indexOf('$')) : name;
                return Files.exists(sources.resolve(outer + ".java"));
            });
        }
    }

    private static boolean isShim(String name) {
        return name != null && name.startsWith(FlashbackPatcher.SHIM_PREFIX);
    }
}
