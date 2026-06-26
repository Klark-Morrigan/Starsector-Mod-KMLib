package kmlib.starsector.entities;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.GateEntityPlugin;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins {@link Gates#activateGate}: the helper must reproduce vanilla's
 * activation flags exactly, since a gate only flips active when its plugin
 * later reads the network flags and the per-gate scanned mark this sets.
 *
 * <p>The two static seams {@code activateGate} reaches through -
 * {@code Global.getSector()} and {@code GateEntityPlugin.getGateData()} - are
 * stubbed via static mocks. The gate-data mock returns a real
 * {@link GateEntityPlugin.GateData} so its {@code scanned} registry can be
 * asserted directly rather than through a verify. Cases live in a
 * {@link Nested} group named for the method under test, so the suite reports as
 * a per-method tree.
 */
final class GatesTest {

    private MockedStatic<Global> globalMock;
    private MockedStatic<GateEntityPlugin> gatePluginMock;

    private MemoryAPI sectorMemoryMock;
    private GateEntityPlugin.GateData gateData;

    @BeforeEach
    void setUp() {
        sectorMemoryMock = mock(MemoryAPI.class);
        var sectorMock = mock(SectorAPI.class);
        when(sectorMock.getMemoryWithoutUpdate()).thenReturn(sectorMemoryMock);

        globalMock = mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sectorMock);

        gateData = new GateEntityPlugin.GateData();
        gatePluginMock = mockStatic(GateEntityPlugin.class);
        gatePluginMock.when(GateEntityPlugin::getGateData).thenReturn(gateData);
    }

    @AfterEach
    void tearDown() {
        gatePluginMock.close();
        globalMock.close();
    }

    @Nested
    class ActivateGate {
        @Test
        void null_gate_touches_no_state() {
            // Early return guards a no-op call site (e.g. a console command run
            // against a system with no gate) from powering the network for a
            // gate that does not exist.
            Gates.activateGate(null);

            globalMock.verifyNoInteractions();
            gatePluginMock.verifyNoInteractions();
            assertThat(gateData.scanned).isEmpty();
        }

        @Test
        void powers_the_network_so_no_gate_is_left_unusable() {
            var gate = gateWithMemory(mock(MemoryAPI.class));

            Gates.activateGate(gate);

            // Both global flags gate the "travel" option, so activation is
            // meaningless without them.
            verify(sectorMemoryMock).set(GateEntityPlugin.GATES_ACTIVE, true);
            verify(sectorMemoryMock).set(GateEntityPlugin.PLAYER_CAN_USE_GATES, true);
        }

        @Test
        void scans_only_the_target_gate() {
            var gateMemoryMock = mock(MemoryAPI.class);
            var gate = gateWithMemory(gateMemoryMock);

            Gates.activateGate(gate);

            // Per-gate scan mark keeps activation targeted: other unscanned
            // gates stay dark even though the network is now powered.
            verify(gateMemoryMock).set(GateEntityPlugin.GATE_SCANNED, true);
        }

        @Test
        void registers_the_gate_as_a_transit_destination() {
            var gate = gateWithMemory(mock(MemoryAPI.class));

            Gates.activateGate(gate);

            // Adding to the scanned registry is what makes the gate a known
            // destination the plugin will offer as a jump target.
            assertThat(gateData.scanned).containsExactly(gate);
        }
    }

    private static SectorEntityToken gateWithMemory(MemoryAPI memory) {
        var gateMock = mock(SectorEntityToken.class);
        when(gateMock.getMemoryWithoutUpdate()).thenReturn(memory);
        return gateMock;
    }
}
