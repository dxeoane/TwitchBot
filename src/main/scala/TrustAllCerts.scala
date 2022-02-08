import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

// for debugging purposes only
class TrustAllCerts extends X509TrustManager {
  def checkClientTrusted(cert: Array[X509Certificate], authType: String): Unit = {}
  def checkServerTrusted(cert: Array[X509Certificate], authType: String): Unit = {}
  def getAcceptedIssuers: Array[X509Certificate] = Array[X509Certificate]()
}
