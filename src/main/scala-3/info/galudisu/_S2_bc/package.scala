package info.galudisu

import org.bouncycastle.asn1.pkcs.{ PKCSObjectIdentifiers, PrivateKeyInfo }
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers
import org.bouncycastle.asn1.{ ASN1Sequence, DERNull }
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.crypto.params.*
import org.bouncycastle.crypto.util.PrivateKeyFactory
import org.bouncycastle.jcajce.provider.asymmetric.dsa.DSAUtil
import org.bouncycastle.jcajce.provider.asymmetric.util.{ EC5Util, ECUtil }
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.bc.BcPEMDecryptorProvider
import org.bouncycastle.openssl.{ PEMException, PEMParser }
import org.bouncycastle.pkcs.PKCSException
import org.bouncycastle.tls.crypto.impl.bc.{ BcTlsCertificate, BcTlsCrypto }
import org.bouncycastle.util.encoders.Hex
import org.bouncycastle.util.io.pem.{ PemHeader, PemObject }

import java.io.StringReader
import java.math.BigInteger
import java.security.*
import java.security.cert.{ CertificateException, X509Certificate }
import java.security.interfaces.*
import java.security.spec.{ ECParameterSpec, ECPoint, InvalidKeySpecException, X509EncodedKeySpec }
import java.util.StringTokenizer
import scala.annotation.showAsInfix
import scala.collection.mutable

