package net.packet;

/**
 * This packet represents the response that is sent by Server to the
 * client upon the net.server detecting a connection.
 *
 * @author Jacob Gordon
 * @version 1.0
 * @date 6/20/19
 **/
public class ConnectResponsePacket {
    public Boolean allowed;
    public String PIN;
    public Integer targetLength;
}
