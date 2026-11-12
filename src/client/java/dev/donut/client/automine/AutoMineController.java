package dev.donut.client.automine;

import dev.donut.client.blockesp.BlockEspController;
import dev.donut.client.blockesp.OreMarker;
import dev.donut.client.config.ClientConfig;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class AutoMineController implements AutoCloseable {
    private enum State { IDLE, SELECT_TARGET, PATHFIND, APPROACH, MINE, BATCH_MINE, RECOVER }

    private static final int STUCK_THRESHOLD_TICKS = 120;
    private static final int DIG_STUCK_TICKS = 240;

    private final ClientConfig config;
    private final TargetQueue targets = new TargetQueue();
    private State state = State.IDLE;
    private TargetQueue.Target current;
    private List<BlockPos> path;
    private int pathIndex;
    private List<BlockPos> batchList = new ArrayList<>();
    private int stuckTicks;
    private int debugTicks;

    public AutoMineController(ClientConfig config) { this.config = config; }

    public void tick(Minecraft client) {
        if (client.player == null || client.level == null || !config.autoMine.enabled) {
            if (state != State.IDLE) {
                MovementController.releaseKeys(client);
                state = State.IDLE;
            }
            DigController.reset();
            MovementController.reset();
            resetStuck();
            return;
        }
        if (SafetyGuard.isUnsafe(client)) {
            if (state != State.IDLE) {
                MovementController.releaseKeys(client);
                state = State.IDLE;
            }
            resetStuck();
            return;
        }
        targets.tickBlacklist();
        DigController.tickRotation(client);

        if (++debugTicks >= 60) {
            debugTicks = 0;
            BlockEspController esp = BlockEspController.get();
            System.out.println("[AutoMine] state=" + state
                    + " esp=" + (esp != null ? esp.status() : "null")
                    + " targets=" + (esp != null ? esp.snapshot().size() : 0)
                    + " current=" + (current != null ? current.pos() : "null")
                    + " pathIdx=" + pathIndex + "/" + (path != null ? path.size() : 0));
        }

        switch (state) {
            case IDLE, SELECT_TARGET -> selectTarget(client);
            case PATHFIND -> pathfind(client);
            case APPROACH -> approach(client);
            case MINE -> mine(client);
            case BATCH_MINE -> batchMine(client);
            case RECOVER -> recover(client);
        }
    }

    private void selectTarget(Minecraft client) {
        BlockEspController esp = BlockEspController.get();
        if (esp == null) { state = State.IDLE; return; }
        String status = esp.status();
        if (status.startsWith("Disabled") || status.startsWith("Overworld only")
                || status.startsWith("Select an ore") || status.startsWith("Join a world")
                || status.startsWith("Prediction failed")) {
            state = State.IDLE;
            return;
        }
        targets.refresh(client.player.position());
        current = targets.poll();
        if (current == null) { state = State.IDLE; return; }
        path = PathPlanner.findPath(client, client.player.blockPosition(), current.pos());
        pathIndex = 0;
        resetStuck();
        if (path == null) {
            targets.blacklist(current.pos());
            current = null;
            state = State.SELECT_TARGET;
            return;
        }
        state = State.PATHFIND;
    }

    private void pathfind(Minecraft client) {
        if (path == null || pathIndex >= path.size()) { state = State.APPROACH; return; }
        BlockPos next = path.get(pathIndex);
        BlockPos playerPos = client.player.blockPosition();

        if (playerPos.equals(next)) {
            pathIndex++;
            resetStuck();
            return;
        }

        BlockState feet = client.level.getBlockState(next);
        BlockState head = client.level.getBlockState(next.above());

        if (!feet.isAir()) {
            DigController.lookAt(client, next);
            DigController.dig(client, next);
            if (++stuckTicks > DIG_STUCK_TICKS) state = State.RECOVER;
            return;
        }
        if (!head.isAir()) {
            DigController.lookAt(client, next.above());
            DigController.dig(client, next.above());
            if (++stuckTicks > DIG_STUCK_TICKS) state = State.RECOVER;
            return;
        }

        MovementController.stepTo(client, next);
        if (++stuckTicks > STUCK_THRESHOLD_TICKS) state = State.RECOVER;
    }

    private void approach(Minecraft client) {
        if (current == null) { state = State.SELECT_TARGET; return; }
        if (DigController.inRange(client, current.pos())) {
            batchList = batchTargets(client);
            state = batchList.isEmpty() ? State.MINE : State.BATCH_MINE;
            return;
        }
        path = PathPlanner.findPath(client, client.player.blockPosition(), current.pos());
        pathIndex = 0;
        if (path == null) { state = State.RECOVER; return; }
        state = State.PATHFIND;
    }

    private void mine(Minecraft client) {
        if (current == null) { state = State.SELECT_TARGET; return; }
        DigController.lookAt(client, current.pos());
        DigController.dig(client, current.pos());
        BlockState st = client.level.getBlockState(current.pos());
        if (st.isAir()) {
            current = null;
            resetStuck();
            state = State.SELECT_TARGET;
        } else if (++stuckTicks > DIG_STUCK_TICKS) {
            targets.blacklist(current.pos());
            current = null;
            state = State.RECOVER;
        }
    }

    private void batchMine(Minecraft client) {
        batchList.removeIf(p -> client.level.getBlockState(p).isAir());
        if (batchList.isEmpty()) {
            current = null;
            batchList.clear();
            resetStuck();
            state = State.SELECT_TARGET;
            return;
        }
        BlockPos pos = batchList.get(0);
        if (!DigController.inRange(client, pos)) {
            batchList.clear();
            state = State.APPROACH;
            return;
        }
        DigController.lookAt(client, pos);
        DigController.dig(client, pos);
        if (++stuckTicks > DIG_STUCK_TICKS) {
            targets.blacklist(pos);
            batchList.remove(0);
            resetStuck();
        }
    }

    private void recover(Minecraft client) {
        if (current != null) targets.blacklist(current.pos());
        current = null;
        path = null;
        batchList.clear();
        DigController.reset();
        MovementController.releaseKeys(client);
        MovementController.reset();
        resetStuck();
        state = State.SELECT_TARGET;
    }

    private List<BlockPos> batchTargets(Minecraft client) {
        Vec3 eye = client.player.getEyePosition();
        BlockEspController esp = BlockEspController.get();
        if (esp == null) return List.of();
        var blacklist = targets.snapshotBlacklist();
        return esp.snapshot().stream()
            .filter(m -> !blacklist.contains(m.pos()))
            .filter(m -> eye.distanceToSqr(Vec3.atCenterOf(m.pos())) <= 4.5 * 4.5)
            .sorted(Comparator
                .comparingInt((OreMarker m) -> m.kind() == OreMarker.Kind.EMERALD ? 0 : 1)
                .thenComparingDouble(m -> eye.distanceToSqr(Vec3.atCenterOf(m.pos()))))
            .map(OreMarker::pos)
            .toList();
    }

    private void resetStuck() { stuckTicks = 0; }

    @Override public void close() {
        state = State.IDLE;
        current = null;
        path = null;
        batchList.clear();
        resetStuck();
        DigController.reset();
        MovementController.reset();
        Minecraft client = Minecraft.getInstance();
        if (client != null) MovementController.releaseKeys(client);
    }
}
