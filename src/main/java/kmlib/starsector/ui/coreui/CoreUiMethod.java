package kmlib.starsector.ui.coreui;

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
 * <p>Wraps the member rather than exposing it, so a caller holds something it can call rather than a
 * reflection object it could drive itself. Reaching for reflection at all stays this package's
 * business, and a member handed out of it is one that has already been through
 * {@link ReflectionBypass}.
 */
public final class CoreUiMethod {

    private final ReflectedMethod method;

    /**
     * Wraps a member the reach beside this one has already found, which is why it is not reachable
     * from outside the package: what goes in is the reach's own answer, not anything a caller has.
     *
     * @param method the member as the reach hands it over
     */
    CoreUiMethod(ReflectedMethod method) {
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

        // Copied rather than wrapped. The reach holds one array per member and hands that same
        // instance back at every ask, so anything written through it would be written into what
        // every later reader sees.
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
     *                          {@link RuntimeException} - the target's own throw comes back wrapped
     *                          in a checked exception, rethrown as it was thrown rather than
     *                          replaced - so a caller guarding this has to catch {@link Throwable}
     */
    public Object invokeOn(Object instance, Object... arguments) {
        return method.invokeOn(instance, arguments);
    }
}
