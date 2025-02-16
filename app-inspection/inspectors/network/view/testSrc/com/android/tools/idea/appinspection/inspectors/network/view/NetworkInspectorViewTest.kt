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
package com.android.tools.idea.appinspection.inspectors.network.view

import com.android.tools.adtui.RangeSelectionComponent
import com.android.tools.adtui.RangeTooltipComponent
import com.android.tools.adtui.TreeWalker
import com.android.tools.adtui.chart.linechart.LineChart
import com.android.tools.adtui.model.FakeTimer
import com.android.tools.adtui.model.Range
import com.android.tools.adtui.stdui.TooltipLayeredPane
import com.android.tools.adtui.swing.FakeKeyboard
import com.android.tools.adtui.swing.FakeUi
import com.android.tools.idea.appinspection.inspectors.network.model.FakeCodeNavigationProvider
import com.android.tools.idea.appinspection.inspectors.network.model.FakeNetworkInspectorDataSource
import com.android.tools.idea.appinspection.inspectors.network.model.NetworkInspectorModel
import com.android.tools.idea.appinspection.inspectors.network.model.TestNetworkInspectorServices
import com.android.tools.idea.appinspection.inspectors.network.model.analytics.StubNetworkInspectorTracker
import com.android.tools.idea.appinspection.inspectors.network.view.constants.DEFAULT_BACKGROUND
import com.google.common.truth.Truth.assertThat
import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.ThreeComponentsSplitter
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.SystemInfoRt
import com.intellij.testFramework.EdtRule
import com.intellij.testFramework.RunsInEdt
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import studio.network.inspection.NetworkInspectorProtocol
import java.awt.BorderLayout
import java.awt.Dimension
import java.util.concurrent.TimeUnit
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.LayoutFocusTraversalPolicy

private fun createSpeedEvent(time: Long, sent: Long, received: Long): NetworkInspectorProtocol.Event {
  return NetworkInspectorProtocol.Event.newBuilder().apply {
    timestamp = TimeUnit.SECONDS.toNanos(time)
    speedEvent = NetworkInspectorProtocol.SpeedEvent.newBuilder().apply {
      txSpeed = sent
      rxSpeed = received
    }.build()
  }.build()
}

private val VIEW_RANGE = Range(0.0, TimeUnit.SECONDS.toMicros(60).toDouble())

@RunsInEdt
class NetworkInspectorViewTest {

  private lateinit var fakeUi: FakeUi
  private lateinit var model: NetworkInspectorModel
  private lateinit var inspectorView: NetworkInspectorView
  private lateinit var disposable: Disposable
  private val timer = FakeTimer()

  @get:Rule
  val edtRule = EdtRule()

  @Before
  fun setUp() {
    disposable = Disposer.newDisposable()

    val codeNavigationProvider = FakeCodeNavigationProvider()
    val services = TestNetworkInspectorServices(codeNavigationProvider, timer)
    model = NetworkInspectorModel(services, FakeNetworkInspectorDataSource(
      speedEventList = listOf(
        createSpeedEvent(0, 0, 0),
        createSpeedEvent(10, 1, 1),
        createSpeedEvent(20, 0, 0),
        createSpeedEvent(30, 1, 1),
        createSpeedEvent(34, 1, 1),
        createSpeedEvent(38, 1, 1),
        createSpeedEvent(40, 1, 1),
        createSpeedEvent(50, 1, 1)
      )
    ))


    val parentPanel = JPanel(BorderLayout())
    parentPanel.background = DEFAULT_BACKGROUND
    val splitter = ThreeComponentsSplitter(disposable)
    splitter.focusTraversalPolicy = LayoutFocusTraversalPolicy()
    splitter.dividerWidth = 0
    splitter.setDividerMouseZoneSize(-1)
    splitter.setHonorComponentsMinimumSize(true)
    splitter.lastComponent = parentPanel
    val component = TooltipLayeredPane(splitter)
    val stagePanel = JPanel(BorderLayout())
    parentPanel.add(stagePanel, BorderLayout.CENTER)
    inspectorView = NetworkInspectorView(model, FakeUiComponentsProvider(), component, StubNetworkInspectorTracker())
    stagePanel.add(inspectorView.component)
    component.size = Dimension(1000, 200)
    fakeUi = FakeUi(component)
    model.timeline.viewRange.set(VIEW_RANGE)
  }

