package net;

import util.FileUtils;
import util.PINUtils;

import java.io.File;
import java.util.HashMap;

/**
 * This class handles the clientSettings file and persistent values for the BuzzBin Client.
 * Upon 'init()' the clientSettings file is generated with default values.
 * An instance of this class contains 'keyValueMap' which contains KEYS and
 * their values accordingly.
 * net.ClientSettings can be reset with 'generate()'.
 * net.ClientSettings can be updated with 'update()'.
 * net.ClientSettings must contain [KEYS] for a valid generation. (INVALID's get regenerated).
 * Keys and Values are represented as Strings in the following format: 'KEY':'VALUE'
 * These can be loaded and unloaded into a HashMap containing 'String
 *
 *
 * @author Jacob Gordon
 * @version 1.0
 * @date 7/3/19
 **/
public class ClientSettings {

    /**
     * Path of all directory that all files exist in for this application.
     */
    public static final String STORAGE_DIR_PATH = FileUtils.getHomeDirectory() + "buzzbin" + FileUtils.getSeparator() + "client" + FileUtils.getSeparator();

    /**
     * Path of the basic clientSettings file for BuzzBin.
     */
    public static final String SETTINGS_FILE_PATH = STORAGE_DIR_PATH + "settings.txt";

    /**
     * Path of the working-directory for 'FileSendPacketWrap' or other files.
     * Should be a different work-space in the event that the server is hosted on the same
     * computer as this client.
     */
    public static final String WORKING_DIR_WRAP = STORAGE_DIR_PATH + "temp_wrap" + FileUtils.getSeparator();

    /**
     * Working directory for temp encryption/decryption.
     */
    public static final String WORKING_DIR_CRYPT = STORAGE_DIR_PATH + FileUtils.getSeparator() + "temp_crypt" + FileUtils.getSeparator();

    /**
     * Keys that represent various values for retrieval via the 'keyValueMap' of an instance of clientSettings.
     */
    public static final String UID = "uid", IP = "ip", TCP = "port_tcp", UDP =  "port_udp", MEM = "m";

    /**
     * Keys that exist/need to be written in 'keyValueMap'.
     */
    public static String[] KEYS = {"uid", "ip", "port_tcp", "port_udp", "m"};

    /**
     * Length of generated UIDs.
     */
    public static final int UID_LENGTH = 64;

    /**
     * Default fields when the clientSettings file is generated/regenerated.
     */
    public static final String DEFAULT_IP = "0.0.0.0";
    public static final int DEFAULT_PORT_TCP = 5555;
    public static final int DEFAULT_PORT_UDP = 5555;
    public static final int DEFAULT_MEM = 1200;

    /**
     * HashMap containing keys and values that are embedded in the clientSettings file.
     */
    public HashMap<String, Object> keyValueMap;

    /**
     * Writes our clientSettings file given the default clientSettings.
     */
    public void generate() {
        if (!FileUtils.doesDirectoryExist(STORAGE_DIR_PATH)) FileUtils.createDir(STORAGE_DIR_PATH);
        keyValueMap = new HashMap<>();
        keyValueMap.put(KEYS[0], PINUtils.gen(UID_LENGTH));
        keyValueMap.put(KEYS[1], DEFAULT_IP);
        keyValueMap.put(KEYS[2], DEFAULT_PORT_TCP);
        keyValueMap.put(KEYS[3], DEFAULT_PORT_UDP);
        keyValueMap.put(KEYS[4], DEFAULT_MEM);
        FileUtils.writeFileAsStrings(true, SETTINGS_FILE_PATH, FileUtils.convertKeyValueMap(keyValueMap, true));
    }

    /**
=     * Checks for a BuzzBin directory at 'STORAGE_PATH' and creates if not existant.
     * Checks for and if needed creates "SETTINGS_FILE'.
     * Checks for a VALID clientSettings file, where all keys exists in the file.
     */
    public static ClientSettings init() {
        ClientSettings s = new ClientSettings();
        if (!FileUtils.doesDirectoryExist(STORAGE_DIR_PATH)) FileUtils.createDir(STORAGE_DIR_PATH);
        if (!FileUtils.doesDirectoryExist(WORKING_DIR_WRAP)) FileUtils.createDir(WORKING_DIR_WRAP);
        if (!FileUtils.doesDirectoryExist(WORKING_DIR_CRYPT)) FileUtils.createDir(WORKING_DIR_CRYPT);
        //init wrap
        FileSendPacketWrap.initReadWrite(WORKING_DIR_WRAP);
        if (!FileUtils.doesFileExist(SETTINGS_FILE_PATH)) {
            s.generate();
        } else {
            //check validity
            //KEYS must be contained in keys
            s.keyValueMap = FileUtils.readKeyValueMap(FileUtils.readFileAsStrings(SETTINGS_FILE_PATH));
            String[] keys = s.keyValueMap.keySet().toArray(new String[0]);
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
     */
    public void update() {
        FileUtils.writeFileAsStrings(true, STORAGE_DIR_PATH, FileUtils.convertKeyValueMap(keyValueMap, true));
    }

    public boolean cleanAll() {
        return new File(STORAGE_DIR_PATH).delete();
    }

    public boolean cleanDirs() {
        boolean b = true;
        if (!new File(WORKING_DIR_WRAP).delete()) b = false;
        if (!new File(WORKING_DIR_CRYPT).delete()) b = false;

        return b;
    }
}
