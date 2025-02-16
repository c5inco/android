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
package com.android.tools.idea.observable.expressions.value;

import static com.google.common.truth.Truth.assertThat;

import com.android.tools.idea.observable.core.OptionalProperty;
import com.android.tools.idea.observable.core.OptionalValueProperty;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public final class ValueExpressionsTest {
  @Test
  public void testFromOptionalExpression() {

    OptionalProperty<Integer> intProperty = OptionalValueProperty.of(42);

    TransformOptionalExpression<Integer, String> toStringExpr = new TransformOptionalExpression<>("(null int)", intProperty) {
      @Override
      @NotNull
      protected String transform(@NotNull Integer intValue) {
        return intValue.toString();
      }
    };

    assertThat(toStringExpr.get()).isEqualTo("42");

    intProperty.clear();
    assertThat(toStringExpr.get()).isEqualTo("(null int)");
  }
}
