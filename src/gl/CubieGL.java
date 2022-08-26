package gl;

import model.Axis;
import model.Point3DInt;
import model.cubie.Cubie;
import model.cubie.CubieFace;
import org.jetbrains.annotations.NotNull;
import processing.core.PApplet;

import java.util.EnumSet;

public class CubieGL implements Drawable, Cubie.Listener {

    @NotNull
    private final Cubie cubie;
    @NotNull
    private final CubieFaceGL[] facesGL;

    public CubieGL(@NotNull Cubie cubie) {
        this.cubie = cubie;

        if (GLConfig.CUBIE_DRAW_INTERNAL_FACES) {
            final EnumSet<Axis> normals = EnumSet.allOf(Axis.class);
            facesGL = new CubieFaceGL[normals.size()];

            int i = 0;

            // Main Faces
            for (; i < cubie.noOfFaces(); i++) {
                final CubieFace face = cubie.getFace(i);
                normals.remove(face.currentNormal());
                facesGL[i] = new CubieFaceGL(face);
            }

            // Internal Faces
            for (Axis norm: normals) {
                facesGL[i] = CubieFaceGL.internal(norm);
                i++;
            }
        } else {
            final int n = cubie.noOfFaces();
            facesGL = new CubieFaceGL[n];
            for (int i = 0; i < n; i++) {
                facesGL[i] = new CubieFaceGL(cubie.getFace(i));
            }
        }

        cubie.setListener(this);
    }

    @NotNull
    public final Cubie getCubie() {
        return cubie;
    }

    @Override
    public void draw(@NotNull PApplet p) {
        p.pushMatrix();

        final Point3DInt center = cubie.center();
        p.translate(center.x, center.y, center.z);
        for (CubieFaceGL faceGL: facesGL) {
            faceGL.draw(p);
        }

        p.popMatrix();
    }

    @Override
    public void onCubieRotated(@NotNull Axis around, int quarters) {
        for (int i = cubie.noOfFaces(); i < facesGL.length; i++) {
            facesGL[i].getCubieFace().rotate(around, quarters);
        }
    }
}
