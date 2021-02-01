package util;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;

/*
This class should be treated as a singleton and a  superclass, where
various directories and data files can be handled.
Almost all functions and variables implemented by this class are done statically.
A base directory for all directories and data files is used, called 'storage_dir'.
Sub directories, whom parent must be 'storage_dir', and are treated simply
as file paths associated by a title.
Such fields must be setup via the functions 'setup_base', 'setup_dir', 'setup_dtf' before
the unc 'init' is invoked.
'init' is responsible for creating the base and all directories, while also setting up
data files through default-key-writing and key-reading from an existing file.
 */
public class Settings {

    /**
     * Private singleton of this class with an included getter.
     */
    private static Settings instance;

    public static Settings instance() {
        if (instance == null) return instance =  new Settings();
        return instance;
    }

    private String storage_dir = null;

    public String storage_dir() {
        return storage_dir;
    }

    private Directory[] directories;

    public boolean has_dir(String title) {
        if (directories == null) return false;
        for (Directory d: directories) {
            if (d.title.equals(title)) return true;
        }
        return false;
    }

    public String[] get_all_dirs() {
        String[] strs = new String[directories.length];
        for (int i = 0; i < directories.length; i++) strs[i] = directories[i].path;
        return strs;
    }

    public boolean put_dir(String title, String path) {
        if (new File(path).exists()) return false;
        if (has_dir(title)) return false;
        directories = Arrays.copyOf(directories, directories.length + 1);
        directories[directories.length - 1] = new Directory(title, path);
        return true;
    }

    public boolean init_dirs() {
        if (directories == null) return false;
        for (Directory d: directories) {
            File f = new File(d.path);
            if (!f.exists()) f.mkdirs();
        }
        return true;
    }

    /**
     * Sets up the file system for the instance of this class by creating
     * parent folders and any extra directories.
     * Settings files are written with default values or read from existing values.
     * The following parameters should be setup prior to this function call:
     * {@param storage_dir}, {@param directories}, {@param data_files}
     * @return
     */
    public boolean init() {
        if (storage_dir != null) return false;
        //setup base directories
        //try sub directories, fail if created
        File base = new File(storage_dir);
        if (!base.exists()) base.mkdirs();
        if (directories != null) init_dirs();
        return true;
    }

    public boolean has_dtf(String title) {
        return false;
    }

    //public boolean add_dtf(String title, String path, HashMap<String, Object> keyDefaultMap) {
       // if (!has_dtf)
    //}
}

/**
 * Directories are folders as children within {@param storage_dir}.
 */
class Directory {
    String title;
    String path;

    Directory(String title, String path) {
        this.title = title;
        this.path = path;
    }
}

/**
 * Data files are linked to a path and title, its most basic function is to
 * write default values, treated as {@param key} and {@param defs} and read
 * to {@param key_value_map}.
 * As a result, these values may be changed in the file or at runtime
 * through {@param key_value_map}.
 * Preforming {@func flush} will flush {@param key_value_map} as it is in memory.
 * {@func init_defaults} writes the file with given keys and default values.
 */
class DataFile {
    String parent_dir;
    String title;
    String path;
    String[] keys;
    Object[] defs;
    HashMap<String, Object> key_value_map;

    public DataFile() {}

    public DataFile(String title, String path) {
        this.title = title;
        this.path = path;
    }

    public boolean init_defaults() {
        if (key_value_map == null) return false;
        //write in key value map
        //write in file
        for (int i = 0; i < keys.length; i++) {
            key_value_map.put(keys[i], defs[i]);
        }
        FileUtils.writeFileAsKeyValueMap(path, key_value_map);
        return true;
    }

    public boolean read_map() {
        //if file doesnt exist then file
        //read key value map of file & discard current
        if (!new File(path).exists()) return false;
        key_value_map = FileUtils.readKeyValueMap(FileUtils.readFileAsStrings(path));
        return true;
    }

    public boolean init() {
        //fail if parent directory does not exist
        //if file doesn't exist set defaults, otherwise read from file
        if (!new File(parent_dir).exists()) return false;
        File f = new File(path);
        if (!f.exists()) init_defaults();
        read_map();
        return true;
    }

    public boolean flush_map() {
        if (key_value_map == null) return false;
        FileUtils.writeFileAsKeyValueMap(path, key_value_map);
        return true;
    }

    public boolean has_key(String key) {
        for (String k: keys) if (k.equals(key)) return true;
        return false;
    }

    public boolean add_key(String key) {
        if (has_key(key)) return false;
        keys = Arrays.copyOf(keys, keys.length + 1);
        keys[keys.length - 1] = key;
        return true;
    }

    public boolean add_def(String key, Object o) {
        if (!has_key(key)) add_key(key);
        defs = Arrays.copyOf(defs, defs.length + 1);
        defs[defs.length - 1] = o;
        return true;
    }

    public Object get(String key) {
        if (!has_key(key)) return null;
        for (int i = 0; i < keys.length; i++) {
            if (keys[i].equals(key)) return defs[i];
        }
        return null;
    }
}
