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
package com.android.tools.idea.gradle.project.upgrade

import com.android.ide.common.repository.GradleVersion
import com.android.testutils.ignore.IgnoreTestRule
import com.android.tools.adtui.HtmlLabel
import com.android.tools.adtui.TreeWalker
import com.android.tools.adtui.model.stdui.EditingErrorCategory
import com.android.tools.idea.gradle.plugin.LatestKnownPluginVersionProvider
import com.android.tools.idea.gradle.project.sync.GradleSyncInvoker
import com.android.tools.idea.gradle.project.upgrade.AgpUpgradeComponentNecessity.MANDATORY_CODEPENDENT
import com.android.tools.idea.gradle.project.upgrade.AgpUpgradeComponentNecessity.MANDATORY_INDEPENDENT
import com.android.tools.idea.gradle.project.upgrade.AgpUpgradeComponentNecessity.OPTIONAL_CODEPENDENT
import com.android.tools.idea.gradle.project.upgrade.AgpUpgradeComponentNecessity.OPTIONAL_INDEPENDENT
import com.android.tools.idea.gradle.project.upgrade.Java8DefaultRefactoringProcessor.NoLanguageLevelAction.ACCEPT_NEW_DEFAULT
import com.android.tools.idea.gradle.project.upgrade.Java8DefaultRefactoringProcessor.NoLanguageLevelAction.INSERT_OLD_DEFAULT
import com.android.tools.idea.gradle.project.upgrade.ToolWindowModel.UIState.AgpVersionNotLocatedError
import com.android.tools.idea.gradle.project.upgrade.ToolWindowModel.UIState.AllDone
import com.android.tools.idea.gradle.project.upgrade.ToolWindowModel.UIState.InvalidVersionError
import com.android.tools.idea.gradle.project.upgrade.ToolWindowModel.UIState.Loading
import com.android.tools.idea.gradle.project.upgrade.ToolWindowModel.UIState.ReadyToRun
import com.android.tools.idea.gradle.project.upgrade.ToolWindowModel.UIState.RunningSync
import com.android.tools.idea.gradle.project.upgrade.ToolWindowModel.UIState.RunningUpgrade
import com.android.tools.idea.testing.AndroidProjectRule
import com.android.tools.idea.testing.IdeComponents
import com.android.tools.idea.testing.onEdt
import com.google.common.truth.Truth.assertThat
import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiFile
import com.intellij.testFramework.RunsInEdt
import com.intellij.ui.CheckboxTree
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.components.JBLabel
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@RunsInEdt
class ContentManagerTest {
  val currentAgpVersion by lazy { GradleVersion.parse("4.1.0") }
  val latestAgpVersion by lazy { GradleVersion.parse(LatestKnownPluginVersionProvider.INSTANCE.get()) }

  @get:Rule
  val projectRule = AndroidProjectRule.withSdk().onEdt()

  @get:Rule
  val ignoreTests = IgnoreTestRule()

  val project by lazy { projectRule.project }

  private val uiStates: MutableList<ToolWindowModel.UIState> = ArrayList()

  @Before
  fun replaceSyncInvoker() {
    val ideComponents = IdeComponents(projectRule.fixture)
    ideComponents.replaceApplicationService(GradleSyncInvoker::class.java, GradleSyncInvoker.FakeInvoker())
  }

  private fun addMinimalBuildGradleToProject() : PsiFile {
    return projectRule.fixture.addFileToProject(
      "build.gradle",
      """
        buildscript {
          dependencies {
            classpath 'com.android.tools.build:gradle:$currentAgpVersion'
          }
        }
      """.trimIndent()
    )
  }

  private fun ToolWindowModel.listeningStatesChanges() = apply { uiState.addListener { uiStates.add(uiState.get()) }}

  @Test
  fun testContentManagerConstructable() {
    val contentManager = ContentManager(project)
  }

