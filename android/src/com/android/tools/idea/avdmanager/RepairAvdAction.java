/*
 * Copyright (C) 2015 The Android Open Source Project
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

import com.intellij.icons.AllIcons;
import com.intellij.openapi.ui.DialogWrapper;
import java.awt.event.ActionEvent;

final class RepairAvdAction extends AvdUiAction {
  public RepairAvdAction(AvdInfoProvider avdInfoProvider) {
    super(avdInfoProvider, "Repair Device", "Repair Device", AllIcons.General.BalloonWarning);
  }

  @Override
  public boolean isEnabled() {
    return getAvdInfo() != null;
  }

  @Override
  public void actionPerformed(ActionEvent event) {
    DialogWrapper dialog = AvdWizardUtils.createAvdWizard(myAvdInfoProvider.getAvdProviderComponent(), getProject(), getAvdInfo());

    if (dialog.showAndGet()) {
      refreshAvds();
    }
  }
}
