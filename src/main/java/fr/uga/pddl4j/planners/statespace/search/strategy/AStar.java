/*
 * Copyright (c) 2016 by Damien Pellier <Damien.Pellier@imag.fr>.
 *
 * This file is part of PDDL4J library.
 *
 * PDDL4J is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * PDDL4J is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with PDDL4J.  If not, see
 * <http://www.gnu.org/licenses/>
 */

package fr.uga.pddl4j.planners.statespace.search.strategy;

import fr.uga.pddl4j.encoding.CodedProblem;
import fr.uga.pddl4j.heuristics.relaxation.Heuristic;
import fr.uga.pddl4j.heuristics.relaxation.HeuristicToolKit;
import fr.uga.pddl4j.util.BitOp;
import fr.uga.pddl4j.util.BitState;
import fr.uga.pddl4j.util.MemoryAgent;
import fr.uga.pddl4j.util.SolutionEvent;

import java.util.*;
import java.util.concurrent.*;

class Solution {
    public ArrayList<Node> solutions = new ArrayList<>();
    public int maxSolutions;
}

/**
 * This class implements A* search strategy.
 *
 * @author D. Pellier
 * @version 1.0 - 01.06.2018
 */
public final class AStar extends AbstractStateSpaceStrategy {

    /**
     * The serial id of the class.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new AStar search strategy with default parameters.
     */
    public AStar() {
        super();
    }

    /**
     * Creates a new AStar search strategy.
     *
     * @param timeout   the time out of the planner.
     * @param heuristic the heuristicType to use to solve the planning problem.
     * @param weight    the weight set to the heuristic.
     */
    public AStar(int timeout, Heuristic.Type heuristic, double weight) {
        super(timeout, heuristic, weight);
    }

    private Node threadSearch(PriorityQueue<Node> open, HashMap<BitState, Node> openSet, HashMap<BitState, Node> closeSet, CodedProblem codedProblem, Heuristic heuristic) {
        Node current;
        current = open.poll();

        if (current == null) {
            // System.out.println("Nothing to satisfy");
            return null;
        }

        openSet.remove(current);
        closeSet.put(current, current);

        // If the goal is satisfy in the current node then extract the search and return it
        if (current.satisfy(codedProblem.getGoal())) {
            // checkSolution.solution = current;
            fireSolution(new SolutionEvent(this, current, codedProblem));
            return current;
        } else {
            // Try to apply the operators of the problem to this node
            int index = 0;
            for (BitOp op : codedProblem.getOperators()) {
                // Test if a specified operator is applicable in the current state
                if (op.isApplicable(current)) {
                    Node state = new Node(current);
                    this.setCreatedNodes(this.getCreatedNodes() + 1);
                    // Apply the effect of the applicable operator
                    // Test if the condition of the effect is satisfied in the current state
                    // Apply the effect to the successor node
                    op.getCondEffects().stream().filter(ce -> current.satisfy(ce.getCondition())).forEach(ce ->
                            // Apply the effect to the successor node
                            state.apply(ce.getEffects())
                    );
                    final double g = current.getCost() + op.getCost();
                    Node result = openSet.get(state);
                    if (result == null) {
                        result = closeSet.get(state);
                        if (result != null) {
                            if (g < result.getCost()) {
                                result.setCost(g);
                                result.setParent(current);
                                result.setOperator(index);
                                result.setDepth(current.getDepth() + 1);
                                open.add(result);
                                openSet.put(result, result);
                                closeSet.remove(result);
                            }
                        } else {
                            state.setCost(g);
                            state.setParent(current);
                            state.setOperator(index);

                            synchronized (heuristic) {
                                state.setHeuristic(heuristic.estimate(state, codedProblem.getGoal()));
                            }
                            state.setDepth(current.getDepth() + 1);
                            open.add(state);
                            openSet.put(state, state);
                        }
                    } else if (g < result.getCost()) {
                        result.setCost(g);
                        result.setParent(current);
                        result.setOperator(index);
                        result.setDepth(current.getDepth() + 1);
                    }

                }
                index++;
            }
        }

        return null;
    }

