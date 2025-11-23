package com.computerx.chatbot.model;

/**
 * Represents a response from the chatbot.
 * @param message The text content of the response.
 * @param endConversation If true, the application should close.
 * @param botState The emotional state of the bot for this response.
 * @param isPersonalizable If true, the user's name can be appended to this message.
 */
public record ChatResponse(
        String message,
        boolean endConversation,
        BotState botState,
        boolean isPersonalizable
) {}