  @Test
  fun testContentManagerShowContent() {
    val contentManager = ContentManager(project)
    contentManager.showContent()
  }

  @Test
  fun testToolWindowModelConstructable() {
    val toolWindowModel = ToolWindowModel(project, { currentAgpVersion })
  }

  @Test
  fun testToolWindowModelStartsWithLatestAgpVersionSelected() {
    val toolWindowModel = ToolWindowModel(project, { currentAgpVersion })
    assertThat(toolWindowModel.selectedVersion).isEqualTo(latestAgpVersion)
  }

  @Test
  fun testToolWindowModelStartsWithValidProcessor() {
    val toolWindowModel = ToolWindowModel(project, { currentAgpVersion })
    assertThat(toolWindowModel.processor?.current).isEqualTo(currentAgpVersion)
    assertThat(toolWindowModel.processor?.new).isEqualTo(latestAgpVersion)
  }

  @Test
  fun testToolWindowModelStartsEnabledWithBuildGradle() {
    addMinimalBuildGradleToProject()
    val toolWindowModel = ToolWindowModel(project, { currentAgpVersion })
    assertThat(toolWindowModel.uiState.get()).isEqualTo(ReadyToRun)
  }

  @Test
  fun testToolWindowModelStartsDisabledWithNoFiles() {
    val toolWindowModel = ToolWindowModel(project, { currentAgpVersion })
    assertThat(toolWindowModel.uiState.get()).isEqualTo(AgpVersionNotLocatedError)
  }

  @Test
  fun testToolWindowModelStartsDisabledWithUnsupportedDependency() {
    projectRule.fixture.addFileToProject(
      "build.gradle",
      """
        buildscript {
          dependencies {
            classpath deps.ANDROID_GRADLE_PLUGIN
          }
        }
      """.trimIndent()
    )
    val toolWindowModel = ToolWindowModel(project, { currentAgpVersion })
    assertThat(toolWindowModel.uiState.get()).isEqualTo(AgpVersionNotLocatedError)
  }

  @Test
  fun testToolWindowModelUIStateOnFailedValidation() {
    val toolWindowModel = ToolWindowModel(project, { currentAgpVersion }).listeningStatesChanges()
    toolWindowModel.newVersionSet("")
    // The following order might be not obvious but in fact it is correct:
    // Firstly version parser validates new version and sets the error,
    // Then UI is cleared showing Loading state,
    // Finally refresh logic results back in InvalidVersionError state.
    assertThat(uiStates).containsExactly(
      InvalidVersionError(AllIcons.General.Error to "Invalid AGP version format."),
      Loading,
      InvalidVersionError(AllIcons.General.Error to "Invalid AGP version format.")
    ).inOrder()
  }

  @Test
  fun testTreeModelInitialState() {
    addMinimalBuildGradleToProject()
    val toolWindowModel = ToolWindowModel(project, { currentAgpVersion })
    val treeModel = toolWindowModel.treeModel
    val root = treeModel.root as? CheckedTreeNode
    assertThat(root).isInstanceOf(CheckedTreeNode::class.java)
    assertThat(root!!.childCount).isEqualTo(1)
    val mandatoryCodependent = root.firstChild as CheckedTreeNode
    assertThat(mandatoryCodependent.userObject).isEqualTo(MANDATORY_CODEPENDENT)
    assertThat(mandatoryCodependent.isEnabled).isTrue()
    assertThat(mandatoryCodependent.isChecked).isTrue()
    assertThat(mandatoryCodependent.childCount).isEqualTo(1)
    val step = mandatoryCodependent.firstChild as CheckedTreeNode
    assertThat(step.isEnabled).isFalse()
    assertThat(step.isChecked).isTrue()
    val stepPresentation = step.userObject as ToolWindowModel.DefaultStepPresentation
    assertThat(stepPresentation.processor).isInstanceOf(AgpClasspathDependencyRefactoringProcessor::class.java)
    assertThat(stepPresentation.treeText).contains("Upgrade AGP dependency from $currentAgpVersion to $latestAgpVersion")
  }

