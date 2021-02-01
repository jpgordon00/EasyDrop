import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import javafx.application.Platform;
import net.FileSendPacketWrap;
import net.IdleSender;
import net.packet.*;
import util.FileUtils;
import util.PINUtils;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This class contains all the network operations for our Server.
 * KryoNet is used for packet sending and receiving.
 *
 * @author Jacob Gordon
 * @version 1.0
 * @date 7/6/19
 **/
public class BinServer extends Listener {

    /**
     * Personal (for this Server only) net_target_size is determined by
     * 'Server' and its constructor argument. This float is multiplied by the
     * lowest value to produce our personal net_target_size.
     */
    public static final float SPLIT_PERC = 0.2f;

    /**
     * Maximum amount of time for a client to send a 'ConnectRequestPacket' before our
     * client is removed.
     * In milis.
     */
    private static final int MAX_RESPONSE_TIME = 5000;

    /**
     * HashMap containing the connection ID and Client object associated
     * with a given Client.
     */
    private HashMap<Integer, Client> clientMap;

    /**
     * Instance of our KryoServer.
     */
    private Server server;

    /**
     * Ports of operation given by 'init()'.
     */
    private int portTCP, portUDP;

    /**
     * Write sizes and object buffer sizes in bytes.
     * Set by 'init()'.
     */
    private int writeBufferSize, objectBufferSize;

    /**
     * TRUE if and only if the com.net.server successfully ran on the
     * given ports.
     */
    public boolean success = false;

    /**
     * String to store our exceptions to print to console.
     */
    private String errorString;

    /**
     * Max length of files to be sent.
     */
    private int targetLength = 0;

    /**
     * Initiates the network operations for this com.net.server.
     */
    public void init(int portTCP, int portUDP, int m) {
        this.portTCP = portTCP;
        this.portUDP = portUDP;
        clientMap = new HashMap<>();
        server = new Server(writeBufferSize = (m * 16384), objectBufferSize = (m * 2048));
        registerClasses();
        server.addListener(this);
        try {
            server.bind(portTCP, portUDP);
            success = true;
        } catch (Exception ex) {
            errorString = ex.getMessage();
            return;
        }
        new Thread(server).start();

        targetLength = (int) (((writeBufferSize < objectBufferSize) ? writeBufferSize : objectBufferSize) * SPLIT_PERC);
    }

    /**
     * Register all classes and data types being sent in any packet.
     */
    private void registerClasses() {
        server.getKryo().register(ConnectRequestPacket.class);
        server.getKryo().register(ConnectResponsePacket.class);
        server.getKryo().register(DisconnectRequestPacket.class);
        server.getKryo().register(FileAcceptPacket.class);
        server.getKryo().register(FileRejectedPacket.class);
        server.getKryo().register(FileSendPacket.class);
        server.getKryo().register(FileSendPacketSplit.class);
        server.getKryo().register(FileSendRequestPacket.class);
        server.getKryo().register(PinCheckResponsePacket.class);
        server.getKryo().register(PinCheckRequestPacket.class);
        server.getKryo().register(PinUpdatePacket.class);
        server.getKryo().register(ContinueSplitPacket.class);
        server.getKryo().register(HandshakePacket.class);
        server.getKryo().register(Byte[].class);
        server.getKryo().register(String[].class);
        server.getKryo().register(String.class);
        server.getKryo().register(Integer.class);
        server.getKryo().register(Boolean.class);
    }

    /**
     * Invoked when a client is connected.
     * Adds this client to our map.
     * @param connection obj assigned to the com.net.server.
     */
    @Override
    public void connected(Connection connection) {
        super.connected(connection);
        clientMap.put(connection.getID(), new Client());

        //remove the client if they have not connected
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        final int id = connection.getID();
        Runnable task = () -> {
            if (clientMap.get(id).UID.equals("") || clientMap.get(id).PIN.equals("")) {
                server.sendToTCP(id, new DisconnectRequestPacket());
            }
        };
        int delay = MAX_RESPONSE_TIME;
        scheduler.schedule(task, delay, TimeUnit.MILLISECONDS);
        scheduler.shutdown();
    }

