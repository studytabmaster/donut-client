// language: Java, file: src/client/java/dev/donut/client/automine/AutoMineController.java
package dev.donut.client.automine;

import dev.donut.client.config.ClientConfig;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class AutoMineController implements AutoCloseable {
    private enum State { IDLE, SELECT_TARGET, PATHFIND, APPROACH, MINE }

    private final ClientConfig config;
    private final TargetQueue targets = new TargetQueue();
    private State state = State.IDLE;
    private TargetQueue.Target current;
    private List<BlockPos> path;
    private int pathIndex;

    public AutoMineController(ClientConfig config) { this.config = config; }

    public void tick(Minecraft client) {
        if (client.player == null || client.level == null || !config.autoMine.enabled) {
            state = State.IDLE;
            return;
        }
        if (SafetyGuard.isUnsafe(client)) {
            state = State.IDLE;
            return;
        }

        switch (state) {
            case IDLE, SELECT_TARGET -> selectTarget(client);
            case PATHFIND -> pathfind(client);
            case APPROACH -> approach(client);
            case MINE -> mine(client);
        }
    }

    private void selectTarget(Minecraft client) {
        targets.refresh(client.player.position());
        current = targets.poll();
        if (current == null) { state = State.IDLE; return; }
        path = PathPlanner.findPath(client, client.player.blockPosition(), current.pos());
        pathIndex = 0;
        state = path == null ? State.SELECT_TARGET : State.PATHFIND;
    }

    private void pathfind(Minecraft client) {
        if (pathIndex >= path.size()) { state = State.APPROACH; return; }
        MovementController.stepTo(client, path.get(pathIndex));
        if (client.player.blockPosition().equals(path.get(pathIndex))) pathIndex++;
    }

    private void approach(Minecraft client) {
        if (DigController.inRange(client, current.pos())) state = State.MINE;
        else state = State.PATHFIND;
    }

    private void mine(Minecraft client) {
        DigController.lookAt(client, current.pos());
        DigController.dig(client, current.pos());
        BlockState st = client.level.getBlockState(current.pos());
        if (st.isAir()) state = State.SELECT_TARGET;
    }

    @Override public void close() { state = State.IDLE; }
}
