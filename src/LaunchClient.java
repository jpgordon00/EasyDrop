import util.FileUtils;
import javafx.application.Application;
import javafx.scene.image.Image;
import net.BinClient;
import net.ClientSettings;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Use this Singleton to launch the BuzzBin client.
 * Holds settings and network classes, which start after an initial delay.
 *
 * @author Jacob Gordon
 * @version 1.0
 * @date 7/5/19
 **/
public class LaunchClient {

    /**
     * Delay before network is initiated for this app.
     * In miliseconds.
     */
    public static final int START_DELAY_NETWORK = 3000;

    /**
     * Delay before the window is initiated for this app.
     * In miliseconds.
     */
    public static final int START_DELAY_FX = 100;

    /**
     * Delay in miliseconds from exit click to exit action.
     */
    public static final int EXIT_CLICK_DELAY = 400;

    /**
     * Index of all assets in 'images'.
     */
    public static final int ICON_EXIT = 0;

    /**
     * Singleton for this class.
     */
    private static LaunchClient instance;
    static {
        instance = new LaunchClient();
    }

    /**
     * Gets the only instance for our LaunchClient.
     * @return the instance for dis class.
     */
    public static LaunchClient getInstance() {
        return instance;
    }

    /**
     * Ensure Singleton.
     */
    private LaunchClient() {
    }

    /**
     * Instance of our net.ClientSettings.
     * This class is used for clientSettings and persistent values.
     */
    public ClientSettings clientSettings;

    /**
     * Instance of our BinServer.
     * This class is used for all things networking in BuzzBin.
     */
    public BinClient client;

    /**
     * All image assets that need loading.
     * Invoked by SplashScreen.
     */
    public Image[] images;

    /**
     * Main func.
     * 'start()' is invoked after a delay.e
     * @param args ignore args.
     */
    public static void main(String[] args) {
        //initiate network & settings functions after each after delay
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        Runnable task = () -> getInstance().startNet();
        scheduler.schedule(task, START_DELAY_NETWORK, TimeUnit.MILLISECONDS);
        scheduler.shutdown();
        getInstance().clientSettings = ClientSettings.init();
        Application.launch(SplashScreen.class,null);
    }


    /**
     * Starts the network functionality.
     * Loads all assets.
     */
    public void startNet() {
        Runnable r = () -> {
                client = new BinClient();
                client.setConnectionFinishedListener(pin -> {
                    SplashScreen.instance.nextScreen(pin);
                });
                client.init((String) clientSettings.keyValueMap.get(ClientSettings.IP), (int) clientSettings.keyValueMap.get(ClientSettings.TCP), (int) clientSettings.keyValueMap.get(ClientSettings.UDP), (int) clientSettings.keyValueMap.get(ClientSettings.MEM), (String) clientSettings.keyValueMap.get(ClientSettings.UID));
            };
        /**
         * TODO debug
         */
        FileUtils.printKeyValueMap(getInstance().clientSettings.keyValueMap, true);
        new Thread(r).start();
    }

    /**
     * Loads all assets for this app in this Thread.
     */
    public void loadImages() {
        images = loadImages("icon_exit.png", "icon_minimize.png", "icon_menu.png", "icon_retry.png", "icon_send.png");
    }

    /**
     * Helper func to load an array of images.
     * @param images
     * @return
     */
    private Image[] loadImages(String ... images) {
        Image[] img = new Image[images.length];
        int c = 0;
        for (String i : images) {
            //img[c++] = new Image(getClass().getClassLoader().getResourceAsStream("/" + i));
            String loc = getClass().getResource("assets/" + i).toExternalForm();
            System.out.println("FOR: " + loc);
            img[c++] = new Image(loc);
        }
        int rl = 0;
        for (Image i: img)  if (i != null) rl++;
        return Arrays.copyOf(img, rl);
    }
}
