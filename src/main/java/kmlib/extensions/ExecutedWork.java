package kmlib.extensions;

/**
 * The work was performed by the installed implementation, and nothing else should do it.
 *
 * <p>Carries nothing. What was done is the implementation's own business and the operation that
 * offered the work has no use for a description of it - only for knowing that it is done, which is
 * the whole of what this says.
 */
public record ExecutedWork() implements WorkOutcome {

    @Override
    public boolean wasExecuted() {
        return true;
    }
}
