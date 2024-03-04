package camera;

import math.geometry.CardanEulerSingularityException;
import math.geometry.Rotation;
import math.geometry.RotationOrder;
import math.geometry.Vector3D;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.event.KeyEvent;
import processing.event.MouseEvent;
import processing.opengl.PGraphicsOpenGL;

import java.util.Arrays;

/**
 * A camera for Processing P3D
 * */
public class PCamera {
	
	private static final Vector3D LOOK = Vector3D.plusK;
	private static final Vector3D UP = Vector3D.plusJ;
	private static final double SMALLEST_MINIMUM_DISTANCE = 0.01;

	public enum DragConstraint {
		PITCH,
		YAW,
		ROLL,
		SUPPRESS_ROLL
	}

	private final PGraphics g;
	private final PApplet p;

	@NotNull
	private Rotation startRotation;
	private double startDistance;
	@NotNull
	private Vector3D startCenter;

	private boolean resetOnDoubleClick = true;
	private long defaultResetAnimationMills = 300;

	private double minimumDistance = 1;
	private double maximumDistance = Double.MAX_VALUE;

	private final DampedAction rotateX, rotateY, rotateZ, dampedZoom, dampedPanX,
			dampedPanY;

	private double distance;
	private Vector3D center;
	private Rotation rotation;

	// viewport for the mouse-pointer [x,y,w,h]
	private final int[] viewport = new int[4];

	private DragConstraint dragConstraint = null;
	private DragConstraint permanentDragConstraint = null;

	private final InterpolationManager rotationInterps = new InterpolationManager();
	private final InterpolationManager centerInterps = new InterpolationManager();
	private final InterpolationManager distanceInterps = new InterpolationManager();

	private final PCameraDragHandler panHandler /* ha ha ha */ = new PCameraDragHandler() {
		public void handleDrag(final double dx, final double dy) {
			dampedPanX.impulse(dx / 8.);
			dampedPanY.impulse(dy / 8.);
		}
	};
	private PCameraDragHandler centerDragHandler = panHandler;

	private final PCameraDragHandler defaultFreeRotationHandler = this::mouseRotate;

	private PCameraDragHandler leftDragHandler = defaultFreeRotationHandler;

	private final PCameraDragHandler zoomHandler = new PCameraDragHandler() {
		public void handleDrag(final double dx, final double dy) {
			dampedZoom.impulse(dy / 10.0);
		}
	};
	private PCameraDragHandler rightDraghandler = zoomHandler;

	private final PCameraWheelHandler zoomWheelHandler = new PCameraWheelHandler() {
		public void handleWheel(final int delta) {
			dampedZoom.impulse(wheelScale * delta);
		}
	};
	private PCameraWheelHandler wheelHandler = zoomWheelHandler;
	private double wheelScale = 1.0;

	private final PCameraEventListener cameraEventListener = new PCameraEventListener();
	private boolean isActive = false;



//	public PCamera(final PApplet parent, final double distance) {
//		this(parent, parent.g, 0, 0, 0, distance);
//	}
//
//	public PCamera(final PApplet parent, final double lookAtX, final double lookAtY,
//			final double lookAtZ, final double distance) {
//		this(parent, parent.g, lookAtX, lookAtY, lookAtZ, distance);
//	}
//
//	public PCamera(final PApplet parent, final PGraphics pg, final double distance) {
//		this(parent, pg, 0, 0, 0, distance);
//	}


	public PCamera(final @NotNull PApplet parent, @NotNull CameraState cameraState) {
		this(parent, parent.g, cameraState);
	}

	public PCamera(final @NotNull PApplet parent, final @NotNull PGraphics pg, @NotNull CameraState cameraState) {
		this(parent, pg, cameraState.getCenter(), cameraState.getDistance(), cameraState.getRotation());
	}

	public PCamera(final @NotNull PApplet parent, @Nullable Vector3D startLookAt, final double startDistance, @Nullable Rotation startRotation) {
		this(parent, parent.g, startLookAt, startDistance, startRotation);
	}

