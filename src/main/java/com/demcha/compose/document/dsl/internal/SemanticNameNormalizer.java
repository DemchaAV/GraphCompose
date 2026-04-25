package com.demcha.compose.document.dsl.internal;

/** Normalizes free-form labels into stable semantic node names. */
public final class SemanticNameNormalizer {
    private SemanticNameNormalizer() {
    }

    /**
     * Converts a free-form label into a stable semantic node name.
     *
     * @param raw user-facing label
     * @return normalized semantic name, never blank
     */
    public static String normalize(String raw) {
        String safe = raw == null ? "" : raw.strip();
        if (safe.isEmpty()) {
            return "Module";
        }
        StringBuilder normalized = new StringBuilder();
        boolean capitalize = true;
        for (int index = 0; index < safe.length(); index++) {
            char current = safe.charAt(index);
            if (!Character.isLetterOrDigit(current)) {
                capitalize = true;
                continue;
            }
            normalized.append(capitalize ? Character.toUpperCase(current) : current);
            capitalize = false;
        }
        if (normalized.isEmpty()) {
            return "Module";
        }
        if (!Character.isLetter(normalized.charAt(0))) {
            normalized.insert(0, "Module");
        }
        return normalized.toString();
    }
}
