/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.tools.idea.common.surface;

import com.android.annotations.concurrency.UiThread;
import com.android.tools.idea.common.model.NlComponent;
import com.android.tools.idea.common.model.NlModel;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface implemented by listeners for {@link DesignSurface} events
 */
public interface DesignSurfaceListener {
  /** The set of currently selected components in the given surface changed */
  default void componentSelectionChanged(@NotNull DesignSurface surface, @NotNull List<NlComponent> newSelection) {}

  /** The current model changed */
  @UiThread
  default void modelChanged(@NotNull DesignSurface surface, @Nullable NlModel model) {}

  /** Ask the tools to activate an editor for the component specified, return true if such an editor is found */
  default boolean activatePreferredEditor(@NotNull DesignSurface surface, @NotNull NlComponent component) {
    return false;
  }

  /** Change the visibility of related accessory panel */
  default void showAccessoryPanel(@NotNull DesignSurface surface, boolean show) {}
}
