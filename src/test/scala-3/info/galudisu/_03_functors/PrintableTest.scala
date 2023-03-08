package info.galudisu._03_functors

import info.galudisu._03_functors.Printable.{Box, Codec, Printable}
import org.scalacheck.Prop.forAll
import org.scalacheck.Properties
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class PrintableTest extends AnyWordSpec, Matchers, ScalaCheckPropertyChecks {

  "printable trait test case " should {

    given stringPrintable: Printable[String] = new Printable[String] {
      override def format(value: String): String = s"'$value'"
    }

    import scala.language.implicitConversions
    given Conversion[Boolean, String] = param => if param then "yes" else "no"

    given booleanPrintable: Printable[Boolean] = new Printable[Boolean] {
      override def format(value: Boolean): String = value
    }

    "stringPrintable" in {
      forAll((param: String) => Printable.format(param) shouldEqual s"'$param'")
    }

    "booleanPrintable" in {
      forAll((param: Boolean) => Printable.format(param) shouldBe param.convert)
    }

    given boxPrintable[A](using p: Printable[A]): Printable[Box[A]] =
      p.contramap[Box[A]](_.value)

    "boxPrintable" in {
      forAll { (a: Boolean, b: String) =>
        Printable.format(Box(b)) == b
        Printable.format(Box(a)) == a.convert
      }
    }

    given stringCodec: Codec[String] = new Codec[String] {
      override def encode(value: String): String = value

      override def decode(value: String): String = value
    }

    given intCodec: Codec[Int] = stringCodec.imap(_.toInt, _.toString)

    given booleanCodec: Codec[Boolean] =
      stringCodec.imap(_.toBoolean, _.toString)

    given doubleCodec: Codec[Double] =
      stringCodec.imap[Double](_.toDouble, _.toString)

    given boxCodec[A](using c: Codec[A]): Codec[Box[A]] =
      c.imap[Box[A]](Box(_), _.value)

    "codec" in {
      forAll { (a: Int) =>
        Printable.encode(a) == a.toString
        Printable.decode[Double]("123.4") == 123.4
        Printable.encode(Box(a)) == a.toString
        Printable.decode[Box[Double]]("123.4") == Box(123.4)
      }
    }
  }
}
