package net.packet;

/**
 * This class represents a request from the Server to the Client, which
 * shows the client that another client sent a file.
 * Client needs to display this in 'pending'.
 *
 *
 * @author Jacob Gordon
 * @version 1.0
 * @date 6/22/19
 **/
public class FileSendRequestPacket {
    public String pin;
    public String senderUID;
    public String fileUID;
    public Byte[] fileName;
    public Byte[] fileNameParams;
    public Byte[] fileSize;
    public Byte[] fileSizeParams;
}
