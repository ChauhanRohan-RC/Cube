package solver;

import model.Point3DInt;
import model.cube.Cube;
import model.cube.CubeI;
import model.cubie.Cubie;
import model.cubie.Move;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.kociemba.twophase.Search;

import java.util.*;

public class Solver {

    public static final int ERR_CODE_UNKNOWN = -1;
    public static final int ERR_CODE_FACELETS = 1;
    public static final int ERR_CODE_EDGES = 2;
    public static final int ERR_CODE_CORNERS = 3;
    public static final int ERR_CODE_FLIP = 4;
    public static final int ERR_CODE_TWIST = 5;
    public static final int ERR_CODE_PARITY = 6;
    public static final int ERR_CODE_LOW_DEPTH = 7;
    public static final int ERR_CODE_TIMEOUT = 8;

    public static final int ERR_CODE_INVALID_CUBE = 9;
    public static final int ERR_CODE_MOVE_PARSE = 10;

    @NotNull
    public static String errMessage(int errorCode) {
        return switch (errorCode) {
            case ERR_CODE_INVALID_CUBE -> "Cube is invalid";
            case ERR_CODE_MOVE_PARSE -> "Move parsing failed";
            case ERR_CODE_FACELETS -> "There is not exactly one facelet of each colour";
            case ERR_CODE_EDGES -> "Not all 12 edges exist exactly once";
            case ERR_CODE_CORNERS -> "Not all corners exist exactly once";
            case ERR_CODE_FLIP -> "Flip error: One edge has to be flipped";
            case ERR_CODE_TWIST -> "Twist error: One corner has to be twisted";
            case ERR_CODE_PARITY -> "Parity error: Two corners or two edges have to be exchanged";
            case ERR_CODE_LOW_DEPTH -> "Depth error: No solution exists for given max depth";
            case ERR_CODE_TIMEOUT -> "Timeout error!";
            default -> "Unknown error!";
        };
    }

    public static class SolveException extends RuntimeException {

        private final int errorCode;

        public SolveException(int errorCode, @Nullable String message) {
            super(message == null || message.isEmpty()? errMessage(errorCode): message);
            this.errorCode = errorCode;
        }

        public SolveException(int errorCode) {
            this(errorCode, null);
        }

        public final int getErrorCode() {
            return errorCode;
        }
    }


    public static class Solution {

        @NotNull
        public static final Solution EMPTY = new Solution(Collections.emptyList(), "");

        @NotNull
        @Unmodifiable
        public final List<Move> moves;         // list of moves from start -> goal state

        @Nullable
        private String mSequence;                // cached string representation of moves sequence
        @Nullable
        private Long mMsTaken;

        public Solution(@NotNull List<Move> moves, @Nullable String sequence) {
            this.moves = Collections.unmodifiableList(moves);
            mSequence = sequence;
        }

        @NotNull
        public final String getSequence() {
            if (mSequence == null) {
                mSequence = Move.sequence(moves);
            }

            return mSequence;
        }

        public final boolean isEmpty() {
            return moves.isEmpty();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;

            if (o instanceof Solution) {
                final Solution other = (Solution) o;
                return moves.equals(other.moves);
            }

            return false;
        }

        @Override
        public int hashCode() {
            return moves.hashCode();
        }

        @Override
        public String toString() {
            return getSequence();
        }

        @Nullable
        public Long getMsTaken() {
            return mMsTaken;
        }
    }



