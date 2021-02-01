import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * This screen represents our main screen in BuzzBin.
 * The receive screen is first loaded, and then the send screen slides
 * up from the top. The top bar retains minimize, exit, and title, other
 * buttons are faded in and out accordingly.
 *
 * @author Jacob Gordon
 * @version 1.0
 * @date 7/9/19
 **/
public class MainScreen {

    /**
     * Delays in milliseconds from button clicks to action.
     */
    public static final int EXIT_BUTTON_DELAY = 100;
    public static final int MIN_BUTTON_DELAY= 100;

    /**
     * Delay in miliseconds before select buttons and the select action.
     */
    public static final long SELECT_BUTTON_DELAY = 100;
    /**
     * Animation duration in miliseconds of the send screen animation pop-in
     */
    public static final int NAV_DURATION = 500;
    /**
     * Animation duration in miliseconds of the nav fade for the top bar
     */
    public static final int NAV_FADE_DURATION = 220;

    /**
     * Gets the current instance of our main screen
     * SINGLETON
     * @return instance
     */
    public static MainScreen getInstance() {
        if (instance == null) instance = new MainScreen();
        return instance;
    }

    /**
     * SINGLETON
     */
    private static MainScreen instance;

    /**
     * Controller for this screen.
     */
    public MainScreenC mainScreenC;

    /**
     * Start function called upon initialization of the splash screen.
     */
    public void start(String PIN) {
        Stage primaryStage = new Stage();
        FXMLLoader loader = new FXMLLoader();
        URL u = null;
        try {
            u = new URL(getClass().getResource("assets/layouts/layout_main.fxml").toExternalForm());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        loader.setLocation(u);
        mainScreenC = new MainScreenC();
        mainScreenC.primaryStage = primaryStage;
        mainScreenC.PIN = PIN;
        //setup controller
        loader.setController(mainScreenC);
        //load
        Parent root = null;
        try {
            root = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //setup stage
        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setTitle("BuzzBin");
        primaryStage.setScene(new Scene(root, 800, 550));
        primaryStage.setResizable(false);
        //final init step
        primaryStage.show();
        //set this instance
        instance = this;
    }
}
