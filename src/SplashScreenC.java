import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import net.ClientSettings;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Jacob Gordon
 * @version 1.0
 * @date 7/5/19
 **/
public class SplashScreenC {

    public Stage primaryStage;
    @FXML
    public Button buttonExit, buttonRetry;

    @FXML
    public AnchorPane anchorPane;

    @FXML
    public TextArea t;

    public Label label;

    /**
     * Function that's called when the view is created.
     */
    @FXML
    private void initialize() {
        label = new Label();
        label.setText((String) LaunchClient.getInstance().clientSettings.keyValueMap.get(ClientSettings.IP));
        anchorPane.getChildren().add(label);
        label.setTranslateY(50);
        label.setTranslateX(100);

        buttonExit.setText("");
        Image img = LaunchClient.getInstance().images[0];
        buttonExit.setGraphic(new ImageView(img));
        buttonRetry.setGraphic(new ImageView(LaunchClient.getInstance().images[3]));

        //this button should not get first focus
        buttonExit.setFocusTraversable(false);
        buttonRetry.setFocusTraversable(false);

        buttonExit.setOnMouseClicked(buttonExit -> {
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            Runnable task = () -> {
                System.exit(0);
            };
            scheduler.schedule(task, LaunchClient.EXIT_CLICK_DELAY, TimeUnit.MILLISECONDS);
            scheduler.shutdown();
        });
        //buttonExit.setRipplerFill(Paint.valueOf("#f00299"));
        //buttonExit.setButtonType(Button.ButtonType.RAISED);
        buttonExit.setShape(new Circle(1));

        buttonRetry.setOnMouseClicked(buttonRetry -> {
            LaunchClient.getInstance().startNet();
        });
        //buttonRetry.setRipplerFill(Paint.valueOf("#f00299"));
        //buttonRetry.setButtonType(Button.ButtonType.RAISED);
        buttonRetry.setShape(new Circle(1));


        final AnchorPane ap = anchorPane;
        anchorPane.setOnMouseClicked(anchorPane -> ap.requestFocus());
    }
}
