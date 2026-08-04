package kmlib.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.OrbitAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;

import kmlib.starsector.systems.StarSystems;
import kmlib.testfixtures.console.output.CommandOutputFake;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Pins {@link ListSystemEntitiesCommand}: {@code buildReport} nests each entity
 * under its focus with distance/speed, with unorbited entities and fleets
 * following with coordinates, and the gates filter keeps gates plus their orbit
 * chain while dropping everything else; and {@code runCommand} wires the
 * {@code gates} flag through to the gates-only report, rejects a surplus
 * argument, and returns the validation result outside a campaign. The
 * command's static seams ({@code Global}, {@code StarSystems}) are stubbed and
 * the report read back through a recording {@code CommandOutput}, so no live
 * console is needed.
 */
final class ListSystemEntitiesCommandTest {
    @Nested
    class BuildReport {
        @Test
        void buildsOrbitTreeWithTrailingUnorbitedAndFleets() {
            var star = buildEntity("star", "Star", 0f, 0f, null, 0f, false);
            var planet = buildEntity("planet", "Planet", 1000f, 0f, star, 360f, false);
            var gate = buildEntity("gate1", "Gate", 1100f, 0f, planet, 180f, true);
            var drifting = buildEntity("probe", "Probe", 500f, 0f, null, 0f, false);
            var fleet = buildFleet("Patrol", 2000f, 0f);
            var system = buildSystem(star, List.of(star, planet, gate, drifting), List.of(fleet));

            var report = ListSystemEntitiesCommand.buildReport(system, false);

            assertThat(report).contains("Star [star]");
            assertThat(report).contains("Planet [planet]  dist=1000  speed=1.00 deg/day");
            assertThat(report).contains("Gate [gate1]  dist=100  speed=2.00 deg/day");
            assertThat(report).contains("Unorbited entities (nearest center first):");
            assertThat(report).contains("Probe [probe]  (500, 0)");
            assertThat(report).contains("Fleets (nearest center first):");
            assertThat(report).contains("Patrol [Patrol]  (2000, 0)");
        }

        @Test
        void gatesFilterKeepsGatesAndTheirOrbitChainOnly() {
            var star = buildEntity("star", "Star", 0f, 0f, null, 0f, false);
            var planet = buildEntity("planet", "Planet", 1000f, 0f, star, 360f, false);
            var gate = buildEntity("gate1", "Gate", 1100f, 0f, planet, 180f, true);
            var otherPlanet = buildEntity("planet2", "Other", -1000f, 0f, star, 360f, false);
            var drifting = buildEntity("probe", "Probe", 500f, 0f, null, 0f, false);
            var fleet = buildFleet("Patrol", 2000f, 0f);
            var system = buildSystem(star,
                List.of(star, planet, gate, otherPlanet, drifting), List.of(fleet));

            var report = ListSystemEntitiesCommand.buildReport(system, true);

            // Gate plus its orbit chain (planet, star) are kept.
            assertThat(report).contains("Star [star]");
            assertThat(report).contains("Planet [planet]");
            assertThat(report).contains("Gate [gate1]");
            // A sibling planet with no gate, the drifting probe, and fleets are gone.
            assertThat(report).doesNotContain("Other [planet2]");
            assertThat(report).doesNotContain("Probe [probe]");
            assertThat(report).doesNotContain("Patrol");
        }
    }

    @Nested
    class RunCommand {
        private MockedStatic<Global> globalMock;
        private MockedStatic<StarSystems> starSystemsMock;
        private CommandOutputFake outputFake;
        private ListSystemEntitiesCommand command;

        @BeforeEach
        void setUp() {
            var star = buildEntity("star", "Star", 0f, 0f, null, 0f, false);
            var gate = buildEntity("gate1", "Gate", 100f, 0f, star, 180f, true);
            var systemMock = buildSystem(star, List.of(star, gate), List.of());

            globalMock = mockStatic(Global.class);
            globalMock.when(Global::getSector).thenReturn(mock(SectorAPI.class));
            starSystemsMock = mockStatic(StarSystems.class);
            starSystemsMock.when(() -> StarSystems.getPlayerStarSystem(any()))
                .thenReturn(systemMock);

            outputFake = new CommandOutputFake();
            command = new ListSystemEntitiesCommand(outputFake);
        }

        @AfterEach
        void tearDown() {
            starSystemsMock.close();
            globalMock.close();
        }

        @Test
        void lists_the_full_tree_for_a_bare_invocation() {
            var result = command.runCommand("", CommandContext.CAMPAIGN_MAP);

            assertThat(result).isEqualTo(CommandResult.SUCCESS);
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("System entities in Test System")
                        && !message.contains("(gates only)"));
        }

        @Test
        void narrows_to_gates_when_the_gates_flag_is_given() {
            var result = command.runCommand("gates", CommandContext.CAMPAIGN_MAP);

            assertThat(result).isEqualTo(CommandResult.SUCCESS);
            // The flag flips the report into its gates-only form.
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("(gates only)"));
        }

        @Test
        void reports_a_surplus_argument_as_bad_syntax() {
            var result = command.runCommand("gates extra", CommandContext.CAMPAIGN_MAP);

            assertThat(result).isEqualTo(CommandResult.BAD_SYNTAX);
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("Too many arguments"));
        }

        @Test
        void returns_the_validation_result_outside_a_campaign() {
            var result = command.runCommand("", CommandContext.COMBAT_MISSION);

            assertThat(result).isEqualTo(CommandResult.WRONG_CONTEXT);
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("can only run in a campaign"));
        }
    }

    private static StarSystemAPI buildSystem(SectorEntityToken center,
            List<SectorEntityToken> allEntities, List<CampaignFleetAPI> fleets) {
        var systemMock = mock(StarSystemAPI.class);
        when(systemMock.getName()).thenReturn("Test System");
        when(systemMock.getCenter()).thenReturn(center);
        when(systemMock.getAllEntities()).thenReturn(allEntities);
        when(systemMock.getFleets()).thenReturn(fleets);
        return systemMock;
    }

    private static SectorEntityToken buildEntity(String id, String name, float x, float y,
            SectorEntityToken focus, float orbitalPeriodDays, boolean isGate) {
        var entityMock = mock(SectorEntityToken.class);
        when(entityMock.getId()).thenReturn(id);
        when(entityMock.getName()).thenReturn(name);
        when(entityMock.getLocation()).thenReturn(new Vector2f(x, y));
        when(entityMock.getOrbitFocus()).thenReturn(focus);
        when(entityMock.hasTag(Tags.GATE)).thenReturn(isGate);
        if (focus != null) {
            var orbitMock = mock(OrbitAPI.class);
            when(orbitMock.getOrbitalPeriod()).thenReturn(orbitalPeriodDays);
            when(entityMock.getOrbit()).thenReturn(orbitMock);
        }
        return entityMock;
    }

    private static CampaignFleetAPI buildFleet(String name, float x, float y) {
        var fleetMock = mock(CampaignFleetAPI.class);
        when(fleetMock.getId()).thenReturn(name);
        when(fleetMock.getName()).thenReturn(name);
        when(fleetMock.getLocation()).thenReturn(new Vector2f(x, y));
        return fleetMock;
    }
}
