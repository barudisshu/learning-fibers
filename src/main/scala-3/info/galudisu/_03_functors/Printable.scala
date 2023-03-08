package info.galudisu._03_functors

object Printable:

  def format[A](value: A)(using p: Printable[A]): String = p.format(value)

  def encode[A](value: A)(using c: Codec[A]): String = c.encode(value)

  def decode[A](value: String)(using c: Codec[A]): A = c.decode(value)

  trait Printable[A]:
    self =>
    def format(value: A): String

    // 逆变函子(contravariant functor)
    def contramap[B](func: B => A): Printable[B] = (value: B) => self.format(func(value))

  trait Codec[A]:
    self =>
    def encode(value: A): String

    def decode(value: String): A

    // 不变函子(invariant functor)
    def imap[B](dec: A => B, enc: B => A): Codec[B] = new Codec[B]:
      override def encode(value: B): String = self.encode(enc(value))

      override def decode(value: String): B = dec(self.decode(value))

  final case class Box[A](value: A)
