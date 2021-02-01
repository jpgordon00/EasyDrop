package net;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import util.Handshake;
import javafx.application.Platform;
import net.packet.*;
import util.FileUtils;
import util.PINUtils;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * This class handles all the network activity for our client.
 * KryoNet is used for packet sending and receiving.
 *
 * @author Jacob Gordon
 * @version 1.0
 * @date 7/1/19
 **/
public class BinClient extends Listener {

    public BinClient() {
    }

    /**
     * Maximum allowable size of files to be stored in memory (in bytes).
     */
    public static final long SIZE_IN_MEM_LIM = FileUtils.BYTES_IN_MB * 100;

    /**
     * Chunk size that large files are to be read in.
     */
    public static final int READ_CHUNK_SIZE = (int) FileUtils.BYTES_IN_MB * 100;

    /**
     * Percent of the maximum allowable network target size, decrease for avoidance
     * of network overloading and increase for larger allowable sizes.
     */
    public static final float SPLIT_PERC = 0.2f;

    /**
     * Time in milis after connection attempt to give up.
     */
    public static final int INITIAL_TIMEOUT = 5000;

    /**
     * Time in milis to wait after connection to send a 'ConnectRequestPacket'.
     */
    public static final int INITIAL_REQUEST_WAIT = 1000;

    /**
     * Unique PIN given from the Server.
     */
    public String PIN;

    /**
     * IP address of attempted connection to net.server.
     */
    public String ip;

    /**
     * UID of our client, as assigned from settings.
     */
    public String UID;

    /**
     * TRUE if and only if the net.server successfully ran on the
     * given ports.
     */
    public boolean success = false;

    /**
     * Instance of our Kryo client.
     */
    private Client client;

    /**
     * Maps UID's to Handshake objects which contain a shared secret-key.
     */
    private HashMap<String, Handshake> secretMap;

    /**
     * TODO: write UID's to dsk & do handshake when desired.
     */
    private HashMap<String, String> uidToPinMap;

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
     * This integer represents the maximum allowed file size for sent file over the network.
     * All files above this limit must be split up so that no file is > targetLength.
     * This limit is set by gathering the write and read sizes for both the client
     * and the server, choosing the smallest attribute and then scaling this number
     * by 'SPLIT_PERC' to allow for overhead.
     *
     * TODO: mutual agreement for each client.
     */
    private int targetLength = 0;

    /**
     * Length of generated UIDs for each FileSendPacket.
     */
    private static final int FILE_SEND_UID_LENGTH = 24;

    /**
     * Last PIN submitted to a PinCheckRequest packet.
     * Used in the net.PinResponseListener.
     */
    private String lastPIN = "";

    /**
     * Instance of our 'DisconnectListener', which is invoked when a valid connection
     * between this client and the server has been forsaken. Kevin is a furry, by the way.`
     */
    private DisconnectListener disconnectListener;

    /**
     * Instance of our ConnectionFinishedListener, which is invoked after
     * a valid connection & PIN is assigned with the net.server.
     */
    private ConnectionFinishedListener connectionFinishedListener;

    /**
     * Instance of our net.FileRequestListener, which handles the response to
     * receiving files & file requests.
     */
    private FileRequestListener fileRequestListener;

    /**
     * Instance of our net.PinResponseListener, which handles the response to
     * a PinRequestPacket.
     */
    private PinResponseListener pinResponseListener;

    /**
     * Instance of our net.PinUpdateListener, which is invoked whenever another
     * client has disconnected. Their given PIN should be checked.
     */
    private PinUpdateListener pinUpdateListener;

    /**
     * Finds the lowest target byte size out of read and object size (see 'init').
     */
    private int localTargetLength = 0;

