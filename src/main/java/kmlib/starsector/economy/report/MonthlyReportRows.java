package kmlib.starsector.economy.report;

import com.fs.starfarer.api.campaign.econ.MonthlyReport;

/**
 * Rows a mod books on the monthly report.
 */
public final class MonthlyReportRows {

    // Vanilla's own label for the fleet parent. Vanilla writes it on every economy tick; a mod sets it only where no
    // one has yet, so a translated label survives.
    private static final String FLEET_NODE_NAME = "Fleet";

    private MonthlyReportRows() {
    }

    /**
     * The row {@code rowId} under the report's fleet parent, where vanilla books crew pay and commission stipends. The
     * caller adds its income and upkeep to the row it gets back.
     *
     * <p>The fleet parent is set up the way vanilla sets it - its label, its key and the report's tooltip - where no
     * writer has yet, so the panel renders it the same whichever writer lands first. The row takes {@code rowName}
     * only on its first write and keeps it after. Its key is left unset, so vanilla's tooltip does not describe it as
     * the fleet's own line.
     *
     * @param report  the report to book on
     * @param rowId   the row's ID, unique under the fleet parent
     * @param rowName the label the row shows
     * @return the row
     */
    public static MonthlyReport.FDNode openFleetRow(MonthlyReport report, String rowId, String rowName) {

        var fleetNode = report.getNode(MonthlyReport.FLEET);
        if (fleetNode.name == null) {

            fleetNode.name = FLEET_NODE_NAME;
            fleetNode.custom = MonthlyReport.FLEET;
            fleetNode.tooltipCreator = report.getMonthlyReportTooltip();
        }

        var row = report.getNode(fleetNode, rowId);
        if (row.name == null) {
            row.name = rowName;
        }

        return row;
    }
}
