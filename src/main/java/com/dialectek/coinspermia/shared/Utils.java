// For conditions of distribution and use, see copyright notice in Readme.

package com.dialectek.coinspermia.shared;

import java.net.*;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Enumeration;
import java.util.logging.Logger;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import com.dialectek.coinspermia.shared.Parameters;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * Utilities.
 */

public class Utils implements Parameters
{
   // Logging.
   private static Logger logger = Logger.getLogger(Utils.class .getName());

   // Get local address.
   public static String getLocalAddress() throws Exception
   {
      return(getLocalHostLANAddress().getHostAddress());
   }


   /**
    * Returns an <code>InetAddress</code> object encapsulating what is most likely the machine's LAN IP address.
    * <p/>
    * This method is intended for use as a replacement of JDK method <code>InetAddress.getLocalHost</code>, because
    * that method is ambiguous on Linux systems. Linux systems enumerate the loopback network interface the same
    * way as regular LAN network interfaces, but the JDK <code>InetAddress.getLocalHost</code> method does not
    * specify the algorithm used to select the address returned under such circumstances, and will often return the
    * loopback address, which is not valid for network communication. Details
    * <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4665037">here</a>.
    * <p/>
    * This method will scan all IP addresses on all network interfaces on the host machine to determine the IP address
    * most likely to be the machine's LAN address. If the machine has multiple IP addresses, this method will prefer
    * a site-local IP address (e.g. 192.168.x.x or 10.10.x.x, usually IPv4) if the machine has one (and will return the
    * first site-local address if the machine has more than one), but if the machine does not hold a site-local
    * address, this method will return simply the first non-loopback address found (IPv4 or IPv6).
    * <p/>
    * If this method cannot find a non-loopback address using this selection algorithm, it will fall back to
    * calling and returning the result of JDK method <code>InetAddress.getLocalHost</code>.
    * <p/>
    *
    * @throws UnknownHostException If the LAN address of the machine cannot be found.
    * Source: https://issues.apache.org/jira/browse/JCS-40
    */
   public static InetAddress getLocalHostLANAddress() throws UnknownHostException
   {
      try
      {
         InetAddress candidateAddress = null;
         // Iterate all NICs (network interface cards)...
         for (Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements(); )
         {
            NetworkInterface iface = (NetworkInterface)ifaces.nextElement();
            // Iterate all IP addresses assigned to each card...
            for (Enumeration<InetAddress> inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements(); )
            {
               InetAddress inetAddr = (InetAddress)inetAddrs.nextElement();
               if (!inetAddr.isLoopbackAddress())
               {
                  if (inetAddr.isSiteLocalAddress())
                  {
                     // Found non-loopback site-local address. Return it immediately...
                     return(inetAddr);
                  }
                  else if (candidateAddress == null)
                  {
                     // Found non-loopback address, but not necessarily site-local.
                     // Store it as a candidate to be returned if site-local address is not subsequently found...
                     candidateAddress = inetAddr;
                     // Note that we don't repeatedly assign non-loopback non-site-local addresses as candidates,
                     // only the first. For subsequent iterations, candidate will be non-null.
                  }
               }
            }
         }
         if (candidateAddress != null)
         {
            // We did not find a site-local address, but we found some other non-loopback address.
            // Server might have a non-site-local address assigned to its NIC (or it might be running
            // IPv6 which deprecates the "site-local" concept).
            // Return this non-loopback candidate address...
            return(candidateAddress);
         }
         // At this point, we did not find a non-loopback address.
         // Fall back to returning whatever InetAddress.getLocalHost() returns...
         InetAddress jdkSuppliedAddress = InetAddress.getLocalHost();
         if (jdkSuppliedAddress == null)
         {
            throw new UnknownHostException("The JDK InetAddress.getLocalHost() method unexpectedly returned null.");
         }
         return(jdkSuppliedAddress);
      }
      catch (Exception e)
      {
         UnknownHostException unknownHostException = new UnknownHostException("Failed to determine LAN address: " + e);
         unknownHostException.initCause(e);
         throw unknownHostException;
      }
   }


   // Get remote address.
   public static String getRemoteAddress(Socket socket)
   {
      InetAddress inetAddress = socket.getInetAddress();

      return(inetAddress.getHostAddress());
   }


   // Sign message.
   public static byte[] signMessage(PrivateKey privKey, byte[] message)
   {
      Signature sig = null;

      try
      {
         sig = Signature.getInstance("SHA256withRSA");
      }
      catch (NoSuchAlgorithmException e)
      {
         logger.severe(e.getMessage());
      }
      try
      {
         sig.initSign(privKey);
      }
      catch (InvalidKeyException e)
      {
         logger.severe(e.getMessage());
      }
      try
      {
         sig.update(message);
         return(sig.sign());
      }
      catch (SignatureException e)
      {
         logger.severe(e.getMessage());
      }
      return(null);
   }


   // Verify signature.
   public static boolean verifySignature(PublicKey pubKey, byte[] message, byte[] signature)
   {
      Signature sig = null;

      try
      {
         sig = Signature.getInstance("SHA256withRSA");
      }
      catch (NoSuchAlgorithmException e)
      {
         logger.severe(e.getMessage());
      }
      try
      {
         sig.initVerify(pubKey);
      }
      catch (InvalidKeyException e)
      {
         logger.severe(e.getMessage());
      }
      try
      {
         sig.update(message);
         return(sig.verify(signature));
      }
      catch (SignatureException e)
      {
         logger.severe(e.getMessage());
      }
      return(false);
   }