  @After
  fun tearDown() {
    Disposer.dispose(disposable)
  }

  @Test
  fun draggingSelectionOpensConnectionsViewAndPressingEscapeClosesIt() {
    if (SystemInfoRt.isWindows) {
      return  // b/163140665
    }
    val stageWalker = TreeWalker(inspectorView.component)
    val lineChart = stageWalker.descendants().first { it is LineChart }
    val rangeSelectionComponent = stageWalker.descendants().first { it is RangeSelectionComponent }
    val connectionsView = inspectorView.connectionsView
    val connectionsViewWalker = TreeWalker(connectionsView.component)
    assertThat(connectionsViewWalker.ancestors().all { it.isVisible }).isFalse()
    val start = fakeUi.getPosition(lineChart)
    assertThat(connectionsViewWalker.ancestors().all { it.isVisible }).isFalse()
    fakeUi.mouse.press(start.x, start.y)
    assertThat(connectionsViewWalker.ancestors().all { it.isVisible }).isFalse()
    fakeUi.mouse.dragDelta(10, 0)
    assertThat(connectionsViewWalker.ancestors().all { it.isVisible }).isFalse()
    fakeUi.mouse.release()
    assertThat(connectionsViewWalker.ancestors().all { it.isVisible }).isTrue()
    fakeUi.keyboard.setFocus(rangeSelectionComponent)
    fakeUi.keyboard.press(FakeKeyboard.Key.ESC)
    assertThat(connectionsViewWalker.ancestors().all { it.isVisible }).isFalse()
  }

  @Test
  fun dragSelectionToggleInfoPanelVisibility() {
    val treeWalker = TreeWalker(inspectorView.component)
    val infoPanel = treeWalker.descendants().first { c -> "Info" == c.name } as JComponent
    assertThat(infoPanel.isVisible).isFalse()
    val lineChart = treeWalker.descendants().first { it is LineChart }
    val microSecondToX = lineChart.size.getWidth() / (VIEW_RANGE.max - VIEW_RANGE.min)
    val start = fakeUi.getPosition(lineChart)
    fakeUi.mouse.drag(start.x, start.y, (9000000 * microSecondToX).toInt(), 0)
    assertThat(infoPanel.isVisible).isFalse()
    fakeUi.mouse.drag((start.x + 10000000 * microSecondToX).toInt(), start.y, (5000000 * microSecondToX).toInt(), 0)
    assertThat(infoPanel.isVisible).isTrue()
    fakeUi.mouse.drag((start.x + 20000000 * microSecondToX).toInt(), start.y, (5000000 * microSecondToX).toInt(), 0)
    assertThat(infoPanel.isVisible).isFalse()
    fakeUi.mouse.drag((start.x + 35000000 * microSecondToX).toInt(), start.y, (2500000 * microSecondToX).toInt(), 0)
    assertThat(infoPanel.isVisible).isTrue()
    fakeUi.mouse.drag(start.x, start.y, (40000000 * microSecondToX).toInt(), 0)
    assertThat(infoPanel.isVisible).isTrue()
  }

  @Test
  fun testTooltipComponentIsFirstChild() {
    val treeWalker = TreeWalker(inspectorView.component)
    val tooltipComponent = treeWalker.descendants().first { it is RangeTooltipComponent }
    assertThat(tooltipComponent.parent.getComponent(0)).isEqualTo(tooltipComponent)
  }
}