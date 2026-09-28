package kmlib.starsector.ui.font;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AtlasSmoothingTest {

    @Nested
    class ResolveFromInfoLine {

        @Test
        void resolveFromInfoLineReadsASingleSampleAsPixelExact() {
            // Vanilla's victor10 line, hard-edged at aa=1.
            assertThat(AtlasSmoothing.resolveFromInfoLine("info face=\"Victor\" size=-10 smooth=1 aa=1"))
                .isEqualTo(AtlasSmoothing.PIXEL_EXACT);
        }

        @Test
        void resolveFromInfoLineReadsSeveralSamplesAsSmoothed() {

            assertThat(AtlasSmoothing.resolveFromInfoLine("info face=\"InsigniaLT\" size=15 smooth=1 aa=4"))
                .isEqualTo(AtlasSmoothing.SMOOTHED);
        }

        @Test
        void resolveFromInfoLineReadsTheSampleCountAndNotTheSmoothFlag() {
            // orbitron12condensed's case: smooth=1 and still hard-edged, which only aa says.
            assertThat(AtlasSmoothing.resolveFromInfoLine("info face=\"Orbitron\" size=-12 smooth=1 aa=1"))
                .isEqualTo(AtlasSmoothing.PIXEL_EXACT);
        }

        @Test
        void resolveFromInfoLineReadsALineStatingNoSampleCountAsSmoothed() {

            assertThat(AtlasSmoothing.resolveFromInfoLine("info face=\"Pack\" size=15"))
                .isEqualTo(AtlasSmoothing.SMOOTHED);
        }

        @Test
        void resolveFromInfoLineReadsNoLineAsSmoothed() {

            assertThat(AtlasSmoothing.resolveFromInfoLine(null))
                .isEqualTo(AtlasSmoothing.SMOOTHED);
        }
    }
}
