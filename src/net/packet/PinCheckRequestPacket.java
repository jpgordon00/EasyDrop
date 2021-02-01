package net.packet;

/**
 * This packet is sent by the Client when they want to
 * test a PIN. The net.server needs to respond with a PINCheckResponsePacket.
 *
 * @author Jacob Gordon
 * @version 1.0
 * @date 6/21/19
 **/
public class PinCheckRequestPacket {
    public String PIN;
}
