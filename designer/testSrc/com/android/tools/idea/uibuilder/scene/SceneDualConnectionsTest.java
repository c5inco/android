/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.tools.idea.uibuilder.scene;

import static com.android.SdkConstants.CONSTRAINT_LAYOUT;
import static com.android.SdkConstants.TEXT_VIEW;

import com.android.tools.adtui.common.AdtUiUtils;
import com.android.tools.idea.common.fixtures.ModelBuilder;
import com.android.tools.idea.common.scene.target.AnchorTarget;
import org.jetbrains.annotations.NotNull;

/**
 * Test dual connection interactions
 */
public class SceneDualConnectionsTest extends SceneTest {

  public void testDeleteLeft() {
    myInteraction.setModifiersEx(AdtUiUtils.getActionMask());
    myInteraction.select("button", true);
    myInteraction.mouseDown("button", AnchorTarget.Type.LEFT);
    myInteraction.mouseRelease("button", AnchorTarget.Type.LEFT);
    myScreen.get("@id/button")
      .expectXml("<TextView\n" +
                 "        android:id=\"@id/button\"\n" +
                 "        android:layout_width=\"100dp\"\n" +
                 "        android:layout_height=\"20dp\"\n" +
                 "        app:layout_constraintBottom_toBottomOf=\"parent\"\n" +
                 "        app:layout_constraintRight_toRightOf=\"parent\"\n" +
                 "        app:layout_constraintTop_toTopOf=\"parent\" />");
    myInteraction.mouseDown("button", AnchorTarget.Type.RIGHT);
    myInteraction.mouseRelease("button", AnchorTarget.Type.RIGHT);
    myScreen.get("@id/button")
      .expectXml("<TextView\n" +
                 "        android:id=\"@id/button\"\n" +
                 "        android:layout_width=\"100dp\"\n" +
                 "        android:layout_height=\"20dp\"\n" +
                 "        app:layout_constraintBottom_toBottomOf=\"parent\"\n" +
                 "        app:layout_constraintTop_toTopOf=\"parent\"\n" +
                 "        tools:layout_editor_absoluteX=\"450dp\" />");
  }

  public void testDeleteTop() {
    myInteraction.setModifiersEx(AdtUiUtils.getActionMask());
    myInteraction.select("button", true);
    myInteraction.mouseDown("button", AnchorTarget.Type.TOP);
    myInteraction.mouseRelease("button", AnchorTarget.Type.TOP);
    myScreen.get("@id/button")
      .expectXml("<TextView\n" +
                 "        android:id=\"@id/button\"\n" +
                 "        android:layout_width=\"100dp\"\n" +
                 "        android:layout_height=\"20dp\"\n" +
                 "        app:layout_constraintBottom_toBottomOf=\"parent\"\n" +
                 "        app:layout_constraintLeft_toLeftOf=\"parent\"\n" +
                 "        app:layout_constraintRight_toRightOf=\"parent\" />");
    myInteraction.mouseDown("button", AnchorTarget.Type.BOTTOM);
    myInteraction.mouseRelease("button", AnchorTarget.Type.BOTTOM);
    myScreen.get("@id/button")
      .expectXml("<TextView\n" +
                 "        android:id=\"@id/button\"\n" +
                 "        android:layout_width=\"100dp\"\n" +
                 "        android:layout_height=\"20dp\"\n" +
                 "        app:layout_constraintLeft_toLeftOf=\"parent\"\n" +
                 "        app:layout_constraintRight_toRightOf=\"parent\"\n" +
                 "        tools:layout_editor_absoluteY=\"490dp\" />");
  }

  public void testDeleteRight() {
    myInteraction.setModifiersEx(AdtUiUtils.getActionMask());
    myInteraction.select("button", true);
    myInteraction.mouseDown("button", AnchorTarget.Type.RIGHT);
    myInteraction.mouseRelease("button", AnchorTarget.Type.RIGHT);
    myScreen.get("@id/button")
      .expectXml("<TextView\n" +
                 "        android:id=\"@id/button\"\n" +
                 "        android:layout_width=\"100dp\"\n" +
                 "        android:layout_height=\"20dp\"\n" +
                 "        app:layout_constraintBottom_toBottomOf=\"parent\"\n" +
                 "        app:layout_constraintLeft_toLeftOf=\"parent\"\n" +
                 "        app:layout_constraintTop_toTopOf=\"parent\" />");
    myInteraction.mouseDown("button", AnchorTarget.Type.LEFT);
    myInteraction.mouseRelease("button", AnchorTarget.Type.LEFT);
    myScreen.get("@id/button")
      .expectXml("<TextView\n" +
                 "        android:id=\"@id/button\"\n" +
                 "        android:layout_width=\"100dp\"\n" +
                 "        android:layout_height=\"20dp\"\n" +
                 "        app:layout_constraintBottom_toBottomOf=\"parent\"\n" +
                 "        app:layout_constraintTop_toTopOf=\"parent\"\n" +
                 "        tools:layout_editor_absoluteX=\"450dp\" />");
  }

  public void testDeleteBottom() {
    myInteraction.setModifiersEx(AdtUiUtils.getActionMask());
    myInteraction.select("button", true);
    myInteraction.mouseDown("button", AnchorTarget.Type.BOTTOM);
    myInteraction.mouseRelease("button", AnchorTarget.Type.BOTTOM);
    myScreen.get("@id/button")
      .expectXml("<TextView\n" +
                 "        android:id=\"@id/button\"\n" +
                 "        android:layout_width=\"100dp\"\n" +
                 "        android:layout_height=\"20dp\"\n" +
                 "        app:layout_constraintLeft_toLeftOf=\"parent\"\n" +
                 "        app:layout_constraintRight_toRightOf=\"parent\"\n" +
                 "        app:layout_constraintTop_toTopOf=\"parent\" />");
    myInteraction.mouseDown("button", AnchorTarget.Type.TOP);
    myInteraction.mouseRelease("button", AnchorTarget.Type.TOP);
    myScreen.get("@id/button")
      .expectXml("<TextView\n" +
                 "        android:id=\"@id/button\"\n" +
                 "        android:layout_width=\"100dp\"\n" +
                 "        android:layout_height=\"20dp\"\n" +
                 "        app:layout_constraintLeft_toLeftOf=\"parent\"\n" +
                 "        app:layout_constraintRight_toRightOf=\"parent\"\n" +
                 "        tools:layout_editor_absoluteY=\"490dp\" />");
  }

  @Override
  @NotNull
  public ModelBuilder createModel() {
    return model("constraint.xml",
                 component(CONSTRAINT_LAYOUT.defaultName())
                   .id("@id/root")
                   .withBounds(0, 0, 2000, 2000)
                   .width("1000dp")
                   .height("1000dp")
                   .withAttribute("android:padding", "20dp")
                   .children(
                     component(TEXT_VIEW)
                       .id("@id/button")
                       .withBounds(900, 980, 200, 40)
                       .width("100dp")
                       .height("20dp")
                       .withAttribute("app:layout_constraintLeft_toLeftOf", "parent")
                       .withAttribute("app:layout_constraintRight_toRightOf", "parent")
                       .withAttribute("app:layout_constraintTop_toTopOf", "parent")
                       .withAttribute("app:layout_constraintBottom_toBottomOf", "parent")
                   ));
  }
}
