import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This class represents a com.dialog that is shown once items are
 * requested to be downloaded by the user.
 *
 * @author Jacob Gordon
 * @version 1.0
 * @date 18 Jul 2019
 **/
public class AcceptDialog {

    /**
     * Title label for this com.dialog.
     */
    public Label label;

    /**
     * Directory that is chosen from this com.dialog.
     */
    public File dir;

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
     * @param dc directory chooser to be used for this com.dialog.
     * @param window to display the DC.
     */
    public AcceptDialog(DirectoryChooser dc, Window window) {
        primaryStage = new Stage();

        Button buttonAcc = null;
        Button buttonCancel = null;
        Button buttonSelect = null;
        TextField textField = null;
        label = null;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("assets/layouts/dialog_accept.fxml"));
            Parent root = loader.load();
            buttonAcc = (Button) loader.getNamespace().get("buttonAcc");
            buttonCancel = (Button) loader.getNamespace().get("buttonCancel");
            buttonSelect = (Button) loader.getNamespace().get("buttonSelect");
            label = (Label) loader.getNamespace().get("label");
            textField = (TextField) loader.getNamespace().get("textField");
            buttonAcc.setFocusTraversable(false);
            buttonAcc.setDisable(true);
            buttonCancel.setFocusTraversable(false);
            buttonSelect.setFocusTraversable(false);

            final Button bAcc = buttonAcc;
            final Button bCanc = buttonCancel;

            final TextField tf = textField;
            anchorPane = (AnchorPane) loader.getNamespace().get("anchorPane");
            textField.setEditable(false);
            buttonSelect.setOnMouseClicked(event -> {
                primaryStage.setAlwaysOnTop(false);
                System.out.println("pre");
                dir = dc.showDialog(window);
                System.out.println("post");
                primaryStage.setAlwaysOnTop(true);
                bAcc.setDisable(dir == null);
                tf.setText(dir == null ? "..." : dir.getAbsolutePath());
                ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
                Runnable task = () -> {
                    Runnable r = () -> {
                        primaryStage.requestFocus();
                        anchorPane.requestFocus();
                    };
                    Platform.runLater(r);
                };
                scheduler.schedule(task, 75, TimeUnit.MILLISECONDS);
                scheduler.shutdown();
            });

            //use enter key as accept
            //use del key as cancel
            anchorPane.requestFocus();
            anchorPane.setOnMouseClicked(event -> {
                anchorPane.requestFocus();
            });
            anchorPane.setOnKeyPressed(event -> {
                System.out.println("KP");
                if (event.getCode().getCode() == KeyCode.ENTER.getCode()) {
                    System.out.println("ENTER");
                    if (!bAcc.isDisabled()) {
                        SUCCESS = true;
                        onFinish();
                    }
                } else if (event.getCode().getCode() == KeyCode.ESCAPE.getCode()) {
                    onFinish();
                }
            });
            buttonAcc.setOnMouseClicked(event -> {
                if (!bAcc.isDisabled()) {
                    SUCCESS = true;
                    onFinish();
                }
            });
            buttonCancel.setOnMouseClicked(event -> {
                onFinish();
            });
            textField.setFocusTraversable(false);

            primaryStage.setAlwaysOnTop(true);
            primaryStage.initStyle(StageStyle.UNDECORATED);
            primaryStage.setTitle("Download Pending Files");
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
