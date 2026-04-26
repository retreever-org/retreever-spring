package dev.retreever.view;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.retreever.schema.model.JsonPropertyType;
import dev.retreever.schema.model.MapSchema;
import dev.retreever.schema.model.ObjectSchema;
import dev.retreever.schema.model.Property;
import dev.retreever.schema.model.ValueSchema;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaViewRendererTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void renderRequestUsesJsonTypedPlaceholdersForPrimitiveModelValues() throws JsonProcessingException {
        ObjectSchema schema = new ObjectSchema();
        schema.addProperty(property("name", JsonPropertyType.STRING));
        schema.addProperty(property("price", JsonPropertyType.NUMBER));
        schema.addProperty(property("active", JsonPropertyType.BOOLEAN));
        schema.addProperty(property("deletedAt", JsonPropertyType.NULL));

        Map<String, Object> rendered = SchemaViewRenderer.renderRequest(schema);
        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) rendered.get(SchemaViewRenderer.MODEL_KEY);

        assertThat(model)
                .containsEntry("name", "string")
                .containsEntry("price", 0)
                .containsEntry("active", false)
                .containsEntry("deletedAt", null);

        String json = objectMapper.writeValueAsString(rendered);
        assertThat(json)
                .contains("\"price\":0")
                .contains("\"active\":false")
                .contains("\"deletedAt\":null")
                .doesNotContain("\"price\":\"number\"")
                .doesNotContain("\"active\":\"boolean\"")
                .doesNotContain("\"deletedAt\":\"null\"");
    }

    @Test
    void generatedExamplesAlsoUseJsonTypedPrimitiveValues() {
        ObjectSchema schema = new ObjectSchema();
        schema.addProperty(property("count", JsonPropertyType.NUMBER));
        schema.addProperty(property("enabled", JsonPropertyType.BOOLEAN));

        Map<String, Object> rendered = SchemaViewRenderer.renderResponse(schema);
        @SuppressWarnings("unchecked")
        Map<String, Object> exampleModel = (Map<String, Object>) rendered.get(SchemaViewRenderer.EXAMPLE_MODEL_KEY);

        assertThat(exampleModel)
                .containsEntry("count", 0)
                .containsEntry("enabled", false);
    }

    @Test
    void renderModelAllowsNullPlaceholderInsideMapSchemas() throws JsonProcessingException {
        MapSchema schema = new MapSchema(JsonPropertyType.STRING, new ValueSchema(JsonPropertyType.NULL));

        Map<String, Object> rendered = SchemaViewRenderer.renderResponse(schema);
        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) rendered.get(SchemaViewRenderer.MODEL_KEY);

        assertThat(model).containsEntry("string", null);
        assertThat(objectMapper.writeValueAsString(rendered)).contains("\"string\":null");
    }

    private static Property property(String name, JsonPropertyType type) {
        return new Property(name, type, new ValueSchema(type));
    }
}
