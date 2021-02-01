import net.FileSendPacketWrap;
import util.FileUtils;

import java.io.File;
import java.util.HashMap;

/**
 * This class handles the clientSettings file and persistent values for the BuzzBin Server.
 * Upon 'init()' the clientSettings file is generated with default values.
 * An instance of this class contains 'keyValueMap' which contains KEYS and
 * their values accordingly.
 * ClientSettings can be reset with 'generate()'.
 * ClientSettings can be updated with 'update()'.
 * ClientSettings must contain [KEYS] for a valid generation. (INVALID's get regenerated).
 * Keys and Values are represented as Strings in the following format: 'KEY':'VALUE'
 * These can be loaded and unloaded into a HashMap containing 'String
 *
 * @author Jacob Gordon
 * @version 1.0
 * @date 7/7/19
 **/
public class ServerSettings {

    /**
     * Path of all directory that all files exist in for this application.
     */
    public static final String STORAGE_DIR_PATH = FileUtils.getHomeDirectory() + "buzzbin" + FileUtils.getSeparator() + "server" + FileUtils.getSeparator();

    /**
     * Path of our clientSettings for the BuzzBin com.net.server.
     */
    public static final String SETTINGS_FILE_PATH = STORAGE_DIR_PATH + "settings.txt";

    /**
     * Path of the working-directory for 'FileSendPacketWrap' or other files.
     */
    public static final String WORKING_DIR_WRAP = STORAGE_DIR_PATH + "temp_wrap" + FileUtils.getSeparator();


    /**
     * Keys that represent various values for retrieval via the 'keyValueMap' of an instance of clientSettings.
     */
    public static final String TCP = "port_tcp", UDP =  "port_udp", MEM_SIZE = "m", PRINT_IP = "print_ip";

    /**
     * HashMap containing keys and values that are embedded in the clientSettings file.
     */
    public HashMap<String, Object> keyValueMap;

    /**
     * Keys that exist/need to be written in 'keyValueMap'.
     */
    public static String[] KEYS = {"port_tcp", "port_udp", "m", "print_ip"};

    /**
     * Default fields when the clientSettings file is generated/regenerated.
     */
    public static final int DEFAULT_PORT_TCP = 5555;
    public static final int DEFAULT_PORT_UDP = 5555;
    public static final int DEFAULT_MEM_SIZE = 1200;
    public static final boolean DEFAULT_PRINT_IP = true;

    /**
     * Writes our clientSettings file given the default clientSettings.
     */
    public void generate() {
        if (!FileUtils.doesDirectoryExist(STORAGE_DIR_PATH)) FileUtils.createDir(STORAGE_DIR_PATH);
        keyValueMap = new HashMap<>();
        keyValueMap.put(KEYS[0], DEFAULT_PORT_TCP);
        keyValueMap.put(KEYS[1], DEFAULT_PORT_UDP);
        keyValueMap.put(KEYS[2], DEFAULT_MEM_SIZE);
        keyValueMap.put(KEYS[3], DEFAULT_PRINT_IP);
        FileUtils.writeFileAsStrings(true, SETTINGS_FILE_PATH, FileUtils.convertKeyValueMap(keyValueMap, true));
    }

    /**
     * Initiates the clientSettings file for the BuzzBin com.net.server.
     * Generates the file if needed using some default fields.
     * Inits the user-data file.
     */
    public static ServerSettings init() {
        //com.net.server settings
        ServerSettings s = new ServerSettings();
        if (!FileUtils.doesDirectoryExist(STORAGE_DIR_PATH)) FileUtils.createDir(STORAGE_DIR_PATH);
        if (!FileUtils.doesDirectoryExist(WORKING_DIR_WRAP)) FileUtils.createDir(WORKING_DIR_WRAP);
        //init wrap
        FileSendPacketWrap.initReadWrite(WORKING_DIR_WRAP);
        if (!FileUtils.doesFileExist(SETTINGS_FILE_PATH)) {
            s.generate();
        } else {
            //check validity
            //KEYS must be contained in keys
            s.keyValueMap = FileUtils.readKeyValueMap(FileUtils.readFileAsStrings(SETTINGS_FILE_PATH));
            String[] keys = s.keyValueMap.keySet().toArray(new String[s.keyValueMap.keySet().size()]);
            int n = 0;
            for (int i = 0; i < keys.length; i++) {
                for (int r = 0; r < KEYS.length; r++) {
                    if (KEYS[r].equals(keys[i])) {
                        n++;
                    }
                }
            }
            if (n != KEYS.length) {
                s.generate();
            }
        }
        return s;
    }

    /**
     * Rewrites to the clientSettings file in the clientSettings instance given a key-value-map.
     * @param keyValueMap map of key-values with KEYS and their values, accordingly.
     */
    public void update(HashMap<String, Object> keyValueMap) {
        FileUtils.writeFileAsStrings(true, SETTINGS_FILE_PATH, FileUtils.convertKeyValueMap(keyValueMap, true));
        this.keyValueMap = keyValueMap;
    }

    /**
     * Rewrites to the clientSettings file in the clientSettings instance given the stored key-value-map.
     */
    public void update() {
        FileUtils.writeFileAsStrings(true, SETTINGS_FILE_PATH, FileUtils.convertKeyValueMap(keyValueMap, true));
    }

    public boolean clearDirs() {
        return new File(STORAGE_DIR_PATH).delete();
    }
}
