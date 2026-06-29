package kmlib.testfixtures.console.output;

import kmlib.console.output.CommandOutput;

import java.util.ArrayList;
import java.util.List;

/**
 * Recording {@link CommandOutput} that keeps every message in order instead of
 * routing it to a live overlay, so a command's player-facing feedback can be
 * read back outside a running game. Shipped from KMLib so every KM mod drives
 * the console-output seam through one shared double.
 */
public final class CommandOutputFake implements CommandOutput {
    private final List<String> messages = new ArrayList<>();

    @Override
    public void showMessage(String message) {
        messages.add(message);
    }

    public List<String> getMessages() {
        return messages;
    }
}
