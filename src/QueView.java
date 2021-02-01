import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This class is able to create and display 'QueItem', which are rectangles that sit on the top
 * of 'send_content.fxml' and represent each que'd file. Each node displays the file's location
 * and a button to close/destroy the que'd file.
 * This class displays ques from a start x, y, and with a margin x for each added node. Once node's
 * exceed the panes width, a margin y is added for the node.
 * Nodes can be expanded and removed.
 * Nodes can be dragged through and any amount can exist on a row.
 * Number of items per nodes can be set explicitly, or when above a target number of nodes, they
 * are split evenly throughout the rows.
 *
 * @author Jacob Gordon
 * @version 1.0
 * @date 13 Jul 2019
 **/
public class QueView {

    /**
     * Location of FXML layout that all que-nodes use.
     */
    public static final String LAYOUT_PATH = "assets/layouts/que_node.fxml";

    /**
     * MS of translation animation for each node.
     */
    public static final long ANIM_TIME = 200;

    /**
     * Maximum number of files to be loaded from a single select or drag and drop action.
     */
    public static final int MAX_QUED_FILES = 250;

    /**
     * Time in MS after loading that the que can be updated.
     */
    public static final long INITIAL_QUE_DELAY = 75;

    /**
     * Minimum time required before a node is toggled.
     */
    public static final long QUE_TOGGLE_DELAY = 50;

    /**
     * Delay in between a node toggling and its position updating.
     */
    public static final long QUE_UPDATE_DELAY = 50;
    public static final long MIN_CLEAR_EXPAND = 50;
    /**
     * Number of additional pixels between each node in the respective
     * axis.
     */
    public int marginX = 12, marginY = 12;
    /**
     * Prevent expand from being invoked less than 'MIN_CLEAR_EXPAND' ms apart.
     */
    public long lastClearExpand = 0;
    /**
     * Target number of items per line.
     * 0 if all items should be contained on one line.
     */
    public int numItemsPerRow = 0;
    /**
     * Minimum number of items that would automatically change 'numItemsPerLine' to fit
     * all items across 'numItemsRow' rows..
     */
    public int minItemsSplit = 20;
    /**
     * Number of rows to split across all items once size() > minItemsSplit.
     */
    public int numItemsSplitRow = 4;
    /**
     * 'numItemsPerRow' is set to this value after its reset.
     * Set this and numItemsPerRow in constructor for default.
     */
    public int targetItemsPerRow = 0;
    /**
     * Initial start in pixels relative to the pane of added QueItems.
     */
    double startX = 0, startY = 0;
    /**
     * Maximum width that QueItem's can't expand past.
     */
    double paneWidth = 820;
    /**
     * AnchorPane that we are attaching all these QueItem's to.
     */
    private AnchorPane pane;
    /**
     * List of all active nodes.
     */
    private ArrayList<QueItem> items = new ArrayList<QueItem>();

    /**
     * Constructor to set pane.
     */
    public QueView(AnchorPane pane) {
        this.pane = pane;
    }

