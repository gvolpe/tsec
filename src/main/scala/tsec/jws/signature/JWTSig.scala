package tsec.jws.signature

import cats.effect.Sync
import tsec.jws.{JWSJWT, JWSSerializer, JWSSignature}
import tsec.jwt.claims.JWTClaims
import tsec.signature.instance.{SigCertificate, SigPrivateKey, SigPublicKey}
import tsec.core.ByteUtils._
import tsec.jwt.algorithms.JWTSigAlgo

case class JWTSig[A](header: JWSSignedHeader[A], body: JWTClaims, signature: JWSSignature[A]) extends JWSJWT[A] {
  def toEncodedString(implicit hs: JWSSerializer[JWSSignedHeader[A]]): String =
    hs.toB64URL(header) + "." + JWTClaims.toB64URL(body) + "." + signature.body.toB64UrlString
}

object JWTSig {

  def signAndBuild[F[_]: Sync, A: JWTSigAlgo](header: JWSSignedHeader[A], body: JWTClaims, sigPrivateKey: SigPrivateKey[A])(
      implicit sigCV: JWSSigCV[F, A]
  ): F[JWTSig[A]] = sigCV.signAndBuild(header, body, sigPrivateKey)

  def signToString[F[_]: Sync, A: JWTSigAlgo](header: JWSSignedHeader[A], body: JWTClaims, sigPrivateKey: SigPrivateKey[A])(
      implicit sigCV: JWSSigCV[F, A]
  ): F[String] = sigCV.signToString(header, body, sigPrivateKey)

  def verifyK[F[_]: Sync, A: JWTSigAlgo](
      jwt: String,
      extract: JWSSignedHeader[A] => SigPublicKey[A]
  )(implicit sigCV: JWSSigCV[F, A]): F[JWTSig[A]] = sigCV.verifyK(jwt, extract)

  def verifyK[F[_]: Sync, A: JWTSigAlgo](
      jwt: String,
      pubKey: SigPublicKey[A]
  )(implicit sigCV: JWSSigCV[F, A]): F[JWTSig[A]] = sigCV.verifyK(jwt, pubKey)

  def verifyC[F[_]: Sync, A: JWTSigAlgo](
      jwt: String,
      extract: JWSSignedHeader[A] => SigCertificate[A]
  )(implicit sigCV: JWSSigCV[F, A]): F[JWTSig[A]] = sigCV.verifyC(jwt, extract)

  def verifyC[F[_]: Sync, A: JWTSigAlgo](
      jwt: String,
      cert: SigCertificate[A]
  )(implicit sigCV: JWSSigCV[F, A]): F[JWTSig[A]] = sigCV.verifyC(jwt, cert)

  def verifyKI[F[_]: Sync, A: JWTSigAlgo](
      jwt: JWTSig[A],
      extract: JWSSignedHeader[A] => SigPublicKey[A]
  )(implicit sigCV: JWSSigCV[F, A], hs: JWSSerializer[JWSSignedHeader[A]]): F[JWTSig[A]] =
    verifyK[F, A](jwt.toEncodedString, extract)

}
