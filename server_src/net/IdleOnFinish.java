package net;

/**
 * This interface is used by 'IdleSender' when its action is complete.
 */
public interface IdleOnFinish {

    /**
     * Invoked whenever an IdleSender has finished its operations pertaining to
     * its list of objects.
     */
    void onFinish();
}
