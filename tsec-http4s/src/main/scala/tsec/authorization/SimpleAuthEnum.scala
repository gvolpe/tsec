package tsec.authorization
import cats.Eq
import io.circe.Decoder.Result
import io.circe._
import io.circe.syntax._

/**
  * Dead simple typed enum with explicitly handled enumeration error
  * It also provides an implicit decoder/encoder for serialization into json.
  *
  * @tparam T the abstract type to enumerate, subclass style
  * @tparam Repr the representation type. i.e string, int, double, whatever.
  */
abstract class SimpleAuthEnum[T, Repr: Decoder: Encoder](implicit primtive: AuthPrimitive[Repr]) {
  implicit val E: Eq[T]
  implicit val authEnum: SimpleAuthEnum[T, Repr] = this

  val getRepr: T => Repr

  protected val values: AuthGroup[T]

  /*
  Since `Repr` does not come necessarily with a classtag,
  this is necessary, unfortunately
   */
  protected lazy val reprValues = primtive.unBoxedFromRepr[T](getRepr, values)

  val orElse: T

  def fromRepr(r: Repr): T = {
    val ix: Int = reprValues.indexOf(r)
    if (ix >= 0)
      values(ix)
    else
      orElse
  }

  @inline def contains(elem: T): Boolean = reprValues.contains(elem)

  implicit val decoder: Decoder[T] = new Decoder[T] {
    def apply(c: HCursor): Result[T] = c.as[Repr].map(fromRepr)
  }

  implicit val encoder: Encoder[T] = new Encoder[T] {
    def apply(a: T): Json = getRepr(a).asJson
  }

  implicit def subClDecoder[A <: T](implicit singleton: A): Decoder[A] =
    new Decoder[A] {
      def apply(c: HCursor): Result[A] = c.as[T].flatMap {
        case a if E.eqv(singleton, a) => Right(singleton)
        case _                        => Left(DecodingFailure("Improperly typed", Nil))
      }
    }

  implicit def subCLEncoder[A <: T](implicit singleton: T): Encoder[A] = new Encoder[A] {
    def apply(a: A): Json = encoder(a)
  }
}
