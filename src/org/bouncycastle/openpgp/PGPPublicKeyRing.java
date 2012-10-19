package org.bouncycastle.openpgp;

import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
//import java.io.OutputStream;
import java.util.ArrayList;
//import java.util.Collections;
//import java.util.Iterator;
import java.util.List;

import org.bouncycastle.bcpg.BCPGInputStream;
import org.bouncycastle.bcpg.PacketTags;
import org.bouncycastle.bcpg.PublicKeyPacket;
import org.bouncycastle.bcpg.TrustPacket;

/**
 * Class to hold a single master public key and its subkeys.
 * <p>
 * Often PGP keyring files consist of multiple master keys, if you are trying to process
 * or construct one of these you should use the PGPPublicKeyRingCollection class.
 */
public class PGPPublicKeyRing
    extends PGPKeyRing
{
    List keys;
    
    public PGPPublicKeyRing(
        byte[]    encoding)
        throws IOException
    {
        this(new ByteArrayInputStream(encoding));
    }
    
    /**
     * @param pubKeys
     */
    PGPPublicKeyRing(
        List pubKeys)
    {
        this.keys = pubKeys;
    }

    public PGPPublicKeyRing(
        InputStream    in)
        throws IOException
    {
        this.keys = new ArrayList();

        BCPGInputStream pIn = wrap(in);

        int initialTag = pIn.nextPacketTag();
        if (initialTag != PacketTags.PUBLIC_KEY && initialTag != PacketTags.PUBLIC_SUBKEY)
        {
            throw new IOException(
                "public key ring doesn't start with public key tag: " +
                "tag 0x" + Integer.toHexString(initialTag));
        }

        PublicKeyPacket pubPk = (PublicKeyPacket)pIn.readPacket();
        TrustPacket     trustPk = readOptionalTrustPacket(pIn);

        // direct signatures and revocations
        List keySigs = readSignaturesAndTrust(pIn);

        List ids = new ArrayList();
        List idTrusts = new ArrayList();
        List idSigs = new ArrayList();
        readUserIDs(pIn, ids, idTrusts, idSigs);

        keys.add(new PGPPublicKey(pubPk, trustPk, keySigs, ids, idTrusts, idSigs));


        // Read subkeys
        while (pIn.nextPacketTag() == PacketTags.PUBLIC_SUBKEY)
        {
            keys.add(readSubkey(pIn));
        }
    }

    /**
     * Return the first public key in the ring.
     * 
     * @return PGPPublicKey
     */
    public PGPPublicKey getPublicKey()
    {
        return (PGPPublicKey)keys.get(0);
    }
    
    /**
     * Return the public key referred to by the passed in keyID if it
     * is present.
     * 
     * @param keyID
     * @return PGPPublicKey
     */
    public PGPPublicKey getPublicKey(
        long        keyID)
    {    
        for (int i = 0; i != keys.size(); i++)
        {
            PGPPublicKey    k = (PGPPublicKey)keys.get(i);
            
            if (keyID == k.getKeyID())
            {
                return k;
            }
        }
    
        return null;
    }
        static PGPPublicKey readSubkey(BCPGInputStream in)
        throws IOException
    {
        PublicKeyPacket pk = (PublicKeyPacket)in.readPacket();
        TrustPacket     kTrust = readOptionalTrustPacket(in);

        // PGP 8 actually leaves out the signature.
        List sigList = readSignaturesAndTrust(in);

        return new PGPPublicKey(pk, kTrust, sigList);
    }
}
