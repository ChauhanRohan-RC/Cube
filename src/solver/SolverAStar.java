package solver;

import model.cube.CubeI;
import model.cube.CubeNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class SolverAStar {

    @NotNull
    private static final Comparator<CubeNode> DESCENDING_HEURISTIC_COMPARATOR = (CubeNode n1, CubeNode n2) -> Integer.compare(heuristic(n2), heuristic(n1));

    private int mThreshold;
    private int mMinPrunedNodeCost = Integer.MAX_VALUE;
    @NotNull
    private final CubeNode mRoot;
    private volatile boolean mSolving;

    public SolverAStar(@NotNull CubeI root) {
        mRoot = new CubeNode(root);
    }

    public final boolean isSolving() {
        return mSolving;
    }

    @Nullable
    public CubeNode solve() {
        if (mSolving) {
            throw new IllegalStateException("Already Solving!!");
        }

        mSolving = true;
        final CubeNode solved = doSolve();
        mSolving = false;
        return solved;
    }


    @Nullable
    private CubeNode doSolve() {                        // TODO: set limit of calls
        CubeNode solved;
        mThreshold = heuristic(mRoot);

        do {
            solved = depthAStarSearch(mRoot);
            mThreshold = mMinPrunedNodeCost;
        } while (solved == null);

        return solved;
    }

    /**
     * @return solved cube
     * */
    @Nullable
    private CubeNode depthAStarSearch(@NotNull CubeNode start) {
        Deque<CubeNode> stack = new LinkedList<>();
        // TODO: mark that this state is visited
        CubeNode node = start;

        do {
            if (isGoalState(node)) {         // Hurray!!
                return node;
            }

            if (shouldCheck(node)){
                System.out.println("Solver: At depth: " + depth(node) + ", Heuristic: " + heuristic(node));
                addChildren(stack, node);      // check the children (+1 depth)
            }
        } while ((node = stack.poll()) != null);

        return null;        // Failed to solve
    }


    private boolean shouldCheck(@NotNull CubeNode node){
        final int g = depth(node);
        final int h = heuristic(node);
        final int f = g + h;

        if (f <= mThreshold) {
//            mThreshold = f;         // TODO
            return true;
        }

        if (f < mMinPrunedNodeCost) {
            mMinPrunedNodeCost = f;
        }

        return false;
    }

    private void addChildren(@NotNull Deque<CubeNode> stack, @NotNull CubeNode node) {
        final ArrayList<CubeNode> children = node.createChildren(true);
        children.sort(DESCENDING_HEURISTIC_COMPARATOR);

        for (CubeNode child: children) {
            stack.push(child);
        }
    }


    private static int depth(@NotNull CubeNode node) {
        return node.getDepth();
    }

    private static int heuristic(@NotNull CubeNode node) {
        return node.cacheHeuristic();

//        int h = 0;
//
//        for (int i=0; i < node.noOfCubies(); i++) {
//            Cubie qb = node.getCubie(i);
//            if (qb.noOfFaces() == 1) {          // single face cubie only
//                h += qb.getFace(0).calculateHeuristic();
//            }
//        }
//
//        return h;
    }

    private static boolean isGoalState(@NotNull CubeNode node) {
        return node.cacheIsSolved();

//        for (int i=0; i < node.noOfCubies(); i++) {
//            Cubie qb = node.getCubie(i);
//            if (qb.noOfFaces() == 1) {          // single face cubie only
//                if (!qb.getFace(0).isAtOriginalFace())
//                    return false;
//            }
//        }
//
//        return true;
    }

}
