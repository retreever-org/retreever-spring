package dev.retreever.json;

import java.io.IOException;
import java.lang.reflect.Field;

public interface RetreeverJsonMapper {

    RetreeverJsonMapper copyWithNonNullInclusion();

    byte[] writeValueAsBytes(Object value) throws IOException;

    <T> T readValue(byte[] value, Class<T> type) throws IOException;

    String resolvePropertyName(Field field, Class<?> declaringClass);
}
