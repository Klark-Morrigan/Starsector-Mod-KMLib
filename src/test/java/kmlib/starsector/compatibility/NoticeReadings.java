package kmlib.starsector.compatibility;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads a reported failure the ways the integration suites compare it: every line of its notice, the
 * values of the notice's rows, and the rows of its log block.
 *
 * <p>Shared because every kind of report is held to the same two rules - every highlighted run shows,
 * and the log block carries the notice's rows - and a copy per kind is a copy that drifts.
 */
final class NoticeReadings {

    private NoticeReadings() {
    }

    // The notice's lines in reading order: heading, diagnosis, rows, closing line.
    static List<CompatibilityNoticeLine> listNoticeLines(ReportedFailure failure) {

        var lines = new ArrayList<CompatibilityNoticeLine>();

        lines.add(failure.describeHeadingForPlayer());
        lines.addAll(failure.describeDiagnosisForPlayer());
        lines.addAll(failure.describeRowsForPlayer());
        lines.add(failure.describeClosingForPlayer());

        return lines;
    }

    // The value of each notice row, in reading order. Every row highlights exactly one run, its value,
    // so comparing values rather than rendered rows lets a translated label or a re-padded column pass.
    static String[] readNoticeRowValues(ReportedFailure failure) {

        return failure
            .describeRowsForPlayer()
            .stream()
            .map(row -> row.emphasisedRuns().get(0).runText())
            .toArray(String[]::new);
    }

    // The log block's rows, which is every line of it but the heading.
    static int countLogRows(ReportedFailure failure) {

        return failure.describeForLog().split("\n").length - 1;
    }
}