    /**
     * Normalizes an odd dimensional cube against flipping transformations (that changes center cubies orientation)
     * for kociemba algorithm
     *
     * @return list of moves required for normalisation, pr an empty list
     * */
    @Unmodifiable
    @NotNull
    public static List<Move> normalizeCube(@NotNull Cube cube) {
        if (cube.n < 3 || cube.n % 2 == 0)
            return Collections.emptyList();

        // Normalize
        Move normUpMove = null;
        Move normRightMove = null;

        norm: {
            Integer u = null, r = null, f = null, d = null, l = null, b = null;
            final int l_start = 0;
            final int l_mid = (cube.n - 1) / 2;
            final int l_end = cube.n - 1;

            final int c_start = cube.layerToCubieCenter(l_start);
            final int c_mid = cube.layerToCubieCenter(l_mid);
            final int c_end = cube.layerToCubieCenter(l_end);
            final Point3DInt cu = new Point3DInt(c_mid, c_end, c_mid);
            final Point3DInt cr = new Point3DInt(c_end, c_mid, c_mid);
            final Point3DInt cf = new Point3DInt(c_mid, c_mid, c_end);
            final Point3DInt cd = new Point3DInt(c_mid, c_start, c_mid);
            final Point3DInt cl = new Point3DInt(c_start, c_mid, c_mid);
            final Point3DInt cb = new Point3DInt(c_mid, c_mid, c_start);

            for (int i=0; i < cube.noOfCubies(); i++) {
                Cubie qb = cube.getCubie(i);
                if (qb.noOfFaces() != 1)
                    continue;

                Point3DInt center = qb.center();
                int fc = qb.getFace(0).originalFaceCode();
                if (u == null && cu.equals(center)) {
                    u = fc;
                } else if (r == null && cr.equals(center)) {
                    r = fc;
                } else if (f == null && cf.equals(center)) {
                    f = fc;
                } else if (d == null && cd.equals(center)) {
                    d = fc;
                } else if (l == null && cl.equals(center)) {
                    l = fc;
                } else if (b == null && cb.equals(center)) {
                    b = fc;
                }
            }

            if (u == null || r == null || f == null || d == null || l == null || b == null)
                break norm;

            if (u != CubeI.FACE_UP) {
                if (r == CubeI.FACE_UP) {
                    normUpMove = Move.fp(new int[] { l_mid });
                    r = d; l = u;
                } else if (l == CubeI.FACE_UP) {
                    normUpMove = Move.f(new int[] { l_mid });
                    r = u; l = d;
                } else if (f == CubeI.FACE_UP) {
                    normUpMove = Move.r(new int[] { l_mid });
                    f = d; b = u;
                } else if (b == CubeI.FACE_UP) {
                    normUpMove = Move.rp(new int[] { l_mid });
                    f = u; b = d;
                } else if (d == CubeI.FACE_UP) {
                    normUpMove = Move.r2(new int[] { l_mid });
                    final int temp = f;
                    f = b; b = temp;
                }

                if (normUpMove != null) {
                    cube.applyMove(normUpMove);
                }
            }

            if (r != CubeI.FACE_RIGHT) {
                if (f == CubeI.FACE_RIGHT) {
                    normRightMove = Move.d(new int[] { l_mid });
                } else if (b == CubeI.FACE_RIGHT) {
                    normRightMove = Move.dp(new int[] { l_mid });
                } else if (l == CubeI.FACE_RIGHT) {
                    normRightMove = Move.d2(new int[] { l_mid });
                }

                if (normRightMove != null) {
                    cube.applyMove(normRightMove);
                }
            }
        }

        final List<Move> moves = new LinkedList<>();
        if (normUpMove != null) {
            moves.add(normUpMove);
        }

        if (normRightMove != null) {
            moves.add(normRightMove);
        }

        return moves;
    }


