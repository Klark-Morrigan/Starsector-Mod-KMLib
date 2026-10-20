package kmlib.starsector.economy.report;

import com.fs.starfarer.api.campaign.econ.MonthlyReport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers opening a mod's row under the monthly report's fleet parent, against a real report.
 */
final class MonthlyReportRowsTests {

    private MonthlyReport report;

    @BeforeEach
    void setUp() {

        report = new MonthlyReport();
    }

    @Nested
    class OpenFleetRow {

        @Test
        void filesTheRowUnderTheFleetParent() {

            var row = MonthlyReportRows.openFleetRow(report, "mod_row", "Mod Row");

            assertThat(report.getNode(MonthlyReport.FLEET).getChildren())
                .containsEntry("mod_row", row);
        }

        @Test
        void namesTheRowOnItsFirstWrite() {

            assertThat(MonthlyReportRows.openFleetRow(report, "mod_row", "Mod Row").name)
                .isEqualTo("Mod Row");
        }

        @Test
        void keepsTheFirstNameOnALaterWrite() {

            MonthlyReportRows.openFleetRow(report, "mod_row", "Mod Row");

            assertThat(MonthlyReportRows.openFleetRow(report, "mod_row", "Renamed Row").name)
                .isEqualTo("Mod Row");
        }

        @Test
        void returnsTheSameRowSoBookingsAddUp() {

            MonthlyReportRows.openFleetRow(report, "mod_row", "Mod Row").income += 100f;
            MonthlyReportRows.openFleetRow(report, "mod_row", "Mod Row").income += 50f;

            assertThat(report.getNode(MonthlyReport.FLEET).getChildren().get("mod_row").income)
                .isEqualTo(150f);
        }

        @Test
        void setsUpAnUnwrittenFleetParentAsVanillaDoes() {

            MonthlyReportRows.openFleetRow(report, "mod_row", "Mod Row");

            var fleetNode = report.getNode(MonthlyReport.FLEET);

            assertThat(fleetNode.name)
                .isEqualTo("Fleet");
            assertThat(fleetNode.custom)
                .isEqualTo(MonthlyReport.FLEET);
        }

        @Test
        void leavesAFleetParentAnotherWriterNamed() {

            // A translated label vanilla wrote first survives.
            report.getNode(MonthlyReport.FLEET).name = "Flotte";

            MonthlyReportRows.openFleetRow(report, "mod_row", "Mod Row");

            assertThat(report.getNode(MonthlyReport.FLEET).name)
                .isEqualTo("Flotte");
        }
    }
}
