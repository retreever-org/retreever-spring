package dev.retreever.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;

import java.io.IOException;
import java.lang.reflect.Field;

final class Jackson2JsonMapper implements RetreeverJsonMapper {

    private final ObjectMapper mapper;

    Jackson2JsonMapper(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    static Jackson2JsonMapper createDefault() {
        return new Jackson2JsonMapper(new ObjectMapper().findAndRegisterModules());
    }

    @Override
    public RetreeverJsonMapper copyWithNonNullInclusion() {
        return new Jackson2JsonMapper(
                mapper.copy().setSerializationInclusion(JsonInclude.Include.NON_NULL)
        );
    }

    @Override
    public byte[] writeValueAsBytes(Object value) throws IOException {
        return mapper.writeValueAsBytes(value);
    }

    @Override
    public <T> T readValue(byte[] value, Class<T> type) throws IOException {
        return mapper.readValue(value, type);
    }

    @Override
    public String resolvePropertyName(Field field, Class<?> declaringClass) {
        JavaType javaType = mapper.constructType(declaringClass);
        BeanDescription beanDescription = mapper.getSerializationConfig().introspect(javaType);

        for (BeanPropertyDefinition property : beanDescription.findProperties()) {
            if (matchesField(property, field)) {
                return property.getName();
            }
        }

        return null;
    }

    private boolean matchesField(BeanPropertyDefinition property, Field field) {
        if (!property.couldSerialize()) {
            return false;
        }

        if (property.getField() != null && property.getField().getAnnotated().equals(field)) {
            return true;
        }

        if (property.getInternalName().equals(field.getName())) {
            return true;
        }

        if (property.getGetter() != null && matchesAccessor(property.getGetter().getName(), field)) {
            return true;
        }

        return property.getSetter() != null && matchesAccessor(property.getSetter().getName(), field);
    }

    private boolean matchesAccessor(String accessorName, Field field) {
        String fieldName = field.getName();
        String accessorSuffix = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

        return accessorName.equals(fieldName)
                || accessorName.equals("get" + fieldName)
                || accessorName.equals("get" + accessorSuffix)
                || accessorName.equals("is" + fieldName)
                || accessorName.equals("is" + accessorSuffix)
                || accessorName.equals("set" + fieldName)
                || accessorName.equals("set" + accessorSuffix);
    }
}
