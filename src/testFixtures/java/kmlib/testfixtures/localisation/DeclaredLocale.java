package kmlib.testfixtures.localisation;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * One locale a mod's localisation manifest declares.
 *
 * <p>The tag doubles as the bundle's directory name under {@code l10n/} and as a release file suffix,
 * which is why its shape is held here rather than trusted: lowercased BCP 47, so {@code zh-hans} and
 * never {@code zh-Hans}, and nothing that could step out of the directory it names.
 *
 * @param localeTag         the lowercased BCP 47 tag, such as {@code en} or {@code zh-hans}
 * @param displayName       the locale's name in its own language, the one place non-ASCII is expected
 * @param coreLocalisation  the project a locale's players install over {@code starsector-core} for
 *                          glyphs the vanilla atlases lack; empty where the vanilla install suffices.
 *                          A project rather than an edition, since the editions differ only in what the
 *                          install itself reports
 */
public record DeclaredLocale(
    String localeTag,
    String displayName,
    Optional<URI> coreLocalisation) {

    // A primary language subtag, then any number of lowercase subtags. Hyphens only, since the tag is
    // also a filename suffix read on every platform.
    private static final Pattern LOCALE_TAG = Pattern.compile("[a-z]{2,3}(-[a-z0-9]{1,8})*");

    // The only scheme a release body may link to. Anything else is either a typo or a link no player
    // should be handed.
    private static final String LINKABLE_SCHEME = "https";

    /**
     * Holds the declaration to the shapes the tooling depends on.
     *
     * @param localeTag        see the record
     * @param displayName      see the record
     * @param coreLocalisation see the record
     */
    public DeclaredLocale {

        Objects.requireNonNull(localeTag, "localeTag");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(coreLocalisation, "coreLocalisation");

        if (!LOCALE_TAG.matcher(localeTag).matches()) {

            throw new IllegalArgumentException(
                "Locale tag \"" + localeTag + "\" is not a lowercased BCP 47 tag such as en or zh-hans");
        }
        if (displayName.isBlank()) {

            throw new IllegalArgumentException("Locale " + localeTag + " has a blank display name");
        }
        coreLocalisation.ifPresent(project -> requireLinkable(localeTag, project));
    }

    /**
     * A locale whose players need a core localisation installed for its glyphs.
     *
     * @param localeTag        see the record
     * @param displayName      see the record
     * @param coreLocalisation the project supplying the glyphs
     * @return the declaration
     */
    public static DeclaredLocale createLocaleWithCoreLocalisation(
            String localeTag,
            String displayName,
            URI coreLocalisation) {

        return new DeclaredLocale(localeTag, displayName, Optional.of(coreLocalisation));
    }

    /**
     * A locale the vanilla install's atlases draw on their own.
     *
     * @param localeTag   see the record
     * @param displayName see the record
     * @return the declaration
     */
    public static DeclaredLocale createLocaleWithoutCoreLocalisation(String localeTag, String displayName) {
        return new DeclaredLocale(localeTag, displayName, Optional.empty());
    }

    private static void requireLinkable(String localeTag, URI project) {

        if (!LINKABLE_SCHEME.equals(project.getScheme()) || project.getHost() == null) {

            throw new IllegalArgumentException(
                "Locale " + localeTag + " names core localisation " + project + ", which is not an https URL");
        }
    }
}
