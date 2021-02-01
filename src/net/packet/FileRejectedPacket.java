package net.packet;

/**
 * Sent from the client to signify that they don't want to
 * receive the given file. Server should no longer store the
 * file's information.
 *
 * @author Jacob Gordon
 * @version 1.0
 * @date 6/25/19
 **/
public class FileRejectedPacket {
    public String[] UID;
}
