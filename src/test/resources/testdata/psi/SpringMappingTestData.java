package com.example.mappings;

// Non-Spring annotation
@interface MyCustomAnnotation {
    String value() default "";
}

@org.springframework.web.bind.annotation.RestController
@org.springframework.web.bind.annotation.RequestMapping("/api/v1/base")
public class MyController {

    @org.springframework.web.bind.annotation.GetMapping("/simpleGet") // GET_SIMPLE_MARKER
    public String simpleGet() {
        return "simpleGet";
    }

    @org.springframework.web.bind.annotation.PostMapping(value = "/postWithValue") // POST_WITH_VALUE_MARKER
    public String postWithValue() {
        return "postWithValue";
    }

    @org.springframework.web.bind.annotation.PutMapping(path = "/putWithPath/{id}") // PUT_WITH_PATH_MARKER
    public String putWithPath(@PathVariable String id) {
        return "putWithPath";
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/deletePath") // DELETE_NO_CLASS_MAPPING_MARKER
    public String deletePath() {
        return "deletePath";
    }

    @org.springframework.web.bind.annotation.RequestMapping(value = "/requestMappingGet", method = org.springframework.web.bind.annotation.RequestMethod.GET)
    // REQUEST_MAPPING_GET_MARKER
    public String requestMappingGet() {
        return "requestMappingGet";
    }

    @org.springframework.web.bind.annotation.RequestMapping(
            path = {"/requestMappingPostArray1", "/requestMappingPostArray2"},
            method = {org.springframework.web.bind.annotation.RequestMethod.POST}
    ) // REQUEST_MAPPING_POST_ARRAY_MARKER
    public String requestMappingPostArray() {
        return "requestMappingPostArray";
    }

    @org.springframework.web.bind.annotation.RequestMapping("/requestMappingNoMethod")
    // REQUEST_MAPPING_NO_METHOD_MARKER
    public String requestMappingNoMethod() {
        return "requestMappingNoMethod";
    }

    @org.springframework.web.bind.annotation.PatchMapping("/patch/{userId}/item/{itemId}")
    // PATCH_WITH_PLACEHOLDER_MARKER
    public String patchWithPlaceholder(@PathVariable String userId, @PathVariable String itemId) {
        return "patchWithPlaceholder";
    }

    @org.springframework.web.bind.annotation.GetMapping("/") // GET_ROOT_PATH_MARKER
    public String getRootPath() {
        return "getRootPath";
    }

    @org.springframework.web.bind.annotation.GetMapping("") // GET_EMPTY_PATH_MARKER
    public String getEmptyPath() {
        return "getEmptyPath";
    }
}

@org.springframework.web.bind.annotation.RestController
@org.springframework.web.bind.annotation.RequestMapping
class RootPathController {

    @org.springframework.web.bind.annotation.GetMapping("/specific") // GET_MAPPING_ON_ROOT_CONTROLLER_MARKER
    public String specific() {
        return "specific";
    }

    @org.springframework.web.bind.annotation.GetMapping // GET_MAPPING_EMPTY_ON_ROOT_CONTROLLER_MARKER
    public String rootGet() {
        return "rootGet";
    }
}

@org.springframework.web.bind.annotation.RestController
class NoBaseMappingController {

    @org.springframework.web.bind.annotation.GetMapping("/noBase") // GET_NO_BASE_MARKER
    public String getNoBase() {
        return "getNoBase";
    }

    @org.springframework.web.bind.annotation.PostMapping(value = "/noBasePost") // POST_NO_BASE_VALUE_ATTRIBUTE_MARKER
    public String postNoBaseValueAttribute() {
        return "postNoBaseValueAttribute";
    }
}

class NotASpringController {

    @MyCustomAnnotation("/notspring") // NOT_SPRING_MARKER
    public void notSpringMethod() {
    }
}

@org.springframework.web.bind.annotation.RestController
@org.springframework.web.bind.annotation.RequestMapping(method = org.springframework.web.bind.annotation.RequestMethod.PUT)
class ClassLevelDefaultMethodController {

    @org.springframework.web.bind.annotation.RequestMapping("/pathOnly") // CLASS_LEVEL_DEFAULT_METHOD_MARKER
    public String pathOnly() {
        return "pathOnlyUsesClassMethod";
    }

    @org.springframework.web.bind.annotation.RequestMapping(value = "/overrideMethod", method = org.springframework.web.bind.annotation.RequestMethod.POST)
    // CLASS_LEVEL_OVERRIDDEN_METHOD_MARKER
    public String overrideMethod() {
        return "overrideMethodUsesOwn";
    }
}

@org.springframework.web.bind.annotation.RestController
@org.springframework.web.bind.annotation.RequestMapping(path = "/prefixOnly")
class ClassLevelPathOnlyController {

    @org.springframework.web.bind.annotation.GetMapping // CLASS_LEVEL_PATH_ONLY_MARKER
    public String methodWithPathFromClass() {
        return "";
    }
}

@org.springframework.web.bind.annotation.RestController
@org.springframework.web.bind.annotation.RequestMapping(value = "/valueOnly")
class ClassLevelValueOnlyController {

    @org.springframework.web.bind.annotation.GetMapping // CLASS_LEVEL_VALUE_ONLY_MARKER
    public String methodWithValueFromClass() {
        return "";
    }
}
