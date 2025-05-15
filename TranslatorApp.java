import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.*;

public class TranslatorApp {
    private Process pythonProcess;
    private BufferedWriter pythonWriter;
    private BufferedReader pythonReader;
    private BufferedReader pythonErrorReader;

    private JTextPane indoTextPane;
    private JButton startButton;

    private Font emojiFont;

    // Language options: {code, displayName}
    private final String[][] languages = {
        {"id", "Indonesian"},
        {"hi", "Hindi"},
        {"en", "English"},
        {"fr", "French"},
        {"de", "German"},
        {"es", "Spanish"},
        {"zh-cn", "Chinese (Simplified)"},
        {"ja", "Japanese"},
        {"ar", "Arabic"},
        {"ru", "Russian"},
        {"ko", "Korean"},
        {"it", "Italian"},
        {"pt", "Portuguese"},
        {"tr", "Turkish"},
        {"nl", "Dutch"},
        {"th", "Thai"}
    };

    private JComboBox<String> sourceLangCombo;
    private JComboBox<String> targetLangCombo;

    // Styles for text pane
    private Style hindiStyle;
    private Style emojiStyle;
    private Style defaultStyle;

    public TranslatorApp() {
        emojiFont = new Font("Segoe UI Emoji", Font.PLAIN, 14);

        JFrame frame = new JFrame("Speech Translator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 500);
        frame.setLayout(new BorderLayout());

        // Top panel with language selectors
        JPanel topPanel = new JPanel();

        sourceLangCombo = new JComboBox<>();
        targetLangCombo = new JComboBox<>();

        for (String[] lang : languages) {
            sourceLangCombo.addItem(lang[1]);
            targetLangCombo.addItem(lang[1]);
        }

        sourceLangCombo.setSelectedItem("Indonesian");
        targetLangCombo.setSelectedItem("Hindi");

        topPanel.add(new JLabel("Source Language:"));
        topPanel.add(sourceLangCombo);
        topPanel.add(new JLabel("Target Language:"));
        topPanel.add(targetLangCombo);

        frame.add(topPanel, BorderLayout.NORTH);

        // Text pane with styled document
        indoTextPane = new JTextPane();
        indoTextPane.setEditable(false);
        indoTextPane.setBorder(BorderFactory.createTitledBorder("Translation Output"));

        // Setup styles
        StyledDocument doc = indoTextPane.getStyledDocument();

        defaultStyle = doc.addStyle("Default", null);
        StyleConstants.setFontFamily(defaultStyle, "Segoe UI");
        StyleConstants.setFontSize(defaultStyle, 14);

        emojiStyle = doc.addStyle("Emoji", null);
        StyleConstants.setFontFamily(emojiStyle, emojiFont.getFamily());
        StyleConstants.setFontSize(emojiStyle, 14);

        hindiStyle = doc.addStyle("Hindi", null);
        StyleConstants.setFontFamily(hindiStyle, "Nirmala UI");
        StyleConstants.setFontSize(hindiStyle, 16);
        System.out.println("Using font for Hindi: Nirmala UI");

        frame.add(new JScrollPane(indoTextPane), BorderLayout.CENTER);

        // Bottom panel with start button
        startButton = new JButton("Start");
        startButton.addActionListener(e -> sendTranslateCommand());

        JPanel bottomPanel = new JPanel();
        bottomPanel.add(startButton);

        frame.add(bottomPanel, BorderLayout.SOUTH);

        frame.setVisible(true);

        startPythonProcess();
    }

    private void startPythonProcess() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "C:/Users/student/AppData/Local/Programs/Python/Python312/python.exe",
                "D:/Python/translator.py"
            );
            pythonProcess = pb.start();

            pythonWriter = new BufferedWriter(new OutputStreamWriter(pythonProcess.getOutputStream()));
            pythonReader = new BufferedReader(new InputStreamReader(pythonProcess.getInputStream()));
            pythonErrorReader = new BufferedReader(new InputStreamReader(pythonProcess.getErrorStream()));

