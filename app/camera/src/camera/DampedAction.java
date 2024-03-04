package camera;

abstract public class DampedAction {
	private final PCamera p;
	private double velocity;
	private final double damping;

	public DampedAction(final PCamera p) {
		this(p, 0.16);
	}

	public DampedAction(final PCamera p, final double friction) {
		this.p = p;
		this.velocity = 0;
		this.damping = 1.0 - friction;
		p.getApplet().registerMethod("draw", this);
	}

	public void impulse(final double impulse) {
		velocity += impulse;
	}

	public void draw() {
		if (velocity == 0) {
			return;
		}
		behave(velocity);
		p.feed();
		velocity *= damping;
		if (Math.abs(velocity) < .001) {
			velocity = 0;
		}
	}

	public void stop() {
		velocity = 0;
	}

	abstract protected void behave(final double velocity);
}
