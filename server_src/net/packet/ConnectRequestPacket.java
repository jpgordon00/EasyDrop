package net.packet;

/**
 * This packet represents a request from the client upon attempted login,
 * which contains its UID (from clientSettings file) and SecretKey.
 *
 * @author Jacob Gordon
 * @version 1.0
 * @date 7/5/19
 **/
public class ConnectRequestPacket {
     public String UID;
}
