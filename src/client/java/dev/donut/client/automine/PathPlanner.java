package dev.donut.client.automine;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class PathPlanner {
    private static final int MAX_NODES = 50000;
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
            if (n.pos.distManhattan(goal) <= 3) {
                return reconstruct(cameFrom, n.pos);
            }
            for (BlockPos off : NEIGHBORS) {
                BlockPos next = n.pos.offset(off);
                if (!canTraverse(client, next) || !canTraverse(client, next.above())) continue;
                double stepCost = cost(client, next, off);
                double tentative = gScore.get(n.pos) + stepCost;
                if (tentative < gScore.getOrDefault(next, Double.MAX_VALUE)) {
                    cameFrom.put(next, n.pos);
                    gScore.put(next, tentative);
                    open.add(new Node(next, tentative + heuristic(next, goal)));
                }
            }
        }
        return null;
    }

    private static boolean canTraverse(Minecraft client, BlockPos p) {
        BlockState state = client.level.getBlockState(p);
        if (state.isAir()) return true;
        if (state.is(Blocks.BEDROCK) || state.is(Blocks.BARRIER)) return false;
        if (state.is(Blocks.WATER) || state.is(Blocks.LAVA)) return false;
        return state.getDestroySpeed(client.level, p) >= 0;
    }

    private static double cost(Minecraft client, BlockPos next, BlockPos off) {
        double base = off.getY() == 1 ? 1.5 : off.getY() == -1 ? 1.0
                : (off.getX() != 0 && off.getZ() != 0 ? 1.414 : 1.0);
        double dig = 0;
        BlockState feet = client.level.getBlockState(next);
        BlockState head = client.level.getBlockState(next.above());
        if (!feet.isAir()) dig += 2.0 + Math.max(0, feet.getDestroySpeed(client.level, next)) * 0.5;
        if (!head.isAir()) dig += 2.0 + Math.max(0, head.getDestroySpeed(client.level, next.above())) * 0.5;
        return base + dig;
    }

    private static double heuristic(BlockPos a, BlockPos b) { return a.distManhattan(b); }

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
