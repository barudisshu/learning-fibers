package info.galudisu._01_scala3_new_feature.typesystem

import info.galudisu._01_scala3_new_feature.typesystem.types.*

import java.time.LocalDate
import scala.util.Try

final case class Movie(
                        name: String,
                        year: Year,
                        runningTime: RunningTimeInMin,
                        noOfOscarsWon: NoOfOscarsWon)

object types {

  opaque type Year = Int
  opaque type RunningTimeInMin = Int
  opaque type NoOfOscarsWon = Int
  opaque type ReleaseDate <: LocalDate = LocalDate
  opaque type NetflixReleaseDate <: ReleaseDate = ReleaseDate

  object Year {
    def apply(value: Int): Year = value

    def safe(value: Int): Option[Year] =
      if value > 1900 then Some(value) else None

    extension (year: Year) {
      def value: Int = year
    }
  }

  object RunningTimeInMin {
    def apply(value: Int): RunningTimeInMin = value

    def safe(value: Int): Option[RunningTimeInMin] =
      if value > 10 && value < 300 then Some(value) else None

    extension (time: RunningTimeInMin) {
      def value: Int = time
    }
  }

  object NoOfOscarsWon {
    def apply(value: Int): NoOfOscarsWon = value

    def safe(value: Int): Option[NoOfOscarsWon] =
      if value >= 0 then Some(value) else None

    extension (oscars: NoOfOscarsWon) def value: Int = oscars
  }

  object ReleaseDate {
    def apply(date: LocalDate): ReleaseDate = date

    def safeParse(date: String): Option[ReleaseDate] = Try(
      LocalDate.parse(date)
    ).toOption

    extension (releaseDate: ReleaseDate) {
      def toStr = releaseDate.toString()
    }
  }

  object NetflixReleaseDate {
    def apply(date: LocalDate): NetflixReleaseDate = date
  }

}
