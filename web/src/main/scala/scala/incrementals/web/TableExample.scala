package incrementals.web

import org.scalajs.dom
import org.scalajs.dom.{Element, Event, EventInit, UIEvent, document, window}
import scalatags.JsDom.all.*

import scala.scalajs.js.annotation.{JSExportTopLevel, JSGlobal}
import incrementals.core.*
import Incremental.*

import scala.scalajs.js.JSON
import scala.collection.immutable
import scala.concurrent.Future
import scala.scalajs.js
import scala.util.Random
import scala.concurrent.duration.*

@main def run(): Unit =
  val (inc, nodes) = Incremental:
    for i <- 1 to 1e4.toInt yield (i, new Node.Input(Random.nextDouble() * 1e2))

  val root = document.getElementById("root")

  val table = document.getElementById("table")

  def updateAll(): Unit =
    for (_, node) <- nodes do
      node.set(node.value + (Random.nextDouble() * 2 - 1.0) * 1e-2 + 1e-3)
    inc.stabilize()

  window.setInterval(() => updateAll(), 1000)

  val elems = for (id, node) <- nodes yield (id, create(id, table, node))

def create(id: Int, table: Element, node: Node[Double]): Unit =
  val row: Element = document.createElement("tr")
  row.id = s"row-$id"

  val name = document.createElement("td")
  name.innerHTML = id.toString
  val value = document.createElement("td")
  val valueId = s"row-$id-value"
  value.id = valueId
  value.setAttribute("class", "content-visibility-auto")
  value.addEventListener[ContentVisibilityAutoStateChangeEvent]("contentvisibilityautostatechange", evt => {
    val observer: Observer[Double] = v => value.innerHTML = f"$v%.2f"
    if evt.skipped then
      node.addObserver(observer)
    else
      node.removeObserver(observer)
  }, true)

  row.appendChild(name)
  row.appendChild(value)
  table.appendChild(row)


//def createV2(idx: Int, table: Element, node: Node[Double]): Unit =
//  table.appendChild(
//    tr(
//      td(
//        idx.toString
//      ),
//      td(id := s"row-$id-value", attr("contentvisibilityautostatechange") := { () =>
//        val observer: Observer[Double] = v => value.innerHTML = f"$v%.2f"
//        if evt.skipped then
//          node.addObserver(observer)
//        else
//          node.removeObserver(observer)
//      }),
//      id := s"row-$idx")
//      .render
//  )
trait ContentVisibilityAutoStateChangeInit extends EventInit:
  def skipped: js.UndefOr[Boolean] = js.undefined

@js.native
@JSGlobal
class ContentVisibilityAutoStateChangeEvent(typeArg: String, init: js.UndefOr[ContentVisibilityAutoStateChangeInit]) extends Event(typeArg, init) {
  def skipped: Boolean = js.native
}