	public PCamera(final @NotNull PApplet parent, final @NotNull PGraphics pg, @Nullable Vector3D startLookAt, final double startDistance, @Nullable Rotation startRotation) {
		this.p = parent;
		this.g = pg;

		if (startRotation == null) {
			startRotation = new Rotation();
		} else {
			startRotation = startRotation.copy();		// mutable, must copy
		}

		if (startLookAt == null) {
			startLookAt = Vector3D.zero;		// immutable, no need to copy
		}

		this.startRotation = this.rotation = startRotation;
		this.startCenter = this.center = startLookAt;
		this.startDistance = this.distance = Math.max(startDistance, SMALLEST_MINIMUM_DISTANCE);

		viewport[0] = 0;
		viewport[1] = 0;
		viewport[2] = pg.width;
		viewport[3] = pg.height;

		feed();

		rotateX = new DampedAction(this) {
			@Override
			protected void behave(final double velocity) {
				rotation = rotation.applyTo(new Rotation(Vector3D.plusI, velocity));
			}
		};

		rotateY = new DampedAction(this) {
			@Override
			protected void behave(final double velocity) {
				rotation = rotation.applyTo(new Rotation(Vector3D.plusJ, velocity));
			}
		};

		rotateZ = new DampedAction(this) {
			@Override
			protected void behave(final double velocity) {
				rotation = rotation.applyTo(new Rotation(Vector3D.plusK, velocity));
			}
		};

		dampedZoom = new DampedAction(this) {
			@Override
			protected void behave(final double velocity) {
				mouseZoom(velocity);
			}
		};

		dampedPanX = new DampedAction(this) {
			@Override
			protected void behave(final double velocity) {
				mousePan(velocity, 0);
			}
		};

		dampedPanY = new DampedAction(this) {
			@Override
			protected void behave(final double velocity) {
				mousePan(0, velocity);
			}
		};

		setActive(true);
	}

	@NotNull
	public PApplet getApplet() {
		return p;
	}

	@NotNull
	public PGraphics getCanvas() {
		return g;
	}

	public boolean isActive() {
		return isActive;
	}

	/**
	 * <p>
	 * Turn on or off default mouse-handling behavior:
	 *
	 * <p>
	 * <table>
	 * <tr>
	 * <td><b>left-drag</b></td>
	 * <td>rotate camera around look-at point</td>
	 * <tr>
	 * <tr>
	 * <td><b>center-drag</b></td>
	 * <td>pan camera (change look-at point)</td>
	 * <tr>
	 * <tr>
	 * <td><b>right-drag</b></td>
	 * <td>zoom</td>
	 * <tr>
	 * <tr>
	 * <td><b>wheel</b></td>
	 * <td>zoom</td>
	 * <tr>
	 * </table>
	 */
	public void setActive(final boolean active) {
		if (active == isActive) {
			return;
		}

		isActive = active;
		if (isActive) {
			p.registerMethod("mouseEvent", cameraEventListener);
			p.registerMethod("keyEvent", cameraEventListener);
		} else {
			p.unregisterMethod("mouseEvent", cameraEventListener);
			p.unregisterMethod("keyEvent", cameraEventListener);
		}
	}


	public double getWheelScale() {
		return wheelScale;
	}

	public void setWheelScale(final double wheelScale) {
		this.wheelScale = wheelScale;
	}

	public PCameraDragHandler getPanDragHandler() {
		return panHandler;
	}

	public PCameraDragHandler getRotateDragHandler() {
		return defaultFreeRotationHandler;
	}

	public PCameraDragHandler getZoomDragHandler() {
		return zoomHandler;
	}

	public PCameraWheelHandler getZoomWheelHandler() {
		return zoomWheelHandler;
	}

	@NotNull
	public PCameraDragHandler getDefaultFreeRotationHandler() {
		return defaultFreeRotationHandler;
	}

