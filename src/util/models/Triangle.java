package util.models;

import org.jetbrains.annotations.NotNull;

import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

public class Triangle extends Path2D.Double {

    public Triangle(double x, double y, double w, double h) {
        moveTo(x, y + h);
        lineTo(x + (w / 2), y);
        lineTo(x + w, y + h);
        closePath();
    }

    public Triangle(@NotNull Rectangle2D bounds) {
        this(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
    }
}
