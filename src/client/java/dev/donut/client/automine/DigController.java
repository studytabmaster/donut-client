// language: Java, file: src/client/java/dev/donut/client/automine/DigController.java
package dev.donut.client.automine;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public final class DigController {
    private static final double RANGE_SQ = 4.5 * 4.5;

    public static void lookAt(Minecraft client, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        Vec3 eye = client.player.getEyePosition();
        Vec3 diff = center.subtract(eye);
        float yaw = (float) Math.toDegrees(Math.atan2(-diff.x, diff.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(diff.y, Math.hypot(diff.x, diff.z)));
        client.player.setYRot(yaw);
        client.player.setXRot(pitch);
    }

    public static boolean inRange(Minecraft client, BlockPos pos) {
        return client.player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) <= RANGE_SQ;
    }

    public static void dig(Minecraft client, BlockPos pos) {
        if (client.gameMode == null) return;
        Direction dir = Direction.getNearest(
            client.player.getViewVector(1.0f).x,
            client.player.getViewVector(1.0f).y,
            client.player.getViewVector(1.0f).z);
        client.gameMode.continueDestroyBlock(pos, dir);
    }
}