	public void setLeftDragHandler(final PCameraDragHandler handler) {
		leftDragHandler = handler;
	}

	public void setCenterDragHandler(final PCameraDragHandler handler) {
		centerDragHandler = handler;
	}

	public void setRightDragHandler(final PCameraDragHandler handler) {
		rightDraghandler = handler;
	}

	public PCameraWheelHandler getWheelHandler() {
		return wheelHandler;
	}

	public void setWheelHandler(final PCameraWheelHandler wheelHandler) {
		this.wheelHandler = wheelHandler;
	}

	public void setViewport(int x, int y, int w, int h) {
		viewport[0] = x;
		viewport[1] = y;
		viewport[2] = w;
		viewport[3] = h;
	}

	public int[] getViewport() {
		return new int[] { viewport[0], viewport[1], viewport[2], viewport[3] };
	}

	
	public boolean insideViewport(double x, double y) {
		float x0 = viewport[0], x1 = x0 + viewport[2];
		float y0 = viewport[1], y1 = y0 + viewport[3];
		return (x > x0) && (x < x1) && (y > y0) && (y < y1);
	}



	public void feed() {
		final Vector3D pos = rotation.applyTo(LOOK).scalarMultiply(distance).add(center);
		final Vector3D rup = rotation.applyTo(UP);
		g.camera((float)pos.getX(), (float)pos.getY(), (float)pos.getZ(), //
				(float)center.getX(), (float)center.getY(), (float)center.getZ(), //
				(float)rup.getX(), (float)rup.getY(), (float)rup.getZ());
	}

	public static void apply(final @NotNull PGraphics g, final Vector3D center, final @NotNull Rotation rotation,
							 final double distance) {
		final Vector3D pos = rotation.applyTo(LOOK).scalarMultiply(distance).add(center);
		final Vector3D rup = rotation.applyTo(UP);
		g.camera((float)pos.getX(), (float)pos.getY(), (float)pos.getZ(), //
				(float)center.getX(), (float)center.getY(), (float)center.getZ(), //
				(float)rup.getX(), (float)rup.getY(), (float)rup.getZ());
	}



	private double constraintDistance(double distance) {
		return Math.min(maximumDistance, Math.max(minimumDistance, distance));
	}

	public double getDistance() {
		return distance;
	}


//	private void safeSetDistance(final double distance) {
//		this.distance = Math.min(maximumDistance, Math.max(minimumDistance, distance));
//		feed();
//	}

	public void setDistance(final double newDistance, final long animationTimeMillis) {
		final double endDistance = constraintDistance(newDistance);
		distanceInterps.cancel();		// cancel running interpolations

		if (animationTimeMillis > 0) {
			distanceInterps.start(new DistanceInterp(endDistance, animationTimeMillis), false);
		} else {
			distance = endDistance;
			feed();
		}
	}

	public void setMinimumDistance(final double minimumDistance, final long animationTimeMillis) {
		this.minimumDistance = Math.max(minimumDistance, SMALLEST_MINIMUM_DISTANCE);
		setDistance(distance, animationTimeMillis);
	}

	public void setMaximumDistance(final double maximumDistance, final long animationTimeMillis) {
		this.maximumDistance = maximumDistance;
		setDistance(distance, animationTimeMillis);
	}

//	public void setDistance(final double newDistance, final long animationTimeMillis) {
//		distanceInterps
//				.startInterpolation(new DistanceInterp(newDistance, animationTimeMillis));
//	}

	@NotNull
	public Vector3D getLookAt() {
		return center;
	}

	public void lookAt(@NotNull Vector3D lookAt, final long animationTimeMillis) {
		centerInterps.cancel();		// cancel running interpolations

		if (animationTimeMillis > 0) {
			centerInterps.start(new CenterInterp(lookAt, animationTimeMillis), false);
		} else {
			center = lookAt;
			feed();
		}
	}

