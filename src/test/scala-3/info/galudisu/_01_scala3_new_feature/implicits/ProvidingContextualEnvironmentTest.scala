package info.galudisu._01_scala3_new_feature.implicits

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.*

class ProvidingContextualEnvironmentTest extends AnyWordSpec, Matchers {

  import ProvidingContextualEnvironment.*

  "calling square function requiring execution context" should {
    "run without problem by providing the execution context using given keyword" in {
      given ExecutionContext = ExecutionContext.global

      Await.result(square(4), 1.seconds) shouldBe 16
    }
  }
}
