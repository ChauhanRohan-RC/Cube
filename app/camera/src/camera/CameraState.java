package camera;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import math.geometry.Rotation;
import math.geometry.RotationOrder;
import math.geometry.Vector3D;
import org.jetbrains.annotations.NotNull;
import processing.core.PApplet;
import processing.core.PGraphics;

public class CameraState implements Serializable {

	@Serial
	private static final long serialVersionUID = 12386482723547189L;


	public static class Builder {

		private double @NotNull[] rotations = {0, 0, 0};
		private double @NotNull[] center = {0, 0, 0};
		private double distance;

		public Builder(@NotNull CameraState cameraState) {
			setRotation(cameraState.getRotation());
			setCenter(cameraState.getCenter());
			setDistance(cameraState.getDistance());
		}

		public Builder() {
		}

		@NotNull
		public CameraState build() {
			return new CameraState(getRotation(), getCenter(), getDistance());
		}

		public double getRotationX() {
			return rotations[0];
		}

		public double getRotationY() {
			return rotations[1];
		}

		public double getRotationZ() {
			return rotations[2];
		}

		public Builder setRotationX(double rotationX) {
			this.rotations[0] = rotationX;
			return this;
		}

		public Builder setRotationY(double rotationY) {
			this.rotations[1] = rotationY;
			return this;
		}

		public Builder setRotationZ(double rotationZ) {
			this.rotations[2] = rotationZ;
			return this;
		}

		public Builder changeRotationXBy(double rotationXBy) {
			this.rotations[0] += rotationXBy;
			return this;
		}

		public Builder changeRotationYBy(double rotationYBy) {
			this.rotations[1] += rotationYBy;
			return this;
		}

		public Builder changeRotationZBy(double rotationZBy) {
			this.rotations[2] += rotationZBy;
			return this;
		}

		@NotNull
		public Rotation getRotation() {
			return new Rotation(RotationOrder.XYZ, rotations[0], rotations[1], rotations[2]);
		}

		public Builder setRotation(@NotNull Rotation rotation) {
			this.rotations = rotation.getAnglesXYZ();
			return this;
		}


		public double getCenterX() {
			return center[0];
		}

		public double getCenterY() {
			return center[1];
		}

		public double getCenterZ() {
			return center[2];
		}

		public Builder setCenterX(double centerX) {
			this.center[0] = centerX;
			return this;
		}

		public Builder setCenterY(double centerY) {
			this.center[1] = centerY;
			return this;
		}

		public Builder setCenterZ(double centerZ) {
			this.center[2] = centerZ;
			return this;
		}

		public Builder changeCenterXBy(double centerXBy) {
			this.center[0] += centerXBy;
			return this;
		}

		public Builder changeCenterYBy(double centerYBy) {
			this.center[1] += centerYBy;
			return this;
		}

		public Builder changeCenterZBy(double centerZBy) {
			this.center[2] += centerZBy;
			return this;
		}

		@NotNull
		public Vector3D getCenter() {
			return new Vector3D(center[0], center[1], center[2]);
		}

		public Builder setCenter(@NotNull Vector3D center) {
			this.center = center.getXYZ();
			return this;
		}


		public double getDistance() {
			return this.distance;
		}

		public Builder setDistance(double distance) {
			this.distance = distance;
			return this;
		}

		public Builder changeDistanceBy(double distanceBy) {
			this.distance += distanceBy;
			return this;
		}
	}


	@NotNull
	private final Rotation rotation;

	@NotNull
	private final Vector3D center;

	private final double distance;

	public CameraState(@NotNull Rotation rotation, @NotNull Vector3D center, double distance) {
		this.rotation = rotation;
		this.center = center;
		this.distance = distance;
	}

	@NotNull
	public Rotation getRotation() {
		return rotation;
	}

	@NotNull
	public Vector3D getCenter() {
		return center;
	}

	public double getDistance() {
		return distance;
	}


	public void apply(@NotNull PApplet a) {
		if (a.recorder != null) {
			apply(a.recorder);
		}

		apply(a.g);
	}

	public void apply(@NotNull PGraphics g) {
		PCamera.apply(g, center, rotation, distance);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass())
			return false;

		CameraState that = (CameraState) o;
		return Double.compare(distance, that.distance) == 0 && Objects.equals(rotation, that.rotation) && Objects.equals(center, that.center);
	}

	@Override
	public int hashCode() {
		return Objects.hash(rotation, center, distance);
	}


	/* Builders */

	@NotNull
	public Builder buildUpon() {
		return new Builder(this);
	}

	@NotNull
	public CameraState withRotation(@NotNull Rotation rotation) {
		return new CameraState(rotation, this.center, this.distance);
	}

	@NotNull
	public CameraState withCenter(@NotNull Vector3D center) {
		return new CameraState(this.rotation, center, this.distance);
	}

	@NotNull
	public CameraState withDistance(final double distance) {
		return new CameraState(this.rotation, this.center, distance);
	}



}
