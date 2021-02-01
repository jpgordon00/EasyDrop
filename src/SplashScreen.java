import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.ClientSettings;

/**
 * This class represents the SplashScreen for BuzzBin.
 * It should be treated as a Singleton and is invoked by 'LaunchClient'.
 *
 * @author Jacob Gordon
 * @version 1.0
 * @date 7/5/19
 **/
public class SplashScreen extends Application {


    /**
     * Instance of our SplashScreen set upon 'start()'.
     */
    public static SplashScreen instance;

    /**
     * Constructor.
     */
    public SplashScreen() {
        super();
    }

    /**
     * Timer in a separate Thread.
     */
    private Stage primaryStage;
    private long current = 0;
    private long time = 0;
    public boolean runTimer = true;


    /**
     * Start function called upon initialization of the splash screen.
     * @param primaryStage stage given by JavaFX.
     * @throws Exception
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        //load images
        LaunchClient.getInstance().loadImages();
        //temp
        LaunchClient.getInstance().clientSettings = ClientSettings.init();
        LaunchClient.getInstance().startNet();


        FXMLLoader loader = new FXMLLoader(getClass().getResource("assets/layouts/layout_splash.fxml"));
        SplashScreenC sc = new SplashScreenC();
        sc.primaryStage = primaryStage;
        //setup controller
        loader.setController(sc);
        //load
        Parent root = loader.load();
        //setup stage
        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setTitle("BuzzBin");
        primaryStage.setScene(new Scene(root, 400, 225));
        primaryStage.setResizable(false);
        //final init step
        primaryStage.show();
        //set this instance
        instance = this;
        this.primaryStage = primaryStage;
    }

    public void nextScreen(String PIN) {
        Runnable r = () -> {
            primaryStage.hide();
            MainScreen.getInstance().start(PIN);
        };
        Platform.runLater(r);
        runTimer = false;
    }
}
