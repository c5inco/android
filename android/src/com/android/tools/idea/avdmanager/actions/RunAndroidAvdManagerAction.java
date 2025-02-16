/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.android.tools.idea.avdmanager.actions;

import static com.android.tools.idea.avdmanager.HardwareAccelerationCheck.isChromeOSAndIsNotHWAccelerated;

import com.android.sdklib.internal.avd.AvdInfo;
import com.android.tools.idea.IdeInfo;
import com.android.tools.idea.avdmanager.AvdListDialog;
import com.android.tools.idea.devicemanager.DeviceManagerToolWindowFactory;
import com.android.tools.idea.flags.StudioFlags;
import com.intellij.facet.ProjectFacetManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import icons.StudioIcons;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.sdk.AndroidSdkUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RunAndroidAvdManagerAction extends DumbAwareAction {
  public static final String ID = "Android.RunAndroidAvdManager";

  @Nullable private AvdListDialog myDialog;

  @Override
  public void update(@NotNull AnActionEvent event) {
    Presentation presentation = event.getPresentation();

    switch (event.getPlace()) {
      case ActionPlaces.TOOLBAR:
        // Layout editor device menu
        presentation.setIcon(null);
        presentation.setText("Add Device Definition...");
        presentation.setVisible(true);
        break;
      case ActionPlaces.UNKNOWN:
        // run target menu
        presentation.setIcon(StudioIcons.Shell.Toolbar.DEVICE_MANAGER);
        presentation.setText(StudioFlags.ENABLE_NEW_DEVICE_MANAGER_PANEL.get() ? "Open Device Manager" : "Open AVD Manager");
        presentation.setVisible(true);
        break;
      case ActionPlaces.WELCOME_SCREEN:
        // welcome screen, but only on first start?
        presentation.setIcon(StudioIcons.Shell.Toolbar.DEVICE_MANAGER);
        presentation.setText("AVD Manager");
        presentation.setVisible(!StudioFlags.ENABLE_NEW_DEVICE_MANAGER_PANEL.get());
        break;
      default:
        if (event.getPlace().equals(ActionPlaces.getActionGroupPopupPlace(ActionPlaces.WELCOME_SCREEN))) {
          // welcome screen, opening the popup menu
          presentation.setIcon(StudioIcons.Shell.Toolbar.DEVICE_MANAGER);
          presentation.setText("AVD Manager");
          presentation.setVisible(!StudioFlags.ENABLE_NEW_DEVICE_MANAGER_PANEL.get());
        }
        else {
          presentation.setIcon(StudioIcons.Shell.Toolbar.DEVICE_MANAGER);
          presentation.setText(StudioFlags.ENABLE_NEW_DEVICE_MANAGER_PANEL.get() ? "Device Manager" : "AVD Manager");
          presentation.setVisible(true);
        }
        break;
    }

    presentation.setDescription(StudioFlags.ENABLE_NEW_DEVICE_MANAGER_PANEL.get() ?
                                "Opens the device manager which manages virtual and physical devices" :
                                "Opens the Android virtual device (AVD) manager");

    if (ActionPlaces.MAIN_TOOLBAR.equals(event.getPlace()) && !IdeInfo.getInstance().isAndroidStudio()) {
      @Nullable Project project = event.getProject();
      boolean hasAndroidFacets = project != null && ProjectFacetManager.getInstance(project).hasFacets(AndroidFacet.ID);
      presentation.setVisible(hasAndroidFacets);
    }

    if (isChromeOSAndIsNotHWAccelerated()) {
      presentation.setVisible(false);
      return;
    }

    presentation.setEnabled(AndroidSdkUtils.isAndroidSdkAvailable());
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    openAvdManager(e.getProject());
  }

  public void openAvdManager(@Nullable Project project) {
    if (StudioFlags.ENABLE_NEW_DEVICE_MANAGER_PANEL.get()) {
      openDeviceManager(project);
    }
    else {
      openOldAvdManager(project);
    }
  }

  private void openOldAvdManager(@Nullable Project project) {
    if (isChromeOSAndIsNotHWAccelerated()) {
      return;
    }

    if (myDialog == null) {
      myDialog = new AvdListDialog(project);
      myDialog.init();
      myDialog.show();
      // Remove the dialog reference when the dialog is disposed (closed).
      Disposer.register(myDialog, () -> myDialog = null);
    }
    else {
      myDialog.getFrame().toFront();
    }
  }

  private static void openDeviceManager(@Nullable Project project) {
    if (project == null) {
      // TODO(qumeric): investigate if it is possible and let the user know if it is.
      return;
    }

    ToolWindow deviceManager = ToolWindowManager.getInstance(project).getToolWindow(DeviceManagerToolWindowFactory.ID);

    if (deviceManager != null) {
      deviceManager.show(null);
    }
  }

  @Nullable
  public AvdInfo getSelected() {
    return myDialog == null ? null : myDialog.getSelected();
  }
}
