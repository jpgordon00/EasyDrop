package net;

/**
 * This interface is used in 'BinClient' when a connection has been
 * established with the BinServer.
 */
public interface ConnectionFinishedListener {

    /**
     * Invoked when a valid connection with the BinServer has been established.
     * @param pin given by the net.server.
     */
    void respond(String pin);
}
