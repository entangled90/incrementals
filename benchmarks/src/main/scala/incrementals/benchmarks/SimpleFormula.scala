package incrementals.benchmarks

import incrementals.core.*
import Incremental.*

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.infra.Blackhole

/**
 * Run with command like
 * benchmarks/Jmh/run -i 3 -w 1
 */
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
class SimpleFormula:
  var inc: Incremental = _

  var a: InputNode[Double] = _
  var i = 1.0
  var j = 1.0

  @Setup
  def prepare(bh: Blackhole): Unit = 
    val (inc, (a, b, c, root)) =
      Incremental:
        val a = input(2.0)
        val b = input(9.2)
        val c = input(4.3)
        val disc = (a, b, c).map3((a, b, c) => Math.sqrt(b * b - 4 * a * c))
        val root = (a, b, disc).map3((a, b, disc) => (-b + disc) / 2 * a)

        (a, b, c, root)

    this.a = a 
    this.inc = inc

    root.addObserver(i => bh.consume(i)) 



  @Benchmark
  def bench(): Unit =
    i += 0.3
    a.set(i)
    inc.stabilize()


  @Benchmark
  def numeric(bh: Blackhole): Double =
    j += 0.3
    var a = 2.0
    var b = 9.2
    var c = 4.3
    a = i
    -b + Math.sqrt(b * b - 4 * a * c)/ 2 * a
