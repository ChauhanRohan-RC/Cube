package model.cubie;

import model.Axis;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public class CubeLayer {

    @NotNull
    public final Axis normal;
    public final int absLayerIndex;
    @NotNull
    private final List<Cubie> cubies;

    public CubeLayer(@NotNull Axis normal, int absLayerIndex, @NotNull List<Cubie> cubies) {
        this.normal = normal;
        this.absLayerIndex = absLayerIndex;
        this.cubies = cubies;
    }

    public final void forEachCubie(@NotNull Consumer<Cubie> action) {
        for (Cubie qb: cubies) {
            action.accept(qb);
        }
    }


    @NotNull
    public final List<Cubie> getCubiesCopy() {
        return new LinkedList<>(cubies);
    }
}
