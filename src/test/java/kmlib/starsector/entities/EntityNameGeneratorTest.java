package kmlib.starsector.entities;

import com.fs.starfarer.api.campaign.SectorEntityToken;

import kmlib.starsector.strings.StarsectorStrings;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Pins {@link EntityNameGenerator#generateJumpPointName}: the focus label leads,
 * then the localised jump-point word, then the orbit radius in scientific
 * notation - and a focus with no display name falls back to its id so a nameless
 * barycenter still yields a readable label.
 *
 * <p>The localised word is pulled through {@code StarsectorStrings}, which reads
 * strings.json via {@code Global}; it is stubbed to a fixed word so the test
 * pins the assembly, not the localisation lookup. Cases live under a
 * {@link Nested} group named for the method under test.
 */
final class EntityNameGeneratorTest {

    private MockedStatic<StarsectorStrings> stringsMock;

    @BeforeEach
    void setUp() {
        stringsMock = mockStatic(StarsectorStrings.class);
        stringsMock.when(() -> StarsectorStrings.get(anyString(), anyString()))
                .thenReturn("Jump-point");
    }

    @AfterEach
    void tearDown() {
        stringsMock.close();
    }

    @Nested
    class GenerateJumpPointName {
        @Test
        void leadsWithTheFocusNameWhenItHasOne() {
            var focusMock = mock(SectorEntityToken.class);
            when(focusMock.getName()).thenReturn("Corvus A");

            var name = EntityNameGenerator.generateJumpPointName(focusMock, 1500f);

            assertThat(name).isEqualTo("Corvus A Jump-point 1.5e3");
        }

        @Test
        void fallsBackToTheFocusIdWhenItHasNoName() {
            var focusMock = mock(SectorEntityToken.class);
            when(focusMock.getName()).thenReturn(null);
            when(focusMock.getId()).thenReturn("barycenter");

            var name = EntityNameGenerator.generateJumpPointName(focusMock, 1500f);

            assertThat(name).isEqualTo("barycenter Jump-point 1.5e3");
        }
    }
}
