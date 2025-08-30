package com.demcha.components.content.link;

import lombok.RequiredArgsConstructor;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;


public class Email extends LinkUrl {
    public Email(String to, String subject, String body) {
        super(buildMailto(to, subject, body));
    }

    private static String buildMailto(String to, String subject, String body) {
        try {
            return "mailto:" + to
                   + "?subject=" + URLEncoder.encode(subject, StandardCharsets.UTF_8)
                   + "&body=" + URLEncoder.encode(body, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
