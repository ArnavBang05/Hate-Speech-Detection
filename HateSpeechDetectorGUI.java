import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class HateSpeechDetectorGUI extends JFrame {

    private JTextArea chatArea;
    private JTextField inputTextField;
    private JButton analyzeButton;
    private JButton clearChatButton;
    private JTextArea logsArea;

    public HateSpeechDetectorGUI() {
        setTitle("Hate Speech Detector");
        setSize(800, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(50, 70, 90));
        JLabel titleLabel = new JLabel(" Hate Speech Detector", JLabel.LEFT);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        headerPanel.add(titleLabel, BorderLayout.WEST);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JScrollPane chatScrollPane = new JScrollPane(chatArea);

        logsArea = new JTextArea();
        logsArea.setEditable(false);
        logsArea.setLineWrap(true);
        logsArea.setWrapStyleWord(true);
        logsArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        JScrollPane logsScrollPane = new JScrollPane(logsArea);
        logsScrollPane.setPreferredSize(new Dimension(250, 0));

        JPanel messagePanel = new JPanel(new BorderLayout());
        inputTextField = new JTextField();
        inputTextField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        analyzeButton = createStyledButton("Send", new Color(51, 153, 255));
        clearChatButton = createStyledButton("Clear Chat", new Color(255, 102, 102));

        analyzeButton.addActionListener(new AnalyzeButtonListener());
        clearChatButton.addActionListener(new ClearChatButtonListener());

        messagePanel.add(inputTextField, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        buttonPanel.add(clearChatButton);
        buttonPanel.add(analyzeButton);
        messagePanel.add(buttonPanel, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);
        add(chatScrollPane, BorderLayout.CENTER);
        add(logsScrollPane, BorderLayout.EAST);
        add(messagePanel, BorderLayout.SOUTH);

        getContentPane().setBackground(new Color(230, 240, 250));
    }

    private JButton createStyledButton(String text, Color backgroundColor) {
        JButton button = new JButton(text);
        button.setBackground(backgroundColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBorder(BorderFactory.createEmptyBorder(15, 30, 15, 30));
        button.setPreferredSize(new Dimension(150, 40));
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private class AnalyzeButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            String inputText = inputTextField.getText().trim();
            if (inputText.isEmpty()) {
                JOptionPane.showMessageDialog(HateSpeechDetectorGUI.this, "Please enter text to analyze.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String result = analyzeText(inputText);
            String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

            if ("Hate Speech Detected".equals(result) || "Offensive Language Detected".equals(result)) {
                // If the message isn't clean, show pop-up and add to history only
                JOptionPane.showMessageDialog(HateSpeechDetectorGUI.this, result, "Warning", JOptionPane.WARNING_MESSAGE);
                logsArea.append("[" + timestamp + "] Text: " + inputText + "\nResult: " + result + "\n\n");
            } else {
                // If the message is clean, send to both chat box and history without pop-up
                chatArea.append("[" + timestamp + "] You: " + inputText + "\n");
                logsArea.append("[" + timestamp + "] Text: " + inputText + "\nResult: " + result + "\n\n");
            }

            inputTextField.setText("");
        }
    }

    private class ClearChatButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            int confirm = JOptionPane.showConfirmDialog(HateSpeechDetectorGUI.this, "Are you sure you want to clear the chat?", "Clear Chat", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                chatArea.setText("");
                logsArea.setText("");
            }
        }
    }

    private String analyzeText(String text) {
        try {
            URI uri = new URI("http://127.0.0.1:5000/analyze");
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();  // Convert URI to URL here
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String jsonInputString = "{\"text\": \"" + text + "\"}";

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();

                if (response.toString().contains("Hate Speech Detected")) {
                    return "Hate Speech Detected";
                } else if (response.toString().contains("Offensive Language Detected")) {
                    return "Offensive Language Detected";
                } else {
                    return "Message is clean";
                }
            } else {
                return "Error analyzing text. Please try again.";
            }
        } catch (IOException | URISyntaxException ex) {
            ex.printStackTrace();
            return "Error connecting to server.";
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            HateSpeechDetectorGUI gui = new HateSpeechDetectorGUI();
            gui.setVisible(true);
        });
    }
}
