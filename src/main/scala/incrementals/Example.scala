package incrementals

object Example:
  @main def run =

    given s: State = State.apply

    val width = s.createVar(100)
    val height = s.createVar(200)

    width.
    println("Running example")
