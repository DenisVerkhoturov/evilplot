package com.cibo.evilplot.plot.components

import com.cibo.evilplot.geometry._
import com.cibo.evilplot.plot.renderers.LegendRenderer
import com.cibo.evilplot.plot.{LegendContext, Plot}

case class Legend(
  position: Position,
  contexts: Seq[LegendContext],
  legendRenderer: LegendRenderer,
  x: Double,
  y: Double
) extends PlotComponent {

  private lazy val drawable: Drawable = {
    val filteredContexts = contexts.filter(_.elements.nonEmpty)
    if (filteredContexts.nonEmpty) {
      filteredContexts.map { ctx =>
        legendRenderer.render(ctx)
      }.reduce(legendRenderer.reduction)
    } else EmptyDrawable()
  }

  override def size(plot: Plot): Extent = drawable.extent

  def render(plot: Plot, extent: Extent): Drawable = {
    drawable.translate(
      x = (extent.width - drawable.extent.width) * x,
      y = (extent.height - drawable.extent.height) * y
    )
  }
}

trait LegendImplicits {
  protected val plot: Plot

  private def setLegend(
    position: Position,
    renderer: LegendRenderer,
    x: Double,
    y: Double
  ): Plot = if (plot.legendContext.nonEmpty) {
    plot :+ Legend(position, plot.legendContext, renderer, x, y)
  } else plot

  /** Place a legend on the right side of the plot. */
  def rightLegend(
    renderer: LegendRenderer = LegendRenderer.vertical()
  ): Plot = setLegend(Position.Right, renderer, 0, 0.5)

  /** Place a legend on the left side of the plot. */
  def leftLegend(
    renderer: LegendRenderer = LegendRenderer.vertical()
  ): Plot = setLegend(Position.Left, renderer, 0, 0.5)

  /** Place a legend on the top of the plot. */
  def topLegend(
    renderer: LegendRenderer = LegendRenderer.horizontal()
  ): Plot = setLegend(Position.Top, renderer, 0.5, 0)

  /** Place a legend on the bottom of the plot. */
  def bottomLegend(
    renderer: LegendRenderer = LegendRenderer.horizontal()
  ): Plot = setLegend(Position.Bottom, renderer, 0.5, 0)

  /** Overlay a legend on the plot.
    * @param x The relative X position (0 to 1).
    * @param y The relative y position (0 to 1).
    * @param renderer The legend renderer to use.
    */
  def overlayLegend(
    x: Double = 1.0,
    y: Double = 0.0,
    renderer: LegendRenderer = LegendRenderer.vertical()
  ): Plot = {
    setLegend(Position.Overlay, renderer, x, y)
  }

  /** Get the legend as a drawable. */
  def renderLegend(
    renderer: LegendRenderer = LegendRenderer.vertical()
  ): Option[Drawable] = if (plot.legendContext.nonEmpty) {
    val legend = Legend(Position.Right, plot.legendContext, renderer, 0, 0)
    Some(legend.render(plot, legend.size(plot)))
  } else None
}