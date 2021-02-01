package net.packet;

/**
 * Sent by the client to signify that they want the net.server to send
 * them the file associated with the given UID.
 *
 * @author Jacob Gordon
 * @version 1.0
 * @date 6/23/19
 **/
public class FileAcceptPacket {
    public String[] UID;
}
