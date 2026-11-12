package dev.donut.client.automine;

import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class DigController {
    private static final double RANGE_SQ = 4.5 * 4.5;
    private static final Random RNG = new Random();

    private static float targetYaw, targetPitch;
    private static float currentYaw, currentPitch;
    private static boolean hasTarget;
    private static float jitterPhase;
    private static int clickDelay;

    public static void lookAt(Minecraft client, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        Vec3 eye = client.player.getEyePosition();
        Vec3 diff = center.subtract(eye);
        targetYaw = (float) Math.toDegrees(Math.atan2(-diff.x, diff.z));
        targetPitch = (float) -Math.toDegrees(Math.atan2(diff.y, Math.hypot(diff.x, diff.z)));
        hasTarget = true;
    }

    /** Call every client tick to smoothly rotate toward the last lookAt target. */
    public static void tickRotation(Minecraft client) {
        if (!hasTarget || client.player == null) return;
        if (currentYaw == 0 && currentPitch == 0) {
            currentYaw = client.player.getYRot();
            currentPitch = client.player.getXRot();
        }
        // Smooth approach with cubic ease-out over ~6 ticks.
        float k = 0.22f + RNG.nextFloat() * 0.08f;
        float dy = Mth.wrapDegrees(targetYaw - currentYaw);
        float dp = targetPitch - currentPitch;
        currentYaw += dy * k + (RNG.nextFloat() - 0.5f) * 0.4f;
        currentPitch += dp * k + (RNG.nextFloat() - 0.5f) * 0.3f;
        // Periodic micro-tremor to break perfect GCD.
        jitterPhase += 0.15f;
        currentYaw += (float) Math.sin(jitterPhase) * 0.08f;
        currentPitch += (float) Math.cos(jitterPhase * 1.7f) * 0.06f;
        client.player.setYRot(currentYaw);
        client.player.setXRot(Mth.clamp(currentPitch, -90, 90));
    }

    public static boolean inRange(Minecraft client, BlockPos pos) {
        return client.player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) <= RANGE_SQ;
    }

    public static void dig(Minecraft client, BlockPos pos) {
        if (client.gameMode == null) return;
        if (clickDelay > 0) { clickDelay--; return; }
        clickDelay = 1 + RNG.nextInt(3); // never consecutive ticks
        Direction dir = faceToward(client.player.getEyePosition(), pos);
        client.gameMode.continueDestroyBlock(pos, dir);
    }

    private static Direction faceToward(Vec3 eye, BlockPos pos) {
        Vec3 diff = Vec3.atCenterOf(pos).subtract(eye);
        double ax = Math.abs(diff.x), ay = Math.abs(diff.y), az = Math.abs(diff.z);
        if (ax >= ay && ax >= az) return diff.x > 0 ? Direction.WEST : Direction.EAST;
        if (ay >= ax && ay >= az) return diff.y > 0 ? Direction.DOWN : Direction.UP;
        return diff.z > 0 ? Direction.NORTH : Direction.SOUTH;
    }

    public static void reset() {
        hasTarget = false;
        currentYaw = currentPitch = 0;
        jitterPhase = 0;
        clickDelay = 0;
    }
}
