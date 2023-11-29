package examples.StarterPacMan;

import pacman.controllers.PacmanController;
import pacman.game.Constants;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

import java.util.*;

public class Dijkstra extends PacmanController {
    private static final int MIN_DISTANCE = 20;
    private Random random = new Random();

    private ArrayList<Double> current_reward = new ArrayList<>(Collections.singletonList(0.0));
    private static final double PILL_REWARD = 1;
    private static final double EATING_EDIBLE_GHOST_REWARD = 50.0;
    private static final double LEVEL_UP_REWARD = 50.0;
    private static final double CAUGHT_BY_NON_EDIBLE_GHOST_PENALTY = -25.0;
    private static final double DECAY_PENALTY = -0.05;

    @Override
    public MOVE getMove(Game game, long timeDue) {
        int current = game.getPacmanCurrentNodeIndex();
        MOVE lastMoveMade = game.getPacmanLastMoveMade();

        // Check if Pac-Man is in a corner
        if (isInCorner(current, game)) {
            MOVE[] moves = game.getPossibleMoves(current, lastMoveMade);
            if (moves.length > 0) {
                // Exclude the opposite direction
                List<MOVE> validMoves = Arrays.asList(moves);
                validMoves.remove(lastMoveMade.opposite());
                return validMoves.get(random.nextInt(validMoves.size()));
            }
        }

        // Priority queue to store nodes with their distances
        PriorityQueue<NodeDistancePair> priorityQueue = new PriorityQueue<>(
                Comparator.comparingInt(NodeDistancePair::getDistance));

        // Set to keep track of visited nodes
        Set<Integer> visited = new HashSet<>();

        // Map to store distances from the current node
        Map<Integer, Integer> distances = new HashMap<>();

        // Initialize distances and add the current node to the priority queue
        distances.put(current, 0);
        priorityQueue.add(new NodeDistancePair(current, 0));


        System.out.println("Fitness Score time/level: "+game.getTotalTime()/(game.getCurrentLevel()+1));
        System.out.println("Total Time: "+game.getTotalTime());
        System.out.println("Number of Eaten Ghost: "+game.getNumGhostsEaten());
        System.out.println("Score-Time: "+ (game.getScore()-game.getTotalTime()));

        while (!priorityQueue.isEmpty()) {
            NodeDistancePair nodePair = priorityQueue.poll();
            int node = nodePair.getNode();
            int distance = nodePair.getDistance();

            // Skip if the node has already been visited
            if (visited.contains(node)) {
                continue;
            }

            // Mark the current node as visited
            visited.add(node);

            // Strategy 1: Adjusted for PO
            for (Constants.GHOST ghost : Constants.GHOST.values()) {
                if (game.getGhostEdibleTime(ghost) == 0 && game.getGhostLairTime(ghost) == 0) {
                    int ghostLocation = game.getGhostCurrentNodeIndex(ghost);
                    if (ghostLocation != -1) {
                        int ghostDistance = game.getShortestPathDistance(node, ghostLocation);
                        if (ghostDistance < MIN_DISTANCE) {
                            MOVE awayMove = game.getNextMoveAwayFromTarget(node, ghostLocation, Constants.DM.PATH);
                            // Ensure that Pac-Man does not move in the opposite direction
                            // if (awayMove != lastMoveMade.opposite()) {
                            //     return awayMove;
                            // }
                            return awayMove;
                        }
                    }
                }
            }



            // Strategy 2: Find nearest edible ghost and go after them
            for (Constants.GHOST ghost : Constants.GHOST.values()) {
                if (game.getGhostEdibleTime(ghost) > 0) {
                    int ghostLocation = game.getGhostCurrentNodeIndex(ghost);
                    int ghostDistance = game.getShortestPathDistance(node, ghostLocation);

                    if (distances.getOrDefault(ghostLocation, Integer.MAX_VALUE) > distance + ghostDistance) {
                        distances.put(ghostLocation, distance + ghostDistance);
                        priorityQueue.add(new NodeDistancePair(ghostLocation, distance + ghostDistance));
                    }
                }
            }

            // Strategy 3: Go after the pills and power pills that we can see
            int[] pills = game.getPillIndices();
            int[] powerPills = game.getPowerPillIndices();

            for (int pill : pills) {
                int pillDistance = game.getShortestPathDistance(node, pill);
                if (distances.getOrDefault(pill, Integer.MAX_VALUE) > distance + pillDistance) {
                    distances.put(pill, distance + pillDistance);
                    priorityQueue.add(new NodeDistancePair(pill, distance + pillDistance));
                }
            }

            for (int powerPill : powerPills) {
                int powerPillDistance = game.getShortestPathDistance(node, powerPill);
                if (distances.getOrDefault(powerPill, Integer.MAX_VALUE) > distance + powerPillDistance) {
                    distances.put(powerPill, distance + powerPillDistance);
                    priorityQueue.add(new NodeDistancePair(powerPill, distance + powerPillDistance));
                }
            }

        }

        System.out.println("Fitness Score time/level: "+game.getTotalTime()/(game.getCurrentLevel()+1));
        System.out.println("Total Time: "+game.getTotalTime());
        System.out.println("Number of Eaten Ghost: "+game.getNumGhostsEaten());
        System.out.println("Score-Time: "+ (game.getScore()-game.getTotalTime()));
        calculateReward(game);

        // Strategy 4: New PO strategy as now S3 can fail if nothing you can see
        // Going to pick a random action here
        MOVE[] moves = game.getPossibleMoves(current, lastMoveMade);
        if (moves.length > 0) {
            // Exclude the opposite direction
            List<MOVE> validMoves = Arrays.asList(moves);
            validMoves.remove(lastMoveMade.opposite());
            return validMoves.get(random.nextInt(validMoves.size()));
        }
        System.out.println("Fitness Score time/level: "+game.getTotalTime()/(game.getCurrentLevel()+1));
        System.out.println("Total Time: "+game.getTotalTime());
        System.out.println("Number of Eaten Ghost: "+game.getNumGhostsEaten());
        System.out.println("Score-Time: "+ (game.getScore()-game.getTotalTime()));
        calculateReward(game);
        // Must be possible to turn around
        return lastMoveMade.opposite();
    }

    // Helper method to check if Pac-Man is in a corner
    private boolean isInCorner(int node, Game game) {
        return game.isJunction(node) && game.getNeighbouringNodes(node).length == 2;
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

    public ArrayList<Double> getRewards() {
        return this.current_reward;
    }

    // Helper class to represent a pair of node and its distance
    private static class NodeDistancePair {
        private final int node;
        private final int distance;

        public NodeDistancePair(int node, int distance) {
            this.node = node;
            this.distance = distance;
        }

        public int getNode() {
            return node;
        }

        public int getDistance() {
            return distance;
        }
    }
}
