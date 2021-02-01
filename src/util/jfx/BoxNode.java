package util.jfx;

import util.Vector2;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;

public class BoxNode {

    public Node[] nodes;
    public Paint[] colors; //length of 2
    public Rectangle rect_bg;
    public float padding;
    public float width, height;
    public BoxPerim perim;
    public Rectangle[] perims;

    //0 = idle
    //1 = clicked
    public int state = 0;

    //0 = nothong
    //1 = clicked
    public int state_mouse = 0;

    public BoxNode() {
        perim = new BoxPerim();
    }

    public Vector2[] rel_node_positions(Vector2 off) {
        if (nodes == null) return null;
        Vector2[] vecs = new Vector2[nodes.length];
        float tw = (nodes.length - 1) * padding;
        for (Node n: nodes) tw += n.getLayoutBounds().getWidth();
        int i = 0;
        float x = (width / 2) - (tw / 2);
        for (Node n: nodes) {
            vecs[i++] = new Vector2(x, (height / 2) - ((float) n.getLayoutBounds().getHeight() / 2));
            x += padding + (float) n.getLayoutBounds().getWidth();
            if (off != null) vecs[i-1].add(off.x, off.y);
        }
        return vecs;
    }

    public void perimeter_enabled(boolean ... bools) {
        if (bools.length != 4) return;
        perim.lines_enabled = bools;
    }

    public void perimeter_thickness(float thickness) {
        perim.thickness = thickness;
    }

    //accepts many colors or one color.
    public void perimeter_color(Paint ... color) {
        if (color.length == 1) {
            Paint c = color[0];
            perim.colors = new Color[4];
            for (int i = 0; i < 4; i++) {
                perim.colors[i] = c;
            }
            return;
        }
        perim.colors = color;
    }

    public void perimeter_to_rects(Vector2 off) {
        //top, bot, left, right
        if (perims == null) {
            perims = new Rectangle[4];
            perims[0] = new Rectangle(0, 0, width, perim.thickness);
            perims[1] = new Rectangle(0, height - perim.thickness, width, perim.thickness);
            perims[2] = new Rectangle(0, 0, perim.thickness, height);
            perims[3] = new Rectangle(width - perim.thickness, 0, perim.thickness, height);
            perims[0].setFill(perim.colors[0]);
            perims[0].setVisible(perim.lines_enabled[0]);
            perims[1].setFill(perim.colors[1]);
            perims[1].setVisible(perim.lines_enabled[1]);
            perims[2].setFill(perim.colors[2]);
            perims[2].setVisible(perim.lines_enabled[2]);
            perims[3].setFill(perim.colors[3]);
            perims[3].setVisible(perim.lines_enabled[3]);

            //add offset
            if (off != null) {
                perims[0].setX(perims[0].getX() + off.x);
                perims[0].setY(perims[0].getY() + off.y);
                perims[1].setX(perims[0].getX() + off.x);
                perims[1].setY(perims[1].getY() + off.y);
                perims[2].setX(perims[0].getX() + off.x);
                perims[1].setY(perims[2].getY() + off.y);
                perims[3].setX(perims[0].getX() + off.x);
                perims[1].setY(perims[3].getY() + off.y);
            }
        } else {
            if (off != null) {
                perims[0].setTranslateX(off.x);
                perims[0].setTranslateY(off.x);
            }
            perims[0].setFill(perim.colors[0]);
            perims[0].setVisible(perim.lines_enabled[0]);
            perims[1].setFill(perim.colors[1]);
            perims[1].setVisible(perim.lines_enabled[1]);
            perims[2].setFill(perim.colors[2]);
            perims[2].setVisible(perim.lines_enabled[2]);
            perims[3].setFill(perim.colors[3]);
            perims[3].setVisible(perim.lines_enabled[3]);
        }
    }


    //adds listeners to rects
    public void background_to_rect(Vector2 off) {
        if (rect_bg == null) {
            rect_bg = new Rectangle(0, 0, width, height);
            rect_bg.setFill(state == 0 ? colors[0] : colors[1]);
            if (off != null) {
                rect_bg.setTranslateX(off.x);
                rect_bg.setTranslateY(off.y);
            }
            final Rectangle frect_bg = rect_bg;
            //setup state events for altering
            rect_bg.setOnMousePressed(event -> {
                state = 1;
                state_mouse = 1;
                this.background_to_rect(null);
            });
            rect_bg.setOnMouseReleased(event -> {
                state_mouse = 0;
                if (state == 1) {
                    state = 0;
                    this.background_to_rect(null);
                }
            });
            rect_bg.setOnMouseExited(event -> {
                if (state == 1) {
                    state = 0;
                    this.background_to_rect(null);
                }
            });
            rect_bg.setOnMouseEntered(event -> {
                if (state_mouse == 1 && state == 0) {
                    state = 1;
                    this.background_to_rect(null);
                }
            });
        } else {
            if (off == null) {
                //rect_bg.setTranslateX(0);
                //rect_bg.setTranslateY(0);
            } else {
                rect_bg.setTranslateX(off.x);
                rect_bg.setTranslateY(off.y);
            }
            rect_bg.setFill(state == 0 ? colors[0] : colors[1]);
        }

    }
}




class BoxPerim {
    public Node parent;
    Paint[] colors;
    //top, bot, left, right
    public boolean lines_enabled[] = {true, true, true, true};
    public float thickness;
}
