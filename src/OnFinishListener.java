/**
 * This interface is used by dialogs in BuzzBin.
 * Its function is invoked when the com.dialog has finished.
 *
 * @author Jacob Gordon
 * @version 1.0
 * @date 20 Jul 2019
 **/
public interface OnFinishListener {

    /**
     * Invoked when a com.dialog has finished, with its paremeter representing SUCCESS or FAILURE.
     * @param SUCCESS true if the com.dialog has succesfully finished. False if it has been canceled / closed.
     */
    void finish(boolean SUCCESS);
}
