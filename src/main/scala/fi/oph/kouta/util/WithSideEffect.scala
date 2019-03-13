package fi.oph.kouta.util

trait WithSideEffect {
  /**
    * Add side effect to response. Similar to Future.andThen so it returns original value.
    *
    * @param response Response that is both passed to side effect function and returned
    * @param sideEffect Add side effect
    * @tparam A
    * @return Orignal response
    */
  def withSideEffect[A](response: A)(sideEffect: PartialFunction[A, Unit]): A = {
    sideEffect.applyOrElse(response, (_: A) => Unit )
    response
  }
}