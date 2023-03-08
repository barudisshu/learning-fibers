package info.galudisu._09_free_monad_and_coroutines

// here is an example, also see https://typelevel.org/cats/datatypes/freemonad.html

object Monadic {

  // Free your ADT
  type UserInteractionDsl[A] = Free[UserInteraction, A]
  type Id[A] = A

  given monadId: Monad[Id] = new Monad[Id] {
    override def ap[A, B](ff: Id[A => B])(fa: Id[A]): Id[B] = ff(fa)

    override def pure[A](value: A): Id[A] = value

    override def flatMap[A, B](fa: Id[A])(func: A => Id[B]): Id[B] = func(fa)
  }

  def main(args: Array[String]): Unit = {
    def program: Free[UserInteraction, Unit] =
      for {
        _ <- tell("Hello!")
        name <- ask("What is your name?")
        _ <- tell(s"Hi, $name")
      } yield ()

    val evaled: Unit = program.foldMap[Id]
  }

  def tell(statement: String): UserInteractionDsl[Unit] =
    Free.liftM(Tell(statement))

  // "Smart" constructor

  def ask(question: String): UserInteractionDsl[String] =
    Free.liftM(Ask(question))

  sealed trait UserInteraction[A]

  // An effect that takes a String and returns Unit
  case class Tell(statement: String) extends UserInteraction[Unit]

  // An effect that takes a String and returns a String
  case class Ask(question: String) extends UserInteraction[String]

  // Execution can be impure, side effects are OK here
  given(UserInteraction ~> Id) with
    override def apply[A](ui: UserInteraction[A]): Id[A] = ui match {
      case Tell(statement) => println(statement)
      case Ask(question) =>
        // here come side effect
        println(question)
        val answer = scala.io.StdIn.readLine()
        answer.asInstanceOf[A]
    }
}