    /**
     * Invoked when a client has disconnected.
     * @param connection of our BinServer.
     */
    @Override
    public void disconnected(Connection connection) {
        super.disconnected(connection);
        removeClient(connection.getID());
    }

    /**
     * Invoked when this com.net.server received any com.net.
     * Check for a ConnectRequestPacket and send a ConnectResponsePacket.
     * @param connection of the client who sent the packet.
     * @param object that the client sent.
     */
    @Override
    public void received(Connection connection, Object object) {
        super.received(connection, object);
        if (object instanceof ConnectRequestPacket) {
            /*
        Process the user trying to connect!
        TODO: Allow all users.
        Update client obj, assign PIN
        Send ConnectResponsePacket
        Check if a secret_key is on record, if not request it.
        Target byte size should be < write, < read size
         */

            ConnectRequestPacket crp = (ConnectRequestPacket) object;
            Client c = clientMap.get(connection.getID());
            c.UID = crp.UID;
            c.PIN = PINUtils.gen();
            ConnectResponsePacket response = new ConnectResponsePacket();
            response.targetLength = targetLength;
            //TODO: check if client is duplicate
            response.allowed = true;
            response.PIN = c.PIN;
            server.sendToTCP(connection.getID(), response);
            log("PACKET 'ConnectRequestPacket'.");
        } else if (object instanceof PinCheckRequestPacket) {
            /*
            Client wants to check if a PIN is valid; issue a PinCheckResponsePacket.
            Check if PIN exists & if PIN != client's PIN
            Send UID of client w tagret PIN.
             */
            String pin = ((PinCheckRequestPacket) object).PIN;
            PinCheckResponsePacket pcrp = new PinCheckResponsePacket();
            pcrp.valid = getClientFromPIN(pin) != null;
            if (pcrp.valid) if (pin.equals(clientMap.get(connection.getID()).PIN)) pcrp.valid = false;
            pcrp.UID = pcrp.valid ? getClientFromPIN(pin).UID : "";
            server.sendToTCP(connection.getID(), pcrp);
        } else if (object instanceof HandshakePacket) {
            /*
            HandshakePacket received.
            Send to the client w the target 'UID'.
            Change 'UID' to sender UID.
             */
            HandshakePacket hp = (HandshakePacket) object;
            HandshakePacket packet = new HandshakePacket();
            packet.encryptedPubKey = hp.encryptedPubKey;
            packet.pin = clientMap.get(connection.getID()).PIN;
            packet.senderUID = clientMap.get(connection.getID()).UID;
            packet.UID = hp.UID;
            int to = getConnectionFromUID(hp.UID);
            server.sendToTCP(to, packet);
        } else if (object instanceof FileSendPacket) {
            /*
            Write the data to the disk.
            Flush file to file send que.
            Send request.
            TODO: Process offline file sending
             */
            FileSendPacket fsp = (FileSendPacket) object;
            String senderUID = clientMap.get(connection.getID()).UID;
            fsp.senderUID = senderUID;
            if (getClientFromPIN(fsp.pin) != null) {
                if (!FileSendPacketWrap.flush(object)) {
                    log("Error: could not flush FileSendPacket @ " + fsp.fileUID);
                } else {
                    FileSendRequestPacket fsrp = new FileSendRequestPacket();
                    fsrp.pin = fsp.pin;
                    fsrp.senderUID = clientMap.get(connection.getID()).UID;
                    fsrp.fileName = fsp.fileName;
                    fsrp.fileNameParams = fsp.fileNameParams;
                    fsrp.fileSize = fsp.fileSize;
                    fsrp.fileSizeParams = fsp.fileSizeParams;
                    fsrp.fileUID = fsp.fileUID;
                    fsrp.senderUID = senderUID;
                    server.sendToTCP(getConnectionFromPIN(fsp.pin), fsrp);
                    log("Sent Response [FileSendPacket]");
                }
            } else {
                //can not find target
                //TODO: offline sending here
            }
            log("PACKET 'FileSend'.");
        } else if (object instanceof FileSendPacketSplit) {
            /*
            Write the Split data to the disk.
            When the file is complete send a request.
             */
            FileSendPacketSplit fsp = (FileSendPacketSplit) object;
            fsp.senderUID = clientMap.get(connection.getID()).UID;
            if (!FileSendPacketWrap.flush(fsp)) {
                log("Error: could not flush FileSendPacketSplit @ " + fsp.fileUID);
            }
            HashMap<String, Object> map = FileSendPacketWrap.getParamsAsMap(fsp.fileUID);
            if ((boolean) map.get("isFinished")) {
                if (getClientFromPIN(fsp.pin) != null) {
                    FileSendPacketWrap.flush(object);
                    FileSendRequestPacket fsrp = new FileSendRequestPacket();
                    fsrp.pin = fsp.pin;
                    fsrp.fileName = (Byte[]) map.get("fileName");
                    fsrp.fileNameParams = (Byte[]) map.get("fileNameParams");
                    fsrp.fileSize = (Byte[]) map.get("fsp.fileSize");
                    fsrp.fileSizeParams = (Byte[]) map.get("fsp.fileSizeParams");
                    fsrp.fileUID = fsp.fileUID;
                    fsrp.senderUID = fsp.senderUID;
                    server.sendToTCP(getConnectionFromPIN(fsp.pin), fsrp);
                    log("Sent Response [FileSendPacket]");
                } else {
                    log("Could not find target. UID: " + fsp.fileUID);
                    //can not find target
                    //TODO: store in que with empty UID and wait for a user w a matching PIN
                    //TODO: then send 'FileSendRequest' packet.
                }
            };
        } else if (object instanceof FileAcceptPacket) {
            /*
            Client accepted a packet and now wants a 'FileSendPacket'.
            Target used should be saved in params.
            If the given file is singular, load into memory & send.
            Else send each one split at a time.
             */
            FileAcceptPacket fap = (FileAcceptPacket) object;
            for (String uid: fap.UID) {
                if (!FileSendPacketWrap.hasCatalog(uid)) continue;
                if (FileSendPacketWrap.hasSingular(uid)) {
                    server.sendToTCP(connection.getID(), FileSendPacketWrap.readSingle(uid));
                    log("Sent 'FileSendPacket'.");
                } else {
                    if (!((boolean) FileSendPacketWrap.getParamsAsMap(uid).get("isFinished"))) {
                        log("ERROR: File isn't complete: " + uid);
                        return;
                    }
                    int l = FileSendPacketWrap.getLength(uid);
                    for (int i = 0; i < l; i++) {
                        ArrayList<Object> list = new ArrayList<>();
                        list.add(FileSendPacketWrap.readSplit(uid, i));
                        new IdleSender(server, list, connection.getID());
                        boolean rem = FileSendPacketWrap.removeSplit(uid, i);
                        System.out.println("removed for " + uid + "_" + i + ": " + rem);
                    }
                    log("Sent 'FileSendPacketSplit' complete.");
                }
            }
            log("PACKET 'FileAccept[" + fap.UID.length + "]'.");
        } else if (object instanceof FileRejectedPacket) {
            /*
            Client rejected a file. Remove from record.
             */
            FileRejectedPacket frp = (FileRejectedPacket) object;
            for (String uid: frp.UID) {
                if (!FileSendPacketWrap.hasCatalog(uid)) {
                    log("ERROR: File not in catalog " + uid);
                    continue;
                }

                if (!FileSendPacketWrap.removeCompletely(uid)) {
                    log("ERROR: failed to remove file: " + uid);
                } else {
                    log("Removed file @ " + uid);
                }
            }
            log("PACKET 'FileRejected[" + frp.UID.length + "]'.");
        } else {
        }
    }


