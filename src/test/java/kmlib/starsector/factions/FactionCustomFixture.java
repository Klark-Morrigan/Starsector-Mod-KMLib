package kmlib.starsector.factions;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Builds the faction custom-data shapes a suite needs to drive a territoriality read, so the
 * nested shape vanilla stores - a punitive-expedition object holding a territorial flag - is
 * spelled once rather than restated by every suite that wants a territorial faction.
 *
 * <p>Public so suites in other packages can reach it, and sited beside {@link FactionFlags}
 * because that is what reads the shape. The keys are written out again here rather than shared
 * with the production class, so a production read pointed at the wrong key fails instead of
 * agreeing with itself.
 */
public final class FactionCustomFixture {

    private static final String PUNITIVE_EXPEDITION_DATA = "punitiveExpeditionData";
    private static final String TERRITORIAL_FLAG = "territorial";

    private FactionCustomFixture() {
    }

    /**
     * Custom data carrying a punitive-expedition object with the given territorial flag. The
     * checked JSONException cannot arise for literal keys, so it is rethrown unchecked rather
     * than declared across every caller.
     */
    public static JSONObject buildPunitiveExpeditionCustom(boolean isTerritorial) {
        try {
            return new JSONObject().put(PUNITIVE_EXPEDITION_DATA,
                new JSONObject().put(TERRITORIAL_FLAG, isTerritorial));
        } catch (JSONException failure) {
            throw new IllegalStateException(failure);
        }
    }
}
