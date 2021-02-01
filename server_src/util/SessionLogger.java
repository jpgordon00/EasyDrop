package util;

import java.io.File;

public class SessionLogger {
    private static int FILE_NAME_LENGTH = 5;

    public static int FM_WRITE_IMMEDIATELY = 0;
    public static int FM_ON_FLUSH_FUNC = 1;
    public static int FM_ON_EXIT = 2;

    public static int LOC_FILE = 0;
    public static int LOC_MEM = 1;
    public static int LOC_STREAM = 2;



    public static boolean ENABLED = false;

    public static String DIR = null;

    private static String SESH;

    public static boolean init(String path) {
        File f = new File(path);
        if (!f.exists()) {
                f.mkdirs();
        }
        DIR = path;
        if (!DIR.endsWith(FileUtils.getSeparator())) DIR += FileUtils.getSeparator();
        SESH = DIR + "SESH_" + PINUtils.gen(6);
        return true;
    }

    public static boolean log(String str) {
        if (DIR == null) return false;
        if (!str.endsWith(FileUtils.getLineBreak())) str += FileUtils.getLineBreak();
        return ENABLED ? FileUtils.appendFileAsBytes(SESH, str.getBytes()) : false;
    }

    public static String[] read() {
        if (DIR == null) return null;
        return FileUtils.readFileAsStrings(SESH);
    }
}

class Logger {

    int flushMode = -1;
    int flushLoc = -1;
    private String dir;
    private boolean enabled;

    void log() {
    }

    void flush() {
    }
}