    @NotNull
    public static Solution parseKociembaSolution(@Nullable String solution, @Nullable List<Move> normMoves, int errMaxDepth, long errTimeoutSecs) throws SolveException {
        if (solution == null || solution.isEmpty() || solution.startsWith("Error")) {
            int errorCode = ERR_CODE_UNKNOWN;
            final SolveException exc;

            if (!(solution == null || solution.isEmpty())) {
                try {
                    errorCode = Integer.parseInt(solution.substring(solution.length() - 1));
                } catch (NumberFormatException ignored) { }
            }

            exc = switch (errorCode) {
                case 1 -> new SolveException(ERR_CODE_FACELETS);
                case 2 -> new SolveException(ERR_CODE_EDGES);
                case 3 -> new SolveException(ERR_CODE_FLIP);
                case 4 -> new SolveException(ERR_CODE_CORNERS);
                case 5 -> new SolveException(ERR_CODE_TWIST);
                case 6 -> new SolveException(ERR_CODE_PARITY);
                case 7 -> new SolveException(ERR_CODE_LOW_DEPTH, errMessage(ERR_CODE_LOW_DEPTH) + ", max depth: " + errMaxDepth);
                case 8 -> new SolveException(ERR_CODE_TIMEOUT, errMessage(ERR_CODE_TIMEOUT) + ", timeout: " + errTimeoutSecs + "s");
                default -> new SolveException(ERR_CODE_UNKNOWN);
            };

            throw exc;
        }

        String parsedSolution = (solution + " ")
                .replace("R2 ", Move.RIGHT_CLOCKWISE_2 + "_")
                .replace("R' ", Move.RIGHT_ANTICLOCKWISE + "_")
                .replace("R ", Move.RIGHT_CLOCKWISE + "_")
                .replace("L2 ", Move.LEFT_CLOCKWISE_2 + "_")
                .replace("L' ", Move.LEFT_ANTICLOCKWISE + "_")
                .replace("L ", Move.LEFT_CLOCKWISE + "_")
                .replace("F2 ", Move.FRONT_CLOCKWISE_2 + "_")
                .replace("F' ", Move.FRONT_ANTICLOCKWISE + "_")
                .replace("F ", Move.FRONT_CLOCKWISE + "_")
                .replace("B2 ", Move.BACK_CLOCKWISE_2 + "_")
                .replace("B' ", Move.BACK_ANTICLOCKWISE + "_")
                .replace("B ", Move.BACK_CLOCKWISE + "_")
                .replace("U2 ", Move.UP_CLOCKWISE_2 + "_")
                .replace("U' ", Move.UP_ANTICLOCKWISE + "_")
                .replace("U ", Move.UP_CLOCKWISE + "_")
                .replace("D2 ", Move.DOWN_CLOCKWISE_2 + "_")
                .replace("D' ", Move.DOWN_ANTICLOCKWISE + "_")
                .replace("D ", Move.DOWN_CLOCKWISE + "_")
                .replace(" ", "");

        parsedSolution = parsedSolution.substring(0, parsedSolution.length() - 1);      // exclude last _

        final String[] moves_str = parsedSolution.split("_");
        final ArrayList<Move> moves = new ArrayList<>(moves_str.length + 3);
        final StringJoiner sequence = new StringJoiner(Move.SEQUENCE_DELIMITER);

        if (!(normMoves == null || normMoves.isEmpty())) {
            for (Move m: normMoves) {
                moves.add(m);
                sequence.add(m.toString());
            }
        }

        for (String mstr: moves_str) {
            Move move = Move.fromCommand(mstr, 0);

            if (move == null) {
                throw new SolveException(ERR_CODE_MOVE_PARSE, "Move command <"+ mstr + "> is invalid!");
            }

            moves.add(move);
        }

        return new Solution(moves, sequence.add(parsedSolution.replace("_", Move.SEQUENCE_DELIMITER)).toString());
    }




    // 3*3 cube

    public static final int DEFAULT_3BY3_MAX_DEPTH = 30;
    public static final long DEFAULT_3BY3_TIMEOUT_SECS = 10;

    /**
     * Solves a normalized 3*3 cube state
     * */
    @NotNull
    public static Solution solveNormalised3(@NotNull String representation2d, @Nullable List<Move> normMoves, int maxDepth, long timeoutSecs) throws SolveException {
        final long startMs = System.currentTimeMillis();
        final String kociembaSolution = Search.solution(representation2d, maxDepth, timeoutSecs, false);
        final long msTaken = System.currentTimeMillis() - startMs;

        final Solution sol = parseKociembaSolution(kociembaSolution, normMoves, maxDepth, timeoutSecs);
        sol.mMsTaken = msTaken;
        return sol;
    }

    @NotNull
    public static Solution solveNormalised3(@NotNull String representation2d, @Nullable List<Move> normMoves) {
        return solveNormalised3(representation2d, normMoves, DEFAULT_3BY3_MAX_DEPTH, DEFAULT_3BY3_TIMEOUT_SECS);
    }


    /**
     * Solves a 3*3 cube
     * */
    @NotNull
    public static Solution solve3(@NotNull CubeI c, int maxDepth, long timeoutSecs) throws SolveException {
        if (c.n() != 3)
            throw new SolveException(ERR_CODE_INVALID_CUBE, "Cube should be 3*3, given: " + c.n() + "*" + c.n());

        if (c.cacheIsSolved())
            return Solution.EMPTY;

        // copy
        final Cube cube = new Cube(c);

        // Normalise
        final List<Move> normMoves = normalizeCube(cube);

        // Solve
        return solveNormalised3(cube.representation2D(), normMoves, maxDepth, timeoutSecs);
    }


    /**
     * Solves a 3*3 cube with default configurations
     *
     * @see #solve3(CubeI, int, long)
     * */
    @NotNull
    public static Solution solve3(@NotNull CubeI c) {
        return solve3(c, DEFAULT_3BY3_MAX_DEPTH, DEFAULT_3BY3_TIMEOUT_SECS);
    }

}
