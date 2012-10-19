package org.bouncycastle.util;

/**
 * General array utilities.
 */
public class Arrays {
	private Arrays() {
		// static class, hide constructor
	}

	public static boolean areEqual(byte[] a, byte[] b) {
		if (a == b) {
			return true;
		}

		if (a == null || b == null) {
			return false;
		}

		if (a.length != b.length) {
			return false;
		}

		for (int i = 0; i != a.length; i++) {
			if (a[i] != b[i]) {
				return false;
			}
		}

		return true;
	}

	/**
	 * A constant time equals comparison - does not terminate early if test will
	 * fail.
	 * 
	 * @param a
	 *            first array
	 * @param b
	 *            second array
	 * @return true if arrays equal, false otherwise.
	 */
	public static int hashCode(byte[] data) {
		if (data == null) {
			return 0;
		}

		int i = data.length;
		int hc = i + 1;

		while (--i >= 0) {
			hc *= 257;
			hc ^= data[i];
		}

		return hc;
	}
}
