package kmlib.console.factions;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the contract of {@link FactionTargetResolver#resolveOwningFaction}: which faction a
 * command ends up acting for, and - where it ends up acting for none - what the player is told.
 * The cases live in a {@link Nested} group so the suite reports as a per-method tree.
 */
final class FactionTargetResolverTest {

    private static final String PLAYER_FACTION_ID = "player";

    @Nested
    class ResolveOwningFaction {

        @Test
        void resolves_the_faction_the_id_names() {

            var hegemonyMock = mock(FactionAPI.class);

            assertThat(FactionTargetResolver.resolveOwningFaction(
                    buildSectorAnswering("hegemony", hegemonyMock),
                    "hegemony"))
                .isEqualTo(new ResolvedFactionTarget(hegemonyMock));
        }

        @Test
        void takes_the_player_when_no_id_is_given() {
            // What a bare invocation means: a dev tool handing out colonies is nearly always
            // handing them to the person running it.
            var playerMock = mock(FactionAPI.class);

            assertThat(FactionTargetResolver.resolveOwningFaction(
                    buildSectorAnswering(PLAYER_FACTION_ID, playerMock),
                    null))
                .isEqualTo(new ResolvedFactionTarget(playerMock));
        }

        @Test
        void treats_a_blank_id_as_no_id_at_all() {
            // The console hands over whatever the player typed, and a run of spaces is a bare
            // invocation - looking for a faction named by them would find none.
            var playerMock = mock(FactionAPI.class);

            assertThat(FactionTargetResolver.resolveOwningFaction(
                    buildSectorAnswering(PLAYER_FACTION_ID, playerMock),
                    "   "))
                .isEqualTo(new ResolvedFactionTarget(playerMock));
        }

        @Test
        void refuses_an_id_no_faction_answers_to() {
            // Refused rather than passed through: an ownership change takes an id, so a typo
            // applied verbatim leaves a colony held by nobody and the command reporting success.
            assertThat(FactionTargetResolver.resolveOwningFaction(
                    mock(SectorAPI.class),
                    "hegmony"))
                .isEqualTo(new UnresolvedFactionTarget("No faction with id 'hegmony'."));
        }

        @Test
        void names_the_player_faction_in_a_refusal_it_asked_for_itself() {
            // The default is looked up like any other id, so a sector that cannot answer for the
            // player says so under that id rather than under the blank the caller passed.
            assertThat(FactionTargetResolver.resolveOwningFaction(mock(SectorAPI.class), null))
                .isEqualTo(new UnresolvedFactionTarget("No faction with id 'player'."));
        }

        @Test
        void refuses_a_run_made_without_a_sector() {
            // Nothing to look an id up in, and no default to fall back to either - the player's
            // faction is the sector's to answer for like any other.
            assertThat(FactionTargetResolver.resolveOwningFaction(null, "hegemony"))
                .isEqualTo(new UnresolvedFactionTarget("No sector to read factions from."));
        }
    }

    // A sector that answers for one faction under one id, and for nothing else - which is what
    // makes the refusal cases above a real miss rather than a missing stub.
    private static SectorAPI buildSectorAnswering(String factionId, FactionAPI faction) {

        var sectorMock = mock(SectorAPI.class);

        when(sectorMock.getFaction(factionId))
            .thenReturn(faction);

        return sectorMock;
    }
}
