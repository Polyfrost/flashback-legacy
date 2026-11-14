package org.polyfrost.flashbacklegacy.patch;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class FlashbackReport {
    private record Info(String superName, List<String> interfaces, Set<String> members) {
    }

    private final Map<String, Set<String>> missingClasses = new HashMap<>();
    private final Map<String, Map<String, Set<String>>> missingMembers = new TreeMap<>();
    private final Map<String, Optional<Info>> infos = new HashMap<>();
    private final List<ZipFile> jars;
    private final Map<String, ClassNode> flashback = new HashMap<>();
    private String current;

    private final List<String> skipped = new ArrayList<>();

    private FlashbackReport(List<ZipFile> jars) {
        this.jars = jars;
    }

    public static void main(String[] args) throws IOException {
        Path out = Path.of(args[1]);
        Path work = Files.createTempDirectory("flashback-report");
        List<ZipFile> jars = new ArrayList<>();
        for (Path jar : FlashbackPatcher.patch(Path.of(args[0]), work)) jars.add(new ZipFile(jar.toFile()));

        try {
            ZipFile zip = jars.getFirst();
            FlashbackReport report = new FlashbackReport(jars);
            List<String> scanned = new ArrayList<>();
            for (ZipEntry entry : zip.stream().toList()) {
                if (!entry.getName().endsWith(".class") || !entry.getName().startsWith("com/moulberry/")) continue;
                ClassNode node = new ClassNode();
                try (InputStream in = zip.getInputStream(entry)) {
                    new ClassReader(in).accept(node, ClassReader.SKIP_FRAMES);
                }
                report.scan(node);
                report.flashback.put(node.name, node);
                scanned.add(node.name);
            }
            StringBuilder sb = new StringBuilder(report.render(args[0], scanned.size()));
            List<String> traces = Arrays.asList(args).subList(2, args.length);
            traces.stream().filter(t -> t.startsWith("-")).forEach(t -> report.skipped.add(t.substring(1)));
            for (String entry : traces) {
                if (entry.startsWith("-")) continue;
                Set<String> reached = report.trace(entry);
                sb.append("\n## Unresolved, reachable from `").append(entry).append("` (").append(reached.size()).append(")\n\n");
                reached.forEach(m -> sb.append("- `").append(m).append("`\n"));
            }
            Files.createDirectories(out.getParent());
            Files.writeString(out, sb.toString());
        } finally {
            for (ZipFile jar : jars) jar.close();
        }
        System.out.println("Wrote " + out);
    }

    private void scan(ClassNode node) {
        current = node.name;
        classRef(node.superName);
        node.interfaces.forEach(this::classRef);
        for (FieldNode field : node.fields) typeRef(Type.getType(field.desc));
        for (MethodNode method : node.methods) {
            typeRef(Type.getMethodType(method.desc));
            if (method.exceptions != null) method.exceptions.forEach(this::classRef);
            for (TryCatchBlockNode tryCatch : method.tryCatchBlocks) classRef(tryCatch.type);
            for (AbstractInsnNode insn : method.instructions) scanInsn(insn);
        }
    }

    private void scanInsn(AbstractInsnNode insn) {
        switch (insn) {
            case TypeInsnNode type -> typeRef(Type.getObjectType(type.desc));
            case MultiANewArrayInsnNode array -> typeRef(Type.getType(array.desc));
            case FieldInsnNode field -> memberRef(field.owner, field.name, field.desc);
            case MethodInsnNode method -> memberRef(method.owner, method.name, method.desc);
            case LdcInsnNode ldc -> constantRef(ldc.cst);
            case InvokeDynamicInsnNode indy -> {
                typeRef(Type.getMethodType(indy.desc));
                constantRef(indy.bsm);
                for (Object arg : indy.bsmArgs) constantRef(arg);
            }
            default -> {
            }
        }
    }

    private void constantRef(Object constant) {
        if (constant instanceof Type type) typeRef(type);
        else if (constant instanceof Handle handle) memberRef(handle.getOwner(), handle.getName(), handle.getDesc());
    }

    private void typeRef(Type type) {
        switch (type.getSort()) {
            case Type.ARRAY -> typeRef(type.getElementType());
            case Type.OBJECT -> classRef(type.getInternalName());
            case Type.METHOD -> {
                typeRef(type.getReturnType());
                for (Type arg : type.getArgumentTypes()) typeRef(arg);
            }
            default -> {
            }
        }
    }

    private void classRef(String name) {
        if (name == null) return;
        if (name.startsWith("[")) {
            typeRef(Type.getType(name));
        } else if (info(name).isEmpty()) {
            missingClasses.computeIfAbsent(name, k -> new HashSet<>()).add(current);
        }
    }

    private void memberRef(String owner, String name, String desc) {
        typeRef(desc.startsWith("(") ? Type.getMethodType(desc) : Type.getType(desc));
        if (owner.startsWith("[") || owner.equals("java/lang/invoke/MethodHandle") || owner.equals("java/lang/invoke/VarHandle")) {
            classRef(owner);
            return;
        }

        Deque<String> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        queue.add(owner);
        while (!queue.isEmpty()) {
            String cls = queue.poll();
            if (!visited.add(cls)) continue;
            Optional<Info> info = info(cls);
            if (info.isEmpty()) {
                classRef(cls);
                return;
            }
            if (info.get().members.contains(name + desc)) return;
            if (info.get().superName != null) queue.add(info.get().superName);
            queue.addAll(info.get().interfaces);
        }
        missingMembers.computeIfAbsent(owner, k -> new TreeMap<>())
                .computeIfAbsent(name + desc, k -> new HashSet<>()).add(current);
    }

    private Set<String> trace(String entry) {
        Set<String> unresolved = new TreeSet<>();
        Set<String> seen = new HashSet<>();
        Deque<String> work = new ArrayDeque<>(List.of(entry));
        while (!work.isEmpty()) {
            String ref = work.poll();
            if (!seen.add(ref) || skipped.stream().anyMatch(ref::startsWith)) continue;
            int dot = ref.indexOf('.');
            if (ref.indexOf('(') < 0) {
                ClassNode owner = flashback.get(ref.substring(0, dot));
                if (owner != null) owner.methods.stream().filter(m -> m.name.equals(ref.substring(dot + 1))).forEach(m -> work.add(owner.name + "." + m.name + m.desc));
                continue;
            }
            MethodNode method = findMethod(ref.substring(0, dot), ref.substring(dot + 1));
            if (method == null) continue;
            for (AbstractInsnNode insn : method.instructions) {
                switch (insn) {
                    case FieldInsnNode f -> reach(f.owner, f.name, f.desc, false, work, unresolved);
                    case MethodInsnNode m -> reach(m.owner, m.name, m.desc, true, work, unresolved);
                    case TypeInsnNode t when t.getOpcode() == Opcodes.NEW -> work.add(t.desc + ".<clinit>()V");
                    case InvokeDynamicInsnNode indy -> {
                        for (Object arg : indy.bsmArgs) {
                            if (arg instanceof Handle h) reach(h.getOwner(), h.getName(), h.getDesc(), h.getDesc().startsWith("("), work, unresolved);
                        }
                    }
                    default -> {
                    }
                }
            }
        }
        return unresolved;
    }

    private void reach(String owner, String name, String desc, boolean isMethod, Deque<String> work, Set<String> unresolved) {
        if (flashback.containsKey(owner)) {
            work.add(owner + ".<clinit>()V");
            if (isMethod) work.add(owner + "." + name + desc);
        } else if (!owner.startsWith("[") && !resolves(owner, name + desc)) {
            unresolved.add(display(owner) + "." + name + desc);
        }
    }

    private MethodNode findMethod(String owner, String member) {
        for (ClassNode cls = flashback.get(owner); cls != null; cls = flashback.get(cls.superName)) {
            for (MethodNode m : cls.methods) if (member.equals(m.name + m.desc)) return m;
        }
        return null;
    }

    private boolean resolves(String owner, String member) {
        Deque<String> queue = new ArrayDeque<>(List.of(owner));
        Set<String> visited = new HashSet<>();
        while (!queue.isEmpty()) {
            String cls = queue.poll();
            if (!visited.add(cls)) continue;
            Optional<Info> info = info(cls);
            if (info.isEmpty()) return false;
            if (info.get().members.contains(member)) return true;
            if (info.get().superName != null) queue.add(info.get().superName);
            queue.addAll(info.get().interfaces);
        }
        return false;
    }

    private Optional<Info> info(String name) {
        return infos.computeIfAbsent(name, this::load);
    }

    private Optional<Info> load(String name) {
        String path = name + ".class";
        try {
            InputStream in = open(path);
            if (in == null) return Optional.empty();
            ClassNode node = new ClassNode();
            try (in) {
                new ClassReader(in).accept(node, ClassReader.SKIP_CODE);
            }
            Set<String> members = new HashSet<>();
            node.fields.forEach(f -> members.add(f.name + f.desc));
            node.methods.forEach(m -> members.add(m.name + m.desc));
            return Optional.of(new Info(node.superName, node.interfaces, members));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private InputStream open(String path) throws IOException {
        for (ZipFile jar : jars) {
            ZipEntry entry = jar.getEntry(path);
            if (entry != null) return jar.getInputStream(entry);
        }
        return ClassLoader.getSystemResourceAsStream(path);
    }

    private String render(String input, int scanned) {
        StringBuilder sb = new StringBuilder();
        int memberCount = missingMembers.values().stream().mapToInt(Map::size).sum();
        sb.append("# Flashback on 1.8.9: unresolved references\n\n")
                .append("- Input: `").append(Path.of(input).getFileName()).append("`\n")
                .append("- Classes scanned (excluding mixins): ").append(scanned).append('\n')
                .append("- Missing classes: ").append(missingClasses.size()).append('\n')
                .append("- Missing members on existing classes: ").append(memberCount).append("\n\n");

        Map<String, int[]> packages = new TreeMap<>();
        missingClasses.forEach((cls, users) -> {
            int[] stats = packages.computeIfAbsent(group(cls), k -> new int[2]);
            stats[0]++;
            stats[1] += users.size();
        });
        sb.append("## Missing classes by package\n\n| Package | Classes | Uses |\n|---|---|---|\n");
        packages.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<String, int[]> e) -> -e.getValue()[1]))
                .forEach(e -> sb.append("| `").append(e.getKey()).append("` | ").append(e.getValue()[0])
                        .append(" | ").append(e.getValue()[1]).append(" |\n"));

        sb.append("\n## Missing classes\n\nUses = number of Flashback classes referencing it.\n\n| Class | Uses |\n|---|---|\n");
        missingClasses.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<String, Set<String>> e) -> -e.getValue().size())
                        .thenComparing(Map.Entry::getKey))
                .forEach(e -> sb.append("| `").append(display(e.getKey())).append("` | ")
                        .append(e.getValue().size()).append(" |\n"));

        sb.append("\n## Missing members on existing classes\n");
        missingMembers.forEach((owner, members) -> {
            sb.append("\n### `").append(display(owner)).append("`\n\n");
            members.forEach((member, users) -> sb.append("- `").append(member).append("` (")
                    .append(users.size()).append(")\n"));
        });
        return sb.toString();
    }

    private static String display(String name) {
        return name.startsWith(FlashbackPatcher.SHIM_PREFIX) ? "shim:" + name.substring(FlashbackPatcher.SHIM_PREFIX.length()) : name;
    }

    private static String group(String name) {
        String[] parts = display(name).split("/");
        int depth = Math.min(parts.length - 1, parts[0].startsWith("shim:") ? 4 : 3);
        return String.join("/", Arrays.copyOf(parts, depth));
    }
}