	public void lookAt(final @NotNull Vector3D lookAt, final double newDistance, final long animationTimeMillis) {
		final double endDistance = constraintDistance(newDistance);
		centerInterps.cancel();		// cancel running interpolations
		distanceInterps.cancel();		// cancel running interpolations

		if (animationTimeMillis > 0) {
			centerInterps.start(new CenterInterp(lookAt, animationTimeMillis), false);
			distanceInterps.start(new DistanceInterp(endDistance, animationTimeMillis), false);
		} else {
			center = lookAt;
			distance = endDistance;
			feed();
		}
	}

//	public void lookAt(final double x, final double y, final double z) {
//		centerInterps.startInterpolation(new CenterInterp(new Vector3D(x, y, z), 300));
//	}

//	public void lookAt(final double x, final double y, final double z,
//			final double distance) {
//		lookAt(x, y, z);
//		setDistance(distance);
//	}

//	public void lookAt(final double x, final double y, final double z,
//			final long animationTimeMillis) {
//		lookAt(x, y, z, distance, animationTimeMillis);
//	}

//	public void lookAt(final double x, final double y, final double z,
//			final double distance, final long animationTimeMillis) {
//		setState(new CameraState(rotation, new Vector3D(x, y, z), distance),
//				animationTimeMillis);
//	}


//	public void pan(final double dx, final double dy) {
//		center = center.add(rotation.applyTo(new Vector3D(dx, dy, 0)));
//		feed();
//	}

	public void pan(final double dx, final double dy, final long animationTimeInMillis) {
		final Vector3D endCenter = center.add(rotation.applyTo(new Vector3D(dx, dy, 0)));
		centerInterps.cancel();		// cancel running interpolations

		if (animationTimeInMillis > 0) {
			centerInterps.start(new CenterInterp(endCenter, animationTimeInMillis), false);
		} else {
			center = endCenter;
			feed();
		}
	}


	@NotNull
	public Rotation getRotation() {
		return rotation;
	}

	/**
	 * Express the current camera rotation as an equivalent series
	 * of world rotations, in X, Y, Z order. This is useful when,
	 * for example, you wish to orient text towards the camera
	 * at all times, as in
	 *
	 * <pre>double[] rotations = cam.getRotations(rotations);
	 *cam.setRotations(rotations)
	 *text("Here I am!", 0, 0, 0);</pre>
	 */
	public double[] getRotations() {
		return rotation.getAnglesXYZ();
	}

	public float[] getRotationsF() {
		return rotation.getAnglesXYZ_F();
	}

	public void setRotation(final @NotNull Rotation newRotation, final long animationTimeInMillis) {
		rotationInterps.cancel();		// cancel running interpolations

		if (animationTimeInMillis > 0) {
			rotationInterps.start(new RotationInterp(newRotation, animationTimeInMillis), true);
		} else {
			this.rotation = newRotation;
			feed();
		}
	}

	public void setRotations(final double rotationX /* Pitch */, final double rotationY /* Yaw */, final double rotationZ /* Roll */, final long animationTimeInMillis) {
		setRotation(new Rotation(RotationOrder.XYZ, rotationX, rotationY, rotationZ), animationTimeInMillis);
	}

	public void setRotations(double @NotNull[] newRotations, final long animationTimeInMillis) {
		if (newRotations == null || newRotations.length != 3) {
			throw new IllegalArgumentException("Rotations must be an array of exactly 3 values. given " + Arrays.toString(newRotations));
		}

		setRotations(newRotations[0], newRotations[1], newRotations[2], animationTimeInMillis);
	}

	/**
	 * Rotate relatively around the given vector by the given angle
	 * */
	public void rotateAround(final Vector3D axis, final double angleBy, final long animationTimeInMillis) {
		setRotation(rotation.applyTo(new Rotation(axis, angleBy)), animationTimeInMillis);
	}

	/**
	 * Rotate relatively around X-axis (1, 0, 0) by the given angle
	 * */
	public void rotateX(final double angleBy, final long animationTimeInMillis) {
		rotateAround(Vector3D.plusI, angleBy, animationTimeInMillis);
	}