  @Test
  fun testToolWindowView() {
    val contentManager = ContentManager(project)
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Upgrade Assistant")!!
    val model = ToolWindowModel(project, { currentAgpVersion })
    val view = ContentManager.View(model, toolWindow.contentManager)
  }

  @Test
  fun testToolWindowViewHasExpandedTree() {
    addMinimalBuildGradleToProject()
    val contentManager = ContentManager(project)
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Upgrade Assistant")!!
    val model = ToolWindowModel(project, { currentAgpVersion })
    val view = ContentManager.View(model, toolWindow.contentManager)
    assertThat(treeString(view.tree)).isEqualTo("""
      Upgrade
        Upgrade AGP dependency from 4.1.0 to $latestAgpVersion
    """.trimIndent())
  }

  @Test
  fun testToolWindowViewDisablingNodeDisablesChild() {
    addMinimalBuildGradleToProject()
    val contentManager = ContentManager(project)
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Upgrade Assistant")!!
    val model = ToolWindowModel(project, { currentAgpVersion })
    val view = ContentManager.View(model, toolWindow.contentManager)
    val mandatoryCodependentNode = view.tree.getPathForRow(0).lastPathComponent as CheckedTreeNode
    assertThat(mandatoryCodependentNode.isChecked).isTrue()
    view.tree.setNodeState(mandatoryCodependentNode, false)
    assertThat(mandatoryCodependentNode.isChecked).isFalse()
    val classpathRefactoringProcessorNode = mandatoryCodependentNode.firstChild as CheckedTreeNode
    assertThat(classpathRefactoringProcessorNode.isChecked).isFalse()
    assertThat(classpathRefactoringProcessorNode.isEnabled).isFalse()
  }

  @Test
  fun testToolWindowViewMandatoryCodependentDetailsPanel() {
    addMinimalBuildGradleToProject()
    val contentManager = ContentManager(project)
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Upgrade Assistant")!!
    val model = ToolWindowModel(project, { currentAgpVersion })
    val view = ContentManager.View(model, toolWindow.contentManager)
    view.tree.selectionPath = view.tree.getPathForRow(0)
    val detailsPanelContent = TreeWalker(view.detailsPanel).descendants().first { it.name == "content" } as HtmlLabel
    assertThat(detailsPanelContent.text).contains("<b>Upgrade</b>")
    assertThat(detailsPanelContent.text).contains("at the same time")
  }

  @Test
  fun testToolWindowViewClasspathProcessorDetailsPanel() {
    addMinimalBuildGradleToProject()
    val contentManager = ContentManager(project)
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Upgrade Assistant")!!
    val model = ToolWindowModel(project, { currentAgpVersion })
    val view = ContentManager.View(model, toolWindow.contentManager)
    view.tree.selectionPath = view.tree.getPathForRow(1)
    val detailsPanelContent = TreeWalker(view.detailsPanel).descendants().first { it.name == "content" } as HtmlLabel
    assertThat(detailsPanelContent.text).contains("<b>Upgrade AGP dependency from $currentAgpVersion to $latestAgpVersion</b>")
  }

