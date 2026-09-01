package kmlib.starsector.ui.coreui;

import org.magiclib.ReflectionUtils;

import java.util.Arrays;
import java.util.List;

/**
 * What methods a shape offers, for the reaches that cannot name the member they are after.
 *
 * <p>Beside {@link CoreUiTree} rather than inside it, because the two answer about different things.
 * That one walks the live widget tree and names every hop it takes, all of them being part of the
 * core UI's contract; this one asks a class what it declares, for the members that are not - an
 * obfuscated member is renamed with each game build, so a caller reaching one has to recognise it by
 * its signature and has to be handed the whole set to recognise it out of.
 *
 * <p>The two reads are kept apart because the difference between them decides matches. What a shape
 * <em>declares</em> is its own, and is the set to match a signature against: a match run over the
 * inherited members as well would meet every method of every base class, any one of which can happen
 * to share a shape with the member being looked for, and an accidental second candidate is
 * indistinguishable from a genuinely ambiguous one. What a shape <em>publishes</em> includes what it
 * inherits, and is the set for the members a caller is entitled to call anyway.
 *
 * <p>Raises rather than answering empty when a shape cannot be read at all, leaving what that means
 * to the caller, exactly as the by-name reach beside it does. A caller matching a signature has to
 * fail open on it, and the one place that decision belongs is the one that knows what it costs.
 */
public final class CoreUiMethods {

    private CoreUiMethods() {
    }

    /**
     * What a shape declares itself, whatever its access level - which is the set a signature is
     * matched against.
     *
     * @param shape the class to look in
     * @return its own methods, inheriting nothing
     * @throws RuntimeException when the shape cannot be read, so a caller applies its own policy to
     *                          a class whose members will not resolve. Reading a member resolves
     *                          every type in its signature, so a signature naming something absent
     *                          arrives as an {@link Error} rather than as a
     *                          {@link RuntimeException} - a caller guarding this has to catch
     *                          {@link Throwable}
     */
    public static List<CoreUiMethod> readDeclaredMethodsOf(Class<?> shape) {

        return readMethods(shape.getDeclaredMethods());
    }

    /**
     * What a shape publishes, inherited members included.
     *
     * @param shape the class to look in
     * @return its public methods and those of everything it extends
     * @throws RuntimeException as {@link #readDeclaredMethodsOf} does, and for the same reason -
     *                          which includes the failure that is not a
     *                          {@link RuntimeException} at all
     */
    public static List<CoreUiMethod> readPublicMethodsOf(Class<?> shape) {

        return readMethods(shape.getMethods());
    }

    // The methods arrive as bare objects because their own type is one the game's script classloader
    // denies mod code outright: naming it here would resolve it through that loader and fail with a
    // security error, where the array they arrive in is nothing the loader has to be asked about.
    private static List<CoreUiMethod> readMethods(Object[] methods) {

        return Arrays.stream(methods)
            .map(method -> new CoreUiMethod(new ReflectionUtils.ReflectedMethod(method)))
            .toList();
    }
}
