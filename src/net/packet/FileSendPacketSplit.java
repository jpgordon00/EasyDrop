package net.packet;

/**
 * This packet represents a file that is too large to be sent over the
 * network at once.
 */
public class FileSendPacketSplit {
    //header only
    public String pin;
    public Byte[] fileName, fileNameParams;
    public Byte[] fileSize, fileSizeParams;
    public Boolean isZip;

    //header only if fits in memory
    //else it exists for each packet
    public Byte[] content;
    public Byte[] contentParams;

    //all data
    public String senderUID; //assigned by server
    public String fileUID;
    public Integer series;
    public Integer length;
    public Boolean finalPacket;
}
