package com.computerx.chatbot.service;

import com.computerx.chatbot.model.BotState;
import com.computerx.chatbot.model.ChatResponse;
import com.computerx.chatbot.model.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final KnowledgeBaseService knowledgeBase;
    private final Random random = new Random();
    private static final Set<String> ALL_KEYWORDS = Set.of("all", "all items", "all products", "everything");

    private String lastQuestion = "";
    private int repetitionCount = 0;
    private String userName = null;
    private boolean isWaitingForUserName = false;

    public ChatResponse getResponse(String userInput) {
        String normalizedInput = userInput.toLowerCase().trim();

        if (isWaitingForUserName) {
            this.userName = capitalize(userInput.trim());
            isWaitingForUserName = false;
            return new ChatResponse("It's a pleasure to meet you, " + this.userName + "!", false, BotState.NORMAL, false);
        }

        if (normalizedInput.equals(lastQuestion) && !normalizedInput.isEmpty()) {
            repetitionCount++;
        } else {
            lastQuestion = normalizedInput;
            repetitionCount = 1;
        }

        if (normalizedInput.equalsIgnoreCase("sorry")) {
            repetitionCount = 0;
            lastQuestion = "";
            return new ChatResponse(getRandomResponse("It's okay. No problem.", "Apology accepted."), false, BotState.NORMAL, true);
        }

        ChatResponse baseResponse = processQuery(normalizedInput);
        String finalMessage = baseResponse.message();
        BotState finalState = baseResponse.botState();

        if (baseResponse.isPersonalizable() && this.userName != null && random.nextInt(10) < 7) {
            if (finalMessage.endsWith(".") || finalMessage.endsWith("!") || finalMessage.endsWith("?")) {
                finalMessage = finalMessage.substring(0, finalMessage.length() - 1);
            }
            finalMessage = finalMessage + ", " + this.userName + ".";
        }

        if (repetitionCount > 3 && !baseResponse.endConversation()) {
            finalMessage = getRandomResponse("I've already answered that. Please ask a different question.", "Why do you keep asking the same thing? Let's move on.");
            finalState = BotState.ANNOYED;
        } else if (repetitionCount == 3 && !baseResponse.endConversation()) {
            finalMessage += getRandomResponse("\n\n(By the way, you've asked me that a few times now.)", "\n\n(Just letting you know, I believe I've answered this already.)");
        }

        return new ChatResponse(finalMessage, baseResponse.endConversation(), finalState, false);
    }

    private ChatResponse processQuery(String normalizedInput) {
        ChatResponse timeResponse = handleTimeBasedGreeting(normalizedInput);
        if (timeResponse != null) {
            return timeResponse;
        }

        if (normalizedInput.contains("who are you") || normalizedInput.contains("what is your name")) {
            if (this.userName != null) {
                return new ChatResponse("My name is Nayana. It's nice chatting with you, " + this.userName + "!", false, BotState.NORMAL, false);
            } else {
                isWaitingForUserName = true;
                return new ChatResponse("I am Nayana, a virtual assistant for Computer X. What's your name?", false, BotState.NORMAL, false);
            }
        }

        if (normalizedInput.equals("hi") || normalizedInput.equals("hello")) {
            return new ChatResponse(getRandomResponse("Hello!", "Hi there!", "Greetings!"), false, BotState.NORMAL, true);
        }
        if (normalizedInput.equals("thanks") || normalizedInput.equals("thank you")) {
            return new ChatResponse(getRandomResponse("You're welcome!", "No problem!", "Happy to help!"), false, BotState.NORMAL, true);
        }
        if (normalizedInput.equals("bye") || normalizedInput.equals("exit") || normalizedInput.equals("quit")) {
            return new ChatResponse(getRandomResponse("Goodbye!", "See you later!", "Have a great day!"), true, BotState.NORMAL, true);
        }
        if (normalizedInput.contains("how are you")) {
            return new ChatResponse(getRandomResponse("I'm doing great, thanks for asking!", "I'm a bot, so I'm always running at 100%!", "I'm fine, ready to help!"), false, BotState.NORMAL, true);
        }

        if (normalizedInput.contains("categor")) {
            Set<String> categories = knowledgeBase.getUniqueCategories();
            String categoryList = String.join(", ", categories);
            return new ChatResponse("We have the following product categories: " + categoryList + ".", false, BotState.NORMAL, false);
        }
        if (normalizedInput.contains("product names") || normalizedInput.contains("item names")) {
            List<String> names = knowledgeBase.getAllProductNames();
            String nameList = String.join(", ", names);
            return new ChatResponse("Here are all the product names we have: " + nameList + ".", false, BotState.NORMAL, false);
        }

        String smallTalkResponse = knowledgeBase.getSmallTalk().getProperty(normalizedInput.replace(" ", "."));
        if (smallTalkResponse != null) {
            return new ChatResponse(smallTalkResponse, false, BotState.NORMAL, false);
        }
        String learnedResponse = knowledgeBase.getLearnedResponses().get(normalizedInput);
        if (learnedResponse != null) {
            return new ChatResponse(learnedResponse, false, BotState.NORMAL, false);
        }

        if (normalizedInput.contains("show") || normalizedInput.contains("list") || normalizedInput.contains("see")) {
            String keyword = extractKeyword(normalizedInput, "show me", "list all", "see all", "show", "list", "see");
            return findAndListProducts(keyword);
        }
        if (normalizedInput.startsWith("price of") || normalizedInput.startsWith("what is the price of")) {
            String keyword = extractKeyword(normalizedInput, "price of", "what is the price of");
            return findProductPrice(keyword);
        }
        if (normalizedInput.contains("in stock") || normalizedInput.contains("available")) {
            String keyword = extractKeyword(normalizedInput, "is", "are", "in stock", "available");
            return checkStock(keyword);
        }
        if (normalizedInput.contains("do you have") || normalizedInput.contains("any")) {
            String keyword = extractKeyword(normalizedInput, "do you have", "any");
            return findAndListProducts(keyword);
        }

        return new ChatResponse("I'm not sure how to answer that. Could you please tell me the correct response?", false, BotState.LEARNING, false);
    }

    private ChatResponse handleTimeBasedGreeting(String normalizedInput) {
        int currentHour = LocalTime.now().getHour();
        String actualTimeOfDay;
        if (currentHour >= 5 && currentHour < 12) actualTimeOfDay = "morning";
        else if (currentHour >= 12 && currentHour < 17) actualTimeOfDay = "afternoon";
        else if (currentHour >= 17 && currentHour < 22) actualTimeOfDay = "evening";
        else actualTimeOfDay = "night";

        String userGreeting = null;
        if (normalizedInput.contains("morning")) userGreeting = "morning";
        else if (normalizedInput.contains("afternoon")) userGreeting = "afternoon";
        else if (normalizedInput.contains("evening")) userGreeting = "evening";
        else if (normalizedInput.contains("night")) userGreeting = "night";

        if (userGreeting != null) {
            if (userGreeting.equals(actualTimeOfDay)) {
                return new ChatResponse("Good " + actualTimeOfDay + " to you too!", false, BotState.NORMAL, true);
            } else {
                return new ChatResponse("Actually, it's " + actualTimeOfDay + " here, but good " + userGreeting + " to you anyway!", false, BotState.NORMAL, true);
            }
        }
        return null;
    }

    public void learn(String question, String answer) {
        knowledgeBase.saveLearnedResponse(question, answer);
    }

    private String getRandomResponse(String... responses) {
        return responses[random.nextInt(responses.length)];
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    private ChatResponse findAndListProducts(String keyword) {
        if (keyword.isBlank()) {
            return new ChatResponse("What kind of products are you looking for? For example: 'show me laptops'.", false, BotState.NORMAL, false);
        }
        List<Product> foundProducts;
        if (ALL_KEYWORDS.contains(keyword.toLowerCase())) {
            foundProducts = knowledgeBase.getProducts();
        } else {
            foundProducts = knowledgeBase.findProductsByKeyword(keyword);
        }
        if (foundProducts.isEmpty()) {
            return new ChatResponse("Sorry, I couldn't find any products matching '" + keyword + "'.", false, BotState.NORMAL, false);
        }
        String productList = foundProducts.stream().map(Product::toString).collect(Collectors.joining("\n---\n"));
        String responseHeader = ALL_KEYWORDS.contains(keyword.toLowerCase())
                ? "Here are all the products we have:\n"
                : "Here's what I found for '" + keyword + "':\n";
        return new ChatResponse(responseHeader + productList, false, BotState.NORMAL, false);
    }

    private ChatResponse findProductPrice(String keyword) {
        List<Product> foundProducts = knowledgeBase.findProductsByKeyword(keyword);
        if (foundProducts.isEmpty()) {
            return new ChatResponse("Sorry, I couldn't find a product named '" + keyword + "'.", false, BotState.NORMAL, false);
        }
        if (foundProducts.size() > 1) {
            String suggestions = foundProducts.stream().map(Product::name).limit(5).collect(Collectors.joining(", "));
            return new ChatResponse("I found multiple products matching '" + keyword + "': " + suggestions + "... Can you be more specific?", false, BotState.NORMAL, false);
        }
        Product p = foundProducts.get(0);
        return new ChatResponse(String.format("The price of the %s is $%.2f.", p.name(), p.price()), false, BotState.NORMAL, false);
    }

    private ChatResponse checkStock(String keyword) {
        List<Product> foundProducts = knowledgeBase.findProductsByKeyword(keyword);
        if (foundProducts.isEmpty()) {
            return new ChatResponse("Sorry, I couldn't find a product named '" + keyword + "'.", false, BotState.NORMAL, false);
        }
        if (foundProducts.size() > 1) {
            String suggestions = foundProducts.stream().map(Product::name).limit(5).collect(Collectors.joining(", "));
            return new ChatResponse("I found multiple products matching '" + keyword + "': " + suggestions + "... Can you be more specific?", false, BotState.NORMAL, false);
        }
        Product p = foundProducts.get(0);
        String stockStatus = p.stock().equalsIgnoreCase("in stock") ? "is in stock" : "is currently out of stock";
        return new ChatResponse(String.format("The %s %s.", p.name(), stockStatus), false, BotState.NORMAL, false);
    }

    private String extractKeyword(String input, String... prefixes) {
        String result = input;
        for (String prefix : prefixes) {
            result = result.replace(prefix, "");
        }
        return result.replace("?", "").trim();
    }
}