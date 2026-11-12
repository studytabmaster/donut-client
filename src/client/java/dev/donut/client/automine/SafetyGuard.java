package dev.donut.client.automine;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

public final class SafetyGuard {
    public static boolean isUnsafe(Minecraft client) {
        var player = client.player;
        if (player == null || client.level == null) return true;
        if (player.getY() < -60) return true;
        if (player.getHealth() < 6) return true;
        int emptySlots = 0;
        for (int i = 0; i < 36; i++) {
            if (player.getInventory().getItem(i).isEmpty()) emptySlots++;
        }
        if (emptySlots == 0) return true;
        BlockPos origin = player.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(-3, -3, -3), origin.offset(3, 3, 3))) {
            if (client.level.getBlockState(p).is(Blocks.LAVA)) return true;
        }
        return false;
    }
}
