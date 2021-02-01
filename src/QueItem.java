import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

/**
 * This class represents a single item that is contained in a 'QueView'.
 * Each item contains a full string, sub string, and button to close it.
 * Represents a file.
 *
 * @author Jacob Gordon
 * @version 1.0
 * @date 13 Jul 2019
 **/
public class QueItem {

    /**
     * Titles to toggle between displaying.
     */
    public String titleFull, titleSub;
    public long lastClick = 0;

    public HBox hBox;
    public Button button;
    public Label label;

    double transX, transY;
    double clickX = 0;
    boolean dragging = false;

    public QueItem(String titleFull, String titleSub) throws Exception {
        this.titleFull = titleFull;
        this.titleSub = titleSub;
        FXMLLoader loader = new FXMLLoader(LaunchClient.class.getResource(QueView.LAYOUT_PATH));
        hBox = loader.load();
        //hBox.prefHeight(-1);
        //hBox.prefWidth(-1);
        //hBox.setStyle(lHbox.getStyle());
        //hBox.setLayoutX(lHbox.getLayoutX());
        //hBox.setLayoutY(lHbox.getLayoutY());
        button = (Button) loader.getNamespace().get("button");
        ImageView iv = new ImageView(LaunchClient.getInstance().images[0]);
        iv.setTranslateX(-8);
        button.setGraphic(iv);
        button.setStyle("-fx-background-color2");
        label = (Label) loader.getNamespace().get("label");
        label.setText(titleSub);
        transX = 0;
        transY = 0;
    }

    public QueItem(QueItem qi, String titleFull, String titleSub) throws Exception {
        this.titleFull = titleFull;
        this.titleSub = titleSub;
       // FXMLLoader loader = new FXMLLoader(LaunchClient.class.getResource(QueView.LAYOUT_PATH));
        hBox = new HBox();
        hBox.minWidth(qi.hBox.getMinWidth());
        hBox.minHeight(qi.hBox.getMinHeight());
        hBox.prefWidth(qi.hBox.getPrefWidth());
        hBox.prefHeight(qi.hBox.getPrefHeight());
        hBox.maxWidth(qi.hBox.getMaxWidth());
        hBox.maxHeight(qi.hBox.getMaxHeight());
        hBox.setStyle(qi.hBox.getStyle());
        hBox.setPadding(qi.hBox.getPadding());
        hBox.setAlignment(qi.hBox.getAlignment());
        button = new Button();
        button.setMinWidth(qi.button.getMinWidth());
        button.setMinHeight(qi.button.getMinHeight());
        button.prefWidth(qi.button.getPrefWidth());
        button.prefHeight(qi.button.getPrefHeight());
        button.setMaxWidth(qi.button.getMaxWidth());
        button.setMaxHeight(qi.button.getMaxHeight());
        button.setStyle(qi.button.getStyle());
        button.setPadding(qi.button.getPadding());
        HBox.setMargin(button, HBox.getMargin(qi.button));
        //button.setRipplerFill(qi.button.getRipplerFill());
        ImageView iv = new ImageView(LaunchClient.getInstance().images[0]);
        //iv.setTranslateX(-8);
        button.setGraphic(iv);
        label = new Label();
        label.setText(titleSub);
        label.setTextFill(qi.label.getTextFill());
        label.setFont(qi.label.getFont());
        label.setStyle(qi.label.getStyle());
        label.setPadding(qi.label.getPadding());
        label.setMinWidth(qi.label.getMinWidth());
        label.setMinHeight(qi.label.getMinHeight());
        label.prefWidth(qi.label.getPrefWidth());
        label.prefHeight(qi.label.getPrefHeight());
        label.setMaxWidth(qi.label.getMaxWidth());
        label.setMaxHeight(qi.label.getMaxHeight());
        transX = 0;
        transY = 0;
        hBox.getChildren().addAll(label, button);
    }

    public boolean toggleExpand() {
        if (lastClick == 0) {
            lastClick = System.currentTimeMillis();
        } else {
            if (System.currentTimeMillis() - lastClick <= QueView.QUE_TOGGLE_DELAY) return false;
        }
        lastClick = System.currentTimeMillis();
        if (label.getText().equals(titleSub)) {
            label.setText(titleFull);
        } else {
            label.setText(titleSub);
        }
        return true;
    }

    public double getWidth() {
        return hBox.getWidth();
    }

    public double getHeight() {
        return hBox.getHeight();
    }

    public void updatePos(double x, double y) {
            hBox.setTranslateX(transX);
            hBox.setTranslateY(transY);
            transX = x;
            transY = y;
    }
}