    /**
     * Creates the network client given a port TCP, port UDP, and multiple of memory size.
     */
    public void init(String ip, int portTCP, int portUDP, int m, String uid) {
        this.ip = ip;
        this.portTCP = portTCP;
        this.portUDP = portUDP;
        this.UID = uid;
        client = new Client(writeBufferSize = (m * 8192), objectBufferSize = (m * 2048));
        registerClasses();
        new Thread(client).start();
        //client.start();
        try {
            client.connect(INITIAL_TIMEOUT, ip, portTCP, portUDP);
            success = true;
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }
        client.addListener(this);
        try {
            Thread.sleep(INITIAL_REQUEST_WAIT);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        localTargetLength = (int) ((writeBufferSize < objectBufferSize ? writeBufferSize : objectBufferSize) * SPLIT_PERC);
        secretMap = new HashMap<>();
        uidToPinMap = new HashMap<>();
        ConnectRequestPacket packet = new ConnectRequestPacket();
        packet.UID = uid;
        client.sendTCP(packet);
    }
    /**
     * Register all classes and data types being sent
     * in any packet.
     */
    private void registerClasses() {
        client.getKryo().register(ConnectRequestPacket.class);
        client.getKryo().register(ConnectResponsePacket.class);
        client.getKryo().register(DisconnectRequestPacket.class);
        client.getKryo().register(FileAcceptPacket.class);
        client.getKryo().register(FileRejectedPacket.class);
        client.getKryo().register(FileSendPacket.class);
        client.getKryo().register(FileSendPacketSplit.class);
        client.getKryo().register(FileSendRequestPacket.class);
        client.getKryo().register(PinCheckResponsePacket.class);
        client.getKryo().register(PinCheckRequestPacket.class);
        client.getKryo().register(PinUpdatePacket.class);
        client.getKryo().register(ContinueSplitPacket.class);
        client.getKryo().register(HandshakePacket.class);
        client.getKryo().register(Byte[].class);
        client.getKryo().register(String[].class);
        client.getKryo().register(String.class);
        client.getKryo().register(Integer.class);
        client.getKryo().register(Boolean.class);
    }

    /**
     * Invoked when a valid connection has been established with the net.server.
     * Send a ConnectRequestPacket with our UID and SecretKey after a delay.
     *
     * @param connection
     */
    @Override
    public void connected(Connection connection) {
        super.connected(connection);
    }

    /**
     * Invoked when the client has disconnected from the net.server.
     * ]
     *
     * @param connection
     */
    @Override
    public void disconnected(Connection connection) {
        super.disconnected(connection);
    }

    /**
     * Invoked whenever this Client receives a Packet.
     * Closes the application upon retrieval of a DisconnectRequestPacket.
     *
     * @param connection connection data of packet sender.
     * @param object     received from the Server.
     */
    @Override
    public void received(Connection connection, Object object) {
        super.received(connection, object);
        if (object instanceof DisconnectRequestPacket) {
            /*
            DisconnectPacket, net.server wants us to close the connection.
            */
            client.close();
            success = false;
        } else if (object instanceof ConnectResponsePacket) {
            /*
            ConnectResponsePacket.
            Get PIN from the packet.
            Get target length from the packet.
            Invoke our 'ConnectionFinishedListener'.
             */
            ConnectResponsePacket crp = (ConnectResponsePacket) object;
            PIN = crp.PIN;
            if (targetLength == 0) {
                targetLength = crp.targetLength;
                targetLength = localTargetLength < targetLength ? localTargetLength : targetLength;
            }
            if (connectionFinishedListener != null) {
                Runnable r = () -> connectionFinishedListener.respond(PIN);
                Platform.runLater(r);
            }
        } else if (object instanceof HandshakePacket) {
            /*
            HandshakePacket.
            If the packet doesn't exist in 'secretMap' yet, assume us to be "Bob".
            If exists, assume "Alice".
             */
            HandshakePacket hp = (HandshakePacket) object;
            Handshake h = secretMap.get(hp.senderUID);
            if (h == null) {
                uidToPinMap.put(hp.pin, hp.senderUID);
                secretMap.put(hp.senderUID, h = new Handshake(FileUtils.convertBytes(hp.encryptedPubKey)));
                HandshakePacket packet = new HandshakePacket();
                packet.UID = hp.senderUID;
                packet.encryptedPubKey = FileUtils.convertBytes(h.getPubEncrypted());
                client.sendTCP(packet);
                System.out.println("handshake complete 1: " + hp.senderUID);
            } else if (h.isInProgress()) {
                uidToPinMap.put(hp.pin, hp.senderUID);
                h.generateAlice(FileUtils.convertBytes(hp.encryptedPubKey));
                System.out.println("handshake complete 2");
            }
        } else if (object instanceof PinUpdatePacket) {
            /*
            Another client disconnected, invoke our 'net.PinUpdateListener'.
            Remove from uid map.
             */
            PinUpdatePacket p = ((PinUpdatePacket) object);
            if (pinUpdateListener != null) {
                Runnable r = () -> pinUpdateListener.respond((p.PIN));
                removeClient(uidToPinMap.get(p.PIN));
                //TODO: change this
                uidToPinMap.remove(p.PIN);
                Platform.runLater(r);
            }
        } else if (object instanceof PinCheckResponsePacket) {
            /*
            The net.server responded to a 'PinCheckRequest'. Invoke our listener.
             */
            PinCheckResponsePacket pcrp = (PinCheckResponsePacket) object;
            if (pinResponseListener != null) {
                Runnable r = () -> pinResponseListener.respond(pcrp.valid, lastPIN, pcrp.UID);
                Platform.runLater(r);
            }
        } else if (object instanceof FileSendRequestPacket) {
            /*
            A request for a file to be sent to this client has been made.
            Invoke our listener.

             */
            if (fileRequestListener != null) {
                FileSendRequestPacket p = (FileSendRequestPacket) object;
                fileRequestListener.respondRequest(p, new String(Handshake.Decrypt(secretMap.get(p.senderUID).getSecret(), FileUtils.convertBytes(p.fileName), FileUtils.convertBytes(p.fileNameParams))));
            }
        } else if (object instanceof FileSendPacket) {
            /*
            A file has been sent to this user.
            Keep in memory, no need to flush.
            Account for zip.
             */
            if (fileRequestListener != null) {
                FileSendPacket fsp = (FileSendPacket) object;
                byte[] secret = secretMap.get(fsp.senderUID).getSecret();
                String outTemp = ClientSettings.WORKING_DIR_CRYPT + fsp.fileUID;
                String out = fileRequestListener.respondSend(fsp.fileUID, new String(Handshake.Decrypt(secret, FileUtils.convertBytes(fsp.fileName), FileUtils.convertBytes(fsp.fileNameParams))));
                FileUtils.writeFileAsBytes(fsp.isZip ? outTemp : out, Handshake.Decrypt(secretMap.get(fsp.senderUID).getSecret(), FileUtils.convertBytes(fsp.content), FileUtils.convertBytes(fsp.contentParams)));
                if (fsp.isZip) {
                    try {
                        new ZipFile(outTemp).extractAll(out);
                    } catch (ZipException e) {
                        e.printStackTrace();
                    }
                    new File(outTemp).delete();
                }
            }
        } else if (object instanceof FileSendPacketSplit) {
            /*
            TODO: implement multithreading
            TODO: store objects that can fit in memory into FileSendPacketWrap.
            A part of a file has been sent to this user.
            Flush the packet and wait for it to finish.
            Account for zip.
             */
            FileSendPacketSplit fsps = (FileSendPacketSplit) object;
            if (!FileSendPacketWrap.flush(object)) {
                System.out.println("ERORR: failed to flush split wit uid: " + fsps.fileUID);
            } else if (FileSendPacketWrap.isFinished(fsps.fileUID)) {
                Runnable r = () -> {
                    String outTemp = ClientSettings.WORKING_DIR_CRYPT + fsps.fileUID;
                    System.out.println("SENDER UID: " + fsps.senderUID);
                    System.out.println(secretMap.containsKey(fsps.senderUID) ? secretMap.get(fsps.senderUID).getSecret() == null ? "secret is null; in map" : "secret not null; in map" : "NOT IN MAP BR0");
                    byte[] secret = secretMap.get(fsps.senderUID).getSecret();
                /*
                If it needs encryption every time, decrypt each file first & then append to path.
                If it needs a singular encryption, decrypt file & write to path.
                 */
                    System.out.println("{FileSendPaceket} Finished creating file wit uid " + fsps.fileUID);
                    HashMap<String, Object> map = FileSendPacketWrap.getParamsAsMap(fsps.fileUID);
                    String out = fileRequestListener.respondSend(fsps.fileUID, new String(Handshake.Decrypt(secret, FileUtils.convertBytes((Byte[]) map.get("fileName")), FileUtils.convertBytes((Byte[]) map.get("fileNameParams")))));
                    if (FileSendPacketWrap.isEncryptedEach(fsps.fileUID)) {
                    /*
                    Assuming the given file is completely accounted for, then
                    for each split decrypt and append to the file.
                    If file at out exists, delete. If a split is null, then delete file.
                    Account for zip.
                     */
                        int l = FileSendPacketWrap.getLength(fsps.fileUID);
                        File f = new File(out);
                        if (f.exists()) f.delete();
                        try {
                            f.createNewFile();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                        boolean iz = false;
                        for (int i = 0; i < l; i++) {
                            FileSendPacketSplit split = FileSendPacketWrap.readSplit(fsps.fileUID, i);
                            if (split == null) {
                                System.out.println("ERROR: can't decrypt each for uid: " + fsps.fileUID);
                                FileSendPacketWrap.removeCompletely(split.fileUID);
                                f.delete();
                                break;
                            }
                            FileUtils.appendFileAsBytes((iz = split.isZip) ? outTemp : out, Handshake.Decrypt(secret, FileUtils.convertBytes(split.content), FileUtils.convertBytes(split.contentParams)));
                            System.out.println("removal [ + " + split + "]: " + FileSendPacketWrap.removeSplit(fsps.fileUID, i));
                        }
                        if (iz) {
                            System.out.println("large unzip happening rn");
                            try {
                                new ZipFile(outTemp).extractAll(out);
                            } catch (ZipException e) {
                                e.printStackTrace();
                            }
                            new File(outTemp).delete();
                        }
                    } else {
                    /*
                    Data has only one content parameter and therefore should be loaded
                    completely into memory as a FileSendPacketWrap & concatenated to a FileSendPacket.
                    Remove file completely when done.
                    NOTE: assumes that all packets can fit into memory.
                     */
                        FileSendPacketWrap wrap = FileSendPacketWrap.read(fsps.fileUID);
                        FileSendPacket fsp = wrap.concatenate();
                        FileUtils.writeFileAsBytes(fsp.isZip ? outTemp : out, Handshake.Decrypt(secret, FileUtils.convertBytes(fsp.content), FileUtils.convertBytes(fsp.contentParams)));
                        if (fsp.isZip) {
                            try {
                                new ZipFile(outTemp).extractAll(out);
                            } catch (ZipException e) {
                                e.printStackTrace();
                            }
                            new File(outTemp).delete();
                        }
                        System.out.println("{encrypt_single} Finished creating file wit uid " + fsps.fileUID);
                        FileSendPacketWrap.removeCompletely(fsp.fileUID);
                    }
                };
                new Thread(r).start();
            }
        } else {

            //unknown packet
        }
    }

    /**
     * Request a 'PinCheckResponse' packet from the net.server.
     * The response will be invoked via the 'net.PinResponseListener'.
     *
     * @param PIN of a client to check.
     */
    public void sendPinCheckRequest(String PIN) {
        lastPIN = PIN;
        PinCheckRequestPacket pcrp = new PinCheckRequestPacket();
        pcrp.PIN = PIN;
        client.sendTCP(pcrp);
    }


    /**
     * Sends a file or folder to a given PIN.
     * ASSERT: read_chunk_size >= net_size_limt
     *
     * @param path of teh file to send.
     * @return true if file successfully sent fully through the network.
     */
    public boolean sendFile(final String path, final String pin) {
        //pre-checks
        //TODO: optional compression through zipping/unzipping.
        File f = new File(path);
        if (!f.exists()) {
            System.out.println("doesnt exist");
            return false;
        }
        long l = f.length();
        if (!uidToPinMap.containsKey(pin)) {
            System.out.print("ERROR: No PIN recorded for " + pin);
            return false;
        }
        final String uid = uidToPinMap.get(pin);
        if (!hasSharedSecret(uid)) {
            System.out.println("ERROR: Handshake is not complete with " + pin);
            return false;
        }
        //handle folders
        boolean isZip = false;
        //p is the updated path
        String p;
        if (f.isDirectory()) {
            p = ClientSettings.WORKING_DIR_CRYPT + FileUtils.getFileNameNoExt(path) + ".zip";
            ZipFile zf = new ZipFile(p);
            try {
                getFilesFromFolder(zf, new File(path));
            } catch (ZipException e) {
                e.printStackTrace();
                return false;
            }
            isZip = true;
        } else {
            p = path;
        }
        final boolean isZ = isZip;
        //common data
        byte[] secret = secretMap.get(uid).getSecret();
        String fileName = FileUtils.getFileName(path);
        String fileSize = FileUtils.getFileSize(path);
        final String fileUID = PINUtils.gen(FILE_SEND_UID_LENGTH);
        ArrayList<byte[]> nameData = Handshake.Encrypt(secret, fileName.getBytes());
        ArrayList<byte[]> sizeData = Handshake.Encrypt(secret, fileSize.getBytes());


        /*
         * Loads the entirety of the file into memory and
         * encrypts in a temp directory. Given the file fitting
         * on the network, send singular packet, else split it up
         * and send.
         */
        if (l < SIZE_IN_MEM_LIM) {
            long start = System.currentTimeMillis();
            ArrayList<byte[]> contentData = Handshake.Encrypt(secret, FileUtils.readFileAsBytes(p));

            /*
             * Sends the file with its data split up.
             * Send as FileSendPacketSplit.
             */
            int debug = 0;
            if (l > targetLength) {
                System.out.println("load into mem, but split.");
                Runnable r = () -> {
                    double rcs = getTargetNetSize() >= READ_CHUNK_SIZE ? READ_CHUNK_SIZE : getTargetNetSize();
                    double x = Math.ceil(l / rcs);
                    ArrayList<Object> splits = new ArrayList<>();
                    int c = 0; //count of Bytes in 'content'
                    int n = 0; //number of splits
                    int z = 0;
                    while (c < l) {
                        //get split content
                        Byte[] contentS = new Byte[targetLength];
                        int k = 0;
                        for (int i = 0; i < targetLength; i++) {
                            contentS[k++] = contentData.get(0)[c++];
                            if (c >= contentData.get(0).length) break;
                        }
                        //trim contents
                        if (k < contentS.length) contentS = Arrays.copyOf(contentS, k);
                        //setup packet
                        FileSendPacketSplit fsps = new FileSendPacketSplit();
                        boolean first = n == 0;
                        fsps.content = contentS;
                        fsps.contentParams = first ? FileUtils.convertBytes(contentData.get(1)) : null;
                        fsps.pin = pin;
                        fsps.fileName = first ? FileUtils.convertBytes(nameData.get(0)) : null;
                        fsps.fileNameParams = first ? FileUtils.convertBytes(nameData.get(1)) : null;
                        fsps.fileSize = first ? FileUtils.convertBytes(sizeData.get(0)) : null;
                        fsps.fileSizeParams = first ? FileUtils.convertBytes(sizeData.get(1)) : null;
                        fsps.isZip = isZ;
                        fsps.length = (int) x;
                        fsps.fileUID = fileUID;
                        fsps.finalPacket = ++n == x;
                        fsps.series = z++;
                        splits.add(fsps);
                    }
                    new IdleSender(client, splits, () -> splits.clear());
                };
                new Thread(r).start();
            } else {
                //setup packet
                FileSendPacket fsp = new FileSendPacket();
                fsp.content = FileUtils.convertBytes(contentData.get(0));
                fsp.contentParams = FileUtils.convertBytes(contentData.get(1));
                fsp.pin = pin;
                fsp.fileName = FileUtils.convertBytes(nameData.get(0));
                fsp.fileNameParams = FileUtils.convertBytes(nameData.get(1));
                fsp.fileSize = FileUtils.convertBytes(sizeData.get(0));
                fsp.fileSizeParams = FileUtils.convertBytes(sizeData.get(1));
                fsp.isZip = isZ;
                fsp.fileUID = fileUID;
                //send
                ArrayList<Object> list = new ArrayList<>();
                list.add(fsp);
                new IdleSender(client, list);
                debug = 1;
            }
            if (isZip) new File(p).delete();
            long elapsed = System.currentTimeMillis() - start;
            //todo: debug
            System.out.println("Sent file (" + fileName + ") of (" + fileSize + ").");
            System.out.println("Took " + (elapsed / 1000) + " seconds. {SINGLE READ}" + (debug == 0 ? " {FILE SEND PACKET SPLIT} 3" : "{FILE SEND PACKET}"));
            System.out.println("Is Zip: " + isZip);
            return true;
        } else {
            /*
             * Loads the file into memory in chunks of 'READ_CHUNK_SIZE' size.
             * Within each read the file is encrypted to a temp dir
             * & composing/sending a split packet.
             * Withstand 'getTargetNetSize()', do not send 'READ_CHUNK_SIZE' chunk sizes.
             * Delete folder file in working_dir if applicable.
             */
            double rcs = getTargetNetSize() >= READ_CHUNK_SIZE ? READ_CHUNK_SIZE : getTargetNetSize();
            double x = Math.ceil(l / rcs);
            long start = System.currentTimeMillis();
            final Num n = new Num(0);
            final String pp = p;
            System.out.println("each split");
            Runnable r = () -> {

		//TODO: optimize here.
                FileUtils.readFileAsBytes(p, (int) rcs, (chunk, perc) -> {
                    //enc in temp dir & delete
                    String temp = ClientSettings.WORKING_DIR_CRYPT + fileUID;
                    ArrayList<byte[]> contentData = Handshake.Encrypt(secret, chunk);
                    new File(temp).delete();
                    //setup packet to send
                    FileSendPacketSplit split = new FileSendPacketSplit();
                    split.content = FileUtils.convertBytes(contentData.get(0));
                    split.contentParams = FileUtils.convertBytes(contentData.get(1));
                    split.pin = pin;
                    if (n.value == 0) {
                        split.fileName = FileUtils.convertBytes(nameData.get(0));
                        split.fileNameParams = FileUtils.convertBytes(nameData.get(1));
                        split.fileSize = FileUtils.convertBytes(sizeData.get(0));
                        split.fileSizeParams = FileUtils.convertBytes(sizeData.get(1));
                    }
                    split.isZip = isZ;
                    split.fileUID = fileUID;
                    split.series = n.value;
                    split.length = (int) x;
                    split.finalPacket = ++n.value == x;
                    //send packet
                    ArrayList<Object> list = new ArrayList<>();
                    list.add(split);
                    new IdleSender(client, list);
                });
                long elapsed = System.currentTimeMillis() - start;
                //TODO: debug
                System.out.println("Sent file (" + fileName + ") of (" + fileSize + "). {ENCRYPT_EACH}");
                System.out.println("Took " + (elapsed / 1000) + " seconds.");
                System.out.println("Is Zip: " + isZ);
                System.out.println();
                if (isZ) new File(p).delete();
            };
            new Thread(r).start();
            return true;
        }
    }

    /**
     * Recursive helper function to add all sub-folders of
     * a folder to a zip object (to be zipped in the future).
     *
     * @param zipFile to add the found sub-folders to.
     * @param folder  last found folder.
     * @throws ZipException
     */
    private static void getFilesFromFolder(ZipFile zipFile, File folder) throws ZipException {
        if (!(folder.isFile() || folder.isDirectory())) {
            return;
        } else if (folder.isFile()) {
            zipFile.addFile(folder);
            return;
        }
        for (File f : folder.listFiles()) {
            if (f.isDirectory()) {
                zipFile.addFolder(f);
            } else {
                zipFile.addFile(f);
            }
        }
    }

    //remove from secret map
    //when disconnected
    public void removeClient(String uid) {
        secretMap.remove(uid);
    }


    /**
     * Invoked to send a 'FileAccept' Packet to the net.server.
     *
     * @param uid of the packet we want to accept.
     */
    public void sendAccept(String... uid) {
        FileAcceptPacket fap = new FileAcceptPacket();
        fap.UID = uid;
        client.sendTCP(fap);
    }

    /**
     * Returns true if a shared-secret is established between this Client and the
     * given UID.
     *
     * @param uid of the user to test if a shared-secret exists.
     * @return TRUE if a shared-secret exists.
     */
    public boolean hasSharedSecret(String uid) {
        if (!secretMap.containsKey(uid)) return false;
        return secretMap.get(uid).isComplete();
    }

    /**
     * Sends a request to the given UID for a handshake to
     * establish a shared-secret.
     *
     * @param uid of the user we would like to establish a shared-secret with.
     */
    public void sendHandshake(String uid) {
        if (secretMap.containsKey(uid)) return;
        Handshake h;
        secretMap.put(uid, h = new Handshake());
        Byte[] encryptedPubKey = FileUtils.convertBytes(h.getPubEncrypted());
        HandshakePacket hp = new HandshakePacket();
        hp.encryptedPubKey = encryptedPubKey;
        hp.UID = uid;
        client.sendTCP(hp);
        System.out.println("Sent handshake to " + uid);
    }

    /**
     * Returns the maximum allowable length of data sizes in
     * our network, I.E. 'targetLength'.
     *
     * @return maximum allowable number of bytes.
     */
    private long getTargetNetSize() {
        return targetLength;
    }

    /**
     * Sets a 'DisconnectListener' to be used whenever this client looses connection.
     *
     * @param listener to be invoked.
     */
    public void setDisconnectListener(DisconnectListener listener) {
        this.disconnectListener = listener;
    }

    /**
     * Sets a 'net.PinResponseListener' that will be used when a PinResponse is received.
     *
     * @param listener that is invoked when the pin response is received.
     */
    public void setPinResponseListener(PinResponseListener listener) {
        pinResponseListener = listener;
    }

    /**
     * Sets our net.PinUpdateListener to be used for this client.
     */
    public void setPinUpdateListener(PinUpdateListener listener) {
        pinUpdateListener = listener;
    }

    /**
     * Sets our net.FileRequestListener to be used for this client.
     *
     * @param listener to be used.
     */
    public void setFileRequestListener(FileRequestListener listener) {
        fileRequestListener = listener;
    }

    /**
     * Sets our ConnectionFinishedListener to be used for this client.
     *
     * @param listener to be used.
     */
    public void setConnectionFinishedListener(ConnectionFinishedListener listener) {
        this.connectionFinishedListener = listener;
    }
}

class Num {
    int value;

    public Num(int value) {
        this.value = value;
    }
}