            new Thread(() -> {
                String line;
                try {
                    while ((line = pythonReader.readLine()) != null) {
                        processPythonOutput(line);
                    }
                } catch (IOException ex) {
                    appendToAll("Error reading from Python: " + ex.getMessage());
                }
            }).start();

            new Thread(() -> {
                String line;
                try {
                    while ((line = pythonErrorReader.readLine()) != null) {
                        appendToAll("PYTHON ERROR: " + line + "\n");
                    }
                } catch (IOException ex) {
                    appendToAll("Error reading Python error stream: " + ex.getMessage() + "\n");
                }
            }).start();

            appendToAll("Python process started.\n");
        } catch (IOException ex) {
            appendToAll("Failed to start Python process: " + ex.getMessage() + "\n");
        }
    }

    private void sendTranslateCommand() {
        startButton.setEnabled(false);
        clearTextAreas();

        try {
            if (pythonWriter != null) {
                String sourceLangCode = getLangCode(sourceLangCombo.getSelectedItem().toString());
                String targetLangCode = getLangCode(targetLangCombo.getSelectedItem().toString());

                pythonWriter.write(sourceLangCode + "\n");
                pythonWriter.write(targetLangCode + "\n");
                pythonWriter.flush();

                pythonWriter.write("translate\n");
                pythonWriter.flush();

                appendToAll("Sent languages: " + sourceLangCode + " -> " + targetLangCode + "\n");
                appendToAll("Sent command: translate\n");
            } else {
                appendToAll("Python process not ready.\n");
                startButton.setEnabled(true);
            }
        } catch (IOException e) {
            appendToAll("Error sending command: " + e.getMessage() + "\n");
            startButton.setEnabled(true);
        }
    }

    private String getLangCode(String langName) {
        for (String[] lang : languages) {
            if (lang[1].equals(langName)) {
                return lang[0];
            }
        }
        return "en";
    }

    private void processPythonOutput(String line) {
        SwingUtilities.invokeLater(() -> {
            try {
                StyledDocument doc = indoTextPane.getStyledDocument();

                if (line.startsWith("PHASE2 HINDI RECOGNIZED:") || line.startsWith("PHASE2 HINDI TRANSLATED:")) {
                    String text = line.substring(line.indexOf(':') + 1).trim() + "\n\n";
                    doc.insertString(doc.getLength(), text, hindiStyle);
                } else if (line.startsWith("PHASE1 INDO RECOGNIZED:")) {
                    String text = "You said: " + line.substring(22).trim() + "\n";
                    doc.insertString(doc.getLength(), text, emojiStyle);
                } else if (line.startsWith("PHASE1 INDO TRANSLATED:")) {
                    String text = "Translated: " + line.substring(23).trim() + "\n\n";
                    doc.insertString(doc.getLength(), text, emojiStyle);
                } else if (line.contains("TRANSLATION CYCLE COMPLETE")) {
                    appendToAll("🔄 Translation cycle completed. You can press Start to run again.\n");
                    startButton.setEnabled(true);
                } else if (line.startsWith("Languages received:")) {
                    appendToAll(line + "\n");
                } else {
                    doc.insertString(doc.getLength(), line + "\n", emojiStyle);
                }
            } catch (BadLocationException e) {
                appendToAll("Error appending text: " + e.getMessage() + "\n");
            }
        });
    }

    private void appendToAll(String text) {
        SwingUtilities.invokeLater(() -> {
            try {
                StyledDocument doc = indoTextPane.getStyledDocument();
                doc.insertString(doc.getLength(), text, defaultStyle);
            } catch (BadLocationException e) {
                // ignore
            }
        });
    }

    private void clearTextAreas() {
        indoTextPane.setText("");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(TranslatorApp::new);
    }
}