package model.cubie;

import model.Axis;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a face of the cube i.e ordered list of {@link Facelet}s
 * */
public class CubeFace {

    @NotNull
    public final Axis normal;

    @NotNull
    @Unmodifiable
    public final List<Facelet> faceletsUnmodifiable;

    public CubeFace(@NotNull Axis normal, @NotNull List<Facelet> facelets) {
        this.normal = normal;
        this.faceletsUnmodifiable = Collections.unmodifiableList(facelets);
    }

    @NotNull
    public String getRepresentation() {
        final StringBuilder sb = new StringBuilder();

        for (Facelet fc: faceletsUnmodifiable) {
            sb.append(fc.face.originalFaceChar());
        }

        return sb.toString();
    }

    @Override
    public String toString() {
      return getRepresentation();
    }
}
