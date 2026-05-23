package com.consoleclient.termina;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.RaycastContext;

public final class BoneOrderMacro {
    public static final BoneOrderMacro INSTANCE = new BoneOrderMacro();

    private static final double REACH = 4.5;
    private static final int TOP_ROWS_SLOTS = 36;
    private static final int DEFAULT_ORDERS_CLICK_SLOT = 0;
    private static final int ORDERS_FINAL_SLOT = 15;
    private static final long DEFAULT_STEP_DELAY_MS = 300L;

    private boolean running = false;
    private int phase = 0;
    private long nextAt = 0L;
    private BlockPos spawnerPos = null;
    private int loopCount = 0;
    private long stepDelayMs = DEFAULT_STEP_DELAY_MS;
    private int ordersClickSlot = DEFAULT_ORDERS_CLICK_SLOT;

    public long getStepDelay() {
        return stepDelayMs;
    }

    public void setStepDelay(long ms) {
        if (ms < 0L) ms = 0L;
        this.stepDelayMs = ms;
    }

    public int getOrdersClickSlot() {
        return ordersClickSlot;
    }

    public void setOrdersClickSlot(int slot) {
        if (slot < 0) slot = 0;
        this.ordersClickSlot = slot;
    }

    private BoneOrderMacro() {}

    public void start() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.world == null) return;
        if (running) return;

        BlockPos pos = findSpawner(client);
        if (pos == null) {
            send(client, "§c[Termina] §fNo spawner in view within range.");
            return;
        }

        spawnerPos = pos;
        loopCount = 1;
        running = true;
        phase = 0;
        nextAt = 0L;

        String stopKey = KeyBindings.STOP.getBoundKeyLocalizedText().getString().toUpperCase();
        send(client, String.format(
                "§b[Termina] §fStarted Bone Order: Spawner X: %d Y: %d Z: %d §7(Press %s to stop)",
                pos.getX(), pos.getY(), pos.getZ(), stopKey
        ));
    }

    public void stop() {
        if (!running) return;
        running = false;
        phase = 0;
        spawnerPos = null;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            send(client, "§b[Termina] §fStopped.");
        }
    }

    public void tick(MinecraftClient client) {
        if (!running) return;
        if (client == null || client.player == null || client.world == null || client.interactionManager == null) return;
        if (System.currentTimeMillis() < nextAt) return;
        runPhase(client);
    }

    private void runPhase(MinecraftClient client) {
        long now = System.currentTimeMillis();
        switch (phase) {
            case 0 -> {
                debug(client, 0, "look at spawner (" + spawnerPos.getX() + "," + spawnerPos.getY() + "," + spawnerPos.getZ() + ")");
                lookAt(client.player, Vec3d.ofCenter(spawnerPos));
                advance(now, 1);
            }
            case 1 -> {
                debug(client, 1, "right-click spawner");
                openSpawner(client);
                advance(now, 2);
            }
            case 2 -> {
                int clicked = shiftClickBonesInRange(client, 0, TOP_ROWS_SLOTS);
                debug(client, 2, "shift-click top 4 rows (clicked " + clicked + " bone stacks)" + currentScreenLabel(client));
                advance(now, 3);
            }
            case 3 -> {
                debug(client, 3, "ESC (close spawner GUI)");
                closeScreen(client);
                advance(now, 4);
            }
            case 4 -> {
                debug(client, 4, "send command: /orders bones");
                client.player.networkHandler.sendChatCommand("orders bones");
                advance(now, 5);
            }
            case 5 -> {
                debug(client, 5, "right-click slot " + ordersClickSlot + currentScreenLabel(client));
                rightClickSlot(client, ordersClickSlot);
                advance(now, 6);
            }
            case 6 -> {
                int found = countInventoryBones(client);
                int clicked = shiftClickInventoryBones(client);
                debug(client, 6, "shift-click inv bones into chest (found " + found + " stacks, clicked " + clicked + ")" + currentScreenLabel(client));
                advance(now, 7);
            }
            case 7 -> {
                debug(client, 7, "ESC (close chest GUI)");
                closeScreen(client);
                advance(now, 8);
            }
            case 8 -> {
                debug(client, 8, "left-click slot 15" + currentScreenLabel(client));
                leftClickSlot(client, ORDERS_FINAL_SLOT);
                advance(now, 9);
            }
            case 9 -> {
                debug(client, 9, "ESC (1st)");
                closeScreen(client);
                advance(now, 10);
            }
            case 10 -> {
                debug(client, 10, "ESC (2nd)");
                closeScreen(client);
                advance(now, 11);
            }
            case 11 -> {
                send(client, String.format(
                        "§b[Termina] §fFinished Loop %d, starting loop %d.",
                        loopCount, loopCount + 1
                ));
                loopCount++;
                BlockPos pos = findSpawner(client);
                if (pos == null) {
                    send(client, "§c[Termina] §fLost spawner reference; stopping.");
                    stop();
                    return;
                }
                spawnerPos = pos;
                advance(now, 0);
            }
            default -> {
                phase = 0;
                nextAt = now;
            }
        }
    }

    private void advance(long now, int next) {
        phase = next;
        nextAt = now + stepDelayMs;
    }

    private void debug(MinecraftClient client, int step, String desc) {
        send(client, "§b[Termina] §8step " + step + " §7" + desc);
    }

    private String currentScreenLabel(MinecraftClient client) {
        if (client.currentScreen == null) return " §c[no screen open]";
        if (client.currentScreen instanceof HandledScreen<?> hs) {
            return " §8[screen: " + client.currentScreen.getClass().getSimpleName()
                    + ", slots=" + hs.getScreenHandler().slots.size() + "]";
        }
        return " §8[screen: " + client.currentScreen.getClass().getSimpleName() + "]";
    }

    private void openSpawner(MinecraftClient client) {
        if (spawnerPos == null) return;
        Direction face = faceTowardPlayer(client.player, spawnerPos);
        Vec3d hitVec = faceCenter(spawnerPos, face);
        BlockHitResult hit = new BlockHitResult(hitVec, face, spawnerPos, false);
        client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hit);
    }

    private BlockPos findSpawner(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        ClientWorld world = client.world;
        if (player == null || world == null) return null;

        Vec3d eye = player.getCameraPosVec(1.0f);
        BlockPos origin = BlockPos.ofFloored(eye);
        int r = (int) Math.ceil(REACH);
        BlockPos best = null;
        double bestDistSq = REACH * REACH;

        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos pos = origin.add(dx, dy, dz);
                    if (!world.getBlockState(pos).isOf(Blocks.SPAWNER)) continue;
                    Vec3d center = Vec3d.ofCenter(pos);
                    double distSq = eye.squaredDistanceTo(center);
                    if (distSq > bestDistSq) continue;
                    if (!hasLineOfSight(world, player, eye, center, pos)) continue;
                    best = pos;
                    bestDistSq = distSq;
                }
            }
        }
        return best;
    }

    private boolean hasLineOfSight(ClientWorld world, PlayerEntity player, Vec3d eye, Vec3d target, BlockPos spawner) {
        RaycastContext ctx = new RaycastContext(
                eye, target,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                player
        );
        BlockHitResult hit = world.raycast(ctx);
        if (hit.getType() == HitResult.Type.MISS) return true;
        return hit.getBlockPos().equals(spawner);
    }

    private void lookAt(ClientPlayerEntity player, Vec3d target) {
        double dx = target.x - player.getX();
        double dy = target.y - player.getEyeY();
        double dz = target.z - player.getZ();
        double horiz = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, horiz)));
        player.setYaw(yaw);
        player.setPitch(pitch);
        player.setHeadYaw(yaw);
        player.setBodyYaw(yaw);
    }

    private Direction faceTowardPlayer(ClientPlayerEntity player, BlockPos pos) {
        Vec3d center = Vec3d.ofCenter(pos);
        Vec3d eye = player.getCameraPosVec(1.0f);
        double dx = eye.x - center.x;
        double dy = eye.y - center.y;
        double dz = eye.z - center.z;
        double absX = Math.abs(dx);
        double absY = Math.abs(dy);
        double absZ = Math.abs(dz);
        if (absY >= absX && absY >= absZ) {
            return dy >= 0 ? Direction.UP : Direction.DOWN;
        }
        if (absX >= absZ) {
            return dx >= 0 ? Direction.EAST : Direction.WEST;
        }
        return dz >= 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private Vec3d faceCenter(BlockPos pos, Direction face) {
        Vec3d c = Vec3d.ofCenter(pos);
        Vec3i n = face.getVector();
        return new Vec3d(
                c.x + n.getX() * 0.5,
                c.y + n.getY() * 0.5,
                c.z + n.getZ() * 0.5
        );
    }

    private int shiftClickBonesInRange(MinecraftClient client, int from, int toExclusive) {
        if (!(client.currentScreen instanceof HandledScreen<?> hs)) return 0;
        ScreenHandler handler = hs.getScreenHandler();
        int max = Math.min(toExclusive, handler.slots.size());
        int clicked = 0;
        for (int i = from; i < max; i++) {
            Slot slot = handler.slots.get(i);
            if (!slot.getStack().isOf(Items.BONE)) continue;
            client.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.QUICK_MOVE, client.player);
            clicked++;
        }
        return clicked;
    }

    private int countInventoryBones(MinecraftClient client) {
        if (!(client.currentScreen instanceof HandledScreen<?> hs)) return 0;
        ScreenHandler handler = hs.getScreenHandler();
        int total = handler.slots.size();
        if (total <= 36) return 0;
        int playerInvStart = total - 36;
        int found = 0;
        for (int i = playerInvStart; i < total; i++) {
            if (handler.slots.get(i).getStack().isOf(Items.BONE)) found++;
        }
        return found;
    }

    private int shiftClickInventoryBones(MinecraftClient client) {
        if (!(client.currentScreen instanceof HandledScreen<?> hs)) return 0;
        ScreenHandler handler = hs.getScreenHandler();
        int total = handler.slots.size();
        if (total <= 36) return 0;
        int playerInvStart = total - 36;
        int clicked = 0;
        for (int i = playerInvStart; i < total; i++) {
            Slot slot = handler.slots.get(i);
            if (!slot.getStack().isOf(Items.BONE)) continue;
            client.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.QUICK_MOVE, client.player);
            clicked++;
        }
        return clicked;
    }

    private void rightClickSlot(MinecraftClient client, int slot) {
        if (!(client.currentScreen instanceof HandledScreen<?> hs)) return;
        ScreenHandler handler = hs.getScreenHandler();
        if (slot < 0 || slot >= handler.slots.size()) return;
        client.interactionManager.clickSlot(handler.syncId, slot, 1, SlotActionType.PICKUP, client.player);
    }

    private void leftClickSlot(MinecraftClient client, int slot) {
        if (!(client.currentScreen instanceof HandledScreen<?> hs)) return;
        ScreenHandler handler = hs.getScreenHandler();
        if (slot < 0 || slot >= handler.slots.size()) return;
        client.interactionManager.clickSlot(handler.syncId, slot, 0, SlotActionType.PICKUP, client.player);
    }

    private void closeScreen(MinecraftClient client) {
        if (client.player != null) client.player.closeHandledScreen();
        client.setScreen(null);
    }

    private void send(MinecraftClient client, String msg) {
        if (client.player != null) {
            client.player.sendMessage(Text.literal(msg), false);
        }
    }
}