   public static PrivateKey stringToPrivateKey(String key64) throws GeneralSecurityException
   {
      byte[] clear = Base64.getDecoder().decode(key64);
      PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(clear);
      KeyFactory          fact    = KeyFactory.getInstance("RSA");
      PrivateKey          priv    = fact.generatePrivate(keySpec);
      Arrays.fill(clear, (byte)0);
      return(priv);
   }


   public static PublicKey stringToPublicKey(String stored) throws GeneralSecurityException
   {
      byte[] data = Base64.getDecoder().decode(stored);
      X509EncodedKeySpec spec = new X509EncodedKeySpec(data);
      KeyFactory         fact = KeyFactory.getInstance("RSA");
      return(fact.generatePublic(spec));
   }


   public static String stringFromPrivateKey(PrivateKey priv) throws GeneralSecurityException
   {
      KeyFactory          fact = KeyFactory.getInstance("RSA");
      PKCS8EncodedKeySpec spec = fact.getKeySpec(priv, PKCS8EncodedKeySpec.class );

      byte[] packed = spec.getEncoded();
      String key64 = Base64.getEncoder().encodeToString(packed);
      Arrays.fill(packed, (byte)0);
      return(key64);
   }


   public static String stringFromPublicKey(PublicKey publ) throws GeneralSecurityException
   {
      KeyFactory         fact = KeyFactory.getInstance("RSA");
      X509EncodedKeySpec spec = fact.getKeySpec(publ, X509EncodedKeySpec.class );

      return(Base64.getEncoder().encodeToString(spec.getEncoded()));
   }


   // Integer to bytes.
   public static byte[] intToBytes(int i)
   {
      return(BigInteger.valueOf(i).toByteArray());
   }


   // Hash public key.
   public static int hashPublicKey(PublicKey key)
   {
      try
      {
         return(stringFromPublicKey(key).hashCode());
      }
      catch (GeneralSecurityException e)
      {
         logger.severe(e.getMessage());
      }
      return(-1);
   }


   // Password.
   public static void testPassword() throws NoSuchAlgorithmException, InvalidKeySpecException
   {
      String originalPassword             = "password";
      String generatedSecuredPasswordHash = generateStrongPasswordHash(originalPassword);

      System.out.println(generatedSecuredPasswordHash);

      boolean matched = validatePassword("password", generatedSecuredPasswordHash);
      System.out.println(matched);

      matched = validatePassword("password1", generatedSecuredPasswordHash);
      System.out.println(matched);
   }


   public static String generateStrongPasswordHash(String password) throws NoSuchAlgorithmException, InvalidKeySpecException
   {
      int iterations = 1000;

      char[] chars = password.toCharArray();
      byte[] salt  = getSalt();

      PBEKeySpec       spec = new PBEKeySpec(chars, salt, iterations, 64 * 8);
      SecretKeyFactory skf  = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
      byte[] hash = skf.generateSecret(spec).getEncoded();
      return(iterations + ":" + toHex(salt) + ":" + toHex(hash));
   }


   private static byte[] getSalt() throws NoSuchAlgorithmException
   {
      SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");

      byte[] salt = new byte[16];
      sr.nextBytes(salt);
      return(salt);
   }


   private static String toHex(byte[] array) throws NoSuchAlgorithmException
   {
      BigInteger bi            = new BigInteger(1, array);
      String     hex           = bi.toString(16);
      int        paddingLength = (array.length * 2) - hex.length();

      if (paddingLength > 0)
      {
         return(String.format("%0" + paddingLength + "d", 0) + hex);
      }
      else
      {
         return(hex);
      }
   }


   public static boolean validatePassword(String originalPassword, String storedPassword) throws NoSuchAlgorithmException, InvalidKeySpecException
   {
      String[] parts = storedPassword.split(":");
      int iterations = Integer.parseInt(parts[0]);
      byte[] salt = fromHex(parts[1]);
      byte[] hash = fromHex(parts[2]);

      PBEKeySpec       spec = new PBEKeySpec(originalPassword.toCharArray(), salt, iterations, hash.length * 8);
      SecretKeyFactory skf  = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
      byte[] testHash = skf.generateSecret(spec).getEncoded();

      int diff = hash.length ^ testHash.length;
      for (int i = 0; i < hash.length && i < testHash.length; i++)
      {
         diff |= hash[i] ^ testHash[i];
      }
      return(diff == 0);
   }


   private static byte[] fromHex(String hex) throws NoSuchAlgorithmException
   {
      byte[] bytes = new byte[hex.length() / 2];
      for (int i = 0; i < bytes.length; i++)
      {
         bytes[i] = (byte)Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
      }
      return(bytes);
   }


   public static BigInteger binomial(final int N, final int K)
   {
      BigInteger ret = BigInteger.ONE;

      for (int k = 0; k < K; k++)
      {
         ret = ret.multiply(BigInteger.valueOf(N - k))
                  .divide(BigInteger.valueOf(k + 1));
      }
      return(ret);
   }


   // Probability of intersecting transactions.
   // N=number of nodes, K=number of peers of node K, S=number of nodes of peer S.
   public static float intersectionProbability(int N, int K, int S)
   {
      BigDecimal numerator   = new BigDecimal(binomial((N - K), S));
      BigDecimal denominator = new BigDecimal(binomial(N, S));
      BigDecimal ratio       = numerator.divide(denominator, 3, RoundingMode.CEILING);
      float      P           = 1.0f - ratio.floatValue();

      return(P);
   }
}
