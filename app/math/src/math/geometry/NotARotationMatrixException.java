package math.geometry;

import math.MathException;
import java.io.Serial;

/**
 * This class represents exceptions thrown while building rotations from
 * matrices.
 * 
 * @version $Revision: 627994 $ $Date: 2008-02-15 03:16:05 -0700 (Fri, 15 Feb
 *          2008) $
 * @since 1.2
 */
public class NotARotationMatrixException extends MathException {

	/** Serializable version identifier */
	@Serial
	private static final long serialVersionUID = 5647178478658937642L;

	/**
	 * Simple constructor. Build an exception by translating and formatting a
	 * message
	 * 
	 * @param specifier
	 *            format specifier (to be translated)
	 * @param parts
	 *            to insert in the format (no translation)
	 */
	public NotARotationMatrixException(final String specifier, final Object[] parts) {
		super(specifier, parts);
	}

}
