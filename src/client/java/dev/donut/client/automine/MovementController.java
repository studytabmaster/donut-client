// language: Java, file: src/client/java/dev/donut/client/automine/MovementController.java
package dev.donut.client.automine;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class MovementController {
    public static void stepTo(Minecraft client, BlockPos next) {
        Vec3 target = Vec3.atCenterOf(next);
        Vec3 diff = target.subtract(client.player.position());
        float yaw = (float) Math.toDegrees(Math.atan2(-diff.x, diff.z));
        client.player.setYRot(yaw);
        client.options.keyUp.setDown(true);
        client.options.keyJump.setDown(next.getY() > client.player.blockPosition().getY());
    }
}
