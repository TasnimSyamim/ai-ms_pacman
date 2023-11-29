
import examples.StarterGhostComm.Blinky;
import examples.StarterGhostComm.Inky;
import examples.StarterGhostComm.Pinky;
import examples.StarterGhostComm.Sue;
import examples.StarterISMCTS.InformationSetMCTSPacMan;
import examples.StarterPacMan.*;
import pacman.Executor;
import pacman.controllers.IndividualGhostController;
import pacman.controllers.MASController;
import pacman.controllers.examples.po.POCommGhosts;
import pacman.game.Constants.*;
import pacman.game.internal.POType;

import java.util.ArrayList;
import java.util.EnumMap;

import javax.swing.SwingUtilities;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

/**
 * Created by pwillic on 06/05/2016.
 */
public class Main {

    /*
     * public static void main(String[] args) {
     * 
     * int sightRadius = 10; // 5000 is maximum
     * 
     * Executor executor = new Executor.Builder()
     * .setVisual(true)
     * .setPacmanPO(false)
     * .setTickLimit(20000)
     * .setScaleFactor(3) // Increase game visual size
     * .setPOType(POType.RADIUS) // pacman sense objects around it in a radius wide
     * fashion instead of straight
     * // line sights
     * .setSightLimit(sightRadius) // The sight radius limit, set to maximum
     * .build();
     * 
     * EnumMap<GHOST, IndividualGhostController> controllers = new
     * EnumMap<>(GHOST.class);
     * 
     * controllers.put(GHOST.INKY, new Inky());
     * controllers.put(GHOST.BLINKY, new Blinky());
     * controllers.put(GHOST.PINKY, new Pinky());
     * controllers.put(GHOST.SUE, new Sue());
     * 
     * MASController ghosts = new POCommGhosts(50);
     * 
     * int speed = 2; // smaller number will run faster
     * // executor.runGame(new SecondCustomAI(), ghosts, speed);
     * 
     * // executor.runGame(new RandomWalk(), ghosts, speed);
     * // executor.runGame(new AStar(), ghosts, speed);
     * 
     * // Q_RL qlearning_model = new Q_RL();
     * // executor.runGame(qlearning_model, ghosts, speed);
     * // System.out.println(qlearning_model.getRewards());
     * // executor.runGame(new QLearning(), new MASController(controllers), speed);
     * // executor.runGame(new Q_RL_NEW(), new MASController(controllers), speed);
     * // executor.runGame(new Dijkstra(), ghosts, speed);
     * // executor.runGame(new MCTS(), ghosts, speed);
     * // executor.runGame(new TreeSearchPacMan(), ghosts, speed);
     * // executor.runGame(new MyPacMan(), new MASController(controllers), speed);
     * 
     * }
     */

    private ArrayList<Double> data = new ArrayList<>();

    public static void main(String[] args) {
        int sightRadius = 10; // 5000 is maximum

        Executor executor = new Executor.Builder()
                .setVisual(true)
                .setPacmanPO(false)
                .setTickLimit(20000)
                .setScaleFactor(2) // Increase game visual size
                .setPOType(POType.RADIUS) // pacman sense objects around it in a radius wide fashion instead of straight
                                          // line sights
                .setSightLimit(sightRadius) // The sight radius limit, set to maximum
                .build();

        EnumMap<GHOST, IndividualGhostController> controllers = new EnumMap<>(GHOST.class);

        controllers.put(GHOST.INKY, new Inky());
        controllers.put(GHOST.BLINKY, new Blinky());
        controllers.put(GHOST.PINKY, new Pinky());
        controllers.put(GHOST.SUE, new Sue());

        MASController ghosts = new POCommGhosts(50);

        int speed = 0; // smaller number will run faster

        // executor.runGame(new InformationSetMCTSPacMan(), ghosts, speed);
        
        Dijkstra dijkstra_model = new Dijkstra();
        executor.runGame(dijkstra_model, ghosts, speed);
        ArrayList<Double> rewards_list = dijkstra_model.getRewards();

        RandomWalk random_model = new RandomWalk();
        // executor.runGame(random_model, ghosts, speed);

        AStar astar_model = new AStar();
        // executor.runGame(astar_model, ghosts, speed);
        // ArrayList<Double> rewards_list = astar_model.getRewards();

        Q_RL qlearning_model = new Q_RL();
        // executor.runGame(qlearning_model, ghosts, speed);
        // ArrayList<Double> rewards_list = qlearning_model.getRewards();


        SwingUtilities.invokeLater(() -> {
            Main mainInstance = new Main();
            mainInstance.setData(rewards_list); // Set the rewards_list to the data variable
            LineChart_AWT chart = new LineChart_AWT(
                    "Rewards Chart",
                    "Rewards vs Time Step",
                    mainInstance.getData());

            chart.pack();
            RefineryUtilities.centerFrameOnScreen(chart);
            chart.setVisible(true);
        });
    }

    public Main() {
        // Populate the data arraylist with some values
        
    }

    public ArrayList<Double> getData() {
        return data;
    }

    public void setData(ArrayList<Double> newData) {
        this.data = newData;
    }
}

class LineChart_AWT extends ApplicationFrame {
    private ArrayList<Double> data;

    public LineChart_AWT(String applicationTitle, String chartTitle, ArrayList<Double> data) {
        super(applicationTitle);
        this.data = data;

        JFreeChart lineChart = ChartFactory.createLineChart(
                chartTitle,
                "Index", "Data",
                createDataset(),
                PlotOrientation.VERTICAL,
                true, true, false);

        ChartPanel chartPanel = new ChartPanel(lineChart);
        chartPanel.setPreferredSize(new java.awt.Dimension(560, 367));
        setContentPane(chartPanel);
    }

    private DefaultCategoryDataset createDataset() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for (int i = 0; i < data.size(); i++) {
            dataset.addValue(data.get(i), "Data", String.valueOf(i));
        }

        return dataset;
    }
}
