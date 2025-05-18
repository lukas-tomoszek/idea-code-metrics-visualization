package org.springframework.web.bind.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

enum RequestMethod {
    GET, POST, PUT, DELETE, PATCH
}

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestMapping {
    String[] value() default {};

    String[] path() default {};

    RequestMethod[] method() default {};
}

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface GetMapping {
    String[] value() default {};

    String[] path() default {};
}

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface PostMapping {
    String[] value() default {};

    String[] path() default {};
}

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface PutMapping {
    String[] value() default {};

    String[] path() default {};
}

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DeleteMapping {
    String[] value() default {};

    String[] path() default {};
}

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface PatchMapping {
    String[] value() default {};

    String[] path() default {};
}

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RestController {
}
