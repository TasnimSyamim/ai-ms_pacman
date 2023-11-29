package examples.StarterPacMan;

import pacman.controllers.PacmanController;
import pacman.game.Constants;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.PriorityQueue;
import java.util.Random;

public class AStar extends PacmanController {
    private Random random = new Random();

    private ArrayList<Double> current_reward = new ArrayList<>(Collections.singletonList(0.0));
    private double fitness_score = 0;
    private int numOfGhostEaten = 0;
    private static final double PILL_REWARD = 1;
    private static final double EATING_EDIBLE_GHOST_REWARD = 50.0;
    private static final double LEVEL_UP_REWARD = 50.0;
    private static final double CAUGHT_BY_NON_EDIBLE_GHOST_PENALTY = -25.0;
    private static final double DECAY_PENALTY = -0.05;


    @Override
    public MOVE getMove(Game game, long timeDue) {
        int current = game.getPacmanCurrentNodeIndex();



        // Strategy: Use A* algorithm to find the nearest pill or power pill
        int[] pills = game.getPillIndices();
        int[] powerPills = game.getPowerPillIndices();

        ArrayList<Integer> targets = new ArrayList<>();

        // Collect available pills as targets
        for (int i = 0; i < pills.length; i++) {
            Boolean pillStillAvailable = game.isPillStillAvailable(i);
            if (pillStillAvailable != null && pillStillAvailable) {
                targets.add(pills[i]);
            }

        }
        System.out.println("Fitness Score time/level: "+game.getTotalTime()/(game.getCurrentLevel()+1));
        System.out.println("Total Time: "+game.getTotalTime());
        System.out.println("Number of Eaten Ghost: "+game.getNumGhostsEaten());
        System.out.println("Score-Time: "+ (game.getScore()-game.getTotalTime()));
        calculateReward(game);

        // Collect available power pills as targets
        for (int i = 0; i < powerPills.length; i++) {
            Boolean pillStillAvailable = game.isPillStillAvailable(i);
            if (pillStillAvailable != null && pillStillAvailable) {
                targets.add(powerPills[i]);
            }
        }



        if (!targets.isEmpty()) {
            int[] targetsArray = new int[targets.size()];

            // Convert ArrayList to an array
            for (int i = 0; i < targetsArray.length; i++) {
                targetsArray[i] = targets.get(i);
            }
            // calculateReward(game);

            // Use A* algorithm to find the nearest target
            int nearestTarget = findNearestTarget(current, targetsArray, game);
            return game.getNextMoveTowardsTarget(current, nearestTarget, Constants.DM.PATH);
        }




        // If no targets are available, just move randomly
        return getRandomMove(game.getPacmanLastMoveMade());
    }

    private MOVE getRandomMove(MOVE lastMove) {
        MOVE[] possibleMoves = MOVE.values();

        // Filter out the opposite move to avoid turning back
        MOVE[] filteredMoves = new MOVE[possibleMoves.length - 1];
        int index = 0;
        for (MOVE move : possibleMoves) {
            if (!move.opposite().equals(lastMove)) {
                filteredMoves[index++] = move;
            }
        }


        // Choose a random move from the filtered list
        return filteredMoves[random.nextInt(filteredMoves.length)];
    }

