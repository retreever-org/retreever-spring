package dev.retreever.json;

import org.springframework.context.ApplicationContext;

public final class RetreeverJsonMappers {

    private RetreeverJsonMappers() {
    }

    public static RetreeverJsonMapper defaultMapper() {
        return Jackson2JsonMapper.createDefault();
    }

    public static RetreeverJsonMapper wrap(Object mapper) {
        if (mapper instanceof com.fasterxml.jackson.databind.ObjectMapper jackson2Mapper) {
            return new Jackson2JsonMapper(jackson2Mapper);
        }

        if (mapper != null && "tools.jackson.databind.ObjectMapper".equals(mapper.getClass().getName())) {
            return new Jackson3JsonMapper(mapper);
        }

        throw new IllegalArgumentException("Unsupported mapper type: " + (mapper == null ? "null" : mapper.getClass().getName()));
    }

    public static RetreeverJsonMapper fromApplicationContext(ApplicationContext context) {
        String[] jackson2Beans = context.getBeanNamesForType(com.fasterxml.jackson.databind.ObjectMapper.class, false, false);
        if (jackson2Beans.length > 0) {
            return new Jackson2JsonMapper(context.getBean(jackson2Beans[0], com.fasterxml.jackson.databind.ObjectMapper.class));
        }

        for (String beanName : context.getBeanDefinitionNames()) {
            Class<?> beanType = context.getType(beanName);
            if (beanType != null && "tools.jackson.databind.ObjectMapper".equals(beanType.getName())) {
                return new Jackson3JsonMapper(context.getBean(beanName));
            }
        }

        if (hasClass("com.fasterxml.jackson.databind.ObjectMapper")) {
            return Jackson2JsonMapper.createDefault();
        }

        if (hasClass("tools.jackson.databind.ObjectMapper")) {
            return Jackson3JsonMapper.createDefault();
        }

        throw new IllegalStateException("Retreever requires a Jackson ObjectMapper implementation.");
    }

    private static boolean hasClass(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }
}
