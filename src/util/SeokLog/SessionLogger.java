package util.SeokLog;

public class SessionLogger {

    public static int FM_INVOKATION_ONLY = 0;
    public static int FM_AFTER_LOG = 1;
    public static int FM_ON_EXIT = 2;

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