    /**
     * Removes the client with the given connection ID.
     * Sends out a 'PinUpdatePacket' to all clients.
     * @param id connection ID from KryoServer.
     */
    public void removeClient(int id) {
        if (!clientMap.containsKey(id)) return;
        PinUpdatePacket pup = new PinUpdatePacket();
        pup.PIN = clientMap.get(id).PIN;
        server.sendToAllTCP(pup);
        clientMap.remove(id);
        PINUtils.removePIN(pup.PIN);
        log("Client disconnected.");
    }

    /**
     * Gets a client object based on a UID.
     * @param uid of the client we want to get.
     * @return a client with the given UID or null.
     */
    public Client getClientFromUID(String uid) {
        for (int key: clientMap.keySet()) {
            Client c = clientMap.get(key);
            if (uid.equals(c.UID)) return c;
        }
        return null;
    }

    /**
     * Gets a client object based on a PIN.
     * @param pin of the client we want to get.
     * @return a client with the given PIN or null.
     */
    public Client getClientFromPIN(String pin) {
        for (int key: clientMap.keySet()) {
            Client c = clientMap.get(key);
            if (pin.equals(c.PIN)) return c;
        }
        return null;
    }

    /**
     * Finds the Integer connection ID associated with the given UID.
     * @param uid to find the connection for.
     * @return integer, representing the connection ID. Or -1 if not found.
     */
    public Integer getConnectionFromUID(String uid) {
        for (int key: clientMap.keySet()) {
            Client c = clientMap.get(key);
            if (uid.equals(c.UID)) return key;
        }
        return -1;
    }

