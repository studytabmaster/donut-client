package dev.donut.client.automine;

import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class MovementController {
    private static final Random RNG = new Random();
    private static Vec3 lastPos;
    private static long lastPosChange;
    private static int strafeTicks;
    private static float strafeSign;

    public static void stepTo(Minecraft client, BlockPos next) {
        Vec3 target = Vec3.atCenterOf(next);
        Vec3 diff = target.subtract(client.player.position());
        float baseYaw = (float) Math.toDegrees(Math.atan2(-diff.x, diff.z));

        if (strafeTicks <= 0) {
            strafeSign = RNG.nextBoolean() ? 1 : -1;
            strafeTicks = 8 + RNG.nextInt(12);
        }
        strafeTicks--;
        float yawNoise = (RNG.nextFloat() - 0.5f) * 1.2f + strafeSign * 0.6f;
        client.player.setYRot(baseYaw + yawNoise);
        client.options.keyUp.setDown(true);
        client.options.keyLeft.setDown(false);
        client.options.keyRight.setDown(false);

        boolean needJump = next.getY() > client.player.blockPosition().getY();
        Vec3 pos = client.player.position();
        long now = System.currentTimeMillis();
        if (lastPos == null || pos.distanceToSqr(lastPos) > 0.01) {
            lastPos = pos;
            lastPosChange = now;
        } else if (now - lastPosChange > 600) {
            needJump = true;
        }
        client.options.keyJump.setDown(needJump);
    }

    public static void releaseKeys(Minecraft client) {
        client.options.keyUp.setDown(false);
        client.options.keyDown.setDown(false);
        client.options.keyLeft.setDown(false);
        client.options.keyRight.setDown(false);
        client.options.keyJump.setDown(false);
        client.options.keyShift.setDown(false);
        client.options.keySprint.setDown(false);
    }

    public static void reset() {
        lastPos = null;
        lastPosChange = 0;
        strafeTicks = 0;
        strafeSign = 0;
    }
}