  @Test
  fun testToolWindowViewDetailsPanelWithJava8() {
    // Note that this isn't actually a well-formed build.gradle file, but is constructed to activate both the classpath
    // and the Java8 refactoring processors without needing a full project.
    projectRule.fixture.addFileToProject(
      "build.gradle",
      """
        plugins {
          id 'com.android.application'
        }
        buildscript {
          dependencies {
            classpath 'com.android.tools.build:gradle:4.1.0'
          }
        }
        android {
        }
      """.trimIndent()
    )
    val contentManager = ContentManager(project)
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Upgrade Assistant")!!
    val model = ToolWindowModel(project, { currentAgpVersion })
    val view = ContentManager.View(model, toolWindow.contentManager)
    val java8ProcessorPath = view.tree.getPathForRow(1)
    view.tree.selectionPath = java8ProcessorPath
    val stepPresentation = (java8ProcessorPath.lastPathComponent as CheckedTreeNode).userObject as ToolWindowModel.StepUiPresentation
    assertThat(stepPresentation.treeText).isEqualTo("Accept the new default of Java 8")
    val detailsPanelContent = TreeWalker(view.detailsPanel).descendants().first { it.name == "content" } as HtmlLabel
    assertThat(detailsPanelContent.text).contains("<b>Update default Java language level</b>")
    assertThat(detailsPanelContent.text).contains("explicit Language Level directives")
    val label = TreeWalker(view.detailsPanel).descendants().first { it.name == "label" } as JBLabel
    val comboBox = TreeWalker(view.detailsPanel).descendants().first { it.name == "selection" } as ComboBox<*>
    assertThat(label.text).contains("Action on no explicit Java language level")
    assertThat(comboBox.selectedItem).isEqualTo(ACCEPT_NEW_DEFAULT)
    comboBox.selectedItem = INSERT_OLD_DEFAULT
    assertThat(stepPresentation.treeText).isEqualTo("Insert directives to continue using Java 7")
  }

  @Test
  fun testToolWindowViewDetailsPanelWithOldKotlinPlugin() {
    projectRule.fixture.addFileToProject(
      "build.gradle",
      """
        buildscript {
          dependencies {
            classpath 'com.android.tools.build:gradle:4.1.0'
            classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.11'
          }
        }
      """.trimIndent()
    )
    val contentManager = ContentManager(project)
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Upgrade Assistant")!!
    val model = ToolWindowModel(project, { currentAgpVersion })
    val view = ContentManager.View(model, toolWindow.contentManager)
    val gradleVersionProcessorPath = view.tree.getPathForRow(1)
    view.tree.selectionPath = gradleVersionProcessorPath
    val stepPresentation = (gradleVersionProcessorPath.lastPathComponent as CheckedTreeNode)
      .userObject as ToolWindowModel.StepUiPresentation
    assertThat(stepPresentation.treeText).contains("Upgrade Gradle plugins")
    val detailsPanelContent = TreeWalker(view.detailsPanel).descendants().first { it.name == "content" } as HtmlLabel
    assertThat(detailsPanelContent.text).contains("<b>Upgrade Gradle plugins")
    assertThat(detailsPanelContent.text).contains("The following Gradle plugin versions will be updated:")
    assertThat(detailsPanelContent.text).contains("Update version of org.jetbrains.kotlin:kotlin-gradle-plugin to")
  }

  @Test
  fun testToolWindowViewDetailsPanelWithNewishKotlinPlugin() {
    projectRule.fixture.addFileToProject(
      "build.gradle",
      """
        buildscript {
          dependencies {
            classpath 'com.android.tools.build:gradle:4.1.0'
            classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.20'
          }
        }
      """.trimIndent()
    )
    val contentManager = ContentManager(project)
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Upgrade Assistant")!!
    val model = ToolWindowModel(project, { currentAgpVersion })
    val view = ContentManager.View(model, toolWindow.contentManager)
    val gradleVersionProcessorPath = view.tree.getPathForRow(1)
    view.tree.selectionPath = gradleVersionProcessorPath
    val stepPresentation = (gradleVersionProcessorPath.lastPathComponent as CheckedTreeNode)
      .userObject as ToolWindowModel.StepUiPresentation
    assertThat(stepPresentation.treeText).doesNotContain("Upgrade Gradle plugins")
  }

