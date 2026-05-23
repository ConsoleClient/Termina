package com.consoleclient.termina;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;

public final class TerminaClient implements ClientModInitializer {
    private boolean stopKeyWasDown = false;

    @Override
    public void onInitializeClient() {
        KeyBindings.register();
        TerminaCommand.register();
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(MinecraftClient client) {
        while (KeyBindings.START.wasPressed()) {
            BoneOrderMacro.INSTANCE.start();
        }
        while (KeyBindings.STOP.wasPressed()) {
            BoneOrderMacro.INSTANCE.stop();
        }

        boolean stopDown = isStopKeyPhysicallyDown(client);
        if (stopDown && !stopKeyWasDown && client.currentScreen != null) {
            BoneOrderMacro.INSTANCE.stop();
        }
        stopKeyWasDown = stopDown;

        BoneOrderMacro.INSTANCE.tick(client);
    }

    private boolean isStopKeyPhysicallyDown(MinecraftClient client) {
        Window window = client.getWindow();
        if (window == null) return false;
        InputUtil.Key bound;
        try {
            bound = KeyBindingHelper.getBoundKeyOf(KeyBindings.STOP);
        } catch (Throwable t) {
            return false;
        }
        if (bound == null) return false;
        int code = bound.getCode();
        if (code < 0) return false;
        InputUtil.Type cat = bound.getCategory();
        if (cat == InputUtil.Type.KEYSYM) {
            return InputUtil.isKeyPressed(window, code);
        }
        if (cat == InputUtil.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window.getHandle(), code) == GLFW.GLFW_PRESS;
        }
        return false;
    }
}
