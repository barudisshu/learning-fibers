package info.galudisu._01_scala3_new_feature.typesystem

object CompoundTypes {

  /**
   * 并集
   */
  object Union {

    def parse(input: String): Int | String =
      try input.toInt
      catch case _ => "Not a number"

  }

  /**
   * 交集
   */
  object Intersection {
    type Resource = Show & Closable

    def shutdown(resource: Resource) =
      println(s"Closing resource ${resource.show}")
      resource.close()

    trait Show:
      def show: String

    trait Closable:
      def close(): Unit

    object res extends Show, Closable:
      override def show: String = "resource#1"

      override def close(): Unit = println("Resource closed!")
  }

}