    // A* algorithm to find the nearest target
    /* HOW A* ALGORITHM IS IMPLEMENTED
     * 1. The algorithm maintains a priority queue (openSet) to store nodes yet to be explored. Nodes are ordered based on their fScore, which is the sum of the cost to reach the node (gScore) and a heuristic estimate to the goal (hScore).
     * 2. The algorithm starts with the current node and adds it to the open set with an initial fScore based on the heuristic distance to the first target.
     * 3. In each iteration, the algorithm selects the node with the lowest fScore from the open set, explores its neighbors, and calculates the scores for each neighbor.
     * 4. The neighbors are added to the open set if they haven't been processed and have a lower fScore. This process continues until the target is found or all reachable nodes are explored.
     * 5. The algorithm returns the index of the nearest target or the current node if no path is found.
    */
    private int findNearestTarget(int current, int[] targets, Game game) {
        // Priority queue to store nodes to be explored, ordered by their fScores
        PriorityQueue<AStarNode> openSet = new PriorityQueue<>();

        // List to store nodes that have been processed
        ArrayList<Integer> closedSet = new ArrayList<>();

        // Add the current node to the open set with initial scores
        openSet.add(new AStarNode(current, null, 0, game.getShortestPathDistance(current, targets[0])));

        // Continue until all nodes are explored or the target is found
        while (!openSet.isEmpty()) {
            // Get the node with the lowest fScore from the open set
            AStarNode currentAStarNode = openSet.poll();

            // Skip if the node is already processed
            if (closedSet.contains(currentAStarNode.getNodeIndex())) {
                continue;
            }

            // Check if the current node is one of the target nodes
            if (containsTarget(currentAStarNode.getNodeIndex(), targets)) {
                return currentAStarNode.getNodeIndex();
            }

            // Add the current node to the closed set
            closedSet.add(currentAStarNode.getNodeIndex());

            // Explore neighbors and add them to the open set
            for (Constants.MOVE move : game.getPossibleMoves(currentAStarNode.getNodeIndex())) {
                int successor = game.getNeighbour(currentAStarNode.getNodeIndex(), move);

                // Skip if the successor node is already processed
                if (!closedSet.contains(successor)) {
                    // Calculate the gScore, hScore, and fScore for the successor node
                    int gScore = currentAStarNode.getGScore() + 1;
                    int hScore = game.getShortestPathDistance(successor, targets[0]); // Using the distance to the first
                                                                                      // target as a heuristic
                    int fScore = gScore + hScore;

                    // Add the successor node to the open set with updated scores
                    openSet.add(new AStarNode(successor, currentAStarNode, gScore, fScore));
                }
            }
        }

        // If no path found, return the current node
        return current;
    }

    private boolean containsTarget(int nodeIndex, int[] targets) {
        for (int target : targets) {
            if (nodeIndex == target) {
                return true;
            }
        }
        return false;
    }

    private double calculateReward(Game game){
        double livesPenalty = (3 - game.getPacmanNumberOfLivesRemaining()) * CAUGHT_BY_NON_EDIBLE_GHOST_PENALTY;
        double timeStepPenalty = game.getCurrentLevelTime() * DECAY_PENALTY;

        double eatenPillsReward = (game.getNumberOfPills() - game.getNumberOfActivePills()) * PILL_REWARD;
        double eatenGhostsReward = game.getNumGhostsEaten() * EATING_EDIBLE_GHOST_REWARD;
        double currentLevel = game.getCurrentLevel() * LEVEL_UP_REWARD;

        double reward = (eatenPillsReward +
                eatenGhostsReward +
                currentLevel +
                timeStepPenalty +
                livesPenalty);
        this.current_reward.add(reward);
        return this.current_reward.get(this.current_reward.size() - 1);
    }

    private double calculateFitnessScore(Game game){
        double score = game.getScore();
        double time = game.getTotalTime();

        double fitness_score = score - time;

        return fitness_score; 
    }

    public double getFitness(){
        return fitness_score;
    }

    public int getNumOfGhost(){
        return numOfGhostEaten;
    }

    public ArrayList<Double> getRewards() {
        return this.current_reward;
    }

    private class AStarNode implements Comparable<AStarNode> {
        private int nodeIndex;
        private AStarNode parent;
        private int gScore; // cost from start to current node
        private int fScore; // estimated total cost from start to goal through the current node

        public AStarNode(int nodeIndex, AStarNode parent, int gScore, int fScore) {
            this.nodeIndex = nodeIndex;
            this.parent = parent;
            this.gScore = gScore;
            this.fScore = fScore;
        }

        public int getNodeIndex() {
            return nodeIndex;
        }

        public AStarNode getParent() {
            return parent;
        }

        public int getGScore() {
            return gScore;
        }

        public int getFScore() {
            return fScore;
        }

        @Override
        public int compareTo(AStarNode other) {
            return Integer.compare(this.fScore, other.fScore);
        }
    }
}
