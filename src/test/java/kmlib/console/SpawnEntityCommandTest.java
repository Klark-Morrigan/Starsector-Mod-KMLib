package kmlib.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.JumpPointAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.Entities;

import kmlib.starsector.entities.EntityNameGenerator;
import kmlib.starsector.entities.EntityOrbits;
import kmlib.starsector.entities.EntitySpawner;
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
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

/**
 * Pins {@link SpawnEntityCommand#runCommand} across its kinds, speed sourcing,
 * and shared focus-resolution branches: an unrecognised kind activates nothing
 * and reports the supported list; the gate kind spawns an inactive gate and the
 * jump-point kind a working jump point; an omitted speed is derived from the
 * orbit radius while an explicit one sets the base, and the (default or named)
 * jitter widens whichever before it reaches the spawner; and the focus resolution
 * refuses an ambiguous multi-star system or an unknown id. Feedback is read back
 * through a recording {@code CommandOutput} binding.
 *
 * <p>The static seams - {@code Global}, {@code StarSystems},
 * {@code EntityNameGenerator}, {@code EntitySpawner}, {@code EntityOrbits} - are
 * stubbed via static mocks; the name generator is fixed to a known name and the
 * speed derivation to a known rate so this pins the command's branching and
 * wiring, not the naming or orbital-speed math (each covered by its own suite).
 * With the console output decoupled behind {@code CommandOutput}, the
 * failing-validation branch is pinned here too: a wrong-context run returns the
 * validation result and spawns nothing. Cases live under a {@link Nested} group
 * named for the method under test.
 */
final class SpawnEntityCommandTest {

    private static final String GENERATED_NAME = "Corvus Jump-point 1.0e2";
    // Fleet (100,0) orbiting a focus at the origin: an orbit radius of 100.
    private static final float ORBIT_RADIUS = 100f;
    // The stubbed radius-derived base rate, distinct from any literal so a spawn
    // can be traced to the derivation seam.
    private static final float DERIVED_BASE_SPEED = 7f;
    // The stubbed post-jitter rate the spawner should receive; distinct from the
    // base and from any explicit speed so its arrival proves the jitter ran.
    private static final float JITTERED_SPEED = 9f;

    private MockedStatic<Global> globalMock;
    private MockedStatic<StarSystems> starSystemsMock;
    private MockedStatic<EntityNameGenerator> nameGeneratorMock;
    private MockedStatic<EntitySpawner> spawnerMock;
    private MockedStatic<EntityOrbits> entityOrbitsMock;

    private StarSystemAPI systemMock;
    private SectorEntityToken focusMock;
    private CommandOutputFake outputFake;
    private SpawnEntityCommand command;

    @BeforeEach
    void setUp() {
        focusMock = mock(SectorEntityToken.class);
        when(focusMock.getLocation()).thenReturn(new Vector2f(0f, 0f));

        systemMock = mock(StarSystemAPI.class);
        when(systemMock.getName()).thenReturn("Corvus");
        when(systemMock.getCenter()).thenReturn(focusMock);

        var fleetMock = mock(CampaignFleetAPI.class);
        when(fleetMock.getLocation()).thenReturn(new Vector2f(ORBIT_RADIUS, 0f));
        var sectorMock = mock(SectorAPI.class);
        when(sectorMock.getPlayerFleet()).thenReturn(fleetMock);

        globalMock = mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sectorMock);

        starSystemsMock = mockStatic(StarSystems.class);
        starSystemsMock.when(() -> StarSystems.getPlayerStarSystem(any()))
            .thenReturn(systemMock);

        nameGeneratorMock = mockStatic(EntityNameGenerator.class);
        nameGeneratorMock.when(() -> EntityNameGenerator.generateJumpPointName(any(), anyFloat()))
            .thenReturn(GENERATED_NAME);

        spawnerMock = mockStatic(EntitySpawner.class);

        entityOrbitsMock = mockStatic(EntityOrbits.class);
        entityOrbitsMock.when(() -> EntityOrbits.deriveBaseSpeedDegPerDay(anyFloat()))
            .thenReturn(DERIVED_BASE_SPEED);
        entityOrbitsMock.when(() -> EntityOrbits.applyJitter(anyFloat(), anyFloat(), any()))
            .thenReturn(JITTERED_SPEED);

