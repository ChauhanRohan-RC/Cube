package util.models;

import org.jetbrains.annotations.NotNull;

import java.awt.geom.Point2D;

public class Size {

    public static final Size ZERO = new Size(0, 0);

    public final double width;
    public final double height;

    public Size(double width, double height) {
        this.width = width;
        this.height = height;
    }

    public Size(@NotNull Size size) {
        this(size.width, size.height);
    }

    public Size(@NotNull Point2D start,@NotNull Point2D end) {
        this(end.getX() - start.getX(), end.getY() - start.getY());
    }

    public Size(@NotNull Point2D point) {
        this(point.getX(), point.getY());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o instanceof final Size size) {
            return width == size.width && height == size.height;
        }

        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return (31 * Double.hashCode(width)) + Double.hashCode(height);
    }

    @NotNull
    public Size add(double w, double h) {
        return new Size(width + w, height + h);
    }

    @NotNull
    public Size add(@NotNull Size size) {
        return add(size.width, size.height);
    }
    @NotNull
    public Size subtract(double w, double h) {
        return new Size(width - w, height - h);
    }

    @NotNull
    public Size subtract(@NotNull Size size) {
        return subtract(size.width, size.height);
    }

    @NotNull
    public Size scale(double scale) {
        return new Size(width * scale, height * scale);
    }

    @NotNull
    public Size negate() {
        return scale(-1);
    }

    @NotNull
    public Size copy() {
        return new Size(this);
    }
}
