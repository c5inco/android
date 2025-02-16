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
package org.jetbrains.android.dom.animation.fileDescriptions;

import com.android.resources.ResourceFolderType;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.android.dom.MultipleKnownRootsResourceDomFileDescription;
import org.jetbrains.android.dom.animation.InterpolatorElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class InterpolatorDomFileDescription extends MultipleKnownRootsResourceDomFileDescription<InterpolatorElement> {
  /**
   * Map contains name of a styleable with attributes by a tag name.
   * If key maps to {@link Optional#empty()} it means that such tag exists but doesn't have any attributes.
   */
  public static final Map<String, Optional<String>> STYLEABLE_BY_TAG = Map.of(
    "linearInterpolator", Optional.empty(),
    "accelerateInterpolator", Optional.of("AccelerateInterpolator"),
    "decelerateInterpolator", Optional.of("DecelerateInterpolator"),
    "accelerateDecelerateInterpolator", Optional.empty(),
    "cycleInterpolator", Optional.of("CycleInterpolator"),
    "anticipateInterpolator", Optional.of("AnticipateInterpolator"),
    "overshootInterpolator", Optional.of("OvershootInterpolator"),
    "anticipateOvershootInterpolator", Optional.of("AnticipateOvershootInterpolator"),
    "bounceInterpolator", Optional.empty(),
    "pathInterpolator", Optional.of("PathInterpolator")
    );

  public InterpolatorDomFileDescription() {
    super(InterpolatorElement.class, ResourceFolderType.ANIM, STYLEABLE_BY_TAG.keySet());
  }

  public static @Nullable String getInterpolatorStyleableByTagName(@NotNull String tagName) {
    Optional<String> optional = STYLEABLE_BY_TAG.get(tagName);
    return optional != null && optional.isPresent() ? optional.get() : null;
  }
}
