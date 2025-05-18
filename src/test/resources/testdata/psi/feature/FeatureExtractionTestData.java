package com.example.usage;

import com.example.features.FeatureClient;
import com.example.features.FeatureKey;

class TestUsage {
    private FeatureClient client = new FeatureClient();

    public void useFeatures() {
        boolean f1 = client.getBooleanValue("feature-A" /* USAGE_STRING_A_MARKER */, false);
        boolean f2 = client.getBooleanValue("feature-B" /* USAGE_STRING_B_MARKER */, true);
        String s1 = client.getStringValue(0, "feature-C" /* USAGE_STRING_C_MARKER */, "default");

        boolean e1 = client.isEnabled(FeatureKey.FEATURE_ONE /* USAGE_ENUM_ONE_MARKER */);
        boolean e2 = client.isEnabled(FeatureKey.ANOTHER_FEATURE /* USAGE_ENUM_ANOTHER_MARKER */);
        int i1 = client.getIntValue(0, FeatureKey.DISABLED_FEATURE /* USAGE_ENUM_DISABLED_MARKER */, 1);

        client.noMatchingConfigMethod("some-feature" /* USAGE_NO_CONFIG_MARKER */);

        client.getBooleanValue(System.getenv("RUNTIME_KEY") /* USAGE_STRING_NON_LITERAL_MARKER */, false);
        client.isEnabled(null /* USAGE_ENUM_NULL_ARG_MARKER */);
    }

    public boolean getBooleanValue(String key, boolean defaultValue) { /* METHOD_DECLARATION_MARKER */
        return false;
    }
}
