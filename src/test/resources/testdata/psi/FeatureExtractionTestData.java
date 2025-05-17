package com.example.features;

enum FeatureKey {
    FEATURE_ONE,
    ANOTHER_FEATURE,
    DISABLED_FEATURE
}

public class FeatureClient {
    public boolean getBooleanValue(String key, boolean defaultValue) {
        // STRING_EVAL_MARKER
        return true;
    }

    public String getStringValue(int someOtherParam, String key, String defaultValue) {
        // STRING_EVAL_SECOND_PARAM_MARKER
        return "default";
    }

    public boolean isEnabled(FeatureKey key) {
        // ENUM_EVAL_MARKER
        return true;
    }

    public int getIntValue(int unrelated, FeatureKey key, int anotherUnrelated) {
        // ENUM_EVAL_SECOND_PARAM_MARKER
        return 0;
    }

    public void noMatchingConfigMethod(String feature) {
        // NO_CONFIG_MATCH_MARKER
    }
}

class TestUsage {
    private FeatureClient client = new FeatureClient();

    public void useFeatures() {
        boolean f1 = client.getBooleanValue("feature-A", false); // USAGE_STRING_A_MARKER
        boolean f2 = client.getBooleanValue("feature-B", true);  // USAGE_STRING_B_MARKER
        String s1 = client.getStringValue(0, "feature-C", "default"); // USAGE_STRING_C_MARKER

        boolean e1 = client.isEnabled(FeatureKey.FEATURE_ONE);   // USAGE_ENUM_ONE_MARKER
        boolean e2 = client.isEnabled(FeatureKey.ANOTHER_FEATURE); // USAGE_ENUM_ANOTHER_MARKER
        int i1 = client.getIntValue(0, FeatureKey.DISABLED_FEATURE, 1); // USAGE_ENUM_DISABLED_MARKER

        client.noMatchingConfigMethod("some-feature"); // USAGE_NO_CONFIG_MARKER

        client.getBooleanValue(System.getenv("RUNTIME_KEY"), false); // USAGE_STRING_NON_LITERAL_MARKER
        client.isEnabled(null); // USAGE_ENUM_NULL_ARG_MARKER
    }
}
