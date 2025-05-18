package com.example.fileutils;

import com.example.features.FeatureClient;
import com.example.features.FeatureKey;

class OnlyFeaturesFileUtils { /* FILE_UTILS_ONLY_FEATURES_CLASS_MARKER */
    private FeatureClient featureService = new FeatureClient();

    public void triggerFeaturesFileUtils() { /* FILE_UTILS_ONLY_FEATURES_METHOD_MARKER */
        featureService.getBooleanValue("featureZ.fileutils" /* FILE_UTILS_FEATURE_Z_MARKER */, false);
        featureService.isEnabled(FeatureKey.FEATURE_ONE /* FILE_UTILS_FEATURE_FEATURE_ONE_MARKER */);
    }
}
