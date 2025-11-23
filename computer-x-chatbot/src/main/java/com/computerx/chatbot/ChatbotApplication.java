package com.computerx.chatbot;

import com.computerx.chatbot.gui.ChatFrame;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.awt.*;

@SpringBootApplication
public class ChatbotApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(ChatbotApplication.class)
                .headless(false)
                .run(args);

        EventQueue.invokeLater(() -> {
            ChatFrame chatFrame = context.getBean(ChatFrame.class);
            chatFrame.setVisible(true);
        });
    }
}
