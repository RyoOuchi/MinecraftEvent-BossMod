package com.example.examplemod.Networking.Data;

public class Message {
    private final Header header;
    private final Body body;

    public Message(final Header header, final Body body) {
        this.header = header;
        this.body = body;
    }

    public Message(final byte[] messageBytes) {
        final String message = new String(messageBytes);
        this.header = extractHeader(message);
        this.body = extractBody(message);
    }

    public Header getHeader() {
        return header;
    }

    public Body getBody() {
        return body;
    }

    public String constructMessage() {
        return header.constructHeader() + "\n" + body.getBody();
    }

    public Header extractHeader(final String message) {
        return new Header(message.getBytes());
    }

    public Body extractBody(String input) {
        String startTag = "[BODY]|";
        int startIndex = input.indexOf(startTag);
        if (startIndex == -1) return new Body("");

        startIndex += startTag.length();
        int endIndex = input.indexOf("|", startIndex);
        if (endIndex == -1) return new Body("");

        return new Body(input.substring(startIndex, endIndex));
    }
}
