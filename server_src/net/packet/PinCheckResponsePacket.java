package net.packet;

/**
 * This packet is sent by the Server as a response to the 'PinCheckRequestPacket'.
 * If 'valid', then the net.server approved of the client's PIN that was entered.
 * False means that the PIN is invalid.
 *
 * @author Jacob Gordon
 * @version 1.0
 * @date 6/21/19
 **/
public class PinCheckResponsePacket {
    public Boolean valid;
    public String UID;
}
