package com.consoleclient.termina;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

public final class TerminaCommand {
    private TerminaCommand() {}

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("termina")
                    .executes(ctx -> {
                        showStatus(ctx.getSource());
                        return 1;
                    })
                    .then(ClientCommandManager.literal("delay")
                            .then(ClientCommandManager.argument("ms", IntegerArgumentType.integer(0, 60000))
                                    .executes(ctx -> {
                                        int ms = IntegerArgumentType.getInteger(ctx, "ms");
                                        BoneOrderMacro.INSTANCE.setStepDelay(ms);
                                        ctx.getSource().sendFeedback(Text.literal(
                                                "§b[Termina] §fStep delay set to §a" + ms + "ms"
                                        ));
                                        return 1;
                                    })))
                    .then(ClientCommandManager.literal("slot")
                            .then(ClientCommandManager.argument("n", IntegerArgumentType.integer(0, 53))
                                    .executes(ctx -> {
                                        int slot = IntegerArgumentType.getInteger(ctx, "n");
                                        BoneOrderMacro.INSTANCE.setOrdersClickSlot(slot);
                                        ctx.getSource().sendFeedback(Text.literal(
                                                "§b[Termina] §fOrders click slot set to §a" + slot
                                        ));
                                        return 1;
                                    })))
                    .then(ClientCommandManager.argument("ms", IntegerArgumentType.integer(0, 60000))
                            .executes(ctx -> {
                                int ms = IntegerArgumentType.getInteger(ctx, "ms");
                                BoneOrderMacro.INSTANCE.setStepDelay(ms);
                                ctx.getSource().sendFeedback(Text.literal(
                                        "§b[Termina] §fStep delay set to §a" + ms + "ms"
                                ));
                                return 1;
                            })));
        });
    }

    private static void showStatus(FabricClientCommandSource source) {
        source.sendFeedback(Text.literal(
                "§b[Termina] §fStep delay: §a" + BoneOrderMacro.INSTANCE.getStepDelay()
                        + "ms§f, orders click slot: §a" + BoneOrderMacro.INSTANCE.getOrdersClickSlot()
        ));
        source.sendFeedback(Text.literal(
                "§7Usage: §f/termina delay <ms>§7, §f/termina slot <n>"
        ));
    }
}
