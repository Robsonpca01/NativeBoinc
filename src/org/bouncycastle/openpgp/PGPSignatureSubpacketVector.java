package org.bouncycastle.openpgp;

import org.bouncycastle.bcpg.SignatureSubpacket;


/**
 * Container for a list of signature subpackets.
 */
public class PGPSignatureSubpacketVector
{
    SignatureSubpacket[]    packets;
    
    PGPSignatureSubpacketVector(
        SignatureSubpacket[]    packets)
    {
        this.packets = packets;
    }
    
    public SignatureSubpacket getSubpacket(
        int    type)
    {
        for (int i = 0; i != packets.length; i++)
        {
            if (packets[i].getType() == type)
            {
                return packets[i];
            }
        }
        
        return null;
    }
        /**
     * Return the number of packets this vector contains.
     * 
     * @return size of the packet vector.
     */
    public int size()
    {
        return packets.length;
    }
    
    SignatureSubpacket[] toSubpacketArray()
    {
        return packets;
    }
}
