package net;

/**
 * This interface contains the method 'response', which is
 * invoked after a PinCheckResponse is sent from the net.server.
 */
public interface PinResponseListener {

    /**
     * Invoked after a 'PinCheckResponse' is received by the client.
     * @param valid true if the PIN was determined to be valid.
     * @param pin PIN that was checked by the last 'PinCheckRequest' packet.
     */
    void respond(boolean valid, String pin, String uid);
}
