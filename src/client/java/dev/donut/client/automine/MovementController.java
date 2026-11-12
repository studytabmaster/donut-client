package dev.donut.client.automine;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class MovementController {
    private static Vec3 lastPos;
    private static long lastPosChange;

    public static void stepTo(Minecraft client, BlockPos next) {
        Vec3 target = Vec3.atCenterOf(next);
        Vec3 diff = target.subtract(client.player.position());
        float yaw = (float) Math.toDegrees(Math.atan2(-diff.x, diff.z));
        client.player.setYRot(yaw);
        client.options.keyUp.setDown(true);

        boolean needJump = next.getY() > client.player.blockPosition().getY();
        Vec3 pos = client.player.position();
        long now = System.currentTimeMillis();
        if (lastPos == null || pos.distanceToSqr(lastPos) > 0.01) {
            lastPos = pos;
            lastPosChange = now;
        } else if (now - lastPosChange > 500) {
            needJump = true;
        }
        client.options.keyJump.setDown(needJump);
    }

    public static void reset() {
        lastPos = null;
        lastPosChange = 0;
    }
}
