package util.jfx;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * Cycles through a list of JavaFX.Image at a given speed.
 * ImageAnimation's can Start, Stop, Reset.
 * Play length also variable.
 * All time units are represented in milliseconds.
 *
 * @author Jacob Gordon
 * @version 1.0
 * @date 7/9/19
 **/
public class ImageAnimation {


    private int timePerSlide = 0;
    public boolean playing = false;
    public int index = 0;
    private int length = 0;
    private long lastUpdate = 0L;

    public void update(ImageView view, Image[] slides) {
        if (length != slides.length) length = slides.length;
        if (!playing) return;
        if (timePerSlide == 0) return;
        if (lastUpdate == 0L) {
            lastUpdate = System.currentTimeMillis();
            view.setImage(slides[index]);
        } else {
            long current = System.currentTimeMillis();
            if ((current - lastUpdate) >= timePerSlide) {
                view.setImage(slides[++index]);
                lastUpdate = System.currentTimeMillis();
            }
        }
    }

    public void startAnim() {
        playing = true;
        lastUpdate = 0L;
    }
    public void pauseAnim() {
        playing = false;
        lastUpdate = 0L;
    }
    public void restartAnim() {
        index = 0;
        lastUpdate = 0L;
        playing = true;
    }

    public void resetAnim(int index) {
        this.index = index;
        lastUpdate = 0L;
        playing = true;
    }

    public void reset() {
        timePerSlide = 0;
        playing = false;
        index = 0;
        length = 0;
        lastUpdate = 0L;
    }

    public int getTimePerSlide() {
        return timePerSlide;
    }

    public int getTotalTime() {
        return length * timePerSlide;
    }

    public void setTimePerSlade(int t) {
        timePerSlide = t;
    }

    public void setTimeTotal(int t) {
        timePerSlide = t / length;
    }
}
