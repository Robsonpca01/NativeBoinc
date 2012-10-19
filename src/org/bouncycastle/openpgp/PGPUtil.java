package org.bouncycastle.openpgp;

import java.io.BufferedInputStream;
//import java.io.File;
//import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
//import java.io.OutputStream;
//import java.security.MessageDigest;
//import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
//import java.security.SecureRandom;
import java.security.Security;
//import java.util.Date;

//import javax.crypto.SecretKey;
//import javax.crypto.spec.SecretKeySpec;

//import org.bouncycastle.asn1.ASN1InputStream;
//import org.bouncycastle.asn1.ASN1Sequence;
//import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.bcpg.ArmoredInputStream;
import org.bouncycastle.bcpg.HashAlgorithmTags;
//import org.bouncycastle.bcpg.MPInteger;
import org.bouncycastle.bcpg.PublicKeyAlgorithmTags;


/**
 * Basic utility class
 */
public class PGPUtil
    implements HashAlgorithmTags
{
    private    static String    defProvider = "BC";

    /**
     * Return the provider that will be used by factory classes in situations
     * where a provider must be determined on the fly.
     * 
     * @return String
     */
    public static String getDefaultProvider()
    {
        return defProvider;
    }
    
    /**
     * Set the provider to be used by the package when it is necessary to 
     * find one on the fly.
     * 
     * @param provider
     */
    public static void setDefaultProvider(
        String    provider)
    {
        defProvider = provider;
    }
    
    
    static String getDigestName(
        int        hashAlgorithm)
        throws PGPException
    {
        switch (hashAlgorithm)
        {
        case HashAlgorithmTags.SHA1:
            return "SHA1";
        case HashAlgorithmTags.MD2:
            return "MD2";
        case HashAlgorithmTags.MD5:
            return "MD5";
        case HashAlgorithmTags.RIPEMD160:
            return "RIPEMD160";
        case HashAlgorithmTags.SHA256:
            return "SHA256";
        case HashAlgorithmTags.SHA384:
            return "SHA384";
        case HashAlgorithmTags.SHA512:
            return "SHA512";
        case HashAlgorithmTags.SHA224:
            return "SHA224";
        default:
            throw new PGPException("unknown hash algorithm tag in getDigestName: " + hashAlgorithm);
        }
    }
    
    static String getSignatureName(
        int        keyAlgorithm,
        int        hashAlgorithm)
        throws PGPException
    {
        String     encAlg;
                
        switch (keyAlgorithm)
        {
        case PublicKeyAlgorithmTags.RSA_GENERAL:
        case PublicKeyAlgorithmTags.RSA_SIGN:
            encAlg = "RSA";
            break;
        case PublicKeyAlgorithmTags.DSA:
            encAlg = "DSA";
            break;
        case PublicKeyAlgorithmTags.ELGAMAL_ENCRYPT: // in some malformed cases.
        case PublicKeyAlgorithmTags.ELGAMAL_GENERAL:
            encAlg = "ElGamal";
            break;
        default:
            throw new PGPException("unknown algorithm tag in signature:" + keyAlgorithm);
        }

        return getDigestName(hashAlgorithm) + "with" + encAlg;
    }

 

    private static final int READ_AHEAD = 60;
    
    private static boolean isPossiblyBase64(
        int    ch)
    {
        return (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') 
                || (ch >= '0' && ch <= '9') || (ch == '+') || (ch == '/')
                || (ch == '\r') || (ch == '\n');
    }
    
    /**
     * Return either an ArmoredInputStream or a BCPGInputStream based on
     * whether the initial characters of the stream are binary PGP encodings or not.
     * 
     * @param in the stream to be wrapped
     * @return a BCPGInputStream
     * @throws IOException
     */
    public static InputStream getDecoderStream(
        InputStream    in) 
        throws IOException
    {
        if (!in.markSupported())
        {
            in = new BufferedInputStreamExt(in);
        }
        
        in.mark(READ_AHEAD);
        
        int    ch = in.read();
        

        if ((ch & 0x80) != 0)
        {
            in.reset();
        
            return in;
        }
        else
        {
            if (!isPossiblyBase64(ch))
            {
                in.reset();
        
                return new ArmoredInputStream(in);
            }
            
            throw new IOException("Sorry, unsupported");
            }
    }

    static Provider getProvider(String providerName)
        throws NoSuchProviderException
    {
        Provider prov = Security.getProvider(providerName);

        if (prov == null)
        {
            throw new NoSuchProviderException("provider " + providerName + " not found.");
        }

        return prov;
    }
    
    static class BufferedInputStreamExt extends BufferedInputStream
    {
        BufferedInputStreamExt(InputStream input)
        {
            super(input);
        }

        public synchronized int available() throws IOException
        {
            int result = super.available();
            if (result < 0)
            {
                result = Integer.MAX_VALUE;
            }
            return result;
        }
    }
}
