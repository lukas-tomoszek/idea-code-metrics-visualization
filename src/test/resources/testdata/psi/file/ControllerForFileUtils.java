package com.example.fileutils;

import com.example.features.FeatureClient;
import com.example.features.FeatureKey;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/class-level-fileutils")
public class ControllerForFileUtils {

    private FeatureClient featureService = new FeatureClient();

    @GetMapping("/methodOneFileUtils" /* FILE_UTILS_METHOD_ONE_MARKER */)
    public void methodOneFileUtils() {
        featureService.getBooleanValue("featureX.fileutils" /* FILE_UTILS_FEATURE_X_MARKER */, false);
        featureService.isEnabled(FeatureKey.DISABLED_FEATURE);
    }

    @PostMapping("/methodTwoFileUtils")
    public String methodTwoFileUtils(String body) { /* FILE_UTILS_METHOD_TWO_MARKER */
        featureService.getBooleanValue("featureY.fileutils" /* FILE_UTILS_FEATURE_Y_MARKER */, true);
        return "two";
    }

    public void utilityMethodFileUtils() { /* FILE_UTILS_UTILITY_METHOD_MARKER */
        featureService.getBooleanValue("featureX.fileutils", true);
        featureService.isEnabled(FeatureKey.ANOTHER_FEATURE /* FILE_UTILS_FEATURE_ANOTHER_FEATURE_MARKER */);
    }

    @GetMapping
    public void classLevelPathMethodFileUtils() { /* FILE_UTILS_CLASS_LEVEL_PATH_METHOD_MARKER */ }

    @RequestMapping(value = "/methodThreeFileUtils", method = RequestMethod.PUT)
    public void methodThreeFileUtils() { /* FILE_UTILS_METHOD_THREE_MARKER */ }
}
