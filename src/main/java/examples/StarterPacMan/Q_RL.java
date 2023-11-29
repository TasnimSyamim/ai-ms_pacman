/*
 * Q-learning Pac-Man Controller
 *
 * This Pac-Man controller uses Q-learning, a model-free reinforcement learning algorithm,
 * to make decisions on the best moves in a given game state. Q-learning learns a policy
 * that tells the agent what action to take under what circumstances to maximize the cumulative
 * reward over time. The algorithm maintains a Q-value table, where each entry represents the
 * expected cumulative reward for a state-action pair. During gameplay, the Q-values are updated
 * based on the agent's experiences and rewards received. The agent then selects actions based
 * on a balance between exploration (trying new actions) and exploitation (choosing known
 * high-reward actions).
 *
 * Strategies implemented:
 * 1. Evading ghosts that are not edible and too close.
 * 2. Hunting the nearest edible ghost.
 * 3. Going after visible pills and power pills.
 * 4. Exploring randomly or exploiting the best-known action based on Q-values.
 *
 * Q-learning Parameters:
 * - LEARNING_RATE: Controls the extent to which the agent updates its Q-values.
 * - DISCOUNT_FACTOR: Represents the importance of future rewards in the decision-making process.
 * - EXPLORATION_PROBABILITY: Probability of choosing a random action for exploration.
 *
 * Q-value update equation: Q(s, a) = Q(s, a) + alpha * [R + gamma * max Q(s', a') - Q(s, a)]
 * where:
 *   alpha is the learning rate (LEARNING_RATE),
 *   gamma is the discount factor (DISCOUNT_FACTOR),
 *   R is the observed reward,
 *   max Q(s', a') is the maximum Q-value for the next state,
 *   Q(s, a) is the current Q-value.
 * 
 * 
 * Note: Fine-tune parameters and reward functions for optimal performance in a specific Pac-Man scenario.
 *
 * Implemented By: Waiz Wafiq (17203410/2)
 */

package examples.StarterPacMan;

import pacman.controllers.PacmanController;
import pacman.game.Constants;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

import java.util.ArrayList;
import java.util.Collections;

public class Q_RL extends PacmanController {
    private static final double LEARNING_RATE = 0.1;
    private static final double DISCOUNT_FACTOR = 0.9;
    private static final double EXPLORATION_PROBABILITY = 0.1;
    private static final int MIN_DISTANCE_TO_EVADE_GHOST = 25;

    private ArrayList<Double> current_reward = new ArrayList<>(Collections.singletonList(0.0));
    private static final double PILL_REWARD = 1;
    private static final double EATING_EDIBLE_GHOST_REWARD = 50.0;
    private static final double LEVEL_UP_REWARD = 50.0;
    private static final double CAUGHT_BY_NON_EDIBLE_GHOST_PENALTY = -25.0;
    private static final double DECAY_PENALTY = -0.05;

    private Random random = new Random();
    private Map<StateActionPair, Double> qValues = new HashMap<>();
    private MOVE lastMove;

    private void printMoveInfo(String info, MOVE move, Game game) {
        double reward = calculateReward(game);
        System.out.println(info + " - Current Move: " + move + " - Current Reward: " + reward);
    }

