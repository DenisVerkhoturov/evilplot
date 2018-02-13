package com.cibo.evilplot.plot

import com.cibo.evilplot.geometry._
import com.cibo.evilplot.numeric.{Bounds, Point}
import com.cibo.evilplot.plot.components.{PlotComponent, Position}
import com.cibo.evilplot.plot.renderers.{ComponentRenderer, PlotElementRenderer, PlotRenderer}

/** A plot.
  * @param xbounds The x-axis bounds of the plot.
  * @param ybounds The y-axis bounds of the plot.
  * @param renderer The PlotRenderer used to render the plot area.
  * @param componentRenderer The ComponentRenderer used to render components (axes, labels, backgrounds, etc.).
  * @param xtransform Transformation to convert from plot X-coordinates to pixel coordinates.
  * @param ytransform Transformation to convert from plot Y-coordinates to pixel coordinates.
  * @param xfixed Set if the X bounds are fixed by an axis/grid/facet/etc. or the user.
  * @param yfixed Set if the Y bounds are fixed by an axis/grid/facet/etc. or the user.
  * @param components Plot components (axes, labels, etc.).
  * @param legendContext Contexts used to display a legend for this plot.
  */
final case class Plot private[evilplot] (
  xbounds: Bounds,
  ybounds: Bounds,
  private[plot] val renderer: PlotRenderer,
  private[plot] val componentRenderer: ComponentRenderer = ComponentRenderer.Default(),
  private[plot] val xtransform: Plot.Transformer = Plot.DefaultXTransformer(),
  private[plot] val ytransform: Plot.Transformer = Plot.DefaultYTransformer(),
  private[plot] val xfixed: Boolean = false,
  private[plot] val yfixed: Boolean = false,
  private[plot] val components: Seq[PlotComponent] = Seq.empty,
  private[plot] val legendContext: Seq[LegendContext] = Seq.empty
) {
  private[plot] def inBounds(point: Point): Boolean = xbounds.isInBounds(point.x) && ybounds.isInBounds(point.y)

  private[plot] def :+(component: PlotComponent): Plot = copy(components = components :+ component)
  private[plot] def +:(component: PlotComponent): Plot = copy(components = component +: components)

  def xbounds(newBounds: Bounds): Plot = copy(xbounds = newBounds, xfixed = true)
  def xbounds(lower: Double, upper: Double): Plot = xbounds(Bounds(lower, upper))
  def ybounds(newBounds: Bounds): Plot = copy(ybounds = newBounds, yfixed = true)
  def ybounds(lower: Double, upper: Double): Plot = ybounds(Bounds(lower, upper))

  private[plot] def updateBounds(xb: Bounds, yb: Bounds): Plot = copy(xbounds = xb, ybounds = yb)

  def setXTransform(xt: Plot.Transformer, fixed: Boolean): Plot = copy(xtransform = xt, xfixed = fixed)
  def setYTransform(yt: Plot.Transformer, fixed: Boolean): Plot = copy(ytransform = yt, yfixed = fixed)

  lazy val topComponents: Seq[PlotComponent] = components.filter(_.position == Position.Top)
  lazy val bottomComponents: Seq[PlotComponent] = components.filter(_.position == Position.Bottom)
  lazy val leftComponents: Seq[PlotComponent] = components.filter(_.position == Position.Left)
  lazy val rightComponents: Seq[PlotComponent] = components.filter(_.position == Position.Right)
  lazy val backgroundComponents: Seq[PlotComponent] = components.filter(_.position == Position.Background)
  lazy val overlayComponents: Seq[PlotComponent] = components.filter(_.position == Position.Overlay)

  // Get the offset of the plot area.
  private[plot] lazy val plotOffset: Point = {

    // y offset for sides due to the annotations at the top.
    val yoffset = topComponents.map(_.size(this).height).sum

    // x offset for top/bottom due to the annotations on the left.
    val xoffset = leftComponents.map(_.size(this).width).sum

    Point(xoffset, yoffset)
  }

  // Get the size of the actual plot area.
  // Annotations on the left/right reduce the width of the plot area and
  // annotations on the top/bottom reduce the height of the plot area.
  private[plot] def plotExtent(extent: Extent): Extent = {
    components.foldLeft(extent) { (oldExtent, annotation) =>
      val size = annotation.size(this)
      annotation.position match {
        case Position.Top        => oldExtent.copy(height = oldExtent.height - size.height)
        case Position.Bottom     => oldExtent.copy(height = oldExtent.height - size.height)
        case Position.Left       => oldExtent.copy(width = oldExtent.width - size.width)
        case Position.Right      => oldExtent.copy(width = oldExtent.width - size.width)
        case Position.Overlay    => oldExtent
        case Position.Background => oldExtent
      }
    }
  }

  def render(extent: Extent = Plot.defaultExtent): Drawable = {
    val overlays = componentRenderer.renderFront(this, extent)
    val backgrounds = componentRenderer.renderBack(this, extent)
    val pextent = plotExtent(extent)
    val renderedPlot = Resize(
      renderer.render(this, pextent),
      pextent
    ).translate(x = plotOffset.x, y = plotOffset.y)
    backgrounds behind renderedPlot behind overlays
  }
}

