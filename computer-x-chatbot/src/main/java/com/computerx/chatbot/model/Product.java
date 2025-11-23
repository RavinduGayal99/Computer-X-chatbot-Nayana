package com.computerx.chatbot.model;

import java.util.Map;

public record Product(String category,
                      String name,
                      String brand,
                      double price,
                      String stock,
                      String description,
                      Map<String, String> attributes)
{
    @Override
    public String toString()
    {
        return String.format(
                "  - Name: %s (%s)\n" +
                        "    Category: %s\n" +
                        "    Price: $%.2f\n" +
                        "    Stock: %s\n" +
                        "    Description: %s",
                name, brand, category, price, stock, description
        );
    }
}
