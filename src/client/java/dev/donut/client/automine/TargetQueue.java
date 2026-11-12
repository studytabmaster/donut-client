// language: Java, file: src/client/java/dev/donut/client/automine/TargetQueue.java
package dev.donut.client.automine;

import dev.donut.client.blockesp.BlockEspController;
import dev.donut.client.blockesp.OreMarker;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class TargetQueue {
    public record Target(BlockPos pos, OreMarker.Kind kind, double distSq) {}

    private final PriorityQueue<Target> queue = new PriorityQueue<>(
        Comparator.comparingInt((Target t) -> t.kind() == OreMarker.Kind.EMERALD ? 0 : 1)
                  .thenComparingDouble(Target::distSq)
    );

    public void refresh(Vec3 playerPos) {
        queue.clear();
        BlockEspController esp = BlockEspController.get();
        if (esp == null) return;
        List<OreMarker> markers = esp.snapshot();
        for (OreMarker m : markers) {
            queue.add(new Target(m.pos(), m.kind(),
                m.pos().distToCenterSqr(playerPos)));
        }
    }

    public Target poll() { return queue.poll(); }
    public boolean isEmpty() { return queue.isEmpty(); }
}