    private void threadSearch(ExecutorService executor, Solution solution, PriorityQueue<Node> open, HashMap<BitState, Node> openSet, HashMap<BitState, Node> closeSet, CodedProblem codedProblem, Heuristic heuristic) {
        try {
            executor.execute(() -> {
                Node result = this.threadSearch(open, openSet, closeSet, codedProblem, heuristic);
                if (result != null) {
                    synchronized (solution) {
                    // add only better solutions
                    if (solution.solutions.size() == 0 || solution.solutions.stream().allMatch(n -> n.getCost() > result.getCost())) {
                        solution.solutions.add(result);
                        System.out.println("Found plan with cost: " + result.getCost());
                        if (solution.maxSolutions <= solution.solutions.size()) {
                            executor.shutdownNow();
                            return;
                        }
                    }
                    }
                }
                this.threadSearch(executor, solution, open, openSet, closeSet, codedProblem, heuristic);

            });
        } catch (RejectedExecutionException ex) {
            System.out.println("Stopping process ...");
        }
    }

    /**
     * Solves the planning problem and returns the first solution search found.
     *
     * @param codedProblem the problem to be solved. The problem cannot be null.
     * @return a solution search or null if it does not exist.
     */
    public Solution findSolution(final CodedProblem codedProblem, int maxSolutions) {
        Objects.requireNonNull(codedProblem);
        final long begin = System.currentTimeMillis();
        final Heuristic heuristic = HeuristicToolKit.createHeuristic(getHeuristicType(), codedProblem);
        // Get the initial state from the planning problem
        final BitState init = new BitState(codedProblem.getInit());
        // Initialize the closed list of nodes (store the nodes explored)
        final HashMap<BitState, Node> closeSet = new HashMap<>();
        final HashMap<BitState, Node> openSet = new HashMap<>();
        // Initialize the opened list (store the pending node)
        final double currWeight = getWeight();
        // The list stores the node ordered according to the A* (getFValue = g + h) function

        // Creates the root node of the tree search
        final Node root = new Node(init, null, -1, 0,
                heuristic.estimate(init, codedProblem.getGoal()));
        // Adds the root to the list of pending nodes
        openSet.put(init, root);

        int cores = Runtime.getRuntime().availableProcessors();

        final ExecutorService executor = Executors.newFixedThreadPool(cores);
        final Solution checkSolution = new Solution();
        checkSolution.maxSolutions  = maxSolutions;

        this.resetNodesStatistics();
        Node solution = null;
        final int timeout = getTimeout();
        long time = 0;
        // Start of the search

        for (int i = 0; i < cores; i++) {
            final PriorityQueue<Node> open = new PriorityQueue<>(100, new NodeComparator(currWeight));
            open.add(root);

            this.threadSearch(executor, checkSolution, open, openSet, closeSet, codedProblem, heuristic);
        }

        try {
            if (executor.awaitTermination(1, TimeUnit.DAYS)) {
                System.out.println("Waiting to terminate");
            } else {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        time = System.currentTimeMillis() - begin;

        this.setExploredNodes(closeSet.size());
        this.setPendingNodes(openSet.size());
        this.setMemoryUsed(MemoryAgent.getDeepSizeOf(closeSet) + MemoryAgent.getDeepSizeOf(openSet));
        this.setSearchingTime(time);

        // return the search computed or null if no search was found
        return checkSolution;
    }

    public Node search(final CodedProblem codedProblem) {
        return this.findSolution(codedProblem, 1).solutions.get(0);
    }

    @Override
    public Node[] search(final CodedProblem codedProblem, int max) {
        var solutions = this.findSolution(codedProblem, max);
        return solutions.solutions.toArray(new Node[solutions.solutions.size()]);
    }
}
