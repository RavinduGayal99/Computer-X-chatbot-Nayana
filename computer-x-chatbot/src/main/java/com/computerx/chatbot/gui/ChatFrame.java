package com.computerx.chatbot.gui;

import com.computerx.chatbot.model.BotState;
import com.computerx.chatbot.model.ChatResponse;
import com.computerx.chatbot.service.ChatService;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

@Component
public class ChatFrame extends JFrame {

    private final ChatService chatService;
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton submitButton;
    private JLabel avatarLabel;

    private ImageIcon normalAvatar;
    private ImageIcon annoyedAvatar;

    public ChatFrame(ChatService chatService) {
        this.chatService = chatService;
        loadAvatars();
        initComponents();
    }

    private void loadAvatars() {
        URL normalUrl = getClass().getClassLoader().getResource("images/nayana-normal.png");
        URL annoyedUrl = getClass().getClassLoader().getResource("images/nayana-annoyed.png");

        if (normalUrl != null) {
            normalAvatar = new ImageIcon(new ImageIcon(normalUrl).getImage().getScaledInstance(200, 200, Image.SCALE_SMOOTH));
        } else {
            System.err.println("Could not find normal avatar image!");
        }

        if (annoyedUrl != null) {
            annoyedAvatar = new ImageIcon(new ImageIcon(annoyedUrl).getImage().getScaledInstance(200, 200, Image.SCALE_SMOOTH));
        } else {
            System.err.println("Could not find annoyed avatar image!");
        }
    }

    private void initComponents() {
        setTitle("Virtual Assistant for Computer X");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(chatArea);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        avatarLabel = new JLabel();
        setAvatar(BotState.NORMAL);
        centerPanel.add(avatarLabel, BorderLayout.EAST);

        add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        inputField = new JTextField();
        inputField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        submitButton = new JButton("Submit");

        bottomPanel.add(new JLabel("You: "), BorderLayout.WEST);
        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(submitButton, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);

        ActionListener actionListener = e -> processUserInput();
        submitButton.addActionListener(actionListener);
        inputField.addActionListener(actionListener);

        appendToChat("Nayana", "Hello! I'm Nayana. How can I help you today?");
    }

    private void processUserInput() {
        String userInput = inputField.getText().trim();
        if (userInput.isEmpty()) {
            return;
        }

        appendToChat("You", userInput);
        inputField.setText("");

        ChatResponse response = chatService.getResponse(userInput);

        if (response.botState() == BotState.LEARNING) {
            String teachingAnswer = JOptionPane.showInputDialog(
                    this,
                    response.message(),
                    "Teach Nayana",
                    JOptionPane.QUESTION_MESSAGE
            );
            if (teachingAnswer != null && !teachingAnswer.isBlank()) {
                chatService.learn(userInput, teachingAnswer);
                appendToChat("Nayana", "Thank you! I've learned that.");
            } else {
                appendToChat("Nayana", "Okay, I won't learn that for now.");
            }
        } else {
            appendToChat("Nayana", response.message());
        }

        setAvatar(response.botState());

        if (response.endConversation()) {
            JOptionPane.showMessageDialog(this, "Goodbye!");
            System.exit(0);
        }
    }

    private void appendToChat(String user, String message) {
        chatArea.append(String.format("%s: %s\n\n", user, message));
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    private void setAvatar(BotState state) {
        if (state == BotState.ANNOYED && annoyedAvatar != null) {
            avatarLabel.setIcon(annoyedAvatar);
        } else if (normalAvatar != null) {
            avatarLabel.setIcon(normalAvatar);
        }
        this.revalidate();
        this.repaint();
    }
}