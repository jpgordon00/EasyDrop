package net.packet;

/**
 * This packet is sent from the Server to all clients when a user with
 * the given PIN disconnects.
 * The client needs to update its PIN validity accordingly.
 *
 * @author Jacob Gordon
 * @version 1.0
 * @date 6/25/19
 **/
public class PinUpdatePacket {
    public String PIN;
}
