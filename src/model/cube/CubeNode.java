package model.cube;

import model.cubie.Cubie;
import model.cubie.Move;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

public class CubeNode implements CubeI {

    private static class CubieNode {

        private final int index;
        @NotNull
        private final Cubie cubie;

        private CubieNode(int index, @NotNull Cubie cubie) {
            this.index = index;
            this.cubie = cubie;
        }
    }


    @Nullable
    private final CubeNode parent;
    private final Move move;
    private final int n;
    private final int noOfCubies;
    private final int depth;
    private int mCachedHeuristic;

    @Nullable
    private final Map<Integer, Cubie> changedCubies;
    private final Cubie[] rootState;        // only for root node

    public CubeNode(@NotNull CubeNode parent, @NotNull Move move) {
        this.parent = parent;
        this.move = move;
        this.n = parent.n;
        this.noOfCubies = parent.noOfCubies;
        this.rootState = null;
        this.depth = parent.depth + 1;
        mCachedHeuristic = parent.cacheHeuristic();

        // Apply move
        final List<CubieNode> modifiableCubies = new LinkedList<>();
        final Predicate<Cubie> filter = move.cubieFilter(n);

        Cubie qb;
        for (int i = 0; i < noOfCubies; i++) {
            qb = parent.getCubie(i);
            if (filter.test(qb)) {
                modifiableCubies.add(new CubieNode(i, qb));
            }
        }

        final int size = modifiableCubies.size();
        if (size == 0) {
            changedCubies = null;
        } else {
            changedCubies = new IdentityHashMap<>(size + 2);
            for (CubieNode node: modifiableCubies) {
                final int h = node.cubie.calculateHeuristic();
                final Cubie copy = node.cubie.copy();
                copy.applyMove(move);
                mCachedHeuristic += (copy.calculateHeuristic() - h);
                changedCubies.put(node.index, copy);
            }
        }
    }

    public CubeNode(@NotNull CubeI root) {
        this.parent = null;
        this.move = null;
        this.n = root.n();
        this.rootState = root.copyState();
        this.noOfCubies = rootState.length;
        this.changedCubies = null;
        this.depth = 0;
        mCachedHeuristic = root.cacheHeuristic();
    }

    @Override
    public int n() {
        return n;
    }

    @Override
    public int noOfCubies() {
        return noOfCubies;
    }

    @Override
    @NotNull
    public Cubie getCubie(int index) {
        if (changedCubies != null) {
            final Cubie qb = changedCubies.get(index);
            if (qb != null)
                return qb;
        }

        if (parent != null) {
            return parent.getCubie(index);
        }

        // ROOT NODE
        return rootState[index];
    }

    @Override
    public final int cacheHeuristic() {
        if (mCachedHeuristic < 0) {
            mCachedHeuristic = calculateHeuristic();
        }

        return mCachedHeuristic;
    }

    @Override
    public final boolean cacheIsSolved() {
        return cacheHeuristic() == 0;
    }

    @Nullable
    public CubeNode getParent() {
        return parent;
    }

    public boolean isRoot() {
        return parent == null;
    }

    @Nullable
    public Move getMove() {
        return move;
    }

    public int getDepth() {
        return depth;
    }

    @Nullable
    public LinkedList<Move> traceMovesFromRoot() {
        if (move == null)       // Root
            return null;

        final LinkedList<Move> moves = new LinkedList<>();
        moves.addFirst(move);

        CubeNode parent = getParent();
        Move m;
        while (parent != null) {
            m = parent.getMove();
            if (m != null)
                moves.addFirst(m);
            parent = parent.getParent();
        }

        return moves;
    }



    /* Children */

    private static final boolean DEFAULT_EXCLUDE_REDUNDANT_CHILDREN = true;

    public static boolean isChildMoveRedundant(@NotNull Move parentMove, @NotNull Move childMove, int n) {
        final Move.Commutativity c;
        return parentMove.equalsIgnoreQuarters(childMove, n) || (c = parentMove.getCommutativity(childMove, n)) == Move.Commutativity.C_21 || c == Move.Commutativity.EQUAL;
    }

    @NotNull
    public ArrayList<CubeNode> createChildren(boolean excludeRedundant) {
        final List<Move> allMoves = Move.allMovesUnmodifiable(n);
        final ArrayList<CubeNode> children = new ArrayList<>(allMoves.size() + 2);

        if (excludeRedundant && move != null) {
            for (Move mv: allMoves) {
                if (!isChildMoveRedundant(move, mv, n)) {
                    children.add(new CubeNode(this, mv));
                }
            }
        } else {
            for (Move mv: allMoves) {
                children.add(new CubeNode(this, mv));
            }
        }

        return children;
    }

    @NotNull
    public ArrayList<CubeNode> createChildren() {
        return createChildren(DEFAULT_EXCLUDE_REDUNDANT_CHILDREN);
    }

    @Override
    public @Nullable List<Move> getInternalSolution() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof CubeI && equals((CubeI) o));
    }

    @Override
    public int hashCode() {
        return hashImpl();
    }
}
