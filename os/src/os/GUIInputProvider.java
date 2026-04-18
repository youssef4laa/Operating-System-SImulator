package os;

import javax.swing.*;

/**
 * GUIInputProvider - Implements InputProvider using Swing popup dialogs
 * Shows input dialog with process context so user knows which process needs input
 * Thread-safe: uses SwingUtilities to ensure dialog is shown on EDT
 */
public class GUIInputProvider implements InputProvider {
    
    /**
     * Show a modal input dialog for user interaction
     * Dialog includes process ID so user understands context
     * 
     * @param processId ID of process requesting input
     * @param prompt Prompt to show user
     * @return User's input string, or null if cancelled/error
     */
    @Override
    public String provideInput(String processId, String prompt) {
        final String[] result = {null};
        
        // Ensure dialog is shown on EDT (Event Dispatch Thread)
        if (SwingUtilities.isEventDispatchThread()) {
            result[0] = showDialog(processId, prompt);
        } else {
            try {
                SwingUtilities.invokeAndWait(() -> {
                    result[0] = showDialog(processId, prompt);
                });
            } catch (Exception e) {
                System.err.println("[INPUT DIALOG ERROR] " + e.getMessage());
                return null;
            }
        }
        
        return result[0];
    }

    /**
     * Show a modal output dialog (error/info) for process-related events.
     */
    @Override
    public void showOutputMessage(String processId, String title, String message, boolean isError) {
        Runnable dialogTask = () -> showMessageDialog(processId, title, message, isError);

        if (SwingUtilities.isEventDispatchThread()) {
            dialogTask.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(dialogTask);
            } catch (Exception e) {
                System.err.println("[OUTPUT DIALOG ERROR] " + e.getMessage());
            }
        }
    }
    
    /**
     * Internal method to show the actual dialog
     */
    private String showDialog(String processId, String prompt) {
        String title = "Process " + processId + " - Input Request";
        String message = "Process " + processId + " is waiting for input:\n\n" + prompt;
        
        String input = JOptionPane.showInputDialog(
            null,
            message,
            title,
            JOptionPane.QUESTION_MESSAGE
        );
        
        return input; // null if cancelled, otherwise the string entered
    }

    /**
     * Internal method to show output/error dialogs.
     */
    private void showMessageDialog(String processId, String title, String message, boolean isError) {
        String fullMessage = "Process " + processId + ":\n\n" + message;
        int messageType = isError ? JOptionPane.ERROR_MESSAGE : JOptionPane.INFORMATION_MESSAGE;

        JOptionPane.showMessageDialog(
            null,
            fullMessage,
            title,
            messageType
        );
    }
}
