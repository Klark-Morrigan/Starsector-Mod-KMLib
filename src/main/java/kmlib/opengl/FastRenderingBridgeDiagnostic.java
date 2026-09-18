package kmlib.opengl;

import org.lwjgl.util.vector.Matrix4f;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Which of the Fast Rendering bridge members KMLib mirrors no longer hold on the installed jar, and
 * which release that jar reports itself as: what turns a {@link LinkageError} out of the bridge
 * binding into a sentence worth sending to the renderer's author.
 *
 * <p>A binding that stops holding surfaces as a {@code NoClassDefFoundError} or a
 * {@code NoSuchMethodError} naming one member, thrown from whichever pass reached it first. That
 * names where the JVM gave up rather than what moved: the member named is the first one resolved,
 * and the rest go unchecked. This probes all six by name and full signature and names every one
 * that does not hold.
 *
 * <p>String-only, like {@link FastRendering}: nothing here names a Fast Rendering type, so the class
 * loads on a stock install and the binding it describes stays the direct, compile-checked one. It
 * runs on the failure path alone - the healthy path pays nothing for it - which is what makes a
 * lookup per member affordable.
 *
 * <p>Looked up through method handles rather than {@code java.lang.reflect}, which the game's script
 * classloader refuses to mod code; {@code java.lang.invoke} it does not. A handle lookup checks the
 * name and the whole signature in one step, so a member that was re-signatured reads the same as one
 * that was removed. That is the honest reading either way: the direct binding fails identically for
 * both, and "missing or re-signatured" is what the report says.
 *
 * <p>The installed version is read apart from the members, off
 * {@code com.genir.renderer.Version.getVersion()} - present from v0.8.2, and genir's own private
 * class rather than a convention. A version read that threw while the members were being reported
 * would lose the whole report, which is worse than reporting an unknown version, so it fails to
 * {@code null} on its own. Display only: nothing branches on it.
 *
 * @param boundVersion     the release the bridge adapter was type-checked against, or {@code null}
 *                         where the build read none
 * @param installedVersion the release the installed jar reports, or {@code null} where reading it
 *                         failed - a jar older than v0.8.2 carries no version class at all
 * @param brokenMembers    every mirrored member that does not hold, in mirror order; empty where all
 *                         six do, which after a failure means the binding broke at call time rather
 *                         than at link time
 */