  @Test
  fun testToolWindowViewWithGradleAndPluginUpgrades() {
    projectRule.fixture.addFileToProject(
      "build.gradle",
      """
        buildscript {
          dependencies {
            classpath 'com.android.tools.build:gradle:3.6.0'
            classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.10'
          }
        }
      """.trimIndent()
    )
    projectRule.fixture.addFileToProject(
      "gradle/wrapper/gradle-wrapper.properties",
      """
        distributionBase=GRADLE_USER_HOME
        distributionPath=wrapper/dists
        zipStoreBase=GRADLE_USER_HOME
        zipStorePath=wrapper/dists
        distributionUrl=https\://services.gradle.org/distributions/gradle-6.1.1-bin.zip
      """.trimIndent()
    )
    val contentManager = ContentManager(project)
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Upgrade Assistant")!!
    val model = ToolWindowModel(project, { currentAgpVersion })
    val view = ContentManager.View(model, toolWindow.contentManager)
    assertThat(treeString(view.tree)).isEqualTo("""
      Upgrade
        Upgrade Gradle version to ${CompatibleGradleVersion.getCompatibleGradleVersion(latestAgpVersion).version}
        Upgrade Gradle plugins
        Upgrade AGP dependency from 4.1.0 to $latestAgpVersion
    """.trimIndent())
  }

  @Test
  fun testToolWindowViewHasEnabledButtons() {
    addMinimalBuildGradleToProject()
    val contentManager = ContentManager(project)
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Upgrade Assistant")!!
    val model = ToolWindowModel(project, { currentAgpVersion })
    val view = ContentManager.View(model, toolWindow.contentManager)
    assertThat(view.okButton.isEnabled).isTrue()
    assertThat(view.okButton.text).isEqualTo("Run selected steps")
    assertThat(view.previewButton.isEnabled).isTrue()
    assertThat(view.previewButton.text).isEqualTo("Show Usages")
    assertThat(view.refreshButton.isEnabled).isTrue()
    assertThat(view.refreshButton.text).isEqualTo("Refresh")
  }

  @Test
  fun testToolWindowOKButtonsAreDisabledWithNoFiles() {
    val contentManager = ContentManager(project)
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Upgrade Assistant")!!
    val model = ToolWindowModel(project, { currentAgpVersion })
    val view = ContentManager.View(model, toolWindow.contentManager)
    assertThat(view.okButton.isEnabled).isFalse()
    assertThat(view.okButton.text).isEqualTo("Run selected steps")
    assertThat(view.previewButton.isEnabled).isFalse()
    assertThat(view.previewButton.text).isEqualTo("Show Usages")
    assertThat(view.refreshButton.isEnabled).isTrue()
    assertThat(view.refreshButton.text).isEqualTo("Refresh")
  }

  @Test
  fun testToolWindowDropdownInitializedWithCurrentAndLatest() {
    val contentManager = ContentManager(project)
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Upgrade Assistant")!!
    val model = ToolWindowModel(project, { currentAgpVersion }) { setOf<GradleVersion>() }
    val view = ContentManager.View(model, toolWindow.contentManager)
    assertThat(view.versionTextField.model.selectedItem).isEqualTo(latestAgpVersion)
    assertThat(view.versionTextField.model.size).isEqualTo(2)
    assertThat(view.versionTextField.model.getElementAt(0)).isEqualTo(latestAgpVersion)
    assertThat(view.versionTextField.model.getElementAt(1)).isEqualTo(currentAgpVersion)
  }

  @Test
  fun testRunProcessor() {
    val psiFile = addMinimalBuildGradleToProject()
    var changingCurrentAgpVersion = currentAgpVersion
    val toolWindowModel = ToolWindowModel(project, { changingCurrentAgpVersion }).listeningStatesChanges()

    toolWindowModel.runUpgrade(false)
    toolWindowModel.syncStarted(project)
    changingCurrentAgpVersion = latestAgpVersion
    toolWindowModel.syncSucceeded(project)
    assertThat(psiFile.text).contains("classpath 'com.android.tools.build:gradle:$latestAgpVersion")
    assertThat(uiStates).containsExactly(RunningUpgrade, RunningSync, Loading, AllDone).inOrder()
  }

