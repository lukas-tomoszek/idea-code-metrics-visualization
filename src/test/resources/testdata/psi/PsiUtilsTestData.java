package com.example;

public class TopLevelClass {

    private String classField = "value"; // FIELD_MARKER

    public TopLevelClass() {
        String varInConstructor = "constructor"; // CONSTRUCTOR_MARKER
    }

    public static void staticMethod() {
        int varInStatic = 2; // STATIC_METHOD_MARKER
    }

    public void simpleMethod() {
        String varInSimpleMethod = "here"; // SIMPLE_METHOD_MARKER
    }

    public void methodWithLambda() {
        Runnable r = () -> {
            String varInLambda = "lambda"; // LAMBDA_MARKER
        };
        r.run();
    }

    public static class NestedClass {
        public void methodInNestedClass() {
            int varInNested = 1; // NESTED_CLASS_METHOD_MARKER
        }
    }
}

class ClassWithoutPackage {
    public void methodInClassWithoutPackage() {
        String varInNoPackage = "no_package"; // NO_PACKAGE_CLASS_METHOD_MARKER
    }
}

// TOP_LEVEL_COMMENT_MARKER
