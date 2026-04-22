package dev.retreever.schema.resolver.util;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ConstraintResolverTest {

    @Test
    void resolvesSupportedBeanValidationConstraints() throws Exception {
        assertThat(ConstraintResolver.resolve(annotations("required")))
                .containsExactly(JsonPropertyConstraint.NOT_NULL);

        assertThat(ConstraintResolver.resolve(annotations("title")))
                .containsExactly(JsonPropertyConstraint.NOT_BLANK);

        assertThat(ConstraintResolver.resolve(annotations("tags")))
                .containsExactly(JsonPropertyConstraint.NOT_EMPTY);

        assertThat(ConstraintResolver.resolve(annotations("code")))
                .containsExactlyInAnyOrder(
                        JsonPropertyConstraint.minLength(2),
                        JsonPropertyConstraint.maxLength(10)
                );

        assertThat(ConstraintResolver.resolve(annotations("minimum")))
                .containsExactly(JsonPropertyConstraint.minValue(3));

        assertThat(ConstraintResolver.resolve(annotations("maximum")))
                .containsExactly(JsonPropertyConstraint.maxValue(9));

        assertThat(ConstraintResolver.resolve(annotations("category")))
                .containsExactly(JsonPropertyConstraint.regex("[A-Z]+"));
    }

    @Test
    void identifiesRequiredConstraintsWithoutDependingOnConcreteValidationTypes() throws Exception {
        assertThat(ConstraintResolver.isRequired(annotations("required"))).isTrue();
        assertThat(ConstraintResolver.isRequired(annotations("title"))).isTrue();
        assertThat(ConstraintResolver.isRequired(annotations("tags"))).isTrue();
        assertThat(ConstraintResolver.isRequired(annotations("code"))).isFalse();
        assertThat(ConstraintResolver.isRequired(annotations("plain"))).isFalse();
    }

    private Annotation[] annotations(String fieldName) throws Exception {
        Field field = SampleConstraints.class.getDeclaredField(fieldName);
        return field.getAnnotations();
    }

    static class SampleConstraints {

        @NotNull
        private String required;

        @NotBlank
        private String title;

        @NotEmpty
        private Set<String> tags;

        @Size(min = 2, max = 10)
        private String code;

        @Min(3)
        private int minimum;

        @Max(9)
        private int maximum;

        @Pattern(regexp = "[A-Z]+")
        private String category;

        private String plain;
    }
}
