import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This com.dialog represents a deletion action where the user wants to
 * delete pending files.
 *
 * @author Jacob Gordon
 * @version 1.0
 * @date 18 Jul 2019
 **/
public class DeleteDialog {

    /**
     * Title label for this com.dialog.
     */
    public Label label;

    /**
     * OUTCOME of this com.dialog.
     */
    public boolean SUCCESS = false;

    /**
     * Stage and pane for this com.dialog.
     */
    private Stage primaryStage;
    private AnchorPane anchorPane;

    /**
     * This listener is invoked when this com.dialog has finished.
     */
    private OnFinishListener listener;

    /**
     * Constructor to setup this com.dialog.
     */
    public DeleteDialog() {
        primaryStage = new Stage();

        Button buttonAcc = null;
        Button buttonCancel = null;
        label = null;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("assets/layouts/dialog_del.fxml"));
            Parent root = loader.load();
            buttonAcc = (Button) loader.getNamespace().get("buttonAcc");
            buttonCancel = (Button) loader.getNamespace().get("buttonCancel");
            label = (Label) loader.getNamespace().get("label");
            buttonAcc.setFocusTraversable(false);
            buttonCancel.setFocusTraversable(false);

            //use enter key as accept
            //use del key as cancel
            anchorPane = (AnchorPane) loader.getNamespace().get("anchorPane");
            anchorPane.requestFocus();
            anchorPane.setOnMouseClicked(event -> {
                anchorPane.requestFocus();
            });
            anchorPane.setOnKeyPressed(event -> {
                if (event.getCode().getCode() == KeyCode.ENTER.getCode()) {
                    SUCCESS = true;
                    onFinish();
                } else if (event.getCode().getCode() == KeyCode.ESCAPE.getCode()) {
                    onFinish();
                }
            });
            buttonAcc.setOnMouseClicked(event -> {
                SUCCESS = true;
                onFinish();
            });
            buttonCancel.setOnMouseClicked(event -> {
                onFinish();
            });

            primaryStage.setAlwaysOnTop(true);
            primaryStage.initStyle(StageStyle.UNDECORATED);
            primaryStage.setTitle("Delete Pending Files");
            primaryStage.setScene(new Scene(root, 300, 140));
            primaryStage.setResizable(false);
            //final init step
            primaryStage.show();

            primaryStage.requestFocus();
            anchorPane.requestFocus();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Invoked to gain focus for this com.dialog.
     */
    public void requestFocus() {
        primaryStage.requestFocus();
        anchorPane.requestFocus();
    }

    /**
     * Sets the finish listener for this com.dialog to use.
     * @param listener instance of 'OnFinishListener' for this com.dialog to use.
     */
    public void addFinishListener(OnFinishListener listener) {
        this.listener = listener;
    }

    /**
     * Invoked by this com.dialog whenever this com.dialog has finished its operations.
     * Invokes our listener.
     * Closes the window in 'DELAY' milis.
     */
    private void onFinish() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        Runnable task = () -> {
            Runnable r = () -> primaryStage.close();
            Platform.runLater(r);
        };
        scheduler.schedule(task, 50, TimeUnit.MILLISECONDS);
        scheduler.shutdown();
        if (listener != null) listener.finish(SUCCESS);
    }

    /**
     * Sets the label for the amount of items that need to be accepted by this com.dialog.
     * @param i number of items.
     */
    public void setNumItems(int i) {
        label.setText(label.getText().replace("x", i + ""));
    }
}
