package incrementals
import Stabilization.Tick

class Var[A] private[incrementals] (
    var watch: Node[A],
    private[incrementals] var latestValue: A,
    private[incrementals] var value: A,
    private[incrementals] var valueSetBeforeStabiliation: Option[A],
    private[incrementals] var setAt: Stabilization.Tick
) {
  def set(a: A): Unit =
    setAt = watch.state.inc().tick
    

}