	/**
	 * Rotate relatively around Y-axis (0, 1, 0) by the given angle
	 * */
	public void rotateY(final double angleBy, final long animationTimeInMillis) {
		rotateAround(Vector3D.plusJ, angleBy, animationTimeInMillis);
	}

	/**
	 * Rotate relatively around Z-axis (0, 0, 1) by the given angle
	 * */
	public void rotateZ(final double angleBy, final long animationTimeInMillis) {
		rotateAround(Vector3D.plusK, angleBy, animationTimeInMillis);
	}


	/**
	 * Set absolute rotation around x-axis to the given angle
	 * */
	public void rotateXTo(final double rotationX, final long animationTimeInMillis) {
		final double[] rots = getRotations();
		rots[0] = rotationX;
		setRotations(rots, animationTimeInMillis);
	}

	/**
	 * Set absolute rotation around Y-axis to the given angle
	 * */
	public void rotateYTo(final double rotationY, final long animationTimeInMillis) {
		final double[] rots = getRotations();
		rots[1] = rotationY;
		setRotations(rots, animationTimeInMillis);
	}

	/**
	 * Set absolute rotation around Z-axis to the given angle
	 * */
	public void rotateZTo(final double rotationZ, final long animationTimeInMillis) {
		final double[] rots = getRotations();
		rots[2] = rotationZ;
		setRotations(rots, animationTimeInMillis);
	}

//	public void rotateBy(final double rotationXBy, final double rotationYBy, final double rotationZBy, final long animationTimeInMillis) {
//		final double[] rots = getRotations();
//		rots[0] += rotationXBy;
//		rots[1] += rotationYBy;
//		rots[2] += rotationZBy;
//
//		setRotations(rots, animationTimeInMillis);
//	}

	/**
	 * Rotate absolutely around X-axis by the given angle<br>
	 * <strong>
	 *     new_rotationX = cur_rotationX + rotationXBy
	 * </strong>
	 * <br><br>
	 * This is different from {@link #rotateX} in that the latter involves relative rotation
	 *
	 * @return true if the operation succeeded
	 * */
	public boolean rotateXBy(final double rotationXBy, final long animationTimeInMillis) {
		try {
			final double[] rots = rotation.getAngles(RotationOrder.XYZ);
			rots[0] += rotationXBy;

			setRotation(new Rotation(RotationOrder.XYZ, rots[0], rots[1], rots[2]), animationTimeInMillis);
			return true;
		} catch (CardanEulerSingularityException ignored) {
			return false;
		}
	}

	/**
	 * Rotate absolutely around Y-axis by the given angle<br>
	 * <strong>
	 *     new_rotationY = cur_rotationY + rotationYBy
	 * </strong>
	 * <br><br>
	 * This is different from {@link #rotateY} in that the latter involves relative rotation
	 *
	 * @return true if the operation succeeded
	 * */
	public boolean rotateYBy(final double rotationYBy, final long animationTimeInMillis) {
//		rotateBy(0, rotationYBy, 0, animationTimeInMillis);

		try {
			final double[] rots = rotation.getAngles(RotationOrder.YZX);
			rots[0] += rotationYBy;

			setRotation(new Rotation(RotationOrder.YZX, rots[0], rots[1], rots[2]), animationTimeInMillis);
			return true;
		} catch (CardanEulerSingularityException ignored) {
			return false;
		}
	}

	/**
	 * Rotate absolutely around Z-axis by the given angle<br>
	 * <strong>
	 *     new_rotationZ = cur_rotationZ + rotationZBy
	 * </strong>
	 * <br><br>
	 * This is different from {@link #rotateZ} in that the latter involves relative rotation
	 *
	 * @return true if the operation succeeded
	 * */
	public boolean rotateZBy(final double rotationZBy, final long animationTimeInMillis) {
//		rotateBy(0, 0, rotationZBy, animationTimeInMillis);

		try {
			final double[] rots = rotation.getAngles(RotationOrder.ZXY);
			rots[0] += rotationZBy;

			setRotation(new Rotation(RotationOrder.ZXY, rots[0], rots[1], rots[2]), animationTimeInMillis);
			return true;
		} catch (CardanEulerSingularityException ignored) {
			return false;
		}
	}




