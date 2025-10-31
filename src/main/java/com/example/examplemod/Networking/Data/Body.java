package com.example.examplemod.Networking.Data;

public class Body {
    private final String body;
    public Body(final String body) {
        if (!body.startsWith("[BODY]|") && !body.endsWith("|")) {
            this.body = constructBody(body);
            return;
        }
        this.body = body;
    }

    public Body(final byte[] bodyBytes) {
        final String bodyString = new String(bodyBytes);
        this.body = new Body(bodyString).getBody();
    }

    public String constructBody(String body) {
        return "[BODY]|" + body + "|";
    }

    public String getBody() {
        return body;
    }

    public String extractBody() {
        if (body == null || !body.startsWith("[BODY]|") || !body.endsWith("|")) {
            return "";
        }

        return body.substring(7, body.length() - 1);
    }
}
