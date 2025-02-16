/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.idea.avdmanager;

import com.android.sdklib.internal.avd.AvdInfo;
import com.android.sdklib.internal.avd.AvdManager;
import icons.StudioIcons;
import java.awt.Dimension;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.android.AndroidTestCase;

public class AvdDisplayListTest extends AndroidTestCase {
  private AvdInfo myAvdInfo;
  private final Map<String, String> myPropertiesMap = new HashMap<>();

  @Override
  public void setUp() throws Exception {
    super.setUp();
    myAvdInfo = new AvdInfo("name", Paths.get("ini"), Paths.get("folder"), null, myPropertiesMap);
  }

  public void testGetResolution() {
    assertEquals("Unknown Resolution", AvdDisplayList.getResolution(myAvdInfo));

    myPropertiesMap.put(AvdManager.AVD_INI_DEVICE_NAME, "Nexus 5");
    myPropertiesMap.put(AvdManager.AVD_INI_DEVICE_MANUFACTURER, "Google");
    assertEquals("1080 × 1920: xxhdpi", AvdDisplayList.getResolution(myAvdInfo));

    myPropertiesMap.put(AvdManager.AVD_INI_DEVICE_NAME, "Nexus 10");
    myPropertiesMap.put(AvdManager.AVD_INI_DEVICE_MANUFACTURER, "Google");
    assertEquals("2560 × 1600: xhdpi", AvdDisplayList.getResolution(myAvdInfo));

    myPropertiesMap.put(AvdManager.AVD_INI_DEVICE_NAME, "Nexus S");
    myPropertiesMap.put(AvdManager.AVD_INI_DEVICE_MANUFACTURER, "Google");
    assertEquals("480 × 800: hdpi", AvdDisplayList.getResolution(myAvdInfo));

    myPropertiesMap.put(AvdManager.AVD_INI_DEVICE_NAME, "Nexus 6");
    myPropertiesMap.put(AvdManager.AVD_INI_DEVICE_MANUFACTURER, "Google");
    assertEquals("1440 × 2560: 560dpi", AvdDisplayList.getResolution(myAvdInfo));
  }

  public void testGetScreenSize() {
    assertNull(AvdDisplayList.getScreenSize(myAvdInfo));

    myPropertiesMap.put(AvdManager.AVD_INI_DEVICE_NAME, "Nexus 5");
    myPropertiesMap.put(AvdManager.AVD_INI_DEVICE_MANUFACTURER, "Google");
    assertDimension(1080, 1920, AvdDisplayList.getScreenSize(myAvdInfo));

    myPropertiesMap.put(AvdManager.AVD_INI_DEVICE_NAME, "Nexus 10");
    myPropertiesMap.put(AvdManager.AVD_INI_DEVICE_MANUFACTURER, "Google");
    assertDimension(2560, 1600, AvdDisplayList.getScreenSize(myAvdInfo));

    myPropertiesMap.put(AvdManager.AVD_INI_DEVICE_NAME, "Nexus S");
    myPropertiesMap.put(AvdManager.AVD_INI_DEVICE_MANUFACTURER, "Google");
    assertDimension(480, 800, AvdDisplayList.getScreenSize(myAvdInfo));

    myPropertiesMap.put(AvdManager.AVD_INI_DEVICE_NAME, "Nexus 6");
    myPropertiesMap.put(AvdManager.AVD_INI_DEVICE_MANUFACTURER, "Google");
    assertDimension(1440, 2560, AvdDisplayList.getScreenSize(myAvdInfo));
  }

  public void testGetDeviceClassIconPair() {
    assertEquals(StudioIcons.Avd.DEVICE_MOBILE_LARGE, AvdDisplayList.getDeviceClassIconPair(myAvdInfo).getBaseIcon());

    myPropertiesMap.put(AvdManager.AVD_INI_TAG_ID, "android-tv");
    assertEquals(StudioIcons.Avd.DEVICE_TV_LARGE, AvdDisplayList.getDeviceClassIconPair(myAvdInfo).getBaseIcon());

    myPropertiesMap.put(AvdManager.AVD_INI_TAG_ID, "android-wear");
    assertEquals(StudioIcons.Avd.DEVICE_WEAR_LARGE, AvdDisplayList.getDeviceClassIconPair(myAvdInfo).getBaseIcon());
  }

  private static void assertDimension(double width, double height, Dimension dimension) {
    assertEquals(width, dimension.getWidth());
    assertEquals(height, dimension.getHeight());
  }
}