  @Test
  fun testNecessityTreeText() {
    assertThat(MANDATORY_INDEPENDENT.treeText()).isEqualTo("Upgrade prerequisites")
    assertThat(MANDATORY_CODEPENDENT.treeText()).isEqualTo("Upgrade")
    assertThat(OPTIONAL_CODEPENDENT.treeText()).isEqualTo("Recommended post-upgrade steps")
    assertThat(OPTIONAL_INDEPENDENT.treeText()).isEqualTo("Recommended steps")
  }

  @Test
  fun testNecessityDescription() {
    fun AgpUpgradeComponentNecessity.descriptionString() : String = this.description().replace("\n", " ")
    assertThat(MANDATORY_INDEPENDENT.descriptionString()).contains("are required")
    assertThat(MANDATORY_INDEPENDENT.descriptionString()).contains("separate steps")
    assertThat(MANDATORY_CODEPENDENT.descriptionString()).contains("are required")
    assertThat(MANDATORY_CODEPENDENT.descriptionString()).contains("must all happen together")
    assertThat(OPTIONAL_CODEPENDENT.descriptionString()).contains("are not required")
    assertThat(OPTIONAL_CODEPENDENT.descriptionString()).contains("only if")
    assertThat(OPTIONAL_INDEPENDENT.descriptionString()).contains("are not required")
    assertThat(OPTIONAL_INDEPENDENT.descriptionString()).contains("with or without")
  }

  @Test
  fun testUpgradeLabelText() {
    assertThat((null as GradleVersion?).upgradeLabelText()).contains("unknown version")
    assertThat(GradleVersion.parse("4.1.0").upgradeLabelText()).contains("version 4.1.0")
  }

  @Test
  fun testContentDisplayName() {
    assertThat((null as GradleVersion?).contentDisplayName()).contains("unknown AGP")
    assertThat(GradleVersion.parse("4.1.0").contentDisplayName()).contains("AGP 4.1.0")
  }

  @Test
  fun testSuggestedVersions() {
    val toolWindowModel = ToolWindowModel(project, { currentAgpVersion })
    val knownVersions = listOf("4.1.0", "20000.1.0").map { GradleVersion.parse(it) }.toSet()
    val suggestedVersions = toolWindowModel.suggestedVersionsList(knownVersions)
    assertThat(suggestedVersions).isEqualTo(listOf(latestAgpVersion, currentAgpVersion))
  }

  @Test
  fun testSuggestedVersionsLatestExplicitlyKnown() {
    val toolWindowModel = ToolWindowModel(project, { currentAgpVersion })
    val knownVersions = listOf("4.1.0", "20000.1.0").map { GradleVersion.parse(it) }.toSet().union(setOf(latestAgpVersion))
    val suggestedVersions = toolWindowModel.suggestedVersionsList(knownVersions)
    assertThat(suggestedVersions).isEqualTo(listOf(latestAgpVersion, currentAgpVersion))
  }

  @Test
  fun testSuggestedVersionsAlreadyAtLatestVersionExplicitlyKnown() {
    val toolWindowModel = ToolWindowModel(project, { latestAgpVersion })
    val knownVersions = listOf("4.1.0", "20000.1.0").map { GradleVersion.parse(it) }.toSet().union(setOf(latestAgpVersion))
    val suggestedVersions = toolWindowModel.suggestedVersionsList(knownVersions)
    assertThat(suggestedVersions).isEqualTo(listOf(latestAgpVersion))
  }

  @Test
  fun testSuggestedVersionsAlreadyAtLatestVersionExplicitlyUnknown() {
    val toolWindowModel = ToolWindowModel(project, { latestAgpVersion })
    val knownVersions = listOf("4.1.0", "20000.1.0").map { GradleVersion.parse(it) }.toSet()
    val suggestedVersions = toolWindowModel.suggestedVersionsList(knownVersions)
    assertThat(suggestedVersions).isEqualTo(listOf(latestAgpVersion))
  }

