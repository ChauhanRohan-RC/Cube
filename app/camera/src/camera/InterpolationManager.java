package camera;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class InterpolationManager {

	@Nullable
	private volatile PCamera.AbstractInterp currentInterpolator = null;


	private synchronized void endCurrent(boolean finish) {
		final PCamera.AbstractInterp current = currentInterpolator;
		currentInterpolator = null;

		if (current != null) {
			if (finish) {
				current.finish();
			} else {
				current.cancel();
			}
		}
	}

	public synchronized void finish() {
		endCurrent(true);
	}

	public synchronized void cancel() {
		endCurrent(false);
	}


	protected synchronized void start(@NotNull final PCamera.AbstractInterp interpolation, boolean finishPrevious) {
		endCurrent(finishPrevious);

		currentInterpolator = interpolation;
		interpolation.start();
	}

}