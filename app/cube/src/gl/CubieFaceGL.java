package gl;

import model.Axis;
import model.cube.CubeI;
import model.cubie.CubieFace;
import org.jetbrains.annotations.NotNull;
import processing.core.PApplet;
import processing.core.PConstants;

import java.awt.*;

public class CubieFaceGL implements Drawable {

    @NotNull
    public static CubieFaceGL internal(@NotNull Axis axis) {
        return new CubieFaceGL(CubieFace.internal(axis));
    }

    @NotNull
    private final CubieFace cubieFace;
    @NotNull
    private final Color color;

    public CubieFaceGL(@NotNull CubieFace cubieFace) {
        this.cubieFace = cubieFace;
        this.color = GLConfig.mapFaceCodeColor(cubieFace.originalFaceCode());
    }

    @NotNull
    public final CubieFace getCubieFace() {
        return cubieFace;
    }

    @NotNull
    public final Color getColor() {
        return color;
    }

    @Override
    public void draw(@NotNull PApplet p) {
        p.pushMatrix();
        p.pushStyle();

        final Axis norm = cubieFace.currentNormal();
        p.translate(norm.unitX * CubeI.CUBIE_HALF_SIDE_LEN, norm.unitY * CubeI.CUBIE_HALF_SIDE_LEN, norm.unitZ * CubeI.CUBIE_HALF_SIDE_LEN);
        if (norm.isX()) {
            p.rotateY((norm.isPositive()? 1: -1) * PApplet.HALF_PI);
        } else if (norm.isY()) {
            p.rotateX((norm.isPositive()? -1: 1) * PApplet.HALF_PI);
        } else if (norm.isNegative()) {
            p.rotateY(PApplet.PI);
        }

//        p.stroke(GLConfig.CUBIE_FACE_STROKE.getRGB());
//        p.strokeWeight(GLConfig.CUBIE_FACE_STROKE_WEIGHT);

        // Sticker
        if (cubieFace.originalFaceCode() != CubeI.FACE_INTERNAL) {
            p.noStroke();
            p.fill(color.getRGB());
            p.rectMode(PConstants.CENTER);
            p.rect(0, 0, CubeI.CUBIE_STICKER_SIDE_LENGTH, CubeI.CUBIE_STICKER_SIDE_LENGTH, CubeI.CUBIE_STICKER_CORNERS, CubeI.CUBIE_STICKER_CORNERS, CubeI.CUBIE_STICKER_CORNERS, CubeI.CUBIE_STICKER_CORNERS);
        }

        // bg
        p.noStroke();
        p.fill(GLConfig.COLOR_INTERNAL.getRGB());
        p.rectMode(PConstants.CENTER);
        p.translate(0, 0, -CubeI.CUBIE_STICKER_ELEVATION);
        p.square(0, 0, CubeI.CUBIE_SIDE_LEN);

        p.popStyle();
        p.popMatrix();
    }
}
