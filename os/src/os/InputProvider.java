package os;

/**
 * InputProvider - Interface for abstracting input sources
 * Allows injection of different input providers (console, GUI popups, test mocks)
 * Used by SystemCall.input() to support multiple input methods
 */
public interface InputProvider {
    
    /**
     * Provide input from user
     * May be blocking (dialog) or non-blocking depending on implementation
     * 
     * @param processId ID of process requesting input (for UI context)
     * @param prompt Prompt message to show user
     * @return User input string, or null if cancelled/error
     */
    String provideInput(String processId, String prompt);

    /**
     * Show a process-related output dialog in GUI mode.
     * Default implementation is a no-op to keep CLI/headless mode safe.
     *
     * @param processId ID of process related to the message
     * @param title Dialog title
     * @param message Message body
     * @param isError Whether this message is an error
     */
    default void showOutputMessage(String processId, String title, String message, boolean isError) {
        // No-op by default for non-GUI providers.
    }
}
