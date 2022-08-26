package model.cubie;

import model.Axis;
import model.cube.CubeI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import util.CollectionUtil;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class Move {

    @Unmodifiable
    public static final int[] NO_LAYERS = {};
    @Unmodifiable
    public static final int[] LAYERS_0 = { 0 };
    @Unmodifiable
    public static final int[] LAYERS_1 = { 1 };
    @Unmodifiable
    public static final int[] LAYERS_0_1 = { 0, 1 };

    /* Quarters */

    public static final int QUARTERS_CLOCKWISE = -1;
    public static final int QUARTERS_ANTICLOCKWISE = 1;
    public static final int QUARTERS_2 = 2;

    public static final int[] POSSIBLE_QUARTERS = {
            QUARTERS_CLOCKWISE,
            QUARTERS_ANTICLOCKWISE,
            QUARTERS_2
    };

    /**
     * @return -1, 0, 1, 2
     * */
    public static int normalizeQuarters(int quarters) {
        int absq = Math.abs(quarters);
        if (absq > 3) {
            quarters %= 4;
            absq = Math.abs(quarters);
        }

        if (absq == 3) {
            quarters = -Integer.signum(quarters);
        } else if (absq == 2) {
            quarters = QUARTERS_2;
        }

        return quarters;
    }

    private static int reverseNormalizedQuarters(int normalizedQuerters) {
        return normalizedQuerters == QUARTERS_2? QUARTERS_2: -normalizedQuerters;
    }



    /* Moves */

//    public static final String LEFT_CLOCKWISE = "l";
//    public static final String LEFT_ANTICLOCKWISE = "L";
//    public static final String LEFT_CLOCKWISE_2 = "ll";
//    public static final String LEFT_ANTICLOCKWISE_2 = "LL";
//
//    public static final String RIGHT_CLOCKWISE = "r";
//    public static final String RIGHT_ANTICLOCKWISE = "R";
//    public static final String RIGHT_CLOCKWISE_2 = "rr";
//    public static final String RIGHT_ANTICLOCKWISE_2 = "RR";
//
//    public static final String UP_CLOCKWISE = "u";
//    public static final String UP_ANTICLOCKWISE = "U";
//    public static final String UP_CLOCKWISE_2 = "uu";
//    public static final String UP_ANTICLOCKWISE_2 = "UU";
//
//    public static final String DOWN_CLOCKWISE = "d";
//    public static final String DOWN_ANTICLOCKWISE = "D";
//    public static final String DOWN_CLOCKWISE_2 = "dd";
//    public static final String DOWN_ANTICLOCKWISE_2 = "DD";
//
//    public static final String FRONT_CLOCKWISE = "f";
//    public static final String FRONT_ANTICLOCKWISE = "F";
//    public static final String FRONT_CLOCKWISE_2 = "ff";
//    public static final String FRONT_ANTICLOCKWISE_2 = "FF";
//
//    public static final String BACK_CLOCKWISE = "b";
//    public static final String BACK_ANTICLOCKWISE = "B";
//    public static final String BACK_CLOCKWISE_2 = "bb";
//    public static final String BACK_ANTICLOCKWISE_2 = "BB";

    public static final boolean COMMAND_ONLY_UPPER_CASE = true;
    public static final char[] COMMAND_CHARS = { 'U', 'R', 'F', 'D', 'L', 'B', '\''};

    public static final String LEFT_CLOCKWISE = "L";
    public static final String LEFT_ANTICLOCKWISE = "L'";
    public static final String LEFT_CLOCKWISE_2 = "LL";
    public static final String LEFT_ANTICLOCKWISE_2 = "LL";

    public static final String RIGHT_CLOCKWISE = "R";
    public static final String RIGHT_ANTICLOCKWISE = "R'";
    public static final String RIGHT_CLOCKWISE_2 = "RR";
    public static final String RIGHT_ANTICLOCKWISE_2 = "RR";

    public static final String UP_CLOCKWISE = "U";
    public static final String UP_ANTICLOCKWISE = "U'";
    public static final String UP_CLOCKWISE_2 = "UU";
    public static final String UP_ANTICLOCKWISE_2 = "UU";

    public static final String DOWN_CLOCKWISE = "D";
    public static final String DOWN_ANTICLOCKWISE = "D'";
    public static final String DOWN_CLOCKWISE_2 = "DD";
    public static final String DOWN_ANTICLOCKWISE_2 = "DD";

    public static final String FRONT_CLOCKWISE = "F";
    public static final String FRONT_ANTICLOCKWISE = "F'";
    public static final String FRONT_CLOCKWISE_2 = "FF";
    public static final String FRONT_ANTICLOCKWISE_2 = "FF";

    public static final String BACK_CLOCKWISE = "B";
    public static final String BACK_ANTICLOCKWISE = "B'";
    public static final String BACK_CLOCKWISE_2 = "BB";
    public static final String BACK_ANTICLOCKWISE_2 = "BB";


    @Nullable
    @Unmodifiable
    private static List<Move> sMaxNAllMoves;

    /**
     * Maps move command to respective moves
     *
     * <pre>MOVES IN THIS MAP DO <b>NOT</b> HAVE LAYERS ASSIGNED. use {@link #withLayers(int[])} to use mapped moves</pre>
     * */
    @Nullable
    @Unmodifiable
    private static Map<String, Move> sMovesMap;

    @NotNull
    private static Map<String, Move> createMovesMap() {
        final Map<String, Move> m = new HashMap<>();

        // UP
        m.put(UP_CLOCKWISE, u());
        m.put(UP_ANTICLOCKWISE, up());
        final Move u2 = u2();
        m.put(UP_CLOCKWISE_2, u2);
        if (!UP_ANTICLOCKWISE_2.equals(UP_CLOCKWISE_2)) {
            m.put(UP_ANTICLOCKWISE_2, u2);
        }

        // RIGHT
        m.put(RIGHT_CLOCKWISE, r());
        m.put(RIGHT_ANTICLOCKWISE, rp());
        final Move r2 = r2();
        m.put(RIGHT_CLOCKWISE_2, r2);
        if (!RIGHT_ANTICLOCKWISE_2.equals(RIGHT_CLOCKWISE_2)) {
            m.put(RIGHT_ANTICLOCKWISE_2, r2);
        }

        // FRONT
        m.put(FRONT_CLOCKWISE, f());
        m.put(FRONT_ANTICLOCKWISE, fp());
        final Move f2 = f2();
        m.put(FRONT_CLOCKWISE_2, f2);
        if (!FRONT_ANTICLOCKWISE_2.equals(FRONT_CLOCKWISE_2)) {
            m.put(FRONT_ANTICLOCKWISE_2, f2);
        }

        // DOWN
        m.put(DOWN_CLOCKWISE, d());
        m.put(DOWN_ANTICLOCKWISE, dp());
        final Move d2 = d2();
        m.put(DOWN_CLOCKWISE_2, d2);
        if (!DOWN_ANTICLOCKWISE_2.equals(DOWN_CLOCKWISE_2)) {
            m.put(DOWN_ANTICLOCKWISE_2, d2);
        }

        // LEFT
        m.put(LEFT_CLOCKWISE, l());
        m.put(LEFT_ANTICLOCKWISE, lp());
        final Move l2 = l2();
        m.put(LEFT_CLOCKWISE_2, l2);
        if (!LEFT_ANTICLOCKWISE_2.equals(LEFT_CLOCKWISE_2)) {
            m.put(LEFT_ANTICLOCKWISE_2, l2);
        }

        // BACK
        m.put(BACK_CLOCKWISE, b());
        m.put(BACK_ANTICLOCKWISE, bp());
        final Move b2 = b2();
        m.put(BACK_CLOCKWISE_2, b2);
        if (!BACK_ANTICLOCKWISE_2.equals(BACK_CLOCKWISE_2)) {
            m.put(BACK_ANTICLOCKWISE_2, b2);
        }

        return m;
    }

    @NotNull
    @Unmodifiable
    private static Map<String, Move> getMovesMap() {
        if (sMovesMap == null) {
            sMovesMap = Collections.unmodifiableMap(createMovesMap());
        }

        return sMovesMap;
    }

    public static boolean isCommandChar(char c) {
        for (char _c: COMMAND_CHARS) {
            if (_c == c)
                return true;
        }

        return false;
    }


    public static boolean isValidCommand(@NotNull String command) {
        return getMovesMap().containsKey(command);
    }

    public static boolean isValidCommand(char command) {
        return isValidCommand(String.valueOf(command));
    }

    /**
     * @return true if the given string is starting part of any move command
     * */
    public static boolean isValidCommandStart(@NotNull String commandStart) {
        final Map<String, Move> map = getMovesMap();
        if (map.containsKey(commandStart))
            return true;

        for (String key: map.keySet()) {
            if (key.startsWith(commandStart))
                return true;
        }

        return false;
    }

    /**
     * @see #isValidCommandStart(String)
     * */
    public static boolean isValidCommandStart(char commandStart) {
        return isValidCommandStart(String.valueOf(commandStart));
    }


    @Nullable
    public static Move fromCommand(@NotNull String command, int relLayerIndex) {
        final Move move = getMovesMap().get(command);
        return move != null? move.withLayers(relLayerIndex): null;
    }

    @Nullable
    public static Move fromCommand(@NotNull String command) {
        return fromCommand(command, 0);
    }




    /* delimiters should be totally exclusive of each others */
    public static final String SEQUENCE_DELIMITER = " ";

    private static final String MULTI_LAYERS_PREFIX = "[";
    private static final String MULTI_LAYERS_SUFFIX = "]";
    private static final String MULTI_LAYERS_DELIMITER = ",";

    /**
     * Exception indicating parsing failed
     * */
    public static class ParseException extends IllegalArgumentException {

        public ParseException() {
            super();
        }

        public ParseException(String s) {
            super(s);
        }

        public ParseException(String message, Throwable cause) {
            super(message, cause);
        }

        public ParseException(Throwable cause) {
            super(cause);
        }
    }

    @NotNull
    public static String layersToString(@Nullable int[] layers) {
        if (layers == null || layers.length == 0)
            return "";

        if (layers.length == 1) {
            return String.valueOf(layers[0]);
        }

        final StringJoiner sj = new StringJoiner(MULTI_LAYERS_DELIMITER, MULTI_LAYERS_PREFIX, MULTI_LAYERS_SUFFIX);
        for (int layer: layers) {
            sj.add(String.valueOf(layer));
        }

        return sj.toString();
    }


    @NotNull
    public static int[] parseLayers(@Nullable String layers) throws ParseException {
        if (layers == null || layers.isEmpty())
            return NO_LAYERS;

        if (layers.startsWith(MULTI_LAYERS_PREFIX) && layers.endsWith(MULTI_LAYERS_SUFFIX)) {
            try {
                final String[] splits = layers.substring(MULTI_LAYERS_PREFIX.length(), layers.length() - MULTI_LAYERS_SUFFIX.length()).split(MULTI_LAYERS_DELIMITER);
                final int[] arr = new int[splits.length];
                for (int i=0; i < splits.length; i++) {
                    arr[i] = Integer.parseInt(splits[i]);
                }

                return arr;
            } catch (Exception exc) {
                throw new ParseException("failed to parse layers from <" + layers + ">");
            }
        }

        try {
            final int layer = Integer.parseInt(layers);
            return new int[] { layer };
        } catch (NumberFormatException ignored) {
            throw new ParseException("failed to parse layers from <" + layers + ">");
        }
    }


    /**
     * Converts a Move to string representation
     *
     * ex r1 -> rotate 2nd layer (index 1) from right clockwise
     * */
    @NotNull
    public static String toString(@NotNull Move move) throws IllegalArgumentException {
        for (Map.Entry<String, Move> entry: getMovesMap().entrySet()) {
            final Move mv = entry.getValue();
            // equal ignore layers
            if (move.axis == mv.axis && move.normalizedQuarters == mv.normalizedQuarters) {
                final String command = entry.getKey();

                final int[] layerIndices = move.relLayerIndices;
                final String layers = layerIndices.length == 0 || (layerIndices.length == 1 && layerIndices[0] == 0)? "": layersToString(move.relLayerIndices);
                return command + layers;
            }
        }

        throw new IllegalArgumentException("Invalid move");
    }


    /**
     * creates a moves from string, literally inverse of {@link #toString(Move)}
     * */
    @NotNull
    public static Move parseMove(String move) throws ParseException {
        final int len;
        if (move == null || (len = move.length()) == 0)
            throw new ParseException("Move is empty!");

        if (COMMAND_ONLY_UPPER_CASE) {
            move = move.toUpperCase();
        }

        // command
        String com;
        int layersStart = move.indexOf(MULTI_LAYERS_PREFIX);
        if (layersStart != -1) {
            com = move.substring(0, layersStart);
        } else {
            final StringBuilder comB = new StringBuilder(len);
            int i = 0;
            for (; i < len; i++) {
                char c = move.charAt(i);
                if (isCommandChar(c)) {
                    comB.append(c);
                } else {
                    break;
                }
            }

            layersStart = i;
            com = comB.toString();
        }

        if (com.isEmpty())
            throw new ParseException("Move <"+ move + "> has no command!");

        final Move m = getMovesMap().get(com);
        if (m == null)
            throw new ParseException("Move <"+ move + "> has invalid command <" + com + "> !");

        final int[] layers = parseLayers(move.substring(layersStart));
        return m.withLayers(layers);
    }


    /**
     * @return sequence of moves string representation, separated by {@link #SEQUENCE_DELIMITER}
     * */
    @NotNull
    public static String sequence(@Nullable List<Move> moves) {
        if (CollectionUtil.isEmpty(moves))
            return "";

        final StringJoiner sj = new StringJoiner(SEQUENCE_DELIMITER);

        for (Move m: moves) {
            sj.add(toString(m));
        }

        return sj.toString();
    }

    /**
     * @param sequence sequence of moves separated by {@link #SEQUENCE_DELIMITER}
     * @return list of moves, or {@code null} if sequence is empty
     *
     * @throws ParseException if sequence is invalid
     * @see #parseMove(String)
     * */
    @Nullable
    public static ArrayList<Move> parseSequence(String sequence) throws ParseException {
        if (sequence == null || sequence.isEmpty())
            return null;

        final String[] moves_repr = sequence.split(SEQUENCE_DELIMITER);
        final ArrayList<Move> moves = new ArrayList<>(moves_repr.length + 2);

        for (String repr: moves_repr) {
            moves.add(parseMove(repr));                 //       throws ParseException
        }

        return moves;
    }



    private static void createAllMoves(int startN, int endN, @NotNull Consumer<Move> consumer) {       // Total (POSSIBLE_QUARTERS.length * Axis.negativesSize * n) possible moves (mostly 9n)
        for (int i = startN; i < endN; i++) {
            for (Axis a: Axis.sharedNegatives()) {                   // TODO Should also include +ve axis
                for (int q: POSSIBLE_QUARTERS) {
                    consumer.accept(new Move(a, q, new int[] { i }));
                }
            }
        }
    }

    @NotNull
    private static List<Move> createAllMoves(int n) {
        final List<Move> allMoves = new LinkedList<>();
        createAllMoves(0, n, allMoves::add);
        return allMoves;
    }

    @NotNull
    @Unmodifiable
    public static List<Move> allMovesUnmodifiable(int n) {
        final List<Move> max = sMaxNAllMoves;

        if (max == null) {
            final List<Move> l = Collections.synchronizedList(createAllMoves(n));
            sMaxNAllMoves = l;
            return Collections.unmodifiableList(l);
        }

        final int f = POSSIBLE_QUARTERS.length * Axis.negativesSize();
        final int maxN = max.size() / f;
        if (n < maxN) {
            return Collections.unmodifiableList(max.subList(0, f * n));
        }

        if (n > maxN) {
            createAllMoves(maxN, n, max::add);
        }

        return Collections.unmodifiableList(max);
    }


    @NotNull
    @Unmodifiable
    public static List<Move> allMovesCopy(int n, boolean randomAccess) {
        final List<Move> moves = allMovesUnmodifiable(n);
        return randomAccess? CollectionUtil.arrayListCopy(moves): CollectionUtil.linkedListCopy(moves);
    }



    public enum Commutativity {
        NONE,
        C_12,
        C_21,
        EQUAL
    }

    @NotNull
    public static Commutativity getCommutativity(@NotNull Move one, @NotNull Move two, int n) {
        final boolean eq;
        if (one.layersCount() > 1 || two.layersCount() > 1 || !((eq = one.axis == two.axis) || one.axis == two.axis.invert())) {
            return Commutativity.NONE;
        }

        final int absL, absLo;

        if (eq) {
            absL = one.primaryRelLayer();
            absLo = two.primaryRelLayer();
        } else {
            absL = one.primaryAbsLayer(n);
            absLo = two.primaryAbsLayer(n);
        }

        if (absL < absLo)
            return Commutativity.C_12;
        if (absL > absLo)
            return Commutativity.C_21;

        if (one.normalizedQuarters < two.normalizedQuarters)
            return Commutativity.C_12;
        if (one.normalizedQuarters > two.normalizedQuarters)
            return Commutativity.C_21;
        return Commutativity.EQUAL;
    }






    /* RIGHT */

    @NotNull
    public static Move r(int quarters, int[] layersFromX) {
        return new Move(Axis.X, quarters, layersFromX);
    }

    @NotNull
    public static Move r(int[] layersFromX) {
        return r(QUARTERS_CLOCKWISE, layersFromX);
    }

    @NotNull
    public static Move r() {
        return r(LAYERS_0);
    }

    @NotNull
    public static Move rp(int[] layersFromX) {
        return r(QUARTERS_ANTICLOCKWISE, layersFromX);
    }

    @NotNull
    public static Move rp() {
        return rp(LAYERS_0);
    }

    @NotNull
    public static Move r2(int[] layersFromX) {
        return r(QUARTERS_2, layersFromX);
    }

    @NotNull
    public static Move r2() {
        return r2(LAYERS_0);
    }



    /* LEFT */

    @NotNull
    public static Move l(int quarters, int[] layers) {
        return new Move(Axis.X_N, quarters, layers);
    }

    @NotNull
    public static Move l(int[] layers) {
        return l(QUARTERS_CLOCKWISE, layers);
    }

    @NotNull
    public static Move l() {
        return l(LAYERS_0);
    }

    @NotNull
    public static Move lp(int[] layers) {
        return l(QUARTERS_ANTICLOCKWISE, layers);
    }

    @NotNull
    public static Move lp() {
        return lp(LAYERS_0);
    }

    @NotNull
    public static Move l2(int[] layers) {
        return l(QUARTERS_2, layers);
    }

    @NotNull
    public static Move l2() {
        return l2(LAYERS_0);
    }



    /* UP */

    @NotNull
    public static Move u(int quarters, int[] layersFromY) {
        return new Move(Axis.Y, quarters, layersFromY);
    }

    @NotNull
    public static Move u(int[] layersFromY) {
        return u(QUARTERS_CLOCKWISE, layersFromY);
    }

    @NotNull
    public static Move u() {
        return u(LAYERS_0);
    }

    @NotNull
    public static Move up(int[] layersFromY) {
        return u(QUARTERS_ANTICLOCKWISE, layersFromY);
    }

    @NotNull
    public static Move up() {
        return up(LAYERS_0);
    }

    @NotNull
    public static Move u2(int[] layersFromY) {
        return u(QUARTERS_2, layersFromY);
    }

    @NotNull
    public static Move u2() {
        return u2(LAYERS_0);
    }



    /* DOWN */

    @NotNull
    public static Move d(int quarters, int[] layers) {
        return new Move(Axis.Y_N, quarters, layers);
    }

    @NotNull
    public static Move d(int[] layers) {
        return d(QUARTERS_CLOCKWISE, layers);
    }

    @NotNull
    public static Move d() {
        return d(LAYERS_0);
    }

    @NotNull
    public static Move dp(int[] layers) {
        return d(QUARTERS_ANTICLOCKWISE, layers);
    }

    @NotNull
    public static Move dp() {
        return dp(LAYERS_0);
    }

    @NotNull
    public static Move d2(int[] layers) {
        return d(QUARTERS_2, layers);
    }

    @NotNull
    public static Move d2() {
        return d2(LAYERS_0);
    }



    /* FRONT */

    @NotNull
    public static Move f(int quarters, int[] layersFromZ) {
        return new Move(Axis.Z, quarters, layersFromZ);
    }

    @NotNull
    public static Move f(int[] layersFromZ) {
        return f(QUARTERS_CLOCKWISE, layersFromZ);
    }

    @NotNull
    public static Move f() {
        return f(LAYERS_0);
    }

    @NotNull
    public static Move fp(int[] layersFromZ) {
        return f(QUARTERS_ANTICLOCKWISE, layersFromZ);
    }

    @NotNull
    public static Move fp() {
        return fp(LAYERS_0);
    }

    @NotNull
    public static Move f2(int[] layersFromZ) {
        return f(QUARTERS_2, layersFromZ);
    }

    @NotNull
    public static Move f2() {
        return f2(LAYERS_0);
    }



    /* BACK */

    @NotNull
    public static Move b(int quarters, int[] layers) {
        return new Move(Axis.Z_N, quarters, layers);
    }

    @NotNull
    public static Move b(int[] layers) {
        return b(QUARTERS_CLOCKWISE, layers);
    }

    @NotNull
    public static Move b() {
        return b(LAYERS_0);
    }

    @NotNull
    public static Move bp(int[] layers) {
        return b(QUARTERS_ANTICLOCKWISE, layers);
    }

    @NotNull
    public static Move bp() {
        return bp(LAYERS_0);
    }

    @NotNull
    public static Move b2(int[] layers) {
        return b(QUARTERS_2, layers);
    }

    @NotNull
    public static Move b2() {
        return b2(LAYERS_0);
    }






    @NotNull
    public final Axis axis;
    public final int quarters;                             // + ve for anticlockwise, -ve for clockwise
    public final int normalizedQuarters;                    // normalized quarters
    @NotNull
    private final int[] relLayerIndices;                    // Relative Layer Index from axis

    @Nullable
    private String mStrRepr;        // String Representation

    public Move(@NotNull Axis axis, int quarters, int[] relLayerIndices) {
        this.axis = axis;
        this.quarters = quarters;
        this.normalizedQuarters = normalizeQuarters(quarters);

        if (relLayerIndices == null)
            relLayerIndices = NO_LAYERS;
        this.relLayerIndices = relLayerIndices;
    }

    @NotNull
    public Move withAxis(@NotNull Axis axis) {
        return new Move(axis, quarters, relLayerIndices);
    }

    @NotNull
    public Move withLayers(int... relLayerIndices) {
        return new Move(axis, quarters, relLayerIndices);
    }

    @NotNull
    public Move withQuarters(int quarters) {
        return new Move(axis, quarters, relLayerIndices);
    }

    @NotNull
    public Move reverse() {
        return withQuarters(-quarters);
    }

    public int layersCount() {
        return relLayerIndices.length;
    }

    public int relLayerAt(int index) {
        return relLayerIndices[index];
    }

    public int primaryRelLayer() {
        return relLayerIndices.length == 0? 0: relLayerIndices[0];
    }

    public int absLayerAt(int index, int n) {
        return CubeI.absLayerIndex(axis, n, relLayerAt(index));
    }

    public int primaryAbsLayer(int n) {
        return CubeI.absLayerIndex(axis, n, primaryRelLayer());
    }


    public int[] absLayerIndices(int n) {
        if (relLayerIndices.length == 0)
            return NO_LAYERS;

        final int[] absLayers = new int[relLayerIndices.length];
        for (int i=0; i < relLayerIndices.length; i++) {
            absLayers[i] = CubeI.absLayerIndex(axis, n, relLayerIndices[i]);
        }

        return absLayers;
    }

    @NotNull
    public Predicate<Cubie> cubieFilter(int n) {
        final int count = layersCount();
        if (count <= 1) {
            return CubeI.layerFilter(axis, n, primaryAbsLayer(n));
        }

        Predicate<Cubie> f = CubeI.layerFilter(axis, n, absLayerAt(0, n));

        for (int i=1; i < count; i++) {
            f = f.or(CubeI.layerFilter(axis, n, absLayerAt(i, n)));
        }

        return f;
    }


    public boolean relLayersEqual(@NotNull Move other) {
        return Arrays.equals(relLayerIndices, other.relLayerIndices);
    }

    public boolean absLayersEqual(@NotNull Move other, int n) {
        if (layersCount() != other.layersCount())
            return false;

        return Arrays.equals(absLayerIndices(n), absLayerIndices(n));
    }

    public boolean equalsIgnoreQuarters(@NotNull Move other) {
        return axis == other.axis && relLayersEqual(other);
    }

    public boolean equalsIgnoreQuarters(@NotNull Move other, int n) {
        if (axis == other.axis)
            return relLayersEqual(other);

        return axis == other.axis.invert() && absLayersEqual(other, n);
    }

    public boolean equals(@NotNull Move other) {
        return normalizedQuarters == other.normalizedQuarters && equalsIgnoreQuarters(other);
    }

    public boolean equals(@NotNull Move other, int n) {
        return normalizedQuarters == other.normalizedQuarters && equalsIgnoreQuarters(other, n);
    }

    public int reverseNormalizedQuarters() {
        return reverseNormalizedQuarters(normalizedQuarters);
    }

    public boolean areQuartersReversed(@NotNull Move other) {
        return normalizedQuarters == other.reverseNormalizedQuarters();
    }

    public boolean isReverse(@NotNull Move other, int n) {
        return equalsIgnoreQuarters(other, n) && areQuartersReversed(other);
    }

    public boolean equalsOrReverse(@NotNull Move other, int n) {
        return equalsIgnoreQuarters(other, n) && (normalizedQuarters == other.normalizedQuarters || areQuartersReversed(other));
    }

    @NotNull
    public Commutativity getCommutativity(@NotNull Move other, int n) {
        return getCommutativity(this, other, n);
    }


    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Move && equals((Move) o));
    }

    @Override
    public int hashCode() {
        int h = 31 * axis.hashCode() + normalizedQuarters;

        for (int relLayer: relLayerIndices) {
            h = 31 * h + relLayer;
        }

        return h;
    }


    /**
     * @see #toString(Move)
     * */
    @Override
    @NotNull
    public String toString() {
        if (mStrRepr == null) {
            mStrRepr = toString(this);
        }

        return mStrRepr;
    }

}
