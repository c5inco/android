load("//tools/base/bazel:bazel.bzl", "iml_module")

# managed by go/iml_to_build
iml_module(
    name = "intellij.android.adt.ui",
    srcs = ["src/main/java"],
    iml_files = ["intellij.android.adt.ui.iml"],
    lint_baseline = "lint_baseline.xml",
    resources = ["resources"],
    test_class = "com.android.tools.adtui.AdtUiTestSuite",
    test_data = [
        "//tools/adt/idea/artwork:device-art-resources",
    ],
    test_flaky = True,  # b/172521726
    test_resources = ["src/test/resources"],
    test_srcs = ["src/test/java"],
    visibility = ["//visibility:public"],
    # do not sort: must match IML order
    deps = [
        "//prebuilts/studio/intellij-sdk:studio-sdk",
        "//prebuilts/studio/intellij-sdk:studio-sdk-plugin-platform-images[test]",
        "//prebuilts/studio/intellij-sdk:studio-sdk-plugin-webp[test]",
        "//tools/base/common:studio.android.sdktools.common[module]",
        "//tools/adt/idea/.idea/libraries:truth[test]",
        "//tools/base/testutils:studio.android.sdktools.testutils[module, test]",
        "//tools/adt/idea/.idea/libraries:mockito[test]",
        "//tools/adt/idea/adt-ui-model:intellij.android.adt.ui.model[module]",
        "//tools/adt/idea/observable:intellij.android.observable[module]",
        "//tools/adt/idea/adt-testutils:intellij.android.adt.testutils[module, test]",
        "//tools/adt/idea/artwork:intellij.android.artwork[module]",
        "//tools/base/layoutlib-api:studio.android.sdktools.layoutlib-api[module]",
        "//tools/base/sdklib:studio.android.sdktools.sdklib[module]",
        "//tools/base/ninepatch:studio.android.sdktools.ninepatch[module]",
        "//tools/adt/idea/android-common:intellij.android.common[module]",
        "//tools/adt/idea/.idea/libraries:jetbrains.kotlinx.coroutines.test[test]",
    ],
)

java_binary(
    name = "baseline-images-generator",
    testonly = True,
    main_class = "com.android.tools.adtui.imagediff.BaselineImagesGenerator",
    runtime_deps = [":intellij.android.adt.ui_testlib"],
)
