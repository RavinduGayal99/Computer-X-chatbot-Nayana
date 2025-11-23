package com.computerx.chatbot.service;

import com.computerx.chatbot.model.Product;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
@Getter
public class KnowledgeBaseService {

    private final List<Product> products = new ArrayList<>();
    private final Map<String, String> learnedResponses = new ConcurrentHashMap<>();
    private final Properties smallTalk = new Properties();

    private static final String PRODUCTS_FILE = "data/products.csv";
    private static final String LEARNED_FILE = "data/learned.txt";
    private static final String SMALLTALK_FILE = "data/smalltalk.properties";
    private static final String LEARNED_DELIMITER = ":::";

    @PostConstruct
    public void initialize() {
        loadProducts();
        loadSmallTalk();
        loadLearnedResponses();
    }

    public Set<String> getUniqueCategories() {
        return products.stream()
                .map(Product::category)
                .map(this::capitalize)
                .collect(Collectors.toSet());
    }

    public List<String> getAllProductNames() {
        return products.stream()
                .map(Product::name)
                .collect(Collectors.toList());
    }

    private void loadProducts() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ClassPathResource(PRODUCTS_FILE).getInputStream()))) {
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                if (parts.length >= 6) {
                    String category = parts[0].trim();
                    String name = parts[1].trim().replace("\"", "");
                    String brand = parts[2].trim();
                    double price = Double.parseDouble(parts[3].trim());
                    String stock = parts[4].trim();
                    String description = parts[5].trim().replace("\"", "");
                    Map<String, String> attributes = new HashMap<>();
                    if (parts.length > 6 && !parts[6].trim().isEmpty()) {
                        String[] attrs = parts[6].trim().replace("\"", "").split(";");
                        for (String attr : attrs) {
                            String[] kv = attr.split(":");
                            if (kv.length == 2) {
                                attributes.put(kv[0].trim(), kv[1].trim());
                            }
                        }
                    }
                    products.add(new Product(category, name, brand, price, stock, description, attributes));
                }
            }
            log.info("Loaded {} products from {}", products.size(), PRODUCTS_FILE);
        } catch (IOException | NumberFormatException e) {
            log.error("Failed to load or parse products file: {}. Make sure it's in src/main/resources/data/", PRODUCTS_FILE, e);
        }
    }

    private void loadSmallTalk() {
        try {
            smallTalk.load(new ClassPathResource(SMALLTALK_FILE).getInputStream());
            log.info("Loaded {} small talk entries.", smallTalk.size());
        } catch (IOException e) {
            log.error("Failed to load small talk properties: {}", SMALLTALK_FILE, e);
        }
    }

    private void loadLearnedResponses() {
        Path externalPath = Paths.get(LEARNED_FILE);
        try {
            if (Files.exists(externalPath)) {
                Files.lines(externalPath).forEach(this::parseAndStoreLearnedLine);
                log.info("Loaded {} learned responses from external file: {}", learnedResponses.size(), externalPath.toAbsolutePath());
            } else {
                Resource resource = new ClassPathResource(LEARNED_FILE);
                if (resource.exists()) {
                    Files.lines(Paths.get(resource.getURI())).forEach(this::parseAndStoreLearnedLine);
                    log.info("Loaded {} default learned responses from classpath.", learnedResponses.size());
                } else {
                    log.warn("No default or external learned.txt found. Will be created on first learn.");
                }
            }
        } catch (IOException e) {
            log.error("Failed to load learned responses: {}", e.getMessage());
        }
    }

    private void parseAndStoreLearnedLine(String line) {
        String[] parts = line.split(LEARNED_DELIMITER, 2);
        if (parts.length == 2) {
            learnedResponses.put(parts[0].trim(), parts[1].trim());
        }
    }

    public void saveLearnedResponse(String question, String answer) {
        String cleanQuestion = question.toLowerCase().trim();
        learnedResponses.put(cleanQuestion, answer);
        Path path = Paths.get(LEARNED_FILE);
        try {
            Path parentDir = path.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
                log.info("Created directory for learned data at: {}", parentDir.toAbsolutePath());
            }

            String lineToAppend = cleanQuestion + LEARNED_DELIMITER + answer + System.lineSeparator();
            Files.write(path, lineToAppend.getBytes(), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
            log.info("Saved new learned response to external file: {}", path.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to save learned response to file. Check write permissions for the application directory.", e);
        }
    }

    public List<Product> findProductsByKeyword(String keyword) {
        String lowerKeyword = keyword.toLowerCase();
        return products.stream()
                .filter(p -> {
                    String category = p.category().toLowerCase();
                    boolean categoryMatch = category.contains(lowerKeyword) || lowerKeyword.contains(category);
                    return p.name().toLowerCase().contains(lowerKeyword) ||
                            categoryMatch ||
                            p.brand().toLowerCase().contains(lowerKeyword) ||
                            p.description().toLowerCase().contains(lowerKeyword) ||
                            p.attributes().values().stream().anyMatch(v -> v.toLowerCase().contains(lowerKeyword));
                })
                .collect(Collectors.toList());
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}