    /**
     * Adds a que item given a file path.
     *
     * @param filePath full path of the file.
     * @param fileName name of the file.
     * @param update   true if to invoke 'update()' in this function.
     * @return the QueItem created.
     */
    public QueItem addItem(String filePath, String fileName, boolean update) {
        QueItem qi = null;
        //avoid duplicate que'd files
        if (getItem(filePath) > -1) return null;
        try {
            if (items.isEmpty()) {
                items.add(qi = new QueItem(filePath, fileName));
                QueItem f = items.remove(0);
                items.add(qi = new QueItem(f, filePath, fileName));
                pane.getChildren().remove(f);
            } else {
                qi = new QueItem(items.get(0), filePath, fileName);
                items.add(qi);

            }
            final QueItem q = qi;
            qi.label.setOnMouseClicked(event -> {
                if (q.toggleExpand()) {
                    clearExpand(q.titleFull);
                    update(QUE_UPDATE_DELAY);
                }
            });
            qi.hBox.setOnMouseClicked(event -> {
                if (q.toggleExpand()) {
                    clearExpand(q.titleFull);
                    update(QUE_UPDATE_DELAY);
                }
            });
            qi.button.setOnMouseClicked(button -> {
                clearExpand(q.titleFull);
                removeItem(filePath);
                update(QUE_UPDATE_DELAY);
            });
            //setup drag
            qi.label.setOnMousePressed(event -> {
                q.clickX = event.getScreenX();
            });
            qi.label.setOnMouseDragged(event -> {
                //lastClearExpand = System.currentTimeMillis();
                double vX = event.getScreenX() - q.clickX;
                if (Math.abs(vX) > 1) {
                    for (Integer z : getItemsAtY(q.transY)) {
                        QueItem item = items.get(z);
                        item.updatePos(item.transX + vX, item.transY);
                    }
                    q.clickX = event.getScreenX();
                    q.dragging = true;
                }
            });
            qi.label.setOnMouseReleased(event -> {
                if (q.dragging) {
                    q.lastClick = System.currentTimeMillis();
                    q.dragging = false;
                    q.transX = q.hBox.getTranslateX();
                    //check if the last two nodes are visible
                    ArrayList<QueItem> row = getItemsAtYObj(q.hBox.getTranslateY());
                    double d = 0;
                    QueItem qq = row.get(0);
                    if (((qq.hBox.getTranslateX() + (qq.hBox.getWidth() / 2)) > (pane.getWidth() / 2))) {
                        d = (startX) - (q.hBox.getTranslateX());
                    }
                    if (row.size() > 1) {
                        qq = row.get(row.size() - 1);
                        if ((qq.hBox.getTranslateX() + (qq.hBox.getWidth() / 2) < (pane.getWidth() / 2))) {
                            d = (paneWidth - 25 - q.getWidth()) - (q.hBox.getTranslateX());
                        }
                    }
                    if (d == 0) return;
                    for (int z = 0; z < row.size(); z++) {
                        QueItem i = row.get(z);
                        i.transX += d;
                        //ensure each node is w + marginX away from each other
                        if (z > 0) {
                            QueItem i2 = items.get(z - 1);
                            if (i.transX - i2.transX != (marginX + i2.getWidth())) {
                                //i.transX = i2.transX + i2.getWidth() + marginX;
                            }
                            if (i2.transX + i2.getWidth() + marginX != i.transX) {
                                i2.transX = i.transX - marginX - i2.getWidth();
                            }
                        }
                    }
                    for (int z = 0; z < row.size(); z++) {
                        QueItem i = row.get(z);
                        if (false) {
                            TranslateTransition trans = new TranslateTransition(Duration.millis(ANIM_TIME), i.hBox);
                            trans.setFromX(i.hBox.getTranslateX());
                            trans.setToX(i.transX);
                            trans.play();
                            trans.setOnFinished(e -> {
                                i.transX = i.hBox.getTranslateX();
                            });
                        } else {
                            i.hBox.setTranslateX(i.transX);
                        }
                    }
                }
            });
        } catch (Exception ex) {
            return null;
        }
        pane.getChildren().add(qi.hBox);
        if (update) update(INITIAL_QUE_DELAY);
        return qi;
    }

    /**
     * Clears all expanded tags in the QueView, ignoring the QueView with the given parameter.
     *
     * @param t the title of the Que to ignore.
     */
    private void clearExpand(String t) {
        if (lastClearExpand == 0) {
            lastClearExpand = System.currentTimeMillis();
        } else {
            if (System.currentTimeMillis() - lastClearExpand <= MIN_CLEAR_EXPAND) return;
            lastClearExpand = System.currentTimeMillis();
        }
        for (QueItem i : items) {
            if (i.titleFull.equals(t) || i.titleSub.equals(t)) continue;
            i.label.setText(i.titleSub);
        }
    }

    /**
     * Sets the translation positions of all items.
     */
    public void update() {
        //split items if needed
        if (size() >= minItemsSplit && minItemsSplit != 0) {
            numItemsPerRow = size() / numItemsSplitRow;
        } else {
            numItemsPerRow = targetItemsPerRow;
        }
        double cX = startX, cY = startY;
        for (int z = 0; z < items.size(); z++) {
            QueItem item = items.get(z);
            double w = item.getWidth();
            double h = item.getHeight();
            item.transX = cX;
            item.transY = cY;
            cX += w + marginX;
            if (numItemsPerRow > 0) {
                if (z % numItemsPerRow == 0 && z > 0) {
                    cX = startX;
                    cY += h + marginY;
                }
            }
        }
        //move an expanded tab to be centered
        if (getExpanded() > -1) {
            QueItem q = items.get(getExpanded());
            if (!isItemOnScreen(q)) {
                double d = (q.transX + (q.getWidth() / 2)) - (paneWidth / 2);
                for (int z = 0; z < getItemsAtY(q.transY).size(); z++) {
                    QueItem item = items.get(z);
                    item.transX -= d;
                }
            }
        }
        //anim if only x has changed and if on screen
        //otherwise snap to pos
        for (QueItem item : items) {
            if (item.hBox.getTranslateX() != item.transX && item.hBox.getTranslateY() == item.transY && item.hBox.getTranslateX() != 0 && isItemOnScreen(item, 30)) {
                TranslateTransition trans = new TranslateTransition(Duration.millis(ANIM_TIME), item.hBox);
                trans.setFromX(item.hBox.getTranslateX());
                trans.setToX(item.transX);
                trans.play();
                trans.setOnFinished(event -> item.transX = item.hBox.getTranslateX());
            } else {
                item.updatePos(item.transX, item.transY);
            }
        }
    }

