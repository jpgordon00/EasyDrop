package util.jfx;

import util.Vector2;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

public class ServerBrowserNode extends ScrollPane {

    public static final Paint BG_TOP = Paint.valueOf("#46505c");
    public static final Paint BG_CELL = Paint.valueOf("#46505c");
    public static final Paint BG_SELECTED = Paint.valueOf("#8cc0ff");
    public final Paint DARK = Paint.valueOf("#333a42");


    private ServerBrowserNode() {}

    public ServerBrowserNode(double width, double height) {
        super();
        setMinWidth(width);
        setMinHeight(height);
        this.width = (float) width;
        this.height = (float) height;
        AnchorPane ap = new AnchorPane();
        ap.setMinWidth(width);
        ap.setMinHeight(cellHeight * (30));
        this.setContent(ap);
        create(ap);
    }

    public AnchorPane ap;

    public float width, height;
    public float cellHeight = 35;

    public String[] data = null;
    public BoxNode[] nodes;

    private void create(AnchorPane ap) {
        this.ap = ap;
        //TODO: fill data
        int n = 30;
        data = new String[n];
        for (int i = 0; i < n; i++) {
            data[i] = i == 29 ? "ADD..." : "Server " + i;
        }

        //add all other nodes
        nodes = new BoxNode[data.length];
        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = new BoxNode();
            nodes[i].width = width;
            nodes[i].height = cellHeight;
            nodes[i].nodes = new Node[2];
            //setup label
            Label l = new Label();
            nodes[i].nodes[1] = l;
            l.setText(data[i]);
            l.setTextFill(Paint.valueOf("#ffffff"));
            //setup circle
            Circle c = new Circle();
            nodes[i].nodes[0] = c;
            c.setRadius(24);
            c.setFill(Paint.valueOf("#22ff00"));
            //add background
            nodes[i].colors = new Paint[2];
            nodes[i].colors[0] = i == 0 ? BG_TOP : BG_CELL;
            nodes[i].colors[1] = BG_SELECTED;
            nodes[i].background_to_rect(null);
            //setup listeners
            if (i != 0) {
                final int fi = i;
                final ScrollPane fsp = this;
                //setup swap
                nodes[i].rect_bg.setOnMouseClicked(event -> {
                    String temp = ((Label) nodes[0].nodes[1]).getText();
                    ((Label) nodes[0].nodes[1]).setText(((Label) nodes[fi].nodes[0]).getText());
                    ((Label) nodes[fi].nodes[0]).setText(temp);
                    fsp.setVvalue(0);
                    updateAll();
                });
            } else {
                //setup perimeter w/ dark
                nodes[i].perimeter_thickness(3);
                nodes[i].perimeter_color(DARK);
                nodes[i].perimeter_enabled(false, true, false, false);
                nodes[i].perimeter_to_rects(null);
                ap.getChildren().addAll(nodes[i].perims);
            }
            ap.getChildren().add(nodes[i].rect_bg);
            //add label
            ap.getChildren().add(l);
            //TODO: note must update perimeter
            //add click listeners
        }
    }

    public void updateAll() {
        int i = 0;
        for (BoxNode bn: nodes) {
            Vector2[] vecs = bn.rel_node_positions(null);
            //for every node
            for (int z = 0; z < bn.nodes.length; z++) {
                bn.nodes[z].setTranslateX(vecs[z].x);
                bn.nodes[z].setTranslateY(vecs[z].y + (i * cellHeight));
            }
            //TODO: update perimeter
            if (bn.perims != null) {
                for (Rectangle r: bn.perims) {
                    if (r != null) r.toFront();
                }
            }
            //background
            bn.rect_bg.setTranslateX(0);
            bn.rect_bg.setTranslateY((i++ * cellHeight));
        }
    }

    public void hideAll() {
         for (BoxNode bn: nodes) {
             //assumes rect_bg is added to scene
             ap.getChildren().remove(bn.rect_bg);
             ap.getChildren().removeAll(bn.nodes);
             ap.getChildren().removeAll(bn.perims);
         }
    }

    public void showAll() {
        for (BoxNode bn: nodes) {
            //assumes rect_bg is added to scene
            ap.getChildren().add(bn.rect_bg);
            ap.getChildren().addAll(bn.nodes);
            ap.getChildren().addAll(bn.perims);
        }
    }
}