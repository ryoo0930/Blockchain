package blockchain;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

public class TextAreaHandler extends Handler {
    private final JTextArea textArea;
    private final Formatter formatter = new SimpleFormatter();

    public TextAreaHandler(JTextArea textArea) {
        this.textArea = textArea;
    }

    @Override
    public void publish(LogRecord record) {
        if (record == null) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            String message = formatter.formatMessage(record);
            textArea.append(record.getLevel().getName() + ": " + message + "\n");
            textArea.setCaretPosition(textArea.getDocument().getLength());
        });
    }

    @Override
    public void flush() {
        // No implementation needed
    }

    @Override
    public void close() throws SecurityException {
        // No implementation needed
    }
}
