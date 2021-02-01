import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

/**
 * This class represents the screen for our BuzzBin com.net.server.
 * This screen contains a text area for a Server Log.
 * Upon closing this com.net.server, BuzzBin will close.
 *
 * @author Jacob Gordon
 * @version 1.0
 * @date 7/7/19
 **/
public class ServerScreen extends Application {

    /**
     * Instance of this screen set after this view is created.
     */
    public static ServerScreen instance = null;

    /**
     * Constructor.
     */
    public ServerScreen() {
        super();
    }

    /**
     * Instance of our controller for this ServerScreen.
     */
    public ServerScreenC sc;

    /**
     * This function is called when this screen is brought to existence.
     * @param primaryStage for this app.
     * @throws Exception
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        //set static loc
        LaunchServer.getInstance().start();

        instance = this;
        FXMLLoader loader = new FXMLLoader();
        URL u = new URL(getClass().getResource("assets/layouts/layout_server.fxml").toExternalForm());
        loader.setLocation(u);
        sc = new ServerScreenC();
        //setup controller
        loader.setController(sc);
        //load
        Parent root = loader.load();
        //setup stage
        primaryStage.setTitle("[Server] BuzzBin");
        primaryStage.setScene(new Scene(root, 400, 225));
        //final init step
        primaryStage.show();

        //close the app upon exit button3
        primaryStage.setOnHiding(event -> Platform.runLater(() -> {
            System.exit(0);
        }));

        //print shit from our BinServer
        LaunchServer.getInstance().server.printStartup();
    }

    /**
     * Appends the current text to our TextArea in this com.net.server screen.
     * @param text to append, with a line break at the end.
     */
    public void appendText(String text) {
        if (sc != null) {
            if (!text.endsWith("\n")) text += "\n";
            sc.textArea.appendText(text);
        }
    }
}
