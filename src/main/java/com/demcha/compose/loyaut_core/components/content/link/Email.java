package com.demcha.compose.loyaut_core.components.content.link;

import lombok.extern.slf4j.Slf4j;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


/**
 * Represents an email link, extending the {@link LinkUrl} class.
 * This class provides functionality to construct "mailto:" URLs with optional
 * subject and body parameters, allowing for easy creation of clickable email links
 * within entityManagers or applications.
 */
@Slf4j
public class Email extends LinkUrl {

    // Конструктор с полным набором параметров
    public Email(String to, String subject, String body) {
        super(buildMailto(to, subject, body));
    }

    // Конструктор только с "to"
    public Email(String to) {
        super(buildMailto(to, null, null));
    }

    private static String buildMailto(String to, String subject, String body) {
        if (to == null || to.isBlank()) {
            log.error("to is null or blank");
            to ="";
        }

        StringBuilder sb = new StringBuilder("mailto:").append(to);

        // Добавляем query параметры только если они не null/blank
        List<String> params = new ArrayList<>();
        if (subject != null && !subject.isBlank()) {
            params.add("subject=" + encode(subject));;
        }
        if (body != null && !body.isBlank()) {
            params.add("body=" +  encode(body));;
        }

        if (!params.isEmpty()) {
            sb.append("?").append(String.join("&", params));
        }

        return sb.toString();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }
}
