package com.example.mappings;

import org.springframework.web.bind.annotation.*;

// Non-Spring annotation
@interface MyCustomAnnotation {
    String value() default "";
}

@RestController
@RequestMapping("/api/v1/base")
public class MyController {

    @GetMapping("/simpleGet" /* GET_SIMPLE_MARKER */)
    public String simpleGet() {
        return "simpleGet";
    }

    @PostMapping(value = "/postWithValue")
    public String postWithValue() { /* POST_WITH_VALUE_MARKER */
        return "postWithValue";
    }

    @PutMapping(path = "/putWithPath/{id}" /* PUT_WITH_PATH_MARKER */)
    public String putWithPath(@PathVariable String id) {
        return "putWithPath";
    }

    @DeleteMapping("/deletePath")
    public String deletePath() { /* DELETE_NO_CLASS_MAPPING_MARKER */
        return "deletePath";
    }

    @RequestMapping(value = "/requestMappingGet", method = RequestMethod.GET /* REQUEST_MAPPING_GET_MARKER */)
    public String requestMappingGet() {
        return "requestMappingGet";
    }

    @RequestMapping(
            path = {"/requestMappingPostArray1", "/requestMappingPostArray2"},
            method = {RequestMethod.POST} /* REQUEST_MAPPING_POST_ARRAY_MARKER */
    )
    public String requestMappingPostArray() {
        return "requestMappingPostArray";
    }

    @RequestMapping("/requestMappingNoMethod")
    public String requestMappingNoMethod() { /* REQUEST_MAPPING_NO_METHOD_MARKER */
        return "requestMappingNoMethod";
    }

    @PatchMapping("/patch/{userId}/item/{itemId}" /* PATCH_WITH_PLACEHOLDER_MARKER */)
    public String patchWithPlaceholder(@PathVariable String userId, @PathVariable String itemId) {
        return "patchWithPlaceholder";
    }

    @GetMapping("/" /* GET_ROOT_PATH_MARKER */)
    public String getRootPath() {
        return "getRootPath";
    }

    @GetMapping("" /* GET_EMPTY_PATH_MARKER */)
    public String getEmptyPath() {
        return "getEmptyPath";
    }
}

@RestController
@RequestMapping
class RootPathController {

    @GetMapping("/specific" /* GET_MAPPING_ON_ROOT_CONTROLLER_MARKER */)
    public String specific() {
        return "specific";
    }

    @GetMapping
    public String rootGet() { /* GET_MAPPING_EMPTY_ON_ROOT_CONTROLLER_MARKER */
        return "rootGet";
    }
}

@RestController
class NoBaseMappingController {

    @GetMapping("/noBase")
    public String getNoBase() { /* GET_NO_BASE_MARKER */
        return "getNoBase";
    }

    @PostMapping(value = "/noBasePost" /* POST_NO_BASE_VALUE_ATTRIBUTE_MARKER */)
    public String postNoBaseValueAttribute() {
        return "postNoBaseValueAttribute";
    }
}

class NotASpringController {

    @MyCustomAnnotation("/notspring" /* NOT_SPRING_MARKER */)
    public void notSpringMethod() {
    }
}

@RestController
@RequestMapping(method = RequestMethod.PUT)
class ClassLevelDefaultMethodController {

    @RequestMapping("/pathOnly" /* CLASS_LEVEL_DEFAULT_METHOD_MARKER */)
    public String pathOnly() {
        return "pathOnlyUsesClassMethod";
    }

    @RequestMapping(value = "/overrideMethod", method = RequestMethod.POST)
    public String overrideMethod() { /* CLASS_LEVEL_OVERRIDDEN_METHOD_MARKER */
        return "overrideMethodUsesOwn";
    }
}

@RestController
@RequestMapping(path = "/prefixOnly")
class ClassLevelPathOnlyController {

    @GetMapping(/* CLASS_LEVEL_PATH_ONLY_MARKER */)
    public String methodWithPathFromClass() {
        return "";
    }
}

@RestController
@RequestMapping(value = "/valueOnly")
class ClassLevelValueOnlyController {

    @GetMapping(/* CLASS_LEVEL_VALUE_ONLY_MARKER */)
    public String methodWithValueFromClass() {
        return "";
    }
}
