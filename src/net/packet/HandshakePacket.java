package net.packet;

/**
 * This packet is sent by the Client to another client, containing its
 * encrypted public key.
 */
public class HandshakePacket {
    public String UID;
    public String senderUID;
    public Byte[] encryptedPubKey;
    public String pin;
}
