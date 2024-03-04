package math.geometry;

import math.MathException;
import java.io.Serial;

/**
 * This class represents exceptions thrown while extracting Cardan or Euler
 * angles from a rotation.
 * 
 * @version $Revision: 620312 $ $Date: 2008-02-10 12:28:59 -0700 (Sun, 10 Feb
 *          2008) $
 * @since 1.2
 */
public class CardanEulerSingularityException extends MathException {

	/** Serializable version identifier */
	@Serial
	private static final long serialVersionUID = -1360952845582206770L;

	/**
	 * Simple constructor. build an exception with a default message.
	 * 
	 * @param isCardan
	 *            if true, the rotation is related to Cardan angles, if false it
	 *            is related to EulerAngles
	 */
	public CardanEulerSingularityException(final boolean isCardan) {
		super(isCardan ? "Cardan angles singularity" : "Euler angles singularity",
				new Object[0]);
	}

}
