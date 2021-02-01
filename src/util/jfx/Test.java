package util.jfx;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Test extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        ServerBrowserNode sbn = new ServerBrowserNode(150, 225);
        ScrollPane root = sbn;
        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setTitle("BuzzBin");
        primaryStage.setScene(new Scene(root, 400, 225));
        primaryStage.setResizable(false);
        primaryStage.show();
        Thread.sleep(50);
        sbn.updateAll();
    }
}


