package com.example.examplemod.Networking.Data;


import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class Header {
    private final String header;

    // SYN, 100 etc
    private final Map<String, String> headerSections;

    public Header(final String header) {
        this.header = header;
        this.headerSections = parseHeaderSections(header);
    }

    public Header(final Map<String, String> headerSections) {
        this.headerSections = headerSections;
        this.header = constructHeader();
    }

    public Header(final byte[] headerBytes) {
        final String message = new String(headerBytes);
        this.header = extractHeader(message);
        this.headerSections = parseHeaderSections(header);
    }

    public String constructHeader() {
        StringBuilder headerBuilder = new StringBuilder("[HEADER]|");

        headerSections.forEach((key, value) -> {
            headerBuilder.append(key).append("=").append(value).append(",");
        });

        if (headerBuilder.charAt(headerBuilder.length() - 1) == ',') {
            headerBuilder.deleteCharAt(headerBuilder.length() - 1);
        }

        headerBuilder.append("|");

        return headerBuilder.toString();
    }


    @Nullable
    public String extractHeader(final String message) {
        if (message == null || !message.contains("[HEADER]|")) {
            return null;
        }

        int startIndex = message.indexOf("[HEADER]|") + "[HEADER]|".length();
        int endIndex = message.indexOf("|", startIndex);
        if (endIndex == -1) endIndex = message.length();

        return message.substring(startIndex, endIndex);
    }


    @Nullable
    public Map<String, String> parseHeaderSections(@Nullable final String header) {
        if (header == null || header.isEmpty()) {
            return null;
        }

        Map<String, String> map = new HashMap<>();
        String[] pairs = header.split(",");

        for (String pair : pairs) {
            if (pair.contains("=")) {
                String[] keyValue = pair.split("=", 2);
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();
                map.put(key, value);
            }
        }

        return map;
    }

    public Map<String, String> getHeaderSections() {
        return this.headerSections;
    }

    public String getHeader() {
        return this.header;
    }

    public int getSeqNumber() {
        if (headerSections.containsKey("SEQ")) {
            try {
                return Integer.parseInt(headerSections.get("SEQ"));
            } catch (NumberFormatException e) {
                System.out.println("❌ Failed to parse SEQ number: " + e.getMessage());
            }
        }
        return -1;
    }

    public int getAckNumber() {
        if (headerSections.containsKey("ACK")) {
            try {
                return Integer.parseInt(headerSections.get("ACK"));
            } catch (NumberFormatException e) {
                System.out.println("❌ Failed to parse ACK number: " + e.getMessage());
            }
        }
        return -1;
    }

    public int getEndNumber() {
        if (headerSections.containsKey("END")) {
            try {
                return Integer.parseInt(headerSections.get("END"));
            } catch (NumberFormatException e) {
                System.out.println("❌ Failed to parse END number: " + e.getMessage());
            }
        }
        return -1;
    }

    public int getResponseNumber() {
        if (headerSections.containsKey("RESPONSE")) {
            try {
                return Integer.parseInt(headerSections.get("RESPONSE"));
            } catch (NumberFormatException e) {
                System.out.println("❌ Failed to parse RESPONSE number: " + e.getMessage());
            }
        }
        return -1;
    }

}
