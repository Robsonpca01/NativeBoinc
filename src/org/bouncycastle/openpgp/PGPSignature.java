package org.bouncycastle.openpgp;


import org.bouncycastle.bcpg.*;
import org.bouncycastle.util.BigIntegers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchProviderException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.Provider;
import java.util.Date;

/**
 *A PGP signature object.
 */
public class PGPSignature
{
    public static final int    BINARY_DOCUMENT = 0x00;
    public static final int    CANONICAL_TEXT_DOCUMENT = 0x01;
    public static final int    STAND_ALONE = 0x02;
    
    public static final int    DEFAULT_CERTIFICATION = 0x10;
    public static final int    NO_CERTIFICATION = 0x11;
    public static final int    CASUAL_CERTIFICATION = 0x12;
    public static final int    POSITIVE_CERTIFICATION = 0x13;
    
    public static final int    SUBKEY_BINDING = 0x18;
    public static final int    PRIMARYKEY_BINDING = 0x19;
    public static final int    DIRECT_KEY = 0x1f;
    public static final int    KEY_REVOCATION = 0x20;
    public static final int    SUBKEY_REVOCATION = 0x28;
    public static final int    CERTIFICATION_REVOCATION = 0x30;
    public static final int    TIMESTAMP = 0x40;
    
    private SignaturePacket    sigPck;
    private Signature          sig;
    private int                signatureType;
    private TrustPacket        trustPck;
    
    private byte               lastb;
    
    PGPSignature(
        BCPGInputStream    pIn)
        throws IOException, PGPException
    {
        this((SignaturePacket)pIn.readPacket());
    }
    
    PGPSignature(
        SignaturePacket    sigPacket)
        throws PGPException
    {
        sigPck = sigPacket;
        signatureType = sigPck.getSignatureType();
        trustPck = null;
    }
    
    PGPSignature(
        SignaturePacket    sigPacket,
        TrustPacket        trustPacket)
        throws PGPException
    {
        this(sigPacket);
        
        this.trustPck = trustPacket;
    }
    
    private void getSig(
        Provider provider)
        throws PGPException
    {
        try
        {
            this.sig = Signature.getInstance(PGPUtil.getSignatureName(sigPck.getKeyAlgorithm(), sigPck.getHashAlgorithm()), provider);
        }
        catch (Exception e)
        {    
            throw new PGPException("can't set up signature object.", e);
        }
    }

    public void initVerify(
        PGPPublicKey    pubKey,
        String          provider)
        throws NoSuchProviderException, PGPException
    {
        initVerify(pubKey, PGPUtil.getProvider(provider));
    }

    public void initVerify(
        PGPPublicKey    pubKey,
        Provider        provider)
        throws PGPException
    {    
        if (sig == null)
        {
            getSig(provider);
        }

        try
        {
            sig.initVerify(pubKey.getKey(provider));
        }
        catch (InvalidKeyException e)
        {
            throw new PGPException("invalid key.", e);
        }
        
        lastb = 0;
    }
        
    public void update(
        byte    b)
        throws SignatureException
    {
        if (signatureType == PGPSignature.CANONICAL_TEXT_DOCUMENT)
        {
            if (b == '\r')
            {
                sig.update((byte)'\r');
                sig.update((byte)'\n');
            }
            else if (b == '\n')
            {
                if (lastb != '\r')
                {
                    sig.update((byte)'\r');
                    sig.update((byte)'\n');
                }
            }
            else
            {
                sig.update(b);
            }

            lastb = b;
        }
        else
        {
            sig.update(b);
        }
    }
        
    public void update(
        byte[]    bytes)
        throws SignatureException
    {
        this.update(bytes, 0, bytes.length);
    }
        
    public void update(
        byte[]    bytes,
        int       off,
        int       length)
        throws SignatureException
    {
        if (signatureType == PGPSignature.CANONICAL_TEXT_DOCUMENT)
        {
            int finish = off + length;
            
            for (int i = off; i != finish; i++)
            {
                this.update(bytes[i]);
            }
        }
        else
        {
            sig.update(bytes, off, length);
        }
    }
    
    public boolean verify()
        throws PGPException, SignatureException
    {
        sig.update(this.getSignatureTrailer());
            
        return sig.verify(this.getSignature());
    }
    public int getSignatureType()
    {
         return sigPck.getSignatureType();
    }
    
    /**
     * Return the id of the key that created the signature.
     * @return keyID of the signatures corresponding key.
     */
    public long getKeyID()
    {
         return sigPck.getKeyID();
    }
    
    /**
     * Return the creation time of the signature.
     * 
     * @return the signature creation time.
     */
    public Date getCreationTime()
    {
        return new Date(sigPck.getCreationTime());
    }
    
    public byte[] getSignatureTrailer()
    {
        return sigPck.getSignatureTrailer();
    }

    /**
     * Return true if the signature has either hashed or unhashed subpackets.
     * 
     * @return true if either hashed or unhashed subpackets are present, false otherwise.
     */
    public boolean hasSubpackets()
    {
        return sigPck.getHashedSubPackets() != null || sigPck.getUnhashedSubPackets() != null;
    }

    public PGPSignatureSubpacketVector getHashedSubPackets()
    {
        return createSubpacketVector(sigPck.getHashedSubPackets());
    }

    public PGPSignatureSubpacketVector getUnhashedSubPackets()
    {
        return createSubpacketVector(sigPck.getUnhashedSubPackets());
    }
    
    private PGPSignatureSubpacketVector createSubpacketVector(SignatureSubpacket[] pcks)
    {
        if (pcks != null)
        {
            return new PGPSignatureSubpacketVector(pcks);
        }
        
        return null;
    }
    
    public byte[] getSignature()
        throws PGPException
    {
        MPInteger[]    sigValues = sigPck.getSignature();
        byte[]         signature;

        if (sigValues != null)
        {
            if (sigValues.length == 1)    // an RSA signature
            {
                signature = BigIntegers.asUnsignedByteArray(sigValues[0].getValue());
            }
            else
            {
            	throw new PGPException("Sorry, not supported");
            }
        }
        else
        {
            signature = sigPck.getSignatureBytes();
        }
        
        return signature;
    }
    
    public byte[] getEncoded() 
        throws IOException
    {
        ByteArrayOutputStream    bOut = new ByteArrayOutputStream();
        
        this.encode(bOut);
        
        return bOut.toByteArray();
    }
    
    public void encode(
        OutputStream    outStream) 
        throws IOException
    {
        BCPGOutputStream    out;
        
        if (outStream instanceof BCPGOutputStream)
        {
            out = (BCPGOutputStream)outStream;
        }
        else
        {
            out = new BCPGOutputStream(outStream);
        }

        out.writePacket(sigPck);
        if (trustPck != null)
        {
            out.writePacket(trustPck);
        }
    }
    
}
