package com.consoleclient.termina;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class KeyBindings {
    public static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of("termina", "main"));

    public static final KeyBinding START = new KeyBinding(
            "key.termina.start",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_I,
            CATEGORY
    );

    public static final KeyBinding STOP = new KeyBinding(
            "key.termina.stop",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            CATEGORY
    );

    private KeyBindings() {}

    public static void register() {
        KeyBindingHelper.registerKeyBinding(START);
        KeyBindingHelper.registerKeyBinding(STOP);
    }
}