  @Test
  fun testSuggestedVersionsEmptyWhenCurrentUnknown() {
    val toolWindowModel = ToolWindowModel(project, { null })
    val knownVersions = listOf("4.1.0", "20000.1.0").map { GradleVersion.parse(it) }.toSet().union(setOf(latestAgpVersion))
    val suggestedVersions = toolWindowModel.suggestedVersionsList(knownVersions)
    assertThat(suggestedVersions).isEqualTo(listOf<GradleVersion>())
  }

  @Test
  fun testSuggestedVersionsDoesNotIncludeForcedUpgrades() {
    val toolWindowModel = ToolWindowModel(project, { currentAgpVersion })
    val knownVersions = listOf("4.1.0", "4.2.0-dev", "4.2.0").map { GradleVersion.parse(it) }.toSet()
    val suggestedVersions  = toolWindowModel.suggestedVersionsList(knownVersions)
    assertThat(suggestedVersions)
      .isEqualTo(setOf(latestAgpVersion, GradleVersion.parse("4.2.0"), currentAgpVersion).toList().sortedDescending())
  }

  @Test
  fun testAgpVersionEditingValidation() {
    val contentManager = ContentManager(project)
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Upgrade Assistant")!!
    val model = ToolWindowModel(project, { currentAgpVersion })
    assertThat(model.editingValidation("").first).isEqualTo(EditingErrorCategory.ERROR)
    assertThat(model.editingValidation("").second).isEqualTo("Invalid AGP version format.")
    assertThat(model.editingValidation("2.0.0").first).isEqualTo(EditingErrorCategory.ERROR)
    assertThat(model.editingValidation("2.0.0").second).isEqualTo("Cannot downgrade AGP version.")
    assertThat(model.editingValidation(currentAgpVersion.toString()).first).isEqualTo(EditingErrorCategory.NONE)
    assertThat(model.editingValidation(latestAgpVersion.toString()).first).isEqualTo(EditingErrorCategory.NONE)
    latestAgpVersion.run {
      val newMajorVersion = GradleVersion(major+1, minor, micro)
      assertThat(model.editingValidation(newMajorVersion.toString()).first).isEqualTo(EditingErrorCategory.ERROR)
      assertThat(model.editingValidation(newMajorVersion.toString()).second).isEqualTo("Target AGP version is unsupported.")
    }
    latestAgpVersion.run {
      val newMinorVersion = GradleVersion(major, minor+1, micro)
      assertThat(model.editingValidation(newMinorVersion.toString()).first).isEqualTo(EditingErrorCategory.WARNING)
      assertThat(model.editingValidation(newMinorVersion.toString()).second).isEqualTo("Upgrade to target AGP version is unverified.")
    }
    latestAgpVersion.run {
      val newPointVersion = GradleVersion(major, minor, micro+1)
      assertThat(model.editingValidation(newPointVersion.toString()).first).isEqualTo(EditingErrorCategory.WARNING)
      assertThat(model.editingValidation(newPointVersion.toString()).second).isEqualTo("Upgrade to target AGP version is unverified.")
    }
  }

  fun treeString(tree: CheckboxTree): String {
    fun addRowText(n: Int, sb: StringBuilder) {
      val path = tree.getPathForRow(n)
      val userObject = (path.lastPathComponent as CheckedTreeNode).userObject
      sb.append("  ".repeat(path.pathCount - 2))
      when(userObject) {
        is ToolWindowModel.DefaultStepPresentation -> sb.append(userObject.treeText)
        is AgpUpgradeComponentNecessity -> sb.append(userObject.treeText())
        else -> sb.append("Unknown entry in tree: $userObject")
      }
    }
    val sb = StringBuilder()
    for (i in 0 until tree.rowCount) {
      addRowText(i, sb)
      if (i < tree.rowCount - 1) sb.append("\n")
    }
    return sb.toString()
  }
}