    @Override
    public MOVE getMove(Game game, long timeDue) {
        int current = game.getPacmanCurrentNodeIndex();

        System.out.println("Fitness Score time/level: "+game.getTotalTime()/(game.getCurrentLevel()+1));
        System.out.println("Total Time: "+game.getTotalTime());
        System.out.println("Number of Eaten Ghost: "+game.getNumGhostsEaten());
        System.out.println("Score-Time: "+ (game.getScore()-game.getTotalTime()));
        // System.out.println(qValues.keySet());
        
        // Strategy 1: Adjusted for PO (Pill Observations)
        for (Constants.GHOST ghost : Constants.GHOST.values()) {
            if (game.getGhostEdibleTime(ghost) == 0 && game.getGhostLairTime(ghost) == 0) {
                int ghostLocation = game.getGhostCurrentNodeIndex(ghost);
                if (ghostLocation != -1) {
                    if (game.getShortestPathDistance(current, ghostLocation) < MIN_DISTANCE_TO_EVADE_GHOST) {
                        // Evade the ghost
                        MOVE evadeMove = game.getNextMoveAwayFromTarget(current, ghostLocation, Constants.DM.PATH);
                        printMoveInfo("Evading ghost", evadeMove, game);
                        return evadeMove;
                    }
                }
            }
        }
        
        System.out.println("Fitness Score time/level: "+game.getTotalTime()/(game.getCurrentLevel()+1));
        System.out.println("Total Time: "+game.getTotalTime());
        System.out.println("Number of Eaten Ghost: "+game.getNumGhostsEaten());
        

        // Strategy 2: Go after the pills and power pills that we can see
        int[] pills = game.getPillIndices();
        int[] powerPills = game.getPowerPillIndices();

        for (int i = 0; i < pills.length; i++) {
            Boolean pillStillAvailable = game.isPillStillAvailable(i);
            if (pillStillAvailable != null && pillStillAvailable) {
                // Move towards the nearest visible pill
                MOVE pillMove = game.getNextMoveTowardsTarget(current, pills[i], Constants.DM.PATH);
                printMoveInfo("Going after visible pill", pillMove, game);
                return pillMove;
            }
        }



        for (int i = 0; i < powerPills.length; i++) {
            Boolean powerPillStillAvailable = game.isPowerPillStillAvailable(i);
            if (powerPillStillAvailable != null && powerPillStillAvailable) {
                // Move towards the nearest visible power pill
                MOVE powerPillMove = game.getNextMoveTowardsTarget(current, powerPills[i], Constants.DM.PATH);
                printMoveInfo("Going after visible power pill", powerPillMove, game);
                return powerPillMove;
            }
        }


        // Strategy 3: Find nearest edible ghost and go after them
        int minDistance = Integer.MAX_VALUE;
        Constants.GHOST minGhost = null;
        for (Constants.GHOST ghost : Constants.GHOST.values()) {
            if (game.getGhostEdibleTime(ghost) > 0) {
                int distance = game.getShortestPathDistance(current, game.getGhostCurrentNodeIndex(ghost));
                if (distance < minDistance) {
                    minDistance = distance;
                    minGhost = ghost;
                }
            }
        }

        if (minGhost != null) {
            // Hunt the nearest edible ghost
            MOVE huntMove = game.getNextMoveTowardsTarget(current, game.getGhostCurrentNodeIndex(minGhost),
                    Constants.DM.PATH);
            printMoveInfo("Hunting the nearest edible ghost", huntMove, game);
            return huntMove;
        }


        // Strategy 4: New PO strategy as now S3 can fail if nothing you can see
        // Going to pick a random action here
        MOVE[] possibleMoves = game.getPossibleMoves(current, lastMove);

        try {
            if (possibleMoves.length > 0) {
                MOVE selectedMove;
                if (random.nextDouble() < EXPLORATION_PROBABILITY) {
                    // Exploration: choose a random move
                    selectedMove = possibleMoves[random.nextInt(possibleMoves.length)];
                    printMoveInfo("Exploration: Choosing a random move", selectedMove, game);
                } else {
                    // Exploitation: choose the move with the highest Q-value
                    selectedMove = getBestMove(current, possibleMoves);
                    printMoveInfo("Exploitation: Choosing the best move based on Q-values", selectedMove, game);
                }

                if (lastMove != null) {
                    // Q-learning update step
                    StateActionPair stateActionPair = new StateActionPair(current, lastMove);
                    double reward = calculateReward(game);
                    double currentQValue = qValues.getOrDefault(stateActionPair, 0.0);
                    double maxNextQValue = getMaxQValue(game, current);
                    double updatedQValue = currentQValue
                            + LEARNING_RATE * (reward + DISCOUNT_FACTOR * maxNextQValue - currentQValue);
                    qValues.put(stateActionPair, updatedQValue);

                    // System.out.println("Q-learning Update:");
                    // System.out.println(" State-Action Pair: " + stateActionPair.state + " - " +
                    // stateActionPair.action);
                    // System.out.println(" Reward: " + reward);
                    // System.out.println(" Current Q-value: " + currentQValue);
                    // System.out.println(" Max Next Q-value: " + maxNextQValue);
                    // System.out.println(" Updated Q-value: " + updatedQValue);
                }

                lastMove = selectedMove;
                return selectedMove;
            }
        } catch (NullPointerException e) {
            System.err.println("Error: NullPointerException occurred during Q-learning update.");
            e.printStackTrace(); // Print the stack trace for debugging
            return MOVE.NEUTRAL;
        }

        displayQValues();

        // Must be possible to turn around
        return game.getPacmanLastMoveMade().opposite();
    }

    // Get the best move based on Q-values
    private MOVE getBestMove(int current, MOVE[] possibleMoves) {
        double maxQValue = Double.NEGATIVE_INFINITY;
        MOVE bestMove = possibleMoves[0];

        for (MOVE move : possibleMoves) {
            StateActionPair stateActionPair = new StateActionPair(current, move);
            double qValue = qValues.getOrDefault(stateActionPair, 0.0);
            if (qValue > maxQValue) {
                maxQValue = qValue;
                bestMove = move;
            }
        }
        System.out.println("Best move: " + bestMove);
        return bestMove;
    }

    // Get the maximum Q-value for the current state
    private double getMaxQValue(Game game, int current) {
        MOVE[] possibleMoves = game.getPossibleMoves(current, lastMove);
        double maxQValue = Double.NEGATIVE_INFINITY;

        for (MOVE move : possibleMoves) {
            StateActionPair stateActionPair = new StateActionPair(current, move);
            double qValue = qValues.getOrDefault(stateActionPair, 0.0);
            if (qValue > maxQValue) {
                maxQValue = qValue;
            }
        }

        return maxQValue;
    }

    // Calculate the reward based on the game state
    private double calculateReward(Game game) {
        // int current = game.getPacmanCurrentNodeIndex();

        // // Reward for eating a pill
        // int[] pills = game.getPillIndices();
        // for (int pill : pills) {
        // if (current == pill && game.isPillStillAvailable(pill)) {
        // return PILL_REWARD;
        // }
        // }

        // // Penalty for getting caught by a non-edible ghost
        // for (Constants.GHOST ghost : Constants.GHOST.values()) {
        // if (game.getGhostEdibleTime(ghost) == 0 && game.getGhostLairTime(ghost) == 0)
        // {
        // int ghostLocation = game.getGhostCurrentNodeIndex(ghost);
        // if (ghostLocation != -1 && current == ghostLocation) {
        // return CAUGHT_BY_NON_EDIBLE_GHOST_PENALTY;
        // }
        // }
        // }

        // // Reward for eating an edible ghost
        // for (Constants.GHOST ghost : Constants.GHOST.values()) {
        // if (game.getGhostEdibleTime(ghost) > 0 &&
        // game.getGhostCurrentNodeIndex(ghost) == current) {
        // return EATING_EDIBLE_GHOST_REWARD;
        // }
        // }

        // // No immediate reward
        // return 0.0;
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

    

    private void displayQValues() {
        System.out.println("Q-values:");
        for (Map.Entry<StateActionPair, Double> entry : qValues.entrySet()) {
            StateActionPair stateActionPair = entry.getKey();
            double qValue = entry.getValue();
            System.out.println("State-Action Pair: " + stateActionPair.state + " - " + stateActionPair.action +
                    " | Q-value: " + qValue);
        }
        System.out.println();
    }

    // Represents a state-action pair for Q-learning
    private static class StateActionPair {
        private final int state;
        private final MOVE action;

        public StateActionPair(int state, MOVE action) {
            this.state = state;
            this.action = action;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            StateActionPair that = (StateActionPair) o;
            return state == that.state && action == that.action;
        }

        @Override
        public int hashCode() {
            return Objects.hash(state, action);
        }
    }

}
