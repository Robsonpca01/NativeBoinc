package org.bouncycastle.openpgp;

import java.io.IOException;
import java.io.InputStream;
//import java.io.OutputStream;
import java.util.ArrayList;
//import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Often a PGP key ring file is made up of a succession of master/sub-key key
 * rings. If you want to read an entire public key file in one hit this is the
 * class for you.
 */
public class PGPPublicKeyRingCollection {
	private Map pubRings = new HashMap();
	private List order = new ArrayList();

	/**
	 * Build a PGPPublicKeyRingCollection from the passed in input stream.
	 * 
	 * @param in
	 *            input stream containing data
	 * @throws IOException
	 *             if a problem parsing the base stream occurs
	 * @throws PGPException
	 *             if an object is encountered which isn't a PGPPublicKeyRing
	 */
	public PGPPublicKeyRingCollection(InputStream in) throws IOException,
			PGPException {
		PGPObjectFactory pgpFact = new PGPObjectFactory(in);
		Object obj;

		while ((obj = pgpFact.nextObject()) != null) {
			if (!(obj instanceof PGPPublicKeyRing)) {
				throw new PGPException(obj.getClass().getName()
						+ " found where PGPPublicKeyRing expected");
			}

			PGPPublicKeyRing pgpPub = (PGPPublicKeyRing) obj;
			Long key = new Long(pgpPub.getPublicKey().getKeyID());

			pubRings.put(key, pgpPub);
			order.add(key);
		}
	}

	/**
	 * Return the number of rings in this collection.
	 * 
	 * @return size of the collection
	 */
	public int size() {
		return order.size();
	}

	/**
	 * return the public key rings making up this collection.
	 */
	public Iterator getKeyRings() {
		return pubRings.values().iterator();
	}

	/**
	 * Return the PGP public key associated with the given key id.
	 * 
	 * @param keyID
	 * @return the PGP public key
	 * @throws PGPException
	 */
	public PGPPublicKey getPublicKey(long keyID) throws PGPException {
		Iterator it = this.getKeyRings();

		while (it.hasNext()) {
			PGPPublicKeyRing pubRing = (PGPPublicKeyRing) it.next();
			PGPPublicKey pub = pubRing.getPublicKey(keyID);

			if (pub != null) {
				return pub;
			}
		}

		return null;
	}
}