    /**
     * Finds the Integer connection ID associated with the given PIN.
     * @param pin to find the connection for.
     * @return integer, representing the connection ID. Or -1 if not found.
     */
    public Integer getConnectionFromPIN(String pin) {
        for (int key: clientMap.keySet()) {
            Client c = clientMap.get(key);
            if (pin.equals(c.PIN)) return key;
        }
        return -1;
    }

    /**
     * Helper function to add/append text to our Server Screen.
     * @param str
     */
    private void log(String str) {
        final ServerScreen ss = ServerScreen.instance;
        if (ss == null) return;
        Runnable r = () -> {
            ss.appendText(str);
        };
        Platform.runLater(r);
    }

    /**
     * Prints some data to the com.net.server screen regarding the
     * startup of the Server.
     */
    public void printStartup() {
        //lets print some shit to our com.net.server upon startup
        final ServerScreen ss = ServerScreen.instance;
        if (ss == null) System.out.println("SERVER SCREEN IS NULL!");
        final int p1 = portTCP;
        final int p2 = portUDP;
        final int s1 = writeBufferSize;
        final int s2 = objectBufferSize;
        StringBuilder sb = new StringBuilder();
        boolean printIP = (boolean) LaunchServer.getInstance().settings.keyValueMap.get(ServerSettings.PRINT_IP);
        if (success) {
            sb.append("***SUCCESS***\n");
            if (printIP) {
                sb.append("public ip: ").append(FileUtils.getPublicAddress()).append("\n");
                sb.append("local ip: " + FileUtils.getLocalAddress()).append("\n");
            }
            sb.append("settings file: ").append(ServerSettings.SETTINGS_FILE_PATH).append("\n");
            sb.append("port tcp, udp: (").append(p1).append(") (").append(p2).append(")").append("\n");
            sb.append("write buffer size, object buffer size (bytes): (").append(s1).append(") (").append(s2).append(")").append("\n");
            sb.append("Close this window to END.\n");
            sb.append("Listening...").append("\n");
        } else {
            sb.append("***FAILURE***\n");
            if (printIP) {
                sb.append("public ip: ").append(FileUtils.getPublicAddress()).append("\n");
                sb.append("local ip: ").append(FileUtils.getLocalAddress()).append("\n");
            }
            sb.append("settings file: ").append(ServerSettings.SETTINGS_FILE_PATH).append("\n");
            sb.append("port tcp, udp: (").append(p1).append(") (").append(p2).append(")").append("\n");
            sb.append(errorString).append("\n");
        }
        log(sb.toString());
    }


}


/**
 * Holds information that represents our client:
 * UID, PIN, SecretKey
 */
class Client {

    /**
     * UniqueID representing the Client.
     * SecretKey for decryption.
     */
    protected String UID, PIN;

    /**
     * Constructor to avoid null values.
     */
    protected Client() {
        UID = "";
        PIN = "";
    }
}