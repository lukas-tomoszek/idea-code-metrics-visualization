package com.example.contextresolver;

import com.example.features.FeatureClient;
import com.example.features.FeatureKey;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/context" /* CLASS_ANNOTATION_MARKER */)
public class PsiContextResolverTestData { /* CLASS_DECLARATION_MARKER */

    private FeatureClient featureClient = new FeatureClient();
    private String field = "test"; /* CLASS_LEVEL_FIELD_MARKER */

    @GetMapping("/methodOne") /* METHOD_ONE_ANNOTATION_MARKER */
    public void methodOne() {
        /* METHOD_ONE_BODY_MARKER */
        System.out.println("Method One");
        boolean isEnabled = featureClient.getBooleanValue("feature.one" /* FEATURE_ONE_CALL_MARKER */, false);
    }

    @PostMapping("/methodTwo/{id}")
    public String methodTwo(@PathVariable String id) {
        /* METHOD_TWO_BODY_MARKER */
        featureClient.isEnabled(FeatureKey.ANOTHER_FEATURE /* FEATURE_TWO_CALL_MARKER */);
        return "Method Two: " + id;
    }

    public void utilityMethod() {
        /* UTILITY_METHOD_BODY_MARKER */
    }

    @GetMapping
    public void methodThree() {
        /* METHOD_THREE_BODY_MARKER */
        System.out.println("Method Three");
    }
}

class AnotherClassInFile {
    public void anotherMethod() {
        /* ANOTHER_METHOD_BODY_MARKER */
    }
}
