package model.cube;

import gl.GLConfig;
import model.Axis;
import model.Point3DInt;
import model.cubie.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public interface CubeI {

    /* Faces */
    int FACE_INTERNAL = -1;
    int FACE_UP = Axis.Y.ordinal();
    int FACE_RIGHT = Axis.X.ordinal();
    int FACE_FRONT = Axis.Z.ordinal();
    int FACE_DOWN = Axis.Y_N.ordinal();
    int FACE_LEFT = Axis.X_N.ordinal();
    int FACE_BACK = Axis.Z_N.ordinal();

    int FACES_COUNT = Axis.size();
//    int[] FACE_CODES = { FACE_UP, FACE_RIGHT, FACE_FRONT, FACE_DOWN, FACE_LEFT, FACE_BACK }

    char FACE_CHAR_UP = Axis.Y.faceChar;
    char FACE_CHAR_RIGHT = Axis.X.faceChar;
    char FACE_CHAR_FRONT = Axis.Z.faceChar;
    char FACE_CHAR_DOWN = Axis.Y_N.faceChar;
    char FACE_CHAR_LEFT = Axis.X_N.faceChar;
    char FACE_CHAR_BACK = Axis.Z_N.faceChar;




    static int faceCode(@NotNull Axis axis) {
        return axis.ordinal();
    }

    static int faceCode(char faceChar) {
        final int fc;

        if (faceChar == FACE_CHAR_UP)  {
            fc = FACE_UP;
        } else if (faceChar == FACE_CHAR_RIGHT) {
            fc = FACE_RIGHT;
        } else if (faceChar == FACE_CHAR_FRONT) {
            fc = FACE_FRONT;
        } else if (faceChar == FACE_CHAR_DOWN) {
            fc = FACE_DOWN;
        } else if (faceChar == FACE_CHAR_LEFT) {
            fc = FACE_LEFT;
        } else if (faceChar == FACE_CHAR_BACK) {
            fc = FACE_BACK;
        } else {
            fc = FACE_INTERNAL;
        }

        return fc;
    }

    @NotNull
    static Axis axis(int faceCode) {
        return Axis.sharedValues()[faceCode];
    }


    @NotNull
    static Axis oppositeAxis(int faceCode) {
        return axis(faceCode).invert();
    }

    static int oppositeFaceCode(int faceCode) {
        return faceCode(oppositeAxis(faceCode));
    }

    static int totalCornerCubies(int n) {
        return 8;       // always 8
    }

    static int edgeCubiesInAFace(int n) {
        return 4 * (n - 2);
    }

    static int totalEdgeCubies(int n) {
        return 3 * edgeCubiesInAFace(n);
    }

    static int singleFaceCubiesInAFace(int n) {
        return (n - 2) * (n - 2);
    }

    static int totalSingleFaceCubies(int n) {
        return 6 * singleFaceCubiesInAFace(n);
    }

    static int totalCubies(int n) {
        return (6 * n * (n - 2)) + 8;
    }



    int DEFAULT_N = 3;
    int DEFAULT_MAX_N = 100;
    int COORDINATE_EXPANSION = 1;
    int CUBIE_HALF_SIDE_LEN = COORDINATE_EXPANSION;
    int CUBIE_SIDE_LEN = 2 * CUBIE_HALF_SIDE_LEN;

    int DEFAULT_SCRAMBLE_MOVES = 30;

    float CUBIE_STICKER_SIDE_LENGTH = CUBIE_SIDE_LEN * (1 - (2 * GLConfig.CUBIE_STICKER_STROKE_WEIGHT));
    float CUBIE_STICKER_CORNERS = CUBIE_SIDE_LEN * GLConfig.CUBIE_STICKER_CORNERS_RADIUS_WEIGHT;
    float CUBIE_STICKER_ELEVATION = CUBIE_SIDE_LEN * GLConfig.CUBIE_STICKER_ELEVATION_WEIGHT;

    /**
     * <pre>
     *     For <b>ABSOLUTE LAYER INDEX</b>, imagine n*n cube in a 3d world, with origin at down-left-back corner (DLB)
     *     and start counting from origin to +ve axis (0 for layer touching origin and n - 1 for end layer)
     * </pre>
     *
     * It transforms cube 3d world such that the cube will be centered at origin
     *
     * @param n  cube dimension
     * @param absLayerIndex absolute index of the layer [0, n)
     *
     * @return center coordinate of a cubie in that layer
     * */
    static int layerToCubieCenter(int n, int absLayerIndex /* [0, n) */) {
        return COORDINATE_EXPANSION * ((2 * absLayerIndex) - n + 1);
    }

    /**
     * @param n cube dimension
     * @param cubieCenter center of cubie
     *
     * @return absolute layer index [0, n) of the cubie
     * @see #layerToCubieCenter(int, int)
     * */
    static int cubieCenterToLayer(int n, int cubieCenter) {
        return ((cubieCenter / COORDINATE_EXPANSION) + n - 1) / 2;
    }


    /**
     * @see #layerToCubieCenter(int, int) for absolute layer index
     * */
    static int absLayerIndex(@NotNull Axis axis, int n, int layerIndexFromAxis) {
        return axis.isPositive()? (n - 1 - layerIndexFromAxis): layerIndexFromAxis;
    }

    static int relativeLayerIndex(@NotNull Axis axis, int n, int absLayerIndex) {
        return axis.isPositive()? (n - 1 - absLayerIndex): absLayerIndex;
    }

    @NotNull
    static int[] createAllLayers(int n) {
        final int[] layers = new int[n];

        for (int i=0; i < n; i++) {
            layers[i] = i;
        }

        return layers;
    }

    @NotNull
    static LinkedList<Move> createScrambleSequence(int n, int moves) {
        final LinkedList<Move> sequence = new LinkedList<>();
        final Random random = new Random();
        final List<Move> allMoves = Move.allMovesCopy(n, true);

        int counter = 0;
        Move prevMove = null;
        while (counter < moves) {
            Move move = allMoves.get(random.nextInt(allMoves.size()));
            if (prevMove == null || !prevMove.equalsIgnoreQuarters(move, n)) {
                sequence.add(move);
                prevMove = move;
                counter++;
            }
        }

        return sequence;
    }

    @NotNull
    static Cubie[] createSolvedState(int n) {
        final Cubie[] cubies = new Cubie[totalCubies(n)];
        final int cmin = layerToCubieCenter(n, 0);
        final int cmax = layerToCubieCenter(n, n - 1);

        final CubieFace up = new CubieFace(FACE_UP, Axis.Y);
        final CubieFace down = new CubieFace(FACE_DOWN, Axis.Y_N);
        final CubieFace right = new CubieFace(FACE_RIGHT, Axis.X);
        final CubieFace left = new CubieFace(FACE_LEFT, Axis.X_N);
        final CubieFace front = new CubieFace(FACE_FRONT, Axis.Z);
        final CubieFace back = new CubieFace(FACE_BACK, Axis.Z_N);

        /* Corner Cubies */
        // 1. Top
        cubies[0] = new CornerCubie(n, new Point3DInt(cmin, cmax, cmin), up.copy(), left.copy(), back.copy());
        cubies[1] = new CornerCubie(n, new Point3DInt(cmax, cmax, cmin), up.copy(), right.copy(), back.copy());
        cubies[2] = new CornerCubie(n, new Point3DInt(cmax, cmax, cmax), up.copy(), right.copy(), front.copy());
        cubies[3] = new CornerCubie(n, new Point3DInt(cmin, cmax, cmax), up.copy(), left.copy(), front.copy());

        // 2. Bottom
        cubies[4] = new CornerCubie(n, new Point3DInt(cmin, cmin, cmin), down.copy(), left.copy(), back.copy());
        cubies[5] = new CornerCubie(n, new Point3DInt(cmax, cmin, cmin), down.copy(), right.copy(), back.copy());
        cubies[6] = new CornerCubie(n, new Point3DInt(cmax, cmin, cmax), down.copy(), right.copy(), front.copy());
        cubies[7] = new CornerCubie(n, new Point3DInt(cmin, cmin, cmax), down.copy(), left.copy(), front.copy());

        int counter = 8;

        for (int i=1; i < n - 1; i++) {
            final int cc = layerToCubieCenter(n, i);

            /* Edge Cubies */
            // 1. x - variation
            cubies[counter] = new EdgeCubie(n, new Point3DInt(cc, cmax, cmin), up.copy(), back.copy());              // Up=Back edges
            cubies[counter + 1] = new EdgeCubie(n, new Point3DInt(cc, cmin, cmin), down.copy(), back.copy());        // Down-Back edges
            cubies[counter + 2] = new EdgeCubie(n, new Point3DInt(cc, cmin, cmax), front.copy(), down.copy());       // Front-Down edges
            cubies[counter + 3] = new EdgeCubie(n, new Point3DInt(cc, cmax, cmax), front.copy(), up.copy());         // Front-Up edges

            // 2. y - variation
            cubies[counter + 4] = new EdgeCubie(n, new Point3DInt(cmin, cc, cmin), left.copy(), back.copy());        // Left-Back edges
            cubies[counter + 5] = new EdgeCubie(n, new Point3DInt(cmax, cc, cmin), right.copy(), back.copy());       // Right-Left edges
            cubies[counter + 6] = new EdgeCubie(n, new Point3DInt(cmax, cc, cmax), right.copy(), front.copy());      // Right-Front edges
            cubies[counter + 7] = new EdgeCubie(n, new Point3DInt(cmin, cc, cmax), left.copy(), front.copy());       // Left-Front edges

            // 2. Z - variation
            cubies[counter + 8] = new EdgeCubie(n, new Point3DInt(cmin, cmax, cc), up.copy(), left.copy());          // Up-Left edges
            cubies[counter + 9] = new EdgeCubie(n, new Point3DInt(cmin, cmin, cc), down.copy(), left.copy());        // Down-Left edges
            cubies[counter + 10] = new EdgeCubie(n, new Point3DInt(cmax, cmin, cc), right.copy(), down.copy());      // Down-Right edges
            cubies[counter + 11] = new EdgeCubie(n, new Point3DInt(cmax, cmax, cc), right.copy(), up.copy());        // Up-Right edges
            counter += 12;

            /* SingleFace Cubies */
            for (int j=1; j < n - 1; j++) {
                final int cc2 = layerToCubieCenter(n, j);

                cubies[counter] = new SingleFaceCubie(n, new Point3DInt(cc2, cmax, cc), up.copy());            // UP
                cubies[counter + 1] = new SingleFaceCubie(n, new Point3DInt(cmax, cc, cc2), right.copy());     // RIGHT
                cubies[counter + 2] = new SingleFaceCubie(n, new Point3DInt(cc2, cc, cmax), front.copy());     // FRONT
                cubies[counter + 3] = new SingleFaceCubie(n, new Point3DInt(cc2, cmin, cc), down.copy());      // DOWN
                cubies[counter + 4] = new SingleFaceCubie(n, new Point3DInt(cmin, cc, cc2), left.copy());      // LEFT
                cubies[counter + 5] = new SingleFaceCubie(n, new Point3DInt(cc2, cc, cmin), back.copy());      // BACK
                counter += 6;
            }
        }

        return cubies;
    }

    static int calculateHeuristic(@NotNull CubeI cube) {
        int h = 0;

        for (int i=0; i < cube.noOfCubies(); i++) {
            h += cube.getCubie(i).calculateHeuristic();
        }

        return h;
    }

    static boolean calculateIsSolved(@NotNull CubeI cube) {
        // better than checking heuristic == 0

        for (int i=0; i < cube.noOfCubies(); i++) {
            if (!cube.getCubie(i).allFacesAtOriginalFaces())
                return false;
        }

        return true;
    }

    @NotNull
    static Predicate<Cubie> layerFilter(@NotNull Axis normal, int n, int absLayerIndex) {
        final int cc = layerToCubieCenter(n, absLayerIndex);

        if (normal.isX()) {
            return qb -> qb.center().x == cc;
        }

        if (normal.isY()) {
            return qb -> qb.center().y == cc;
        }

        return qb -> qb.center().z == cc;
    }

//    @NotNull
//    static Predicate<Cubie> layerFilter(@NotNull Move move, int n) {
//        return layerFilter(move.axis, n, move.absLayerIndex(n));
//    }

    @NotNull
    static CubeLayer queryLayer(@NotNull CubeI cube, @NotNull Axis normal, int absLayerIndex) {
        final Predicate<Cubie> filter = layerFilter(normal, cube.n(), absLayerIndex);
        final List<Cubie> qbs = new LinkedList<>();

        cube.forEachCubie(qb -> {
            if (filter.test(qb)) {
                qbs.add(qb);
            }
        });

        return new CubeLayer(normal, absLayerIndex, qbs);
    }

//    @NotNull
//    static CubeLayer queryLayer(@NotNull CubeI cube, @NotNull Move move) {
//        return queryLayer(cube, move.axis, move.absLayerIndex(cube.n()));
//    }






    int n();

    int noOfCubies();

    @NotNull
    Cubie getCubie(int index);

    default void forEachCubie(@NotNull Consumer<Cubie> action) {
        for (int i=0; i < noOfCubies(); i++) {
            action.accept(getCubie(i));
        }
    }

    @NotNull
    default Cubie[] copyState() {
        final Cubie[] copy = new Cubie[noOfCubies()];
        for (int i=0; i < copy.length; i++) {
            copy[i] = getCubie(i).copy();
        }

        return copy;
    }

    default int layerToCubieCenter(int layerIndex) {
        return layerToCubieCenter(n(), layerIndex);
    }

    default int cubieCenterToLayer(int cubieCenter) {
        return cubieCenterToLayer(n(), cubieCenter);
    }

    default int absLayerIndex(@NotNull Axis axis, int layerIndexFromAxis) {
        return absLayerIndex(axis, n(), layerIndexFromAxis);
    }

    default int relativeLayerIndex(@NotNull Axis axis, int absLayerIndex) {
        return relativeLayerIndex(axis, n(), absLayerIndex);
    }

    @NotNull
    default int[] allLayersShared() {
        return createAllLayers(n());
    }

    @NotNull
    default Move rotateXMove(int quarters) {
        return Move.r(quarters, allLayersShared());
    }

    @NotNull
    default Move rotateYMove(int quarters) {
        return Move.u(quarters, allLayersShared());
    }

    @NotNull
    default Move rotateZMove(int quarters) {
        return Move.f(quarters, allLayersShared());
    }

    @NotNull
    default LinkedList<Move> createScrambleSequence(int moves) {
        return createScrambleSequence(n(), moves);
    }

    @NotNull
    default LinkedList<Move> createScrambleSequence() {
        return createScrambleSequence(DEFAULT_SCRAMBLE_MOVES);
    }

    default int calculateHeuristic() {
        return calculateHeuristic(this);
    }

    default boolean calculateIsSolved() {
        return calculateIsSolved(this);
    }

    default int cacheHeuristic() {
        return calculateHeuristic();
    }

    default boolean cacheIsSolved() {
        return calculateIsSolved();
    }


    @NotNull
    default Predicate<Cubie> layerFilter(@NotNull Axis normal, int absLayerIndex) {
        return layerFilter(normal, n(), absLayerIndex);
    }

//    @NotNull
//    default Predicate<Cubie> layerFilter(@NotNull Move move) {
//        return layerFilter(move, n());
//    }

    @NotNull
    default CubeLayer queryLayer(@NotNull Axis normal, int absLayerIndex) {
        return queryLayer(this, normal, absLayerIndex);
    }

//    @NotNull
//    default CubeLayer queryLayer(@NotNull Move move) {
//        return queryLayer(this, move);
//    }

    default boolean equals(@NotNull CubeI o) {
        if (!(n() == o.n() && noOfCubies() == o.noOfCubies()))
            return false;

        for (int i=0; i < noOfCubies(); i++) {
            if (!getCubie(i).equals(o.getCubie(i)))
                return false;
        }

        return true;
    }

    default int cubiesHash() {
        if (noOfCubies() == 0)
            return 0;

        int h = 1;
        for (int i=0; i < noOfCubies(); i++) {
            h = 31 * h + getCubie(i).hashCode();
        }

        return h;
    }

    default int hashImpl() {
        return 31 * n() + cubiesHash();
    }




    /* Representation */

    class MutableCubeFace {

        @NotNull
        private final Axis normal;

        @NotNull
        private final ArrayList<Facelet> facelets;

        private MutableCubeFace(@NotNull Axis normal, int size) {
            this.normal = normal;
            facelets = new ArrayList<>(size + 1);
        }

        private void add(@NotNull Facelet facelet) {
            facelets.add(facelet);
        }

        private void sort(@NotNull Comparator<Facelet> comparator) {
            facelets.sort(comparator);
        }

        @NotNull
        private CubeFace toCubeFace() {
            return new CubeFace(normal, facelets);
        }
    }


    /**
     * Queries all faces of this cube
     * if sorting is enabled, then facelets in each face will be sorted according to this 2D representation
     *
     * <pre>
     *      The names of the facelet positions of the cube (3 by 3, other dimensional cubes will be analogous)
     *                  |************|
     *                  |*U1**U2**U3*|
     *                  |************|
     *                  |*U4**U5**U6*|
     *                  |************|
     *                  |*U7**U8**U9*|
     *                  |************|
     *      ************|************|************|************|
     *      *L1**L2**L3*|*F1**F2**F3*|*R1**R2**F3*|*B1**B2**B3*|
     *      ************|************|************|************|
     *      *L4**L5**L6*|*F4**F5**F6*|*R4**R5**R6*|*B4**B5**B6*|
     *      ************|************|************|************|
     *      *L7**L8**L9*|*F7**F8**F9*|*R7**R8**R9*|*B7**B8**B9*|
     *      ************|************|************|************|
     *                  |************|
     *                  |*D1**D2**D3*|
     *                  |************|
     *                  |*D4**D5**D6*|
     *                  |************|
     *                  |*D7**D8**D9*|
     *                  |************|
     *      </pre>
     *
     * @param sortFacelets whether to sort facelets in faces
     * @return array containing {@link #FACES_COUNT} no of faces, each face indexed as defined by face constant {@link #FACE_UP}, {@link #FACE_RIGHT} etc
     * */
    @NotNull
    default CubeFace[] getCubeFaces(boolean sortFacelets) {
        final MutableCubeFace[] mutFaces = new MutableCubeFace[FACES_COUNT];

        final int n_2 = n() * n();
        for (int i=0; i < FACES_COUNT; i++) {
            mutFaces[i] = new MutableCubeFace(axis(i), n_2);
        }

        forEachCubie(qb -> {
            final Point3DInt layers = qb.layers();
            qb.forEachFace(face -> mutFaces[face.currentFaceCode()].add(new Facelet(face, layers)));
        });

        final CubeFace[] faces = new CubeFace[FACES_COUNT];

        if (sortFacelets) {
            // up face
            final MutableCubeFace up =  mutFaces[FACE_UP];
            up.sort(Facelet::compareZAscXAsc);
            faces[FACE_UP] = up.toCubeFace();

            // Down face
            final MutableCubeFace down =  mutFaces[FACE_DOWN];
            down.sort(Facelet::compareZDescXAsc);
            faces[FACE_DOWN] = down.toCubeFace();

            // Front
            final MutableCubeFace front =  mutFaces[FACE_FRONT];
            front.sort(Facelet::compareYDescXAsc);
            faces[FACE_FRONT] = front.toCubeFace();

            // Back
            final MutableCubeFace back =  mutFaces[FACE_BACK];
            back.sort(Facelet::compareYDescXDesc);
            faces[FACE_BACK] = back.toCubeFace();

            // right
            final MutableCubeFace right =  mutFaces[FACE_RIGHT];
            right.sort(Facelet::compareYDescZDesc);
            faces[FACE_RIGHT] = right.toCubeFace();

            // left
            final MutableCubeFace left =  mutFaces[FACE_LEFT];
            left.sort(Facelet::compareYDescZAsc);
            faces[FACE_LEFT] = left.toCubeFace();
        } else {
            for (int i=0; i < FACES_COUNT; i++) {
                faces[i] = mutFaces[i].toCubeFace();
            }
        }

        return faces;
    }

    @NotNull
    default CubeFace[] getCubeFaces() {
        return getCubeFaces(true);
    }


    /**
     * @see #getCubeFaces(boolean)
     * */
    @NotNull
    default String representation2D() {
        final CubeFace[] faces = getCubeFaces(true);

        return  faces[FACE_UP].toString() +
                faces[FACE_RIGHT].toString() +
                faces[FACE_FRONT].toString() +
                faces[FACE_DOWN].toString() +
                faces[FACE_LEFT].toString() +
                faces[FACE_BACK].toString();
    }

}
