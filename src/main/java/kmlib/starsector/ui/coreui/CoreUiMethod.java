package kmlib.starsector.ui.coreui;

import org.magiclib.ReflectionUtils;

import java.util.List;

/**
 * One method a shape offers, described by what it looks like rather than by what it is called, and
 * callable without naming it.
 *
 * <p>The distinction is the whole point on an obfuscated shape. A member's name is regenerated with
 * every game build, while its signature follows from what it does and survives - so the only durable
 * way to reach a member the game does not publish is to say what it takes and what it answers with,
 * and let the name fall where it may. That is a different question from {@link CoreUiTree}'s, which
 * names its hops because the ones it takes are part of the core UI's contract.
 *
 * <p>Wraps the reach rather than exposing it, so a caller holds a member it can call rather than a
 * reflection object it could drive itself. Naming that dependency stays this package's business, and
 * a member handed out of it is a member that has already been through the bypass.
 */
public final class CoreUiMethod {

    private final ReflectionUtils.ReflectedMethod method;

    /**
     * Wraps a method the reach beside this one has already read off a shape, which is why it is not
     * reachable from outside the package: what goes in is a reflection object nothing here may name.
     *
     * @param method the method as the reach hands it over
     */
    CoreUiMethod(ReflectionUtils.ReflectedMethod method) {
        this.method = method;
    }

    /**
     * @return the member's own name, which on an obfuscated shape says nothing that survives a game
     *         build - worth matching on only for the members the game leaves unobfuscated
     */
    public String getName() {
        return method.getName();
    }

    /**
     * @return what it takes, in declaration order, a primitive parameter as its own primitive type
     *         rather than as the box for it
     */
    public List<Class<?>> getParameterTypes() {
        return List.of(method.getParameterTypes());
    }

    /**
     * @return what it answers with, {@code void.class} for a method that answers nothing
     */
    public Class<?> getReturnType() {
        return method.getReturnType();
    }

    /**
     * Calls it, reaching a member its shape keeps to itself as readily as one it publishes.
     *
     * @param instance  the object to call on
     * @param arguments what to pass, matching the parameter list above
     * @return whatever it answered, or null for a void one
     * @throws RuntimeException when the call fails. It arrives undeclared and not necessarily as a
     *                          {@link RuntimeException} - the bypass is Kotlin, which lets the
     *                          checked exception wrapping the target's own throw escape unannounced -
     *                          so a caller guarding this has to catch {@link Throwable}
     */
    public Object invokeOn(Object instance, Object... arguments) {
        return method.invoke(instance, arguments);
    }
}
