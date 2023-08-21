import model.cube.Cube;
import model.cube.CubeI;
import model.cube.CubeNode;
import model.cubie.Move;
import processing.core.PApplet;
import solver.Solver;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class Test {

    private static void println(Object o) {
        System.out.println(o);
    }
    
    private static void test3by3() {
        final Cube cube = new Cube(3);

        // SHUFFLE
        final List<Move> sequence = new LinkedList<>();
        final Random random = new Random();
        final List<Move> allMoves = Move.allMovesCopy(cube.n, true);

        final int shuffleMoves = 50;
        int counter = 0;
        Move prevMove = null;
        while (counter < shuffleMoves) {
            Move move = allMoves.get(random.nextInt(allMoves.size()));
//            (move.relLayerIndex == 0 || move.relLayerIndex == cube.n - 1) &&
            if ((prevMove == null || !CubeNode.isChildMoveRedundant(prevMove, move, cube.n))) {
                sequence.add(move);
                prevMove = move;
                counter++;
            }
        }

        println("Shuffling with " + shuffleMoves + " moves...");
        for (Move move: sequence) {
            cube.applyMove(move);
        }


        println("Cube Shuffled, Heuristic -> " + cube.calculateHeuristic() + ", state-> " + cube.representation2D());

        // NORMALIZE

//        up: {
//            final String state = cube.representation2D();
//
//            final char u = state.charAt(4), r = state.charAt(13), f = state.charAt(22), d = state.charAt(31), l = state.charAt(40), b = state.charAt(49);
//            if (u != Axis.Y.faceChar) {
//                Move m = null;
//
//                if (r == Axis.Y.faceChar) {
//                    m = Move.fp(1);
//                } else if (l == Axis.Y.faceChar) {
//                    m = Move.f(1);
//                } else if (f == Axis.Y.faceChar) {
//                    m = Move.r(1);
//                } else if (b == Axis.Y.faceChar) {
//                    m = Move.rp(1);
//                } else if (d == Axis.Y.faceChar) {
//                    m = Move.r2(1);
//                }
//
//                if (m != null) {
//                    cube.applyMove(m);
//                }
//            }
//        }
//
//        right: {
//            final String state = cube.representation2D();
//
//            final char r = state.charAt(13), f = state.charAt(22), l = state.charAt(40), b = state.charAt(49);
//            if (r != Axis.X.faceChar) {
//                Move m = null;
//
//                if (f == Axis.X.faceChar) {
//                    m = Move.d(1);
//                } else if (b == Axis.X.faceChar) {
//                    m = Move.dp(1);
//                } else if (l == Axis.X.faceChar) {
//                    m = Move.d2(1);
//                }
//
//                if (m != null) {
//                    cube.applyMove(m);
//                }
//            }
//        }
//
//
//        final String parsedState = cube.representation2D();
//        println("\nSolving... STATE:  " + parsedState);
//
//        final long startMs = System.currentTimeMillis();
////        CubeNode solution = new Solver(cube).solve();
//
//        final String solution = Search.solution(parsedState, 21, 1, false);
//        final long msTaken = System.currentTimeMillis() - startMs;
//
////        if (solution == null) {
////            println("Solver Failed!!");
////        } else {
////            final LinkedList<Move> moves = solution.traceMovesFromRoot();
////            if (moves == null || moves.isEmpty()) {
////                println("Already solved!!");
////            } else {
////                final StringJoiner sj = new StringJoiner(" ");
////                for (Move move: moves) {
////                    sj.add(move.toString());
////                }
////
////                println("Solved in " + msTaken + " ms, Solution: " + sj.toString());
////            }
////        }
//
//
//        String parsedSolution = (solution + " ")
//                .replace("R2 ", Move.RIGHT_CLOCKWISE_2 + "_")
//                .replace("R' ", Move.RIGHT_ANTICLOCKWISE + "_")
//                .replace("R ", Move.RIGHT_CLOCKWISE + "_")
//                .replace("L2 ", Move.LEFT_CLOCKWISE_2 + "_")
//                .replace("L' ", Move.LEFT_ANTICLOCKWISE + "_")
//                .replace("L ", Move.LEFT_CLOCKWISE + "_")
//                .replace("F2 ", Move.FRONT_CLOCKWISE_2 + "_")
//                .replace("F' ", Move.FRONT_ANTICLOCKWISE + "_")
//                .replace("F ", Move.FRONT_CLOCKWISE + "_")
//                .replace("B2 ", Move.BACK_CLOCKWISE_2 + "_")
//                .replace("B' ", Move.BACK_ANTICLOCKWISE + "_")
//                .replace("B ", Move.BACK_CLOCKWISE + "_")
//                .replace("U2 ", Move.UP_CLOCKWISE_2 + "_")
//                .replace("U' ", Move.UP_ANTICLOCKWISE + "_")
//                .replace("U ", Move.UP_CLOCKWISE + "_")
//                .replace("D2 ", Move.DOWN_CLOCKWISE_2 + "_")
//                .replace("D' ", Move.DOWN_ANTICLOCKWISE + "_")
//                .replace("D ", Move.DOWN_CLOCKWISE + "_")
//                .replace(" ", "");
//
//        parsedSolution = parsedSolution.substring(0, parsedSolution.length() - 1);      // exclude last _
//
//        println("Solved in " + msTaken + " ms, sol: " + solution + ", parsed: " + parsedSolution);
//
//        final String[] moves_str = parsedSolution.split("_");
//        final Move[] moves = new Move[moves_str.length];
//
//        for (int i=0; i < moves_str.length; i++) {
//            moves[i] = Move.fromCommand(moves_str[i], 0);
//        }


        Solver.Solution solution = Solver.solve3(cube, 30, 5);

        for (Move m: solution.moves) {
            cube.applyMove(m);
        }

        println("Solution: " + solution.getSequence());
        println("STATE: " + cube.representation2D() + ", Solved: " + cube.calculateIsSolved());
    }


//    private static double factorial(int n) {
//        double f = 1;
//        for (int i=2; i <= n; i++) {
//            f *= i;
//        }
//
//        return f;
//    }

    private static void createCenterDatabase(int n) {
//        final Cube cube = new Cube(n);
//        final Path p = Paths.get("cube_centers_" + n + "by" + n + ".dat");

    }


    public static void main(String[] args) {

        println("%");
//        final Cube cube = new Cube(5);
//
//        // scramble
//        for (Move m: cube.createScrambleSequence(40)) {
////            if (m.relLayerAt(0) == 2)
////                continue;
//
//            cube.applyMove(m);
//        }
//
//
//        // repr
//        final List<Move> normMoves = Solver.normalizeCube(cube);
//
//        final String repr = cube.representation2D();
//        println("Main cube: " + repr);
//
//        // center cube repr (only above 3)
//
//        final StringBuilder cc = new StringBuilder();
//
//        for (int j=0; j < CubeI.FACES_COUNT; j++) {
//            int start, end;
//            for (int i=0; i < cube.n - 2; i++) {
//                start = (cube.n * ((j * cube.n) + i + 1)) + 1;
//                end = start + cube.n - 2;
//
//                cc.append(repr, start, end);
//            }
//        }
//
//
//        final int cN = cube.n - 2;
//        final String c_repr = cc.toString();
//        println("Center cube: " + c_repr);
//
//        // TODO: hardcoded 3*3 center
//        final Solver.Solution c_solution = Solver.solveNormalised3(c_repr, null);
//
//        if (!c_solution.isEmpty()) {
////            List<Move> c_moves = new LinkedList<>();
//
//            // Transform center moves to main cube moves
//            for (Move m: c_solution.moves) {
//                int relLayer = m.relLayerAt(0);
//                if (relLayer == 0) {
//                    m = m.withLayers(0, 1);
//                } else if (relLayer == cN - 1) {
//                    m = m.withLayers(cube.n - 1, cube.n - 2);
//                } else {
//                    m = m.withLayers(relLayer + 1);
//                }
//
////                c_moves.add(m);
//                cube.applyMove(m);
//            }
//        }
//
//        println("Main cube after center solve: " + cube.representation2D());
//
//        final Main app = new Main(cube);
//        PApplet.runSketch(PApplet.concat(new String[] { app.getClass().getName() }, args), app);
    }

}
