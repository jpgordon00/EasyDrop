package net;

import net.packet.FileSendRequestPacket;

/**
 * This interface is invoked by the client for two scenarios:
 * 1) net.server sends the client a request to send it a packet.
 * 2) net.server sends the client a file (assume step 1 occurred).
 */
public interface FileRequestListener {

    /**
     * Invoked by the client when another client requests the following packet to be sent.
     * @param packet to be sent to the client if accepted.
     */
    void respondRequest(FileSendRequestPacket packet, String fileName);

    /**
     * Invoked by the client when a 'FileAccept' packet is sent and the net.server is
     * ready to deliver the file to the user.
     * @param uid of the file to be downloaded from the given data.
     * @param fileNameFull
     * @return download directory of the file.
     */
    String respondSend(String uid, String fileNameFull);
}
