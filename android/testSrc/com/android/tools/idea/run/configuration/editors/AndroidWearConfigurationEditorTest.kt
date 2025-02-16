/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.tools.idea.run.configuration.editors

import com.android.tools.adtui.TreeWalker
import com.android.tools.idea.run.configuration.AndroidWatchFaceConfiguration
import com.android.tools.idea.run.configuration.AndroidWatchFaceConfigurationType
import com.android.tools.idea.run.configuration.addWatchFace
import com.google.common.truth.Truth.assertThat
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.SimpleListCellRenderer
import org.jetbrains.android.AndroidTestCase
import javax.swing.JList

class AndroidWearConfigurationEditorTest : AndroidTestCase() {
  private lateinit var runConfiguration: AndroidWatchFaceConfiguration
  private lateinit var settingsEditor: AndroidWearConfigurationEditor<AndroidWatchFaceConfiguration>

  override fun setUp() {
    super.setUp()
    val runConfigurationFactory = AndroidWatchFaceConfigurationType().configurationFactories[0]
    runConfiguration = AndroidWatchFaceConfiguration(project, runConfigurationFactory)
    settingsEditor = runConfiguration.configurationEditor as AndroidWearConfigurationEditor<AndroidWatchFaceConfiguration>
  }

  fun testComponentComboBoxDisabled() {
    val editor = settingsEditor.component as DialogPanel
    val modulesComboBox = TreeWalker(editor).descendants().filterIsInstance<ComboBox<*>>().first()
    val componentComboBox = TreeWalker(editor).descendants().filterIsInstance<ComboBox<*>>()[1] as ComboBox<String>
    var comboBoxRenderer = componentComboBox.renderer
      .getListCellRendererComponent(JList(), componentComboBox.item, -1, false, false)
      as SimpleListCellRenderer<String>

    assertThat(modulesComboBox.item).isNull()
    assertThat(componentComboBox.isEnabled).isFalse()
    assertThat(comboBoxRenderer.text).isEqualTo("Module is not chosen")

    runConfiguration.setModule(myModule)
    // To set myModule
    settingsEditor.resetFrom(runConfiguration)
    assertThat(componentComboBox.isEnabled).isFalse()
    comboBoxRenderer = componentComboBox.renderer
      .getListCellRendererComponent(JList(), componentComboBox.item, -1, false, false)
      as SimpleListCellRenderer<String>
    assertThat(comboBoxRenderer.text).isEqualTo("Watch Face not found")
  }

  fun testComponentComboBoxEnabled() {
    val watchFaceClass = myFixture.addWatchFace().qualifiedName

    runConfiguration.setModule(myModule)
    runConfiguration.componentName = watchFaceClass
    settingsEditor.resetFrom(runConfiguration)

    val componentComboBox = TreeWalker(settingsEditor.component).descendants().filterIsInstance<ComboBox<*>>()[1] as ComboBox<String>
    assertThat(componentComboBox.isEnabled).isTrue()
    assertThat(componentComboBox.item).isEqualTo(watchFaceClass)
  }
}