package dev.donut.client.automine;

import dev.donut.client.blockesp.BlockEspController;
import dev.donut.client.blockesp.OreMarker;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class TargetQueue {
    public record Target(BlockPos pos, OreMarker.Kind kind, double distSq) {}

    private final PriorityQueue<Target> queue = new PriorityQueue<>(
        Comparator.comparingInt((Target t) -> t.kind() == OreMarker.Kind.EMERALD ? 0 : 1)
                  .thenComparingDouble(Target::distSq)
    );
    private final Set<BlockPos> blacklist = new HashSet<>();
    private long blacklistStamp;

    public void refresh(Vec3 playerPos) {
        queue.clear();
        BlockEspController esp = BlockEspController.get();
        if (esp == null) return;
        List<OreMarker> markers = esp.snapshot();
        for (OreMarker m : markers) {
            if (blacklist.contains(m.pos())) continue;
            queue.add(new Target(m.pos(), m.kind(),
                m.pos().distToCenterSqr(playerPos)));
        }
    }

    public Target poll() { return queue.poll(); }
    public boolean isEmpty() { return queue.isEmpty(); }

    public void blacklist(BlockPos pos) {
        blacklist.add(pos);
        blacklistStamp = System.currentTimeMillis();
    }

    public void tickBlacklist() {
        if (!blacklist.isEmpty() && System.currentTimeMillis() - blacklistStamp > 30_000) {
            blacklist.clear();
        }
    }

    public Set<BlockPos> snapshotBlacklist() { return Set.copyOf(blacklist); }
}
