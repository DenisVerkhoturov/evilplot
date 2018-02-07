
/*
 * Copyright 2017 CiBO Technologies
 */
package com.cibo.evilplot.oldplot

import com.cibo.evilplot.geometry._
import com.cibo.evilplot.numeric.Bounds
import com.cibo.evilplot.oldplot.ContinuousChartDistributable.HLines
import com.cibo.evilplot.plotdefs.{BarChartDef, PlotOptions}

/**
  * A categorical bar chart. Each bar corresponds to a different category in the x-variable and is labeled by an
  * entry in label.
  * @param chartSize The size of the bounding box which the chart will occupy.
  * @param data Data object containing counts and labels of each bar.
  */
// TODO: The widthGetter / spacingGetter logic is certainly way too complicated, especially since DrawableLater
// is gone.
case class BarChart(chartSize: Extent, data: BarChartDef) extends DiscreteX {
  val options: PlotOptions = data.options
  private val numBars = data.length
  val labels: Seq[String] = data.labels
  val defaultYAxisBounds: Bounds = { // 0 must be present on a bar chart.
    val bounds = data.yBounds.get
    if (bounds.max < 0) bounds.copy(max = 0) else bounds
  }

  // Create functions to get width and spacing, depending on what is specified by caller.
  protected val (widthGetter, spacingGetter) = DiscreteChartDistributable
    .widthAndSpacingFunctions(numBars, data.barWidth, data.barSpacing)

    def plottedData(extent: Extent): Drawable = {
      val _barWidth: Double = widthGetter(extent)
      val _barSpacing: Double = spacingGetter(extent)
      val vScale: Double = extent.height / yAxisDescriptor.axisBounds.range
      val bars = data.counts.map { yValue =>
        val b = Scale(Rect(_barWidth, yValue).flipY(yAxisDescriptor.axisBounds.max), y = vScale)
        Style(b, options.barColor)
      }
      val allBars = bars.seqDistributeH(_barSpacing) padLeft _barSpacing / 2.0
      val hLines = options.hLines.map { lines =>
        HLines(extent, yAxisDescriptor, lines).drawable
      }.getOrElse(EmptyDrawable())

      allBars behind hLines
    }
}
