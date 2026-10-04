package kmlib.testfixtures.localisation;

import kmlib.testfixtures.starsector.settings.StarsectorSettingsFake;

import java.util.List;

/**
 * The locales a mod ships, read from its own {@code localisation/} directory, for a case that composes text
 * once per locale.
 *
 * <p>A parameterised case names {@code kmlib.testfixtures.localisation.ShippedLocales#listLocaleTags} as its
 * source, then stands each locale's strings up with {@link #installLocaleStrings(String)} before composing.
 */
public final class ShippedLocales {

    private ShippedLocales() {
    }

    /**
     * Stands the settings up answering one locale's strings, read from that locale's bundle.
     *
     * @param localeTag a locale the manifest declares
     */
    public static void installLocaleStrings(String localeTag) {

        var directory = new LocalisationDirectory(LocalisationDirectory.LOCALISATION_DIRECTORY);
        var locale = directory
            .readManifest()
            .declaredLocalesByTag()
            .get(localeTag);

        if (locale == null) {
            throw new IllegalArgumentException("The manifest declares no locale " + localeTag);
        }
        StarsectorSettingsFake.installSettings(directory.openBundle(locale).readStringSource());
    }

    /**
     * Every locale the manifest declares.
     *
     * @return their tags, sorted
     */
    public static List<String> listLocaleTags() {

        return List.copyOf(new LocalisationDirectory(LocalisationDirectory.LOCALISATION_DIRECTORY)
            .readManifest()
            .declaredLocalesByTag()
            .keySet());
    }
}
