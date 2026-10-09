package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.KbDocument;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class KnowledgeBaseLoader {
    private KnowledgeBaseLoader() {}
    public static List<KbDocument> load(Path path, ObjectMapper mapper) {
        try (var input = Files.newInputStream(path)) {
            JsonNode root = mapper.readTree(input);
            if (root == null || !root.isArray()) throw new IllegalArgumentException("root must be a JSON array");
            var documents = new ArrayList<KbDocument>();
            var ids = new HashSet<String>();
            for (JsonNode node : root) {
                String id = field(node, "id");
                if (!ids.add(id)) throw new IllegalArgumentException("duplicate document id: " + id);
                documents.add(new KbDocument(id, field(node, "title"), field(node, "text")));
            }
            return List.copyOf(documents);
        } catch (IOException | IllegalArgumentException e) {
            throw new IllegalStateException("Cannot load knowledge base at " + path.toAbsolutePath()
                    + ": " + e.getMessage(), e);
        }
    }
    private static String field(JsonNode node, String name) {
        JsonNode value = node.get(name);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new IllegalArgumentException("each document must have a nonblank string '" + name + "'");
        }
        return value.asText();
    }
}