	@Nullable
	public DragConstraint getPermanentDragConstraint() {
		return permanentDragConstraint;
	}

	/**
	 * Permit arbitrary rotation. (Default mode.)
	 */
	public void setFreeRotationMode() {
		permanentDragConstraint = null;
	}

	/**
	 * Only permit pitch.
	 */
	public void setPitchRotationMode() {
		permanentDragConstraint = DragConstraint.PITCH;
	}

	/**
	 * Only permit yaw.
	 */
	public void setYawRotationMode() {
		permanentDragConstraint = DragConstraint.YAW;
	}

	/**
	 * Only permit roll.
	 */
	public void setRollRotationMode() {
		permanentDragConstraint = DragConstraint.ROLL;
	}

	/**
	 * Only suppress roll.
	 */
	public void setSuppressRollRotationMode() {
		permanentDragConstraint = DragConstraint.SUPPRESS_ROLL;
	}


	public boolean isResetOnDoubleClickEnabled() {
		return resetOnDoubleClick;
	}

	public void setResetOnDoubleClick(final boolean resetOnDoubleClick) {
		this.resetOnDoubleClick = resetOnDoubleClick;
	}

	public long getDefaultResetAnimationMills() {
		return defaultResetAnimationMills;
	}

	public void setDefaultResetAnimationMills(final long defaultResetAnimationMills) {
		this.defaultResetAnimationMills = defaultResetAnimationMills;
	}


	/**
	 * @return position of the camera in 3D world space
	 */
	@NotNull
	public Vector3D getPosition() {
		return rotation.applyTo(LOOK).scalarMultiply(distance).add(center);
	}

	@NotNull
	public CameraState getState() {
		return new CameraState(rotation, center, distance);
	}

	public void setState(final CameraState state, final long animationTimeMillis) {
		rotationInterps.cancel();
		centerInterps.cancel();
		distanceInterps.cancel();

		if (animationTimeMillis > 0) {
			rotationInterps.start(new RotationInterp(state.getRotation(), animationTimeMillis), false);
			centerInterps.start(new CenterInterp(state.getCenter(), animationTimeMillis), false);
			distanceInterps.start(new DistanceInterp(state.getDistance(), animationTimeMillis), false);
		} else {
			this.rotation = state.getRotation();
			this.center = state.getCenter();
			this.distance = state.getDistance();
			feed();
		}
	}



	@NotNull
	public Rotation getStartRotation() {
		return startRotation;
	}

	public void setStartRotation(@Nullable Rotation startRotation) {
		if (startRotation == null) {
			startRotation = new Rotation();
		} else {
			startRotation = startRotation.copy();
		}

		this.startRotation = startRotation;
	}

	@NotNull
	public Vector3D getStartLookAt() {
		return startCenter;
	}

	public void setStartLookAt(@Nullable Vector3D startLookAt) {
		if (startLookAt == null) {
			startLookAt = Vector3D.zero;
		}

		this.startCenter = startLookAt;
	}

	public double getStartDistance() {
		return startDistance;
	}

	public void setStartDistance(double startDistance) {
		this.startDistance = constraintDistance(startDistance);
	}

	@NotNull
	public CameraState getStartState() {
		return new CameraState(startRotation, startCenter, startDistance);
	}

	public void setStartState(@NotNull CameraState startState) {
		setStartRotation(startState.getRotation());
		setStartLookAt(startState.getCenter());
		setStartDistance(startState.getDistance());
	}


	public void reset(final long animationTimeInMillis) {
		setState(getStartState(), animationTimeInMillis);
	}

