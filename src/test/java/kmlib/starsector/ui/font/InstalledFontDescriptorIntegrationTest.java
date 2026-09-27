package kmlib.starsector.ui.font;

import kmlib.testfixtures.starsector.ui.font.FontDescriptorHeader;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Holds every face KM text may draw in to the descriptor the install on this machine actually carries
 * under its basename: present, headed the way LazyLib requires, and on one page, which is all the game's
 * own loader reads. Whichever install the build located - vanilla, or one a core localisation has been
 * laid over - is the one checked, so a machine holding a localised install checks its atlases against
 * the same bar as a vanilla one.
 *
 * <p>Walks the enum, never the fonts folder: an install running a localisation's dynamic-font pipeline
 * writes generated descriptors and a subfolder of its own into that folder, and a listing would take
 * them for faces KM offers.
 *
 * <p>Skips where the build located no install, the check being about one.
 */
class InstalledFontDescriptorIntegrationTest {

    // The system property the build hands the located install's root in.
    private static final String STARSECTOR_ROOT_PROPERTY = "kmlib.starsectorRoot";

    // Where under the root the faces' paths resolve from, the game reading them off its core folder.
    private static final String CORE_DIRECTORY = "starsector-core";

    @Nested
    class ReadDescriptorHeader {

        @ParameterizedTest
        @EnumSource(StarsectorFont.class)
        void readDescriptorHeaderFindsEveryFaceInstalledInAShapeLazyLibAndTheGameBothLoad(StarsectorFont font) {

            var descriptorFile = resolveInstalledDescriptor(font);

            assertThat(descriptorFile)
                .isRegularFile();

            var header = FontDescriptorHeader.readDescriptorHeader(descriptorFile);

            assertThat(header.tokenCount())
                .as("header token count of %s", descriptorFile)
                .isEqualTo(51);
            assertThat(header.pageCount())
                .as("page count of %s", descriptorFile)
                .isEqualTo(1);
        }
    }

    // The descriptor the install carries for a face, or a skip where the build located no install.
    private static Path resolveInstalledDescriptor(StarsectorFont font) {

        var starsectorRoot = System.getProperty(STARSECTOR_ROOT_PROPERTY);
        assumeTrue(starsectorRoot != null, "The build handed no install root");

        var coreDirectory = Path.of(starsectorRoot, CORE_DIRECTORY);
        assumeTrue(Files.isDirectory(coreDirectory), "No install at " + starsectorRoot);

        return coreDirectory.resolve(font.resolvePath());
    }
}
