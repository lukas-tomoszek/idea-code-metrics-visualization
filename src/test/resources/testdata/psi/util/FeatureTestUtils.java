package com.example.features;

enum FeatureKey {
    FEATURE_ONE,
    ANOTHER_FEATURE,
    DISABLED_FEATURE
}

public class FeatureClient {
    public boolean getBooleanValue(String key, boolean defaultValue) { /* STRING_EVAL_MARKER */
        return true;
    }

    public String getStringValue(int someOtherParam, String key, String defaultValue) { /* STRING_EVAL_SECOND_PARAM_MARKER */
        return "default";
    }

    public boolean isEnabled(FeatureKey key) { /* ENUM_EVAL_MARKER */
        return true;
    }

    public int getIntValue(int unrelated, FeatureKey key, int anotherUnrelated) { /* ENUM_EVAL_SECOND_PARAM_MARKER */
        return 0;
    }

    public void noMatchingConfigMethod(String feature) { /* NO_CONFIG_MATCH_MARKER */
    }
}