public record FastRenderingBridgeDiagnostic(
    String boundVersion,
    String installedVersion,
    List<BrokenMember> brokenMembers) {

    // The names probed, spelled out from the one prefix detection matches on. The context package
    // holds everything the adapter reads; the command it enqueues is the one type outside it.
    private static final String BRIDGE_CONTEXT_PACKAGE = FastRendering.BRIDGE_PACKAGE_PREFIX + "bridge.context.";
    private static final String CONTEXT_CLASS_NAME = BRIDGE_CONTEXT_PACKAGE + "Context";
    private static final String CONTEXT_MANAGER_CLASS_NAME = BRIDGE_CONTEXT_PACKAGE + "ContextManager";
    private static final String EXECUTOR_CLASS_NAME = BRIDGE_CONTEXT_PACKAGE + "Executor";
    
    private static final String GL_COMMAND_CLASS_NAME =
        FastRendering.BRIDGE_PACKAGE_PREFIX + "bridge.interfaces.GLCommand";

    private static final String TRANSFORM_MANAGER_CLASS_NAME = BRIDGE_CONTEXT_PACKAGE + "TransformManager";
    private static final String VERSION_CLASS_NAME = FastRendering.BRIDGE_PACKAGE_PREFIX + "Version";
    private static final String VERSION_METHOD_NAME = "getVersion";

    private static final String BROKEN_MEMBER_SEPARATOR = "; ";

    // What the detail says where every member holds. Stated as a finding rather than left blank,
    // because after a failure it is one: the binding broke somewhere a signature check cannot see.
    private static final String NO_BROKEN_MEMBERS_WORDING = "no mirrored bridge member is missing or re-signatured";

    // Taken here rather than where a member is found, because a lookup carries the access of the
    // class that asked for it, and the bridge's members are public - this class's own access is
    // all that reaching them needs.
    private static final MethodHandles.Lookup MEMBER_LOOKUP = MethodHandles.lookup();

    // The six members the compile-only mirrors declare, in the order they are declared, each as the
    // exact signature the adapter is compiled against. A probe that names the same member with a
    // different signature is the same drift the real-jar build leg would refuse to compile.
    private static final List<MirroredMember> MIRRORED_MEMBERS = List.of(
        new MirroredMember("ContextManager.getThreadContext", classes -> MEMBER_LOOKUP.findStatic(
            classes.loadClass(CONTEXT_MANAGER_CLASS_NAME),
            "getThreadContext",
            MethodType.methodType(classes.loadClass(CONTEXT_CLASS_NAME)))),
        new MirroredMember("Context.transformManager", classes -> MEMBER_LOOKUP.findGetter(
            classes.loadClass(CONTEXT_CLASS_NAME),
            "transformManager",
            classes.loadClass(TRANSFORM_MANAGER_CLASS_NAME))),
        new MirroredMember("Context.exec", classes -> MEMBER_LOOKUP.findGetter(
            classes.loadClass(CONTEXT_CLASS_NAME),
            "exec",
            classes.loadClass(EXECUTOR_CLASS_NAME))),
        new MirroredMember("Executor.execute(GLCommand)", classes -> MEMBER_LOOKUP.findVirtual(
            classes.loadClass(EXECUTOR_CLASS_NAME),
            "execute",
            MethodType.methodType(void.class, classes.loadClass(GL_COMMAND_CLASS_NAME)))),
        new MirroredMember("GLCommand.run", classes -> MEMBER_LOOKUP.findVirtual(
            classes.loadClass(GL_COMMAND_CLASS_NAME),
            "run",
            MethodType.methodType(void.class, classes.loadClass(CONTEXT_CLASS_NAME), float[].class, int.class))),
        new MirroredMember("TransformManager.getCPUModelView", classes -> MEMBER_LOOKUP.findVirtual(
            classes.loadClass(TRANSFORM_MANAGER_CLASS_NAME),
            "getCPUModelView",
            MethodType.methodType(Matrix4f.class))));

    public FastRenderingBridgeDiagnostic {

        brokenMembers = List.copyOf(Objects.requireNonNull(
            brokenMembers,
            "A diagnostic with no member list would report neither that the bridge holds nor that it broke."));
    }

    /**
     * The broken members as the phrase a log line appends: each member with why it does not hold,
     * or the statement that every one does.
     *
     * @return the phrase, never blank
     */
    public String describeBrokenMembers() {

        if (brokenMembers.isEmpty()) {
            return NO_BROKEN_MEMBERS_WORDING;
        }
        return String.join(
            BROKEN_MEMBER_SEPARATOR,
            brokenMembers.stream().map(BrokenMember::describe).toList());
    }

    /**
     * @return {@code true} where at least one mirrored member does not hold
     */
    public boolean hasBrokenMembers() {

        return !brokenMembers.isEmpty();
    }

    /**
     * Probes the bridge this jar is bound to, through the loader that binding resolves through, so
     * what is reported is the same set of classes the binding itself reached.
     *
     * <p>Never throws: a probe that failed while describing a failure would replace the report with
     * its own trace. A member whose lookup fails for any reason is reported as broken with that
     * reason, and a version that cannot be read is reported as none.
     *
     * @return what holds, what does not, and the two versions
     */
    public static FastRenderingBridgeDiagnostic probeInstalledBridge() {

        return probeBridge(FastRenderingBridgeDiagnostic::loadThroughOwnLoader);
    }

    /**
     * {@link #probeInstalledBridge()} against an explicit {@link ClassLookup}: the six lookups and
     * the version read, with whatever the caller hands in answering the names.
     *
     * @param classes what answers a bridge class name
     * @return what holds, what does not, and the two versions
     */
    static FastRenderingBridgeDiagnostic probeBridge(ClassLookup classes) {

        Objects.requireNonNull(classes, "A probe with nothing answering class names could find no member.");

        var brokenMembers = new ArrayList<BrokenMember>();

        for (var member : MIRRORED_MEMBERS) {
            try {
                member.probe().findMember(classes);

            // Every way a lookup fails is one finding: the class is gone, or its own dependency is
            // (a LinkageError out of loading it), or the member is not there with this signature,
            // or a loader refused the name. Which one is kept as the reason.
            } catch (ReflectiveOperationException | LinkageError | RuntimeException memberFailure) {
                brokenMembers.add(new BrokenMember(member.name(), describeFailure(memberFailure)));
            }
        }

        return new FastRenderingBridgeDiagnostic(
            FastRenderingBinding.readBoundVersion(),
            readInstalledVersion(classes),
            brokenMembers);
    }

    // The class and message, the message alone being "exec" for a missing field and the class
    // alone being nothing a reader can act on.
    private static String describeFailure(Throwable failure) {

        var failureKind = failure.getClass().getSimpleName();
        return failure.getMessage() == null
            ? failureKind
            : failureKind + ": " + failure.getMessage();
    }

    // Without initialising: the probe reads shapes, and running a bridge class's static
    // initialiser from a failure path is a side effect nothing here wants.
    private static Class<?> loadThroughOwnLoader(String className) throws ClassNotFoundException {

        return Class.forName(className, false, FastRenderingBridgeDiagnostic.class.getClassLoader());
    }

    // Guarded on its own, and over everything a handle call can throw - its invoke declares
    // Throwable - so that a version read failing in any way costs the version and nothing else. A
    // fault in the JVM itself is the one thing not worth trading a report for.
    private static String readInstalledVersion(ClassLookup classes) {

        try {
            var readVersion = MEMBER_LOOKUP.findStatic(
                classes.loadClass(VERSION_CLASS_NAME),
                VERSION_METHOD_NAME,
                MethodType.methodType(String.class));
            return (String) readVersion.invoke();

        } catch (VirtualMachineError jvmFailure) {
            throw jvmFailure;

        } catch (Throwable readFailure) {
            return null;
        }
    }

    /**
     * One mirrored member that does not hold, and why.
     *
     * @param member the member as the mirrors name it, owner and member together
     * @param reason what the lookup threw, as its kind and message
     */
    public record BrokenMember(String member, String reason) {

        /**
         * @return the member with its reason in parentheses
         */
        public String describe() {

            return member + " (" + reason + ")";
        }
    }

    /** What answers a bridge class name, for the probe that takes one explicitly. */
    @FunctionalInterface
    interface ClassLookup {

        /**
         * @param className the binary name of a bridge class
         * @return the class
         * @throws ClassNotFoundException where nothing answers to the name
         */
        Class<?> loadClass(String className) throws ClassNotFoundException;
    }

    // One member's lookup by name and exact signature, against whatever answers the class names.
    @FunctionalInterface
    private interface MemberProbe {

        void findMember(ClassLookup classes) throws ReflectiveOperationException;
    }

    // A mirrored member: how the mirrors name it, and the lookup that finds it or does not.
    private record MirroredMember(String name, MemberProbe probe) {
    }
}
