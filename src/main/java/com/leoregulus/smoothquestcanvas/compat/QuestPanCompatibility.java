package com.leoregulus.smoothquestcanvas.compat;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;
import java.util.Map;

/** Checks bytecode without loading FTB client classes during Mixin discovery. */
public final class QuestPanCompatibility {
    private static final String LIBRARY = "dev/ftb/mods/ftblibrary/";
    private static final String QUESTS = "dev/ftb/mods/ftbquests/";
    public static final String SCREEN = QUESTS + "client/gui/quests/QuestScreen";
    private static final String BASE = LIBRARY + "ui/BaseScreen";
    private static final String BUTTON = LIBRARY + "ui/input/MouseButton";
    private static final String PANEL = QUESTS + "client/gui/quests/QuestPanel";
    private static final String FILE = QUESTS + "client/ClientQuestFile";
    private static final String DRAW = "(Lnet/minecraft/client/gui/GuiGraphics;L" + LIBRARY + "ui/Theme;IIII)V";

    @FunctionalInterface
    public interface ClassReader {
        ClassNode read(String internalName) throws Exception;
    }

    private QuestPanCompatibility() {}

    /** Null means compatible; otherwise returns a diagnostic explaining why the whole mixin must be skipped. */
    public static String incompatibility(ClassReader reader) throws Exception {
        Map<String, ClassNode> cache = new HashMap<>();
        ClassReader classes = name -> {
            ClassNode node = cache.get(name);
            if (node == null) { node = reader.read(name); cache.put(name, node); }
            return node;
        };
        ClassNode screen = classes.read(SCREEN);
        if (!BASE.equals(screen.superName)) return "QuestScreen superclass changed";
        var fields = Map.of("questPanel", "L" + PANEL + ";", "file", "L" + FILE + ";",
                "selectedChapter", "L" + QUESTS + "quest/Chapter;", "grabbed", "L" + BUTTON + ";",
                "movingObjects", "Z", "scrollWidth", "D", "scrollHeight", "D", "prevMouseX", "I", "prevMouseY", "I");
        for (var field : fields.entrySet()) {
            boolean found = screen.fields.stream().anyMatch(f -> f.name.equals(field.getKey()) && f.desc.equals(field.getValue())
                    && (f.access & Opcodes.ACC_STATIC) == 0
                    && (!(f.name.equals("questPanel") || f.name.equals("file")) || (f.access & Opcodes.ACC_FINAL) != 0));
            if (!found) return "QuestScreen field changed: " + field.getKey();
        }
        var methods = Map.of("drawBackground", DRAW, "isViewingQuest", "()Z", "addZoom", "(D)V",
                "selectChapter", "(L" + QUESTS + "quest/Chapter;)V", "scrollTo", "(L" + QUESTS + "quest/Movable;)V",
                "onClosed", "()V", "viewQuest", "(L" + QUESTS + "quest/Quest;)V",
                "keyPressed", "(L" + LIBRARY + "ui/input/Key;)Z");
        for (var method : methods.entrySet()) {
            if (declaredMethod(screen, method.getKey(), method.getValue()) == null)
                return "QuestScreen method changed: " + method.getKey();
        }
        // Our inherited overrides must never replace new FTB-specific mouse handlers.
        if (declaredMethod(screen, "mousePressed", "(L" + BUTTON + ";)Z") != null
                || declaredMethod(screen, "mouseScrolled", "(DDDD)Z") != null)
            return "QuestScreen now implements its own mouse handlers";
        int dragHooks = 0;
        for (var instruction : declaredMethod(screen, "drawBackground", DRAW).instructions) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals(BUTTON)
                    && call.name.equals("isLeft") && call.desc.equals("()Z")) dragHooks++;
        }
        if (dragHooks != 1) return "Expected one canvas left-drag branch, found " + dragHooks;
        ClassNode libraryPanel = classes.read(LIBRARY + "ui/Panel");
        MethodNode panelDraw = declaredMethod(libraryPanel, "draw", DRAW);
        MethodNode setOffset = declaredMethod(libraryPanel, "setOffset", "(Z)V");
        if (panelDraw == null || setOffset == null) return "FTB panel rendering methods changed";
        int offsetCalls = 0;
        for (var instruction : panelDraw.instructions) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals(libraryPanel.name)
                    && call.name.equals("setOffset") && call.desc.equals("(Z)V")) {
                if (offsetCalls >= 2 || call.getPrevious().getOpcode() != (offsetCalls == 0 ? Opcodes.ICONST_1 : Opcodes.ICONST_0))
                    return "FTB panel content scope changed";
                offsetCalls++;
            }
        }
        if (offsetCalls != 2) return "FTB panel content scope changed";
        int integerOffsets = 0;
        for (var instruction : setOffset.instructions) {
            if (instruction instanceof FieldInsnNode field && field.getOpcode() == Opcodes.PUTFIELD
                    && field.owner.equals(libraryPanel.name) && field.desc.equals("I")
                    && (field.name.equals("offsetX") || field.name.equals("offsetY"))
                    && field.getPrevious().getOpcode() == Opcodes.D2I) integerOffsets++;
        }
        if (integerOffsets != 2) return "FTB panel scroll rounding changed";
        String[][] dependencies = {
                {BASE, "getMouseX", "()I"}, {BASE, "getMouseY", "()I"},
                {BASE, "mousePressed", "(L" + BUTTON + ";)Z"}, {BASE, "mouseScrolled", "(DDDD)Z"},
                {PANEL, "getScrollX", "()D"}, {PANEL, "getScrollY", "()D"},
                {PANEL, "setScrollX", "(D)V"}, {PANEL, "setScrollY", "(D)V"},
                {FILE, "canEdit", "()Z"}, {BUTTON, "isLeft", "()Z"}
        };
        for (var method : dependencies) {
            if (!inheritedMethod(classes, method[0], method[1], method[2]))
                return "FTB method changed: " + method[0] + "." + method[1];
        }
        return null;
    }

    private static MethodNode declaredMethod(ClassNode node, String name, String descriptor) {
        return node.methods.stream().filter(m -> m.name.equals(name) && m.desc.equals(descriptor)
                && (m.access & Opcodes.ACC_STATIC) == 0).findFirst().orElse(null);
    }

    private static boolean inheritedMethod(ClassReader reader, String owner, String name, String descriptor) throws Exception {
        while (owner != null && !owner.equals("java/lang/Object")) {
            ClassNode node = reader.read(owner);
            if (declaredMethod(node, name, descriptor) != null) return true;
            owner = node.superName;
        }
        return false;
    }
}
