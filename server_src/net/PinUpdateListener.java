package net;

/**
 * This interface is used by 'net.BinClient' whenever a Client disconnects, as
 * this Client needs to update accordingly.
 * @author Jacob Gordon
 * @version 1.0
 * @date 21 Jul 2019
 **/
public interface PinUpdateListener {

    /**
     * Invoked whenever the Server sends a 'PinUpdate' packet.
     * This signifies that another client with the given PIN has disconnected.
     * @param PIN of the client who disconnected.
     */
    void respond(String PIN);
}