	public void reset() {
		reset(defaultResetAnimationMills);
	}

	public void reset(boolean animate) {
		reset(animate? defaultResetAnimationMills: 0);
	}


	private boolean pushedLights = false;

	/**
	 * 
	 * begin screen-aligned 2D-drawing.
	 * <pre>
	 * beginHUD()
	 *   disabled depth test
	 *   disabled lights
	 *   ortho
	 * endHUD()
	 * </pre>
	 * 
	 */
	public void beginHUD() {
		g.hint(PConstants.DISABLE_DEPTH_TEST);
		g.pushMatrix();
		g.resetMatrix();
		// 3D is always GL (in processing 3), so this check is probably redundant.
		if (g.isGL() && g.is3D()) {
			PGraphicsOpenGL pgl = (PGraphicsOpenGL)g;
			pushedLights = pgl.lights;
			pgl.lights = false;
			pgl.pushProjection();
			g.ortho(0, viewport[2], -viewport[3], 0, -Float.MAX_VALUE, +Float.MAX_VALUE);
		}
	}

	/**
	 * 
	 * end screen-aligned 2D-drawing.
	 * 
	 */
	public void endHUD() {
		if (g.isGL() && g.is3D()) {
			PGraphicsOpenGL pgl = (PGraphicsOpenGL)g;
			pgl.popProjection();
			pgl.lights = pushedLights;
		}
		g.popMatrix();
		g.hint(PConstants.ENABLE_DEPTH_TEST);
	}


	protected class PCameraEventListener {

		public boolean isActive = false;

		public void keyEvent(final KeyEvent e) {
			if (e.getAction() == KeyEvent.RELEASE && e.isShiftDown())
				dragConstraint = null;
		}

		public void mouseEvent(final MouseEvent e) {
			switch (e.getAction()) {

				case MouseEvent.PRESS:
					if (insideViewport(p.mouseX, p.mouseY)) {
						isActive = true;
					}
					break;

				case MouseEvent.RELEASE:
					dragConstraint = null;
					isActive = false;
					break;

				case MouseEvent.CLICK:
					if (insideViewport(p.mouseX, p.mouseY)) {
						if (resetOnDoubleClick && 2 == e.getCount()) {
							reset();
						}
					}
					break;

				case MouseEvent.WHEEL:
					if (wheelHandler != null && insideViewport(p.mouseX, p.mouseY)) {
						wheelHandler.handleWheel(e.getCount());
					}
					break;

				case MouseEvent.DRAG:
					if (isActive) {
						final double dx = p.mouseX - p.pmouseX;
						final double dy = p.mouseY - p.pmouseY;

						if (e.isShiftDown()) {
							if (dragConstraint == null && Math.abs(dx - dy) > 1) {
								dragConstraint = Math.abs(dx) > Math.abs(dy) ? DragConstraint.YAW
										: DragConstraint.PITCH;
							}
						} else if (permanentDragConstraint != null) {
							dragConstraint = permanentDragConstraint;
						} else {
							dragConstraint = null;
						}

						final int b = p.mouseButton;
						if (centerDragHandler != null && (b == PConstants.CENTER
								|| (b == PConstants.LEFT && e.isMetaDown()))) {
							centerDragHandler.handleDrag(dx, dy);
						} else if (leftDragHandler != null && b == PConstants.LEFT) {
							leftDragHandler.handleDrag(dx, dy);
						} else if (rightDraghandler != null && b == PConstants.RIGHT) {
							rightDraghandler.handleDrag(dx, dy);
						}
					}
					break;
			}
		}
	}

	private void mouseZoom(final double delta) {
		double new_distance = distance + delta * distance * 0.02;
		if (new_distance < minimumDistance) {
			new_distance = minimumDistance;
			dampedZoom.stop();
		}
		if (new_distance > maximumDistance) {
			new_distance = maximumDistance;
			dampedZoom.stop();
		}

		setDistance(new_distance, 0);
	}

