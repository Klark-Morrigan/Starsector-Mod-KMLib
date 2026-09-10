package kmlib.starsector.entities;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.GateEntityPlugin;

/**
 * Operations over Domain gates, so KM* mods (and console tools) share one
 * implementation of the gate-state dance rather than re-deriving which memory
 * flags vanilla reads.
 *
 * <p>Lives in {@code entities} because a gate is a {@code SectorEntityToken}:
 * this is the gate-specific specialisation, sibling to the type-agnostic
 * {@link EntitySpawner}.
 *
 * <p>Final class with a private constructor: pure-function utility, no instance
 * state.
 */
public final class Gates {

    private Gates() {
        // utility class, no instances.
    }

    /**
     * Brings {@code gate} online the way vanilla's gate quest does: a gate is
     * active only when the network is usable and the gate itself is scanned
     * ({@link GateEntityPlugin#isActive}), so this powers the network and marks
     * just this gate scanned.
     *
     * <p>Powering the network is unavoidable, not over-reach: vanilla's gate
     * interaction gates the "travel" option on the global {@code gatesActive}
     * and {@code playerCanUseGates} flags, so no single gate is usable without
     * them. Scanning only this gate (per-gate {@code gateScanned}) is what keeps
     * activation targeted - other unscanned gates stay dark. Adding the gate to
     * the scanned registry is what makes it a known transit destination.
     *
     * <p>The gate flips to active on its next advance, when its plugin sees the
     * flags this set.
     *
     * <p>The sector is handed over rather than reached for, since the network
     * flags are that sector's own state: a caller acting on one sector must not
     * power the network of whichever sector happens to be current.
     *
     * @param sector the sector whose gate network is powered; null is a no-op
     * @param gate   the gate to activate; null is a no-op
     */
    public static void activateGate(SectorAPI sector, SectorEntityToken gate) {
        if (sector == null || gate == null) {
            return;
        }
        var sectorMemory = sector.getMemoryWithoutUpdate();
        sectorMemory.set(GateEntityPlugin.GATES_ACTIVE, true);
        sectorMemory.set(GateEntityPlugin.PLAYER_CAN_USE_GATES, true);
        gate.getMemoryWithoutUpdate().set(GateEntityPlugin.GATE_SCANNED, true);
        GateEntityPlugin.getGateData().scanned.add(gate);
    }
}
