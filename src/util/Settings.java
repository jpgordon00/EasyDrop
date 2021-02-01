package util;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;

/*
SINGLETON class used to manage directories and data files;
Directories (dir), maintain directories in init().
Data Files (dtf):
 */
public class Settings {

    /**
     * Private singleton of this class with an included getter.
     */
    private static Settings instance;

    public static Settings instance() {
        if (instance == null) return instance = new Settings();
        return instance;
    }

    public Settings() {

    }
    private Directory[] dirs;
    private DataFile[] dtfs;

    public boolean has_dir(String title) {
        if (dirs == null) return false;
        for (Directory d: dirs) {
            if (d.title.equals(title)) return true;
        }
        return false;
    }

    public String[] get_all_dirs() {
        String[] strs = new String[dirs.length];
        for (int i = 0; i < dirs.length; i++) strs[i] = dirs[i].path;
        return strs;
    }

    public boolean put_dir(String title, String path) {
        if (new File(path).exists()) return false;
        if (has_dir(title)) return false;
        dirs = Arrays.copyOf(dirs, dirs.length + 1);
        dirs[dirs.length - 1] = new Directory(title, path);
        return true;
    }

    public boolean has_dir_path(String path) {
        if (dirs == null) return false;
        for (Directory d: dirs) {
            if (d.path.equals(path)) return true;
        }
        return false;
    }

    public String dir_path_to_title(String path) {
        if (!has_dir_path(path)) return "";
        if (dirs == null) return "";
        for (Directory d: dirs) if (d.title.equals(path)) return d.title;
        return "";
    }

    public String dir_title_to_path(String title) {
        if (!has_dir(title)) return "";
        if (dirs == null) return "";
        for (Directory d: dirs) if (d.title.equals(title)) return d.path;
        return "";
    }

    private Directory _title_to_dir(String title) {
        if (!has_dir(title)) return null;
        if (dirs == null) return null;
        for (Directory d: dirs) if (d.title.equals(title)) return d;
        return null;
    }

    private boolean _init_dirs() {
        if (dirs == null) return false;
        for (Directory d: dirs) {
            File f = new File(d.path);
            if (!f.exists()) f.mkdirs();
        }
        return true;
    }

    /**
     * Sets up the file system for the instance of this class by creating
     * parent folders and any extra dirs.
     * Settings files are written with default values or read from existing values.
     * The following parameters should be setup prior to this function call:
     * {@param storage_dir}, {@param directories}, {@param data_files}
     * @return
     */
    public boolean init() {
        //try sub dirs, fail if created`
        if (dirs != null) _init_dirs();
        return true;
    }

    public boolean has_dtf(String title) {
        if (dtfs == null) return false;
        for (DataFile dtf: dtfs) {
            //if (dtf.title.equals(title)) return true;
        }
        return false;
    }

    public boolean add_dtf(String title, String path) {
        //pre checks: check if existing in memory and file system
        if (has_dtf(title)) return false;
        dtfs = dtfs == null ? new DataFile[1] : Arrays.copyOf(dtfs, dtfs.length + 1);
        dtfs[dtfs.length - 1] = new DataFile();
        return true;
    }

    public boolean remove_dtf(String title) {
        return false;
    }

    //default values are defined as:
    //String[] keys;    Object[] defs;
    public boolean add_dtf_defaults(String title, Tuple<String[], Object[]> defValues, boolean replace) {
        if (!has_dtf(title)) return false;
        return true;
    }

    public boolean dload_sall_single_dir(String parentDir, boolean overrideExisting) {
        return false;
    }
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
class DataFile {
    String path;
    HashMap<String, Object> key_value_map;
    HashMap<String, Object> def_key_value_map;

    DataFile() {
    }

    DataFile(String path) {
        this.path = path;
    }

    boolean read_map() {
        //if file doesnt exist then file
        //read key value map of file & discard current
        if (!new File(path).exists()) return false;
        key_value_map = FileUtils.readKeyValueMap(FileUtils.readFileAsStrings(path));
        return true;
    }

    boolean init() {
        //fail if parent directory does not exist
        //if file doesn't exist set defaults, otherwise read from file
        //if (!FileUtils.validFileFormat(path)) return false;
        if (!new File(FileUtils.getStringBefore(path, FileUtils.getSeparator())).exists()) return false;
        File f = new File(path);
        if (!f.exists()) init_defaults();
        read_map();
        return true;
    }

    public boolean init_defaults() {
        if (key_value_map == null) return false;
        //write in key value map
        //write in file
        for (int i = 0; i < def_key_value_map.size(); i++) {
            String s;
            key_value_map.put(s = ((String) def_key_value_map.keySet().toArray()[i]), def_key_value_map.get(s));
        }
        FileUtils.writeFileAsKeyValueMap(path, key_value_map);
        return true;
    }

    public boolean flush_map() {
        if (key_value_map == null) return false;
        FileUtils.writeFileAsKeyValueMap(path, key_value_map);
        return true;
    }

    public boolean has_key(String key) {
        return key_value_map.containsKey(key);
    }

    public boolean add_def(String key, Object obj) {
        if (has_key(key)) return false;
        //return key_value_map.put(key, obj);
        return true;
    }

    public Object get_def(String key) {
        if (!has_key(key)) return null;
        read_map();
        return null;
    }
}
