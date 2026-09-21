package kmlib;

/**
 * The library's own identity: the ID it is installed, keyed and reported under.
 *
 * <p>Held apart from {@link KMLib_ModPlugin} so a class deep in the library can say who it belongs
 * to without naming the entry point. Referring to the plugin for a constant loads it - a
 * {@code BaseModPlugin} subclass with its own logger and its own imports - into anything that
 * touches the referring class, which is a large amount of machinery to drag in for a string.
 *
 * <p>The library needs an ID of its own for the same reason a consuming mod does. Where one of its
 * start-up steps binds to a third-party mod, the library is the mod that loses something by that
 * binding, and a compatibility record latches on the pair of third party and consumer - so it files
 * under this the way a consuming mod files under its.
 */
public final class KmlibMod {

    /** The mod ID, as {@code mod_info.json} declares it, and the key its LunaLib settings sit under. */
    public static final String MOD_ID = "kmlib";

    private KmlibMod() {
        // constants holder, no instances.
    }
}