package object _S2_bc {

  // Only care's abount RSA/DSA/ECDSA
  val RSA   = "RSA"
  val DSA   = "DSA"
  val ECDSA = "EC"

  Security.addProvider(org.bouncycastle.jce.provider.BouncyCastleProvider())
  val key_factories = mutable.HashMap.empty[String, KeyFactory]

  /**
   * JCE/JDK key base class that wraps a BC native private key.
   *
   * @tparam A
   *   Asymmetric key parameters type wrapped by this class.
   */
  trait KEY[+A <: AsymmetricKeyParameter](final private val algorithm: String) extends Key:
    @transient
    @inline def delegate: A

    override def getFormat: String | Null = null

    override def getEncoded: Array[Byte] = Array.emptyByteArray

    override def getAlgorithm: String = algorithm

  abstract class RSAKEY[T <: RSAKeyParameters] extends KEY[T](RSA):
    def getModulus: BigInteger = delegate.getModulus

  abstract class DSAKEY[T <: DSAKeyParameters] extends KEY[T](DSA):
    def getParams: DSAParams = new DSAParams {
      override def getP: BigInteger = delegate.getParameters.getP

      override def getQ: BigInteger = delegate.getParameters.getQ

      override def getG: BigInteger = delegate.getParameters.getG
    }

  abstract class ECKEY[T <: ECKeyParameters] extends KEY[T](ECDSA):
    def getParams: ECParameterSpec = new ECParameterSpec(
      EC5Util.convertCurve(delegate.getParameters.getCurve, delegate.getParameters.getSeed),
      new ECPoint(delegate.getParameters.getG.getXCoord.toBigInteger,
                  delegate.getParameters.getG.getYCoord.toBigInteger),
      delegate.getParameters.getN,
      delegate.getParameters.getH.intValue()
    )

  case class WrappedRSAPublicKey[T <: RSAKeyParameters](override val delegate: T) extends RSAKEY[T] with RSAPublicKey:
    override def getPublicExponent: BigInteger = delegate.getExponent

  case class WrappedRSAPrivateCrtKey[T <: RSAPrivateCrtKeyParameters](override val delegate: T)
      extends RSAKEY[T]
      with RSAPrivateCrtKey:
    override def getPublicExponent: BigInteger = delegate.getPublicExponent

    override def getPrimeP: BigInteger = delegate.getP

    override def getPrimeQ: BigInteger = delegate.getQ

    override def getPrimeExponentP: BigInteger = delegate.getDP

    override def getPrimeExponentQ: BigInteger = delegate.getDQ

    override def getCrtCoefficient: BigInteger = delegate.getQInv

    override def getPrivateExponent: BigInteger = delegate.getExponent

  case class WrappedDSAPublicKey[T <: DSAPublicKeyParameters](override val delegate: T) extends DSAKEY[T], DSAPublicKey:
    override def getY: BigInteger = delegate.getY

  case class WrappedDSAPrivateKey[T <: DSAPrivateKeyParameters](override val delegate: T)
      extends DSAKEY[T],
        DSAPrivateKey:
    override def getX: BigInteger = delegate.getX

  case class WrappedECPublicKey[T <: ECPublicKeyParameters](override val delegate: T) extends ECKEY[T], ECPublicKey:
    override def getW: ECPoint = new ECPoint(delegate.getQ.getXCoord.toBigInteger, delegate.getQ.getYCoord.toBigInteger)

  case class WrappedECPrivateKey[T <: ECPrivateKeyParameters](override val delegate: T) extends ECKEY[T], ECPrivateKey:
    override def getS: BigInteger = delegate.getD

  /**
   * Only support symmetric cryptography, or else throw exception.
   */
  extension (algorithm: String)
    def getKeyFactory: Either[Throwable, KeyFactory] =
      key_factories.synchronized {
        key_factories.get(algorithm) match {
          case Some(kf) => Right(kf)
          case None =>
            util
              .Try(KeyFactory.getInstance(algorithm, BouncyCastleProvider.PROVIDER_NAME))
              .fold(Left(_),
                    fb =>
                      key_factories.put(algorithm, fb)
                      Right(fb),
              )
        }
      }

  extension (pkInfo: SubjectPublicKeyInfo)
    def asPublicKey: Either[Throwable, PublicKey] =
      val keySpec = X509EncodedKeySpec(pkInfo.getEncoded)
      util
        .Try(pkInfo.getAlgorithm.getAlgorithm)
        .map {
          case PKCSObjectIdentifiers.rsaEncryption => Right(RSA)
          case X9ObjectIdentifiers.id_dsa          => Right(DSA)
          case X9ObjectIdentifiers.id_ecPublicKey  => Right(ECDSA)
          case aid                                 => Left(InvalidKeySpecException(s"unsupported key algorithm: $aid"))
        }
        .fold(Left(_),
              a => a.fold(Left(_), getKeyFactory(_).map(kf => key_factories.synchronized(kf.generatePublic(keySpec)))))

  extension (publicKey: PublicKey)
    def asAsymmetricKeyParameter: Either[Throwable, AsymmetricKeyParameter] =
      publicKey match {
        case publicKey: RSAPublicKey =>
          Right(RSAKeyParameters(false, publicKey.getModulus, publicKey.getPublicExponent))
        case publicKey: DSAPublicKey => Right(DSAUtil.generatePublicKeyParameter(publicKey))
        case publicKey: ECPublicKey  => Right(ECUtil.generatePublicKeyParameter(publicKey))
        case _                       => Left(InvalidKeySpecException(s"Unknown key ${publicKey.getClass.getName}"))
      }

  extension (certificate: org.bouncycastle.tls.Certificate)
    @showAsInfix
    def asX509s: Array[X509Certificate] =
      val converter = JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME)
      certificate.asAsn1s.map(X509CertificateHolder(_)).map(converter.getCertificate)
    def asAsn1s: Array[org.bouncycastle.asn1.x509.Certificate] =
      certificate
        .getCertificateList
        .map(tlsCertificate => org.bouncycastle.asn1.x509.Certificate.getInstance(tlsCertificate.getEncoded))

  extension (x509s: Array[X509Certificate])
    def asCertificate(crypto: BcTlsCrypto): org.bouncycastle.tls.Certificate =
      org.bouncycastle.tls.Certificate(x509s.map(x => BcTlsCertificate(crypto, x.getEncoded)))

  extension (asn1: org.bouncycastle.asn1.x509.Certificate)
    def asPublicKey: Either[Throwable, PublicKey] =
      asn1.getSubjectPublicKeyInfo.asPublicKey

  /**
   * CA for signature certificate
   */
  extension (x509: X509Certificate)
    def isCa: Boolean =
      if x509.getBasicConstraints != -1 then true else if x509.getKeyUsage != null then x509.getKeyUsage()(5) else false

  extension (pemObject: PemObject)
    def asUnion(pass: Option[String] = None): Either[Throwable, X509Certificate | AsymmetricKeyParameter] =
      pemObject.getType match {
        case "CERTIFICATE" => pemObject.asX509
        case _             => pemObject.asAsymmetricKeyParameter(pass)
      }
    def asX509: Either[Throwable, X509Certificate] = {
      pemObject.getType match {
        case "CERTIFICATE" =>
          val converter =
            org.bouncycastle.cert.jcajce.JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME)
          val x509CertificateHolder = X509CertificateHolder(pemObject.getContent)
          util.Try(converter.getCertificate(x509CertificateHolder)).toEither
        case _ => Left(CertificateException("invalid certificate"))
      }
    }
    def asAsymmetricKeyParameter(pass: Option[String] = None): Either[Throwable, AsymmetricKeyParameter] = {
      pemObject.getType match {
        // RSA
        case "RSA PRIVATE KEY" =>
          pass match {
            case Some(passwd) =>
              var isEncrypted             = false
              var dekInfo: Option[String] = None
              val headers                 = pemObject.getHeaders
              headers.forEach {
                case hdr: PemHeader =>
                  if hdr.getName.equals("Proc-Type") && hdr.getValue.equals("4,ENCRYPTED") then {
                    isEncrypted = true
                  }
                  else if hdr.getName.equals("DEK-Info") then {
                    dekInfo = Some(hdr.getValue)
                  }
                case dhr => Left(IllegalArgumentException(s"Unknown PemObject Header type $dhr"))
              }
              if isEncrypted then {
                dekInfo match {
                  case Some(dek) =>
                    val tknz         = StringTokenizer(dek, ",")
                    val dekAlgName   = tknz.nextToken()
                    val iv           = Hex.decode(tknz.nextToken())
                    val provider     = BcPEMDecryptorProvider(passwd.toCharArray)
                    val keyDecryptor = provider.get(dekAlgName)
                    val encoding     = keyDecryptor.decrypt(pemObject.getContent, iv)
                    val keyStruct    = org.bouncycastle.asn1.pkcs.RSAPrivateKey.getInstance(encoding)
                    val pubSpec =
                      org.bouncycastle.asn1.pkcs.RSAPublicKey(keyStruct.getModulus, keyStruct.getPublicExponent)
                    val algId = org
                      .bouncycastle
                      .asn1
                      .x509
                      .AlgorithmIdentifier(PKCSObjectIdentifiers.rsaEncryption, DERNull.INSTANCE)
                    val keyPair = org
                      .bouncycastle
                      .openssl
                      .PEMKeyPair(SubjectPublicKeyInfo(algId, pubSpec), PrivateKeyInfo(algId, keyStruct))
                    Right(PrivateKeyFactory.createKey(keyPair.getPrivateKeyInfo))
                  case _ => Left(PEMException("malformed sequence in RSA private key"))
                }
              }
              else {
                val keyStruct = org.bouncycastle.asn1.pkcs.RSAPrivateKey.getInstance(pemObject.getContent)
                val pubSpec = org.bouncycastle.asn1.pkcs.RSAPublicKey(keyStruct.getModulus, keyStruct.getPublicExponent)
                val algId =
                  org.bouncycastle.asn1.x509.AlgorithmIdentifier(PKCSObjectIdentifiers.rsaEncryption, DERNull.INSTANCE)
                val keyPair = org
                  .bouncycastle
                  .openssl
                  .PEMKeyPair(SubjectPublicKeyInfo(algId, pubSpec), PrivateKeyInfo(algId, keyStruct))
                Right(PrivateKeyFactory.createKey(keyPair.getPrivateKeyInfo))
              }
            case None =>
              val rsa = org.bouncycastle.asn1.pkcs.RSAPrivateKey.getInstance(pemObject.getContent)
              Right(
                org
                  .bouncycastle
                  .crypto
                  .params
                  .RSAPrivateCrtKeyParameters(rsa.getModulus,
                                              rsa.getPublicExponent,
                                              rsa.getPrivateExponent,
                                              rsa.getPrime1,
                                              rsa.getPrime2,
                                              rsa.getExponent1,
                                              rsa.getExponent2,
                                              rsa.getCoefficient))
          }
        // PKCS8
        case "PRIVATE KEY" => Right(PrivateKeyFactory.createKey(pemObject.getContent))
        // PKCS8 encrypted
        case "ENCRYPTED PRIVATE KEY" =>
          pass match {
            case Some(passwd) =>
              val pInfo = org
                .bouncycastle
                .pkcs
                .PKCS8EncryptedPrivateKeyInfo(
                  org.bouncycastle.asn1.pkcs.EncryptedPrivateKeyInfo.getInstance(pemObject.getContent))
              util
                .Try(
                  pInfo.decryptPrivateKeyInfo(
                    org
                      .bouncycastle
                      .openssl
                      .jcajce
                      .JceOpenSSLPKCS8DecryptorProviderBuilder()
                      .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                      .build(passwd.toCharArray)))
                .map(PrivateKeyFactory.createKey)
                .toEither
            case None => Left(PKCSException(s"Unable to parse pkcs8 private key"))
          }
        // DSA
        case "DSA PARAMETERS" =>
          val pKey      = org.bouncycastle.asn1.x509.DSAParameter.getInstance(pemObject.getContent)
          val dsaParams = DSAParameters(pKey.getP, pKey.getG, pKey.getQ)
          Right(DSAPrivateKeyParameters(pKey.getG, dsaParams))
        // ECDSA
        case "EC PRIVATE KEY" =>
          val pKey = org.bouncycastle.asn1.sec.ECPrivateKey.getInstance(pemObject.getContent)
          val algId =
            org.bouncycastle.asn1.x509.AlgorithmIdentifier(X9ObjectIdentifiers.id_ecPublicKey, pKey.getParametersObject)
          val privInfo = PrivateKeyInfo(algId, pKey)
          util.Try(PrivateKeyFactory.createKey(privInfo)).toEither
        case it => Left(InvalidKeySpecException(s"Unknown key type $it"))
      }
    }

  extension (asymmetricKeyParameter: AsymmetricKeyParameter)
    /**
     * convert to public key from asymmetricKeyParameter
     */
    def asPublicKey: Either[Throwable, PublicKey] =
      if asymmetricKeyParameter.isPrivate then
        Left(InvalidKeySpecException(s"AsymmetricKeyParameter is not a public key $asymmetricKeyParameter"))
      asymmetricKeyParameter match {
        case asymmetricKeyParameter: RSAKeyParameters       => Right(WrappedRSAPublicKey(asymmetricKeyParameter))
        case asymmetricKeyParameter: DSAPublicKeyParameters => Right(WrappedDSAPublicKey(asymmetricKeyParameter))
        case asymmetricKeyParameter: ECPublicKeyParameters  => Right(WrappedECPublicKey(asymmetricKeyParameter))
        case _ => Left(InvalidKeySpecException(s"Unsupported public key $asymmetricKeyParameter"))
      }

    /**
     * convert to private key from asymmetricKeyParameter
     */
    def asPrivateKey: Either[Throwable, PrivateKey] =
      if !asymmetricKeyParameter.isPrivate then
        Left(InvalidKeySpecException(s"AsymmetricKeyParameter is not a private key $asymmetricKeyParameter"))
      asymmetricKeyParameter match {
        case asymmetricKeyParameter: RSAPrivateCrtKeyParameters =>
          Right(WrappedRSAPrivateCrtKey(asymmetricKeyParameter))
        case asymmetricKeyParameter: DSAPrivateKeyParameters => Right(WrappedDSAPrivateKey(asymmetricKeyParameter))
        case asymmetricKeyParameter: ECPrivateKeyParameters  => Right(WrappedECPrivateKey(asymmetricKeyParameter))
        case _ => Left(InvalidKeySpecException(s"Unsupported public key $asymmetricKeyParameter"))
      }

  extension (privateKey: PrivateKey)
    def asAsymmetricKeyParameter: Either[Throwable, AsymmetricKeyParameter] =
      privateKey match {
        case privateKey: RSAPrivateCrtKey =>
          Right(
            new RSAPrivateCrtKeyParameters(
              privateKey.getModulus,
              privateKey.getPublicExponent,
              privateKey.getPrivateExponent,
              privateKey.getPrimeP,
              privateKey.getPrimeQ,
              privateKey.getPrimeExponentP,
              privateKey.getPrimeExponentQ,
              privateKey.getCrtCoefficient
            ))
        // pkcs8
        case privateKey: RSAPrivateKey =>
          Right(RSAKeyParameters(true, privateKey.getModulus, privateKey.getPrivateExponent))
        case privateKey: ECPrivateKey  => Right(ECUtil.generatePrivateKeyParameter(privateKey))
        case privateKey: DSAPrivateKey => Right(DSAUtil.generatePrivateKeyParameter(privateKey))
        case _                         => Left(InvalidKeySpecException(s"Unknown key ${privateKey.getClass.getName}"))
      }
}
