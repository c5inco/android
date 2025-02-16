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
package com.android.tools.idea.appinspection.inspectors.network.model

import com.android.tools.adtui.model.AspectModel
import com.android.tools.adtui.model.Range
import com.android.tools.adtui.model.RangeSelectionModel
import com.android.tools.adtui.model.StreamingTimeline
import com.android.tools.adtui.model.TooltipModel
import com.android.tools.adtui.model.axis.ClampedAxisComponentModel
import com.android.tools.adtui.model.formatter.BaseAxisFormatter
import com.android.tools.adtui.model.formatter.NetworkTrafficFormatter
import com.android.tools.idea.appinspection.inspectors.network.model.httpdata.HttpData
import com.android.tools.idea.appinspection.inspectors.network.model.httpdata.HttpDataModel
import com.android.tools.idea.appinspection.inspectors.network.model.httpdata.HttpDataModelImpl
import com.android.tools.idea.appinspection.inspectors.network.model.httpdata.SelectionRangeDataFetcher
import com.android.tools.inspectors.common.api.stacktrace.StackTraceModel
import kotlinx.coroutines.asExecutor

private val TRAFFIC_AXIS_FORMATTER: BaseAxisFormatter = NetworkTrafficFormatter(1, 5, 5)

/**
 * The model class for [NetworkInspectorView].
 */
class NetworkInspectorModel(
  services: NetworkInspectorServices,
  dataSource: NetworkInspectorDataSource,
  val connectionsModel: HttpDataModel = HttpDataModelImpl(dataSource),
  startTimeStampNs: Long = 0
) : AspectModel<NetworkInspectorAspect>() {

  val name = "NETWORK"

  // If null, means no connection to show in the details pane.
  var selectedConnection: HttpData? = null
    private set

  val aspect = AspectModel<NetworkInspectorAspect>()
  val timeline = StreamingTimeline(services.updater)
  val networkUsage = NetworkSpeedLineChartModel(timeline, dataSource, services.workerDispatcher.asExecutor())
  val legends = LegendsModel(networkUsage, timeline.dataRange, false)
  val tooltipLegends = LegendsModel(networkUsage, timeline.tooltipRange, true)
  val trafficAxis = ClampedAxisComponentModel.Builder(networkUsage.trafficRange, TRAFFIC_AXIS_FORMATTER).build()
  val stackTraceModel = StackTraceModel(services.navigationProvider.codeNavigator)
  val rangeSelectionModel = RangeSelectionModel(timeline.selectionRange, timeline.viewRange)
  val selectionRangeDataFetcher = SelectionRangeDataFetcher(connectionsModel, timeline.selectionRange)

  var tooltip: TooltipModel? = null
    set(value) {
      if (value != null && field != null && value.javaClass == field!!.javaClass) {
        return
      }
      field?.dispose()
      field = value
      changed(NetworkInspectorAspect.TOOLTIP)
    }

  init {
    timeline.selectionRange.addDependency(this).onChange(Range.Aspect.RANGE) {
      if (!timeline.selectionRange.isEmpty) {
        timeline.isStreaming = false
      }
    }
    timeline.reset(startTimeStampNs, startTimeStampNs)

    services.updater.register(networkUsage)
    services.updater.register(trafficAxis)
  }

  /**
   * Sets the active connection, or clears the previously selected active connection if given data is null.
   */
  fun setSelectedConnection(data: HttpData?): Boolean {
    if (selectedConnection == data) {
      return false
    }
    selectedConnection = data
    aspect.changed(NetworkInspectorAspect.SELECTED_CONNECTION)
    return true
  }
}