    /**
     * Sets the translation positions of all items in delay time.
     *
     * @param delay in milliseconds from this function's invocation to 'update()' being invoked.
     */
    public void update(long delay) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        Runnable task = () -> {
            Runnable r2 = this::update;
            Platform.runLater(r2);
        };
        scheduler.schedule(task, delay, TimeUnit.MILLISECONDS);
        scheduler.shutdown();
    }

    /**
     * Gets a list of all items at the given y value.
     *
     * @param y translation-y to find QueItems of.
     * @return a list of all items, in order, at the given y value.
     */
    private ArrayList<Integer> getItemsAtY(double y) {
        ArrayList<Integer> items = new ArrayList<>();
        for (int i = 0; i < this.items.size(); i++) {
            if (this.items.get(i).transY == y) items.add(i);
        }
        return items;
    }

    /**
     * Gets a list of all items at the given y value.
     *
     * @param y translation-y to find QueItems of.
     * @return a list of all items, in order, at the given y value.
     */
    private ArrayList<QueItem> getItemsAtYObj(double y) {
        ArrayList<QueItem> items = new ArrayList<>();
        for (int i = 0; i < this.items.size(); i++) {
            if (this.items.get(i).transY == y) items.add(this.items.get(i));
        }
        return items;
    }

    /**
     * Finds a que item with a given fileName.
     *
     * @param s fileName or sub name to retrieve the given item.
     * @return the found QueItem or -1.
     */
    public Integer getItem(String s) {
        for (int z = 0; z < items.size(); z++) {
            QueItem i = items.get(z);
            if (i.titleFull.equals(s) || i.titleSub.equals(s)) return z;
        }
        return -1;
    }

    /**
     * Finds the first expanded item (should be the only expanded item).
     *
     * @return the extended que, or nu-1ll.
     */
    public Integer getExpanded() {
        for (int z = 0; z < this.items.size(); z++) {
            QueItem i = items.get(z);
            if (i.label.getText().equals(i.titleFull)) return z;
        }
        return -1;
    }

    /**
     * Returns true if there is an expanded label in this row.
     *
     * @param y value to compare other node's y to.
     * @return true if their contains an expanded item in the row.
     */
    public boolean hasExpandedInRow(int y) {
        return items.get(getExpanded()).hBox.getTranslateY() == y;
    }

    /**
     * Returns true if the given item is contained within the screen's x and width values.
     *
     * @param qi item to check if its on screen.
     * @return true if the item is fully contained. False if out of bounds.
     */
    public boolean isItemOnScreen(QueItem qi) {
        return qi.transX + qi.getWidth() < paneWidth && qi.transX > 0;
    }

    public boolean isItemOnScreen(QueItem qi, double p) {
        return qi.transX + qi.getWidth() < (paneWidth + p) && qi.transX > (-p);
    }

    /**
     * Removes the item given a file name.
     *
     * @param s fileName or sub name to retrieve the given item.
     * @return true if successfully removed.
     */
    public boolean removeItem(String s) {
        Iterator<QueItem> it = items.iterator();
        while (it.hasNext()) {
            QueItem i = it.next();
            if (i.titleFull.equals(s) || i.titleSub.equals(s)) {
                pane.getChildren().remove(i.hBox);
                it.remove();
                update();
                return true;
            }
        }
        return false;
    }

    /**
     * Removes the item given an index.
     *
     * @param index of the item according to 'items'.
     * @return true if successfully removed.
     */
    public boolean removeItem(int index) {
        Iterator<QueItem> it = items.iterator();
        int c = 0;
        while (it.hasNext()) {
            QueItem i = it.next();
            if (index == c++) {
                pane.getChildren().remove(i.hBox);
                it.remove();
                return true;
            }
        }
        return false;
    }

    /**
     * Sets the target number of items per row.
     * This many que's will be displayed per line given size is less than 'minItemsSplit'.
     *
     * @param q number of rows to target for.
     */
    public void setNumItemsPerRow(int q) {
        numItemsPerRow = q;
        targetItemsPerRow = q;
    }

    /**
     * Sets the minimum number of items that would trigger a split between
     * all rows.
     *
     * @param q minimum number of items.
     */
    public void setNumMinSplit(int q) {
        minItemsSplit = q;
    }

    /**
     * Clears all qued items.
     */
    public void clear() {
        Iterator<QueItem> it = items.iterator();
        while (it.hasNext()) {
            QueItem i = it.next();
                pane.getChildren().remove(i.hBox);
                it.remove();
        }
    }

    /**
     * Gets the number of items this QueView is responsible for.
     *
     * @return size of the que'd items.
     */
    public int size() {
        return items.size();
    }

    /**
     * Gets an array of all file paths in the que.
     *
     * @return an array of file paths.
     */
    public String[] getPaths() {
        String[] paths = new String[items.size()];
        if (items.size() == 0) return paths;
        int c = 0;
        for (QueItem i : items) {
            paths[c++] = i.titleFull;
        }
        return paths;
    }


}

class QueListener {
}