	private void mousePan(final double dxMouse, final double dyMouse) {
		final double panScale = distance * 0.0025;
		pan(dragConstraint == DragConstraint.PITCH ? 0 : -dxMouse * panScale,
				dragConstraint == DragConstraint.YAW ? 0 : -dyMouse * panScale, 0);
	}

	private void mouseRotate(final double dx, final double dy) {
		double mult = -Math.pow(Math.log10(1 + distance), 0.5) * 0.00125f;

		double dmx = dx * mult;
		double dmy = dy * mult;

		double viewX = viewport[0];
		double viewY = viewport[1];
		double viewW = viewport[2];
		double viewH = viewport[3];

		// mouse [-1, +1]
		double mxNdc = Math.min(Math.max((p.mouseX - viewX) / viewW, 0), 1) * 2 - 1;
		double myNdc = Math.min(Math.max((p.mouseY - viewY) / viewH, 0), 1) * 2 - 1;

		if (dragConstraint == null || dragConstraint == DragConstraint.YAW
				|| dragConstraint == DragConstraint.SUPPRESS_ROLL) {
			rotateY.impulse(+dmx * (1.0 - myNdc * myNdc));
		}
		if (dragConstraint == null || dragConstraint == DragConstraint.PITCH
				|| dragConstraint == DragConstraint.SUPPRESS_ROLL) {
			rotateX.impulse(-dmy * (1.0 - mxNdc * mxNdc));
		}
		if (dragConstraint == null || dragConstraint == DragConstraint.ROLL) {
			rotateZ.impulse(-dmx * myNdc);
			rotateZ.impulse(+dmy * mxNdc);
		}
	}

	abstract public class AbstractInterp {
		double startTime;
		final double timeInMillis;

		protected AbstractInterp(final long timeInMillis) {
			this.timeInMillis = timeInMillis;
		}

		void start() {
			startTime = p.millis();
			p.registerMethod("draw", this);
		}

		void cancel() {
			p.unregisterMethod("draw", this);
		}

		public final void draw() {
			final double t = (p.millis() - startTime) / timeInMillis;
			if (t > .99) {
				finish();
			} else {
				interp(t);
				feed();
			}
		}

		protected abstract void interp(double t);

		protected abstract void setEndState();

		public final void finish() {
			cancel();
			setEndState();
			feed();
		}
	}

	class DistanceInterp extends AbstractInterp {
		private final double startDistance = distance;
		private final double endDistance;

		public DistanceInterp(final double endDistance, final long timeInMillis) {
			super(timeInMillis);
			this.endDistance = constraintDistance(endDistance);
		}

		@Override
		protected void interp(final double t) {
			distance = InterpolationUtil.smooth(startDistance, endDistance, t);
		}

		@Override
		protected void setEndState() {
			distance = endDistance;
		}
	}

	class CenterInterp extends AbstractInterp {
		private final Vector3D startCenter = center;
		private final Vector3D endCenter;

		public CenterInterp(final Vector3D endCenter, final long timeInMillis) {
			super(timeInMillis);
			this.endCenter = endCenter;
		}

		@Override
		protected void interp(final double t) {
			center = InterpolationUtil.smooth(startCenter, endCenter, t);
		}

		@Override
		protected void setEndState() {
			center = endCenter;
		}
	}

	class RotationInterp extends AbstractInterp {
		final Rotation startRotation = rotation;
		final Rotation endRotation;

		public RotationInterp(final Rotation endRotation, final long timeInMillis) {
			super(timeInMillis);
			this.endRotation = endRotation;
		}

		@Override
		void start() {
			rotateX.stop();
			rotateY.stop();
			rotateZ.stop();
			super.start();
		}

		@Override
		protected void interp(final double t) {
			rotation = InterpolationUtil.slerp(startRotation, endRotation, t);
		}

		@Override
		protected void setEndState() {
			rotation = endRotation;
		}
	}
}