        outputFake = new CommandOutputFake();
        command = new SpawnEntityCommand(outputFake);
    }

    @AfterEach
    void tearDown() {
        entityOrbitsMock.close();
        spawnerMock.close();
        nameGeneratorMock.close();
        starSystemsMock.close();
        globalMock.close();
    }

    @Nested
    class RunCommand {
        @Test
        void reports_the_supported_list_for_an_unknown_kind_and_spawns_nothing() {
            var result = command.runCommand("bogus", CommandContext.CAMPAIGN_MAP);

            assertThat(result).isEqualTo(CommandResult.ERROR);
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("Unknown entity type 'bogus'")
                        && message.contains("gate")
                        && message.contains("jump_point"));
            spawnerMock.verifyNoInteractions();
        }

        @Test
        void spawns_an_inactive_gate_orbiting_the_center_and_reports_its_id() {
            var spawnedMock = mock(SectorEntityToken.class);
            when(spawnedMock.getId()).thenReturn("gate_42");
            spawnerMock.when(() -> EntitySpawner.spawnOrbitingCustomEntity(
                eq(focusMock), eq(Entities.INACTIVE_GATE), eq("neutral"),
                anyFloat(), anyFloat(), anyFloat()))
                .thenReturn(spawnedMock);

            var result = command.runCommand("gate", CommandContext.CAMPAIGN_MAP);

            assertThat(result).isEqualTo(CommandResult.SUCCESS);
            // The spawned id must reach the player so they can target it with the
            // gate-activation command.
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("gate_42"));
        }

        @Test
        void derives_a_radius_speed_with_default_jitter_when_none_is_given() {
            var spawnedMock = mock(JumpPointAPI.class);
            when(spawnedMock.getName()).thenReturn(GENERATED_NAME);
            spawnerMock.when(() -> EntitySpawner.spawnOrbitingJumpPoint(
                eq(focusMock), eq(GENERATED_NAME), anyFloat(), anyFloat(), anyFloat()))
                .thenReturn(spawnedMock);

            var result = command.runCommand("jump_point", CommandContext.CAMPAIGN_MAP);

            assertThat(result).isEqualTo(CommandResult.SUCCESS);
            // The orbit radius gives the base rate; the default jitter widens it,
            // and that widened rate is what the spawner receives.
            entityOrbitsMock.verify(() -> EntityOrbits.deriveBaseSpeedDegPerDay(eq(ORBIT_RADIUS)));
            entityOrbitsMock.verify(() -> EntityOrbits.applyJitter(
                eq(DERIVED_BASE_SPEED), eq(EntityOrbits.VANILLA_JITTER_FRACTION), any()));
            spawnerMock.verify(() -> EntitySpawner.spawnOrbitingJumpPoint(
                eq(focusMock), eq(GENERATED_NAME), anyFloat(), eq(JITTERED_SPEED), anyFloat()));
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("Spawned " + GENERATED_NAME)
                        && message.contains("Corvus"));
        }

        @Test
        void passes_a_named_jitter_to_the_widening() {
            var spawnedMock = mock(JumpPointAPI.class);
            when(spawnedMock.getName()).thenReturn(GENERATED_NAME);
            spawnerMock.when(() -> EntitySpawner.spawnOrbitingJumpPoint(
                eq(focusMock), eq(GENERATED_NAME), anyFloat(), anyFloat(), anyFloat()))
                .thenReturn(spawnedMock);

            var result = command.runCommand("jump_point jitter=0.5", CommandContext.CAMPAIGN_MAP);

            assertThat(result).isEqualTo(CommandResult.SUCCESS);
            // The named jitter shapes the widening of the radius-derived base in
            // place of the default.
            entityOrbitsMock.verify(() -> EntityOrbits.applyJitter(
                eq(DERIVED_BASE_SPEED), eq(0.5f), any()));
            spawnerMock.verify(() -> EntitySpawner.spawnOrbitingJumpPoint(
                eq(focusMock), eq(GENERATED_NAME), anyFloat(), eq(JITTERED_SPEED), anyFloat()));
        }

        @Test
        void jitters_an_explicit_speed_without_deriving_from_radius() {
            var spawnedMock = mock(JumpPointAPI.class);
            when(spawnedMock.getName()).thenReturn(GENERATED_NAME);
            spawnerMock.when(() -> EntitySpawner.spawnOrbitingJumpPoint(
                eq(focusMock), eq(GENERATED_NAME), anyFloat(), anyFloat(), anyFloat()))
                .thenReturn(spawnedMock);

            var result = command.runCommand("jump_point speed=5", CommandContext.CAMPAIGN_MAP);

            assertThat(result).isEqualTo(CommandResult.SUCCESS);
            // The explicit speed is the base - no radius derivation - but the
            // default jitter still widens it before it reaches the spawner.
            entityOrbitsMock.verify(() -> EntityOrbits.deriveBaseSpeedDegPerDay(anyFloat()), never());
            entityOrbitsMock.verify(() -> EntityOrbits.applyJitter(
                eq(5f), eq(EntityOrbits.VANILLA_JITTER_FRACTION), any()));
            spawnerMock.verify(() -> EntitySpawner.spawnOrbitingJumpPoint(
                eq(focusMock), eq(GENERATED_NAME), anyFloat(), eq(JITTERED_SPEED), anyFloat()));
        }

        @Test
        void applies_a_named_jitter_to_an_explicit_speed() {
            var spawnedMock = mock(JumpPointAPI.class);
            when(spawnedMock.getName()).thenReturn(GENERATED_NAME);
            spawnerMock.when(() -> EntitySpawner.spawnOrbitingJumpPoint(
                eq(focusMock), eq(GENERATED_NAME), anyFloat(), anyFloat(), anyFloat()))
                .thenReturn(spawnedMock);

            var result = command.runCommand("jump_point speed=5 jitter=0.5",
                CommandContext.CAMPAIGN_MAP);

            assertThat(result).isEqualTo(CommandResult.SUCCESS);
            // speed and jitter combine: the explicit speed is the base, widened by
            // the named jitter (this pair was previously rejected).
            entityOrbitsMock.verify(() -> EntityOrbits.deriveBaseSpeedDegPerDay(anyFloat()), never());
            entityOrbitsMock.verify(() -> EntityOrbits.applyJitter(eq(5f), eq(0.5f), any()));
            spawnerMock.verify(() -> EntitySpawner.spawnOrbitingJumpPoint(
                eq(focusMock), eq(GENERATED_NAME), anyFloat(), eq(JITTERED_SPEED), anyFloat()));
        }

        @Test
        void applies_a_positional_focus_and_speed_in_order() {
            var namedFocusMock = mock(SectorEntityToken.class);
            when(namedFocusMock.getLocation()).thenReturn(new Vector2f(0f, 0f));
            when(systemMock.getEntityById("beta")).thenReturn(namedFocusMock);
            var spawnedMock = mock(JumpPointAPI.class);
            when(spawnedMock.getName()).thenReturn(GENERATED_NAME);
            spawnerMock.when(() -> EntitySpawner.spawnOrbitingJumpPoint(
                eq(namedFocusMock), eq(GENERATED_NAME),
                anyFloat(), anyFloat(), anyFloat()))
                .thenReturn(spawnedMock);

            var result = command.runCommand("jump_point beta 5", CommandContext.CAMPAIGN_MAP);

            assertThat(result).isEqualTo(CommandResult.SUCCESS);
            // First bare token is the focus id, the second the speed (base 5), which
            // the default jitter widens before it reaches the spawner.
            entityOrbitsMock.verify(() -> EntityOrbits.applyJitter(
                eq(5f), eq(EntityOrbits.VANILLA_JITTER_FRACTION), any()));
            spawnerMock.verify(() -> EntitySpawner.spawnOrbitingJumpPoint(
                eq(namedFocusMock), eq(GENERATED_NAME), anyFloat(), eq(JITTERED_SPEED), anyFloat()));
        }

        @Test
        void rejects_a_non_numeric_speed() {
            var result = command.runCommand("jump_point speed=fast", CommandContext.CAMPAIGN_MAP);

            assertThat(result).isEqualTo(CommandResult.BAD_SYNTAX);
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("Invalid speed 'fast'"));
            spawnerMock.verifyNoInteractions();
        }

        @Test
        void rejects_a_non_numeric_jitter() {
            var result = command.runCommand("jump_point jitter=lots", CommandContext.CAMPAIGN_MAP);

            assertThat(result).isEqualTo(CommandResult.BAD_SYNTAX);
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("Invalid jitter 'lots'"));
            spawnerMock.verifyNoInteractions();
        }

        @Test
        void rejects_a_negative_jitter() {
            var result = command.runCommand("jump_point jitter=-0.5", CommandContext.CAMPAIGN_MAP);

            assertThat(result).isEqualTo(CommandResult.BAD_SYNTAX);
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("Invalid jitter '-0.5'"));
            spawnerMock.verifyNoInteractions();
        }

        @Test
        void rejects_an_unknown_named_parameter() {
            var result = command.runCommand("jump_point color=red", CommandContext.CAMPAIGN_MAP);

            assertThat(result).isEqualTo(CommandResult.BAD_SYNTAX);
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("Unknown parameter 'color'"));
            spawnerMock.verifyNoInteractions();
        }

        @Test
        void rejects_more_arguments_than_focus_and_speed() {
            var result = command.runCommand("jump_point beta 5 extra", CommandContext.CAMPAIGN_MAP);

            assertThat(result).isEqualTo(CommandResult.BAD_SYNTAX);
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("Too many arguments"));
            spawnerMock.verifyNoInteractions();
        }

        @Test
        void refuses_a_multi_star_system_without_a_focus_id() {
            // Build the star list before opening the static stub: constructing
            // the mocks mid-stub would trip Mockito's unfinished-stubbing guard.
            var stars = List.of(star("alpha"), star("beta"));
            starSystemsMock.when(() -> StarSystems.getStars(systemMock)).thenReturn(stars);

            var result = command.runCommand("jump_point", CommandContext.CAMPAIGN_MAP);

            assertThat(result).isEqualTo(CommandResult.BAD_SYNTAX);
            // The error names the candidate stars so the player can re-run.
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("This system has 2 stars")
                        && message.contains("alpha")
                        && message.contains("beta"));
            spawnerMock.verifyNoInteractions();
        }

        @Test
        void refuses_an_explicit_focus_id_that_matches_nothing() {
            when(systemMock.getEntityById("ghost")).thenReturn(null);

            var result = command.runCommand("jump_point ghost", CommandContext.CAMPAIGN_MAP);

            assertThat(result).isEqualTo(CommandResult.BAD_SYNTAX);
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("No entity with id 'ghost'"));
            spawnerMock.verifyNoInteractions();
        }

        @Test
        void orbits_an_explicit_focus_id_when_one_is_given() {
            var namedFocusMock = mock(SectorEntityToken.class);
            when(namedFocusMock.getLocation()).thenReturn(new Vector2f(0f, 0f));
            when(systemMock.getEntityById("beta")).thenReturn(namedFocusMock);
            var spawnedMock = mock(JumpPointAPI.class);
            when(spawnedMock.getName()).thenReturn(GENERATED_NAME);
            spawnerMock.when(() -> EntitySpawner.spawnOrbitingJumpPoint(
                eq(namedFocusMock), eq(GENERATED_NAME),
                anyFloat(), anyFloat(), anyFloat()))
                .thenReturn(spawnedMock);

            var result = command.runCommand("jump_point beta", CommandContext.CAMPAIGN_MAP);

            assertThat(result).isEqualTo(CommandResult.SUCCESS);
            // The named entity, not the system center, is the orbit focus.
            spawnerMock.verify(() -> EntitySpawner.spawnOrbitingJumpPoint(
                eq(namedFocusMock), eq(GENERATED_NAME), anyFloat(), anyFloat(), anyFloat()));
        }

        @Test
        void returns_the_validation_result_outside_a_campaign() {
            var result = command.runCommand("gate", CommandContext.COMBAT_MISSION);

            assertThat(result).isEqualTo(CommandResult.WRONG_CONTEXT);
            assertThat(outputFake.getMessages())
                .anyMatch(message -> message.contains("can only run in a campaign"));
            spawnerMock.verifyNoInteractions();
        }
    }

    private static PlanetAPI star(String id) {
        var starMock = mock(PlanetAPI.class);
        when(starMock.getId()).thenReturn(id);
        return starMock;
    }
}
