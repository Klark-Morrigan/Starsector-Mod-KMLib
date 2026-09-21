package kmlib.starsector.factions.relation;

import com.fs.starfarer.api.campaign.FactionAPI;

import kmlib.starsector.factions.FactionPalette;
import kmlib.testfixtures.starsector.settings.StarsectorSettingsFake;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static kmlib.starsector.factions.relation.StarsectorRelationColours.resolveRelationColour;
import static kmlib.starsector.factions.relation.StarsectorRelationColours.resolveRelationPalette;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StarsectorRelationColoursTest {

    private static final String TRITACHYON = "tritachyon";

    // A shade no point on the ramp lands on, so a pair the observer paints itself cannot be confused
    // with one that fell through to the number.
    private static final Color PINNED_PAIR_SHADE = new Color(10, 20, 30, 255);

    // The two ends of the engine's own ramp, named here so the shades the subject interpolates
    // toward are known values rather than the fixture's one default colour - with every key
    // answering the same shade, a ramp read and its endpoints are indistinguishable.
    private static final String NEGATIVE_HIGHLIGHT_KEY = "textEnemyColor";
    private static final String POSITIVE_HIGHLIGHT_KEY = "textFriendColor";

    private static final Color NEGATIVE_HIGHLIGHT = new Color(200, 50, 50, 255);
    private static final Color POSITIVE_HIGHLIGHT = new Color(60, 180, 60, 255);

    // The grey Starsector starts every relation shade from, before either highlight pulls it.
    private static final Color RAMP_CENTRE = new Color(125, 125, 125, 255);

    @BeforeEach
    void installNamedHighlightColours() {

        StarsectorSettingsFake.buildSettings()
            .answerColours(key -> switch (key) {
                case POSITIVE_HIGHLIGHT_KEY -> POSITIVE_HIGHLIGHT;
                case NEGATIVE_HIGHLIGHT_KEY -> NEGATIVE_HIGHLIGHT;
                default -> null;
            })
            .installSettings();
    }

    @AfterEach
    void clearSettings() {
        StarsectorSettingsFake.clearSettings();
    }

    @Nested
    class ResolveRelationColour {

        @Test
        void paintsIndifferenceAtTheCentreOfTheRamp() {

            assertThat(resolveRelationColour(0f))
                .isEqualTo(RAMP_CENTRE);
        }

        @Test
        void paintsTheTopOfTheScaleInThePositiveHighlight() {

            assertThat(resolveRelationColour(1f))
                .isEqualTo(POSITIVE_HIGHLIGHT);
        }

        @Test
        void paintsTheBottomOfTheScaleInTheNegativeHighlight() {

            assertThat(resolveRelationColour(-1f))
                .isEqualTo(NEGATIVE_HIGHLIGHT);
        }

        @Test
        void clampsPastTheEndsOfTheScale() {

            assertThat(resolveRelationColour(4f))
                .isEqualTo(POSITIVE_HIGHLIGHT);
        }

        @Test
        void liftsABarelySignedRelationshipOffTheCentreByTheFloor() {

            // A twentieth of the scale would be all but invisible against the centre grey, so the
            // engine floors the pull at 0.15 of the way to the highlight - the whole reason this
            // ramp delegates rather than lerping the two ends itself.
            assertThat(resolveRelationColour(0.05f))
                .isEqualTo(new Color(115, 133, 115, 255));
        }

        @Test
        void takesTheObserversOwnShadeForAPairItPaints() {

            var factionMock = mock(FactionAPI.class);

            when(factionMock.getRelColor(TRITACHYON))
                .thenReturn(PINNED_PAIR_SHADE);

            assertThat(resolveRelationColour(factionMock, TRITACHYON, 0.6f))
                .isEqualTo(PINNED_PAIR_SHADE);
        }

        @Test
        void fallsBackToTheRampForAPairTheObserverPaintsNothingFor() {

            // getRelColor defaults to null, which is the ordinary case: most factions paint no shade
            // of their own for a pair, and the number is then the whole of the answer.
            var factionMock = mock(FactionAPI.class);

            assertThat(resolveRelationColour(factionMock, TRITACHYON, -1f))
                .isEqualTo(NEGATIVE_HIGHLIGHT);
        }
    }

    @Nested
    class ResolveRelationPalette {

        @Test
        void pairsTheRampShadeWithADarkenedFormOfItself() {

            assertThat(resolveRelationPalette(1f))
                .isEqualTo(new FactionPalette(
                    POSITIVE_HIGHLIGHT,
                    new Color(32, 95, 32, 255)));
        }

        @Test
        void pairsTheHostileEndWithADarkenedFormOfItself() {

            // The hostile end as well as the friendly one, since a palette is derived channel by
            // channel: a pair that held only at one end would not be reporting the same rule twice.
            // The two lesser channels land a hair under the half-way mark rather than on it, the
            // factor being a float slightly short of 0.53 - so they round down, not up.
            assertThat(resolveRelationPalette(-1f))
                .isEqualTo(new FactionPalette(
                    NEGATIVE_HIGHLIGHT,
                    new Color(106, 26, 26, 255)));
        }

        @Test
        void darkensTheCentreGreyForAnIndifferentRelationship() {

            assertThat(resolveRelationPalette(0f))
                .isEqualTo(new FactionPalette(
                    RAMP_CENTRE,
                    new Color(66, 66, 66, 255)));
        }
    }
}
