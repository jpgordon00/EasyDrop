import javafx.application.Application;
import java.io.IOException;

/**
 * Use this class to launch the BuzzBin com.net.server.
 * This class holds instances to our settings and com.net.server classes.
 *
 * @author Jacob Gordon
 * @version 1.0
 * @date 7/5/19
 **/
public class LaunchServer {

    /**
     * Singleton for this class.
     */
    private static final LaunchServer instance = new LaunchServer();

    /**
     * Gets our singleton for this class.
     * @return singleton.
     */
    public static final LaunchServer getInstance() {
        return instance;
    }

    /**
     * Private constructor to ensure singleton.
     */
    private LaunchServer() {
    }

    /**
     * Instance of our clientSettings class.
     */
    public ServerSettings settings;

    /**
     * Instance of our BinServer.
     */
    public BinServer server;



    /**
     * Starts the BuzzServer in a separate thread.
     */
    protected void start() {
        Runnable r = () -> {
            settings = ServerSettings.init();
            server = new BinServer();
            server.init((int) settings.keyValueMap.get(ServerSettings.TCP), (int) settings.keyValueMap.get(ServerSettings.UDP), (int) settings.keyValueMap.get(ServerSettings.MEM_SIZE));
        };
        new Thread(r).start();
    }


}