object Plot {
  val defaultExtent: Extent = Extent(800, 600)

  private[plot] trait Transformer {
    def apply(plot: Plot, plotExtent: Extent): Double => Double
  }

  private[plot] case class DefaultXTransformer() extends Transformer {
    def apply(plot: Plot, plotExtent: Extent): Double => Double =
      (x: Double) => (x - plot.xbounds.min) * plotExtent.width / plot.xbounds.range
  }

  private[plot] case class DefaultYTransformer() extends Transformer {
    def apply(plot: Plot, plotExtent: Extent): Double => Double =
      (y: Double) => plotExtent.height - (y - plot.ybounds.min) * plotExtent.height / plot.ybounds.range
  }

  // Add some buffer to the specified bounds.
  private[plot] def expandBounds(bounds: Bounds, buffer: Double): Bounds = {
    require(buffer >= 0.0)
    // For an empty range we update by the buffer amount.
    val amount = if (bounds.range > 0) buffer * bounds.range / 2.0 else buffer / 2.0
    Bounds(bounds.min - amount, bounds.max + amount)
  }

  // Combine the bounds for multiple plots (taking the widest).
  private[plot] def combineBounds(bounds: Seq[Bounds]): Bounds = {
    Bounds(bounds.minBy(_.min).min, bounds.maxBy(_.max).max)
  }

  // Force all plots to have the same size plot area.
  private[plot] def padPlots(
    plots: Seq[Seq[Plot]],
    extent: Extent,
    padRight: Double,
    padBottom: Double
  ): Seq[Seq[Plot]] = {

    // First we get the offsets of all subplots.  By selecting the largest
    // offset, we can pad all plots to start at the same location.
    val plotOffsets = plots.flatMap(_.map(_.plotOffset))
    val xoffset = plotOffsets.maxBy(_.x).x
    val yoffset = plotOffsets.maxBy(_.y).y

    // Update the plots with their offsets.
    val offsetPlots = plots.map { row =>
      row.map { subplot =>
        subplot.padTop(yoffset - subplot.plotOffset.y).padLeft(xoffset - subplot.plotOffset.x)
      }
    }

    // Now the subplots all start at the same place, so we need to ensure they all
    // end at the same place.  We do this by computing the maximum right and bottom
    // fill amounts and then padding.
    val plotAreas = offsetPlots.flatMap(_.map(_.plotExtent(extent)))
    val minWidth = plotAreas.minBy(_.width).width
    val minHeight = plotAreas.minBy(_.height).height
    val rowCount = offsetPlots.length
    offsetPlots.zipWithIndex.map { case (row, rowIndex) =>
      val columnCount = row.length
      row.zipWithIndex.map { case (subplot, columnIndex) =>
        val extraRight = if (columnIndex + 1 < columnCount) padRight else 0
        val extraBottom = if (rowIndex + 1 < rowCount) padBottom else 0
        val pe = subplot.plotExtent(extent)
        val fillx = pe.width - minWidth + extraRight
        val filly = pe.height - minHeight + extraBottom
        subplot.padRight(fillx).padBottom(filly)
      }
    }
  }
}
