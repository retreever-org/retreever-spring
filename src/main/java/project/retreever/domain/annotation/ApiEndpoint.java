package project.retreever.domain.annotation;

import org.springframework.http.HttpStatus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiEndpoint {

    String name() default "";
    boolean secured() default false;

    HttpStatus status() default HttpStatus.OK;

    String[] headers() default {};

    String description() default "";

    Class<? extends Throwable>[] errors() default {};
}
