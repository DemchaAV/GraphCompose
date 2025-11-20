package com.demcha.Templatese.templates_utils;

import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public final class ConfigLoader {


    private ConfigLoader() {}

    /**
     * Load YAML from classpath resource into the given class.
     * Supports running from IDE and from a fat JAR.
     * Optionally resolves ${ENV} or ${ENV:default} placeholders from environment variables.
     */
    public static <T> T loadConfigWithEnv(String fileName, Class<T> clazz, boolean resolveEnv) {
        log.info("Initializing variables from '{}'", fileName);

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) cl = ConfigLoader.class.getClassLoader();

        URL resource = cl.getResource(fileName);
        if (resource == null) {
            throw new IllegalArgumentException("Config resource not found on classpath: " + fileName + "Url: " + resource);
        }
        log.info("Reading config from '{}'", resource);

        String raw;
        try (InputStream is = resource.openStream();
             BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
            raw = sb.toString();
        } catch (Exception e) {
            log.error("Failed to read config '{}': {}", fileName, e.toString(), e);
            throw new RuntimeException("Failed to read config: " + fileName, e);
        }

        String resolved = resolveEnv ? replaceEnvVariables(raw) : raw;

        // Use SnakeYAML with an explicit Constructor for target type
        LoaderOptions options = new LoaderOptions();
        Yaml yaml = new Yaml(new Constructor(clazz, options));
        log.info("Creating a config YAML object '{}'", clazz.getSimpleName());
        return yaml.load(resolved);
    }

    /**
     * Replace ${ENV} or ${ENV:default} with values from SystemECS.getenv().
     * Unknown vars without default → empty string.
     */
    private static String replaceEnvVariables(String text) {
        // ${VAR} or ${VAR:default value}
        Pattern p = Pattern.compile("\\$\\{([A-Za-z_][A-Za-z0-9_]*) (?:: ([^}]*))?\\}", Pattern.COMMENTS);
        Matcher m = p.matcher(text);
        StringBuffer out = new StringBuffer();

        while (m.find()) {
            String var = m.group(1);
            String def = m.group(2); // may be null
            String val = System.getenv(var);
            if (val == null) val = (def != null ? def : "");
            // Escape backslashes and dollars for appendReplacement
            m.appendReplacement(out, Matcher.quoteReplacement(val));
        }
        m.appendTail(out);
        return out.toString();
    }
}
