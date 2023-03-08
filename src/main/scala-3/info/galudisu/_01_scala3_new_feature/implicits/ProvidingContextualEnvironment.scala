package info.galudisu._01_scala3_new_feature.implicits

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

object ProvidingContextualEnvironment {

  def square(i: Int)(using ec: ExecutionContext): Future[Int] = Future(i * i)
}
