// language: Java, file: src/client/java/dev/donut/client/automine/PathPlanner.java
package dev.donut.client.automine;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public final class PathPlanner {
    private static final int MAX_NODES = 5000;
    private static final BlockPos[] NEIGHBORS = {
        new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0),
        new BlockPos(0, 0, 1), new BlockPos(0, 0, -1),
        new BlockPos(1, 0, 1), new BlockPos(1, 0, -1),
        new BlockPos(-1, 0, 1), new BlockPos(-1, 0, -1),
        new BlockPos(0, 1, 0), new BlockPos(0, -1, 0)
    };

    public static List<BlockPos> findPath(Minecraft client, BlockPos start, BlockPos goal) {
        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));
        Map<BlockPos, BlockPos> cameFrom = new HashMap<>();
        Map<BlockPos, Double> gScore = new HashMap<>();
        gScore.put(start, 0.0);
        open.add(new Node(start, heuristic(start, goal)));
        int visited = 0;

        while (!open.isEmpty() && visited++ < MAX_NODES) {
            Node n = open.poll();
            if (n.pos.equals(goal) || n.pos.distManhattan(goal) <= 3) {
                return reconstruct(cameFrom, n.pos);
            }
            for (BlockPos off : NEIGHBORS) {
                BlockPos next = n.pos.offset(off);
                if (!canStand(client, next)) continue;
                double tentative = gScore.get(n.pos) + cost(off);
                if (tentative < gScore.getOrDefault(next, Double.MAX_VALUE)) {
                    cameFrom.put(next, n.pos);
                    gScore.put(next, tentative);
                    open.add(new Node(next, tentative + heuristic(next, goal)));
                }
            }
        }
        return null;
    }

    private static double cost(BlockPos off) {
        if (off.getY() == 1) return 1.5;
        if (off.getY() == -1) return 0.8;
        return (off.getX() != 0 && off.getZ() != 0) ? 1.414 : 1.0;
    }

    private static double heuristic(BlockPos a, BlockPos b) {
        return a.distManhattan(b);
    }

    private static boolean canStand(Minecraft client, BlockPos p) {
        return client.level.getBlockState(p).isAir()
            && client.level.getBlockState(p.above()).isAir()
            && !client.level.getBlockState(p.below()).isAir();
    }

    private static List<BlockPos> reconstruct(Map<BlockPos, BlockPos> cameFrom, BlockPos end) {
        LinkedList<BlockPos> path = new LinkedList<>();
        BlockPos cur = end;
        while (cur != null) {
            path.addFirst(cur);
            cur = cameFrom.get(cur);
        }
        return path;
    }

    private record Node(BlockPos pos, double f) {}
}
