package util;

import javax.crypto.*;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;

/**
 * This class handles all assets of the Diffie-Hellman key exchange between
 * two remote clients; the first client, "Alice", and the second client, "Bob".
 * (1a) Alice generates a public key in its constructor.
 * (1b) Bob takes Alice's public key and generates its public key & secret.
 * (2a) Alice takes Bob's public key and generates its secret.
 *
 * This class also contains utilities for encryption and decryption.
 * Encryption takes a shared secret and an input and output file.
 */
public class Handshake {

    /**
     * Encryption using AES/CBC/PKC5SPadding given a shared secret and an input and output file.
     * @param sharedSecret
     * @param inputFile
     * @param outputFile
     * @return encoded parameters used in the encryption.
     * Without these parameters, the DECRYPTION will fail.
     */
    public static byte[] Encrypt(byte[] sharedSecret, String inputFile, String outputFile) {
        try {
            SecretKeySpec key = new SecretKeySpec(sharedSecret, 0, 16, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key);

            byte[] outputBytes = cipher.doFinal(FileUtils.readFileAsBytes(inputFile));
            FileUtils.writeFileAsBytes(outputFile, outputBytes);
            return cipher.getParameters().getEncoded();
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException | IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Decryption using AES/CBC/PKC5SPadding given a shared secret and an input and output file.
     * @param sharedSecret
     * @param encodedData
     * @param encodedParams
     * @param outputFile
     */
    public static void Decrypt(byte[] sharedSecret, byte[] encodedData, byte[] encodedParams, String outputFile) {
        try {
            SecretKeySpec key = new SecretKeySpec(sharedSecret, 0, 16, "AES");
            AlgorithmParameters aesParams = AlgorithmParameters.getInstance("AES");
            aesParams.init(encodedParams);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key, aesParams);

            byte[] outputBytes = cipher.doFinal(encodedData);
            FileUtils.writeFileAsBytes(outputFile, outputBytes);
        } catch (InvalidAlgorithmParameterException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException | IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Encrypts given data given a shared-secret, returning the encrypted data and the
     * cipher parameters to be used for decryption/re-encryption.
     * @param sharedSecret
     * @param inputData
     * @return
     */
    public static ArrayList<byte[]> Encrypt(byte[] sharedSecret, byte[] inputData) {
        try {
            SecretKeySpec key = new SecretKeySpec(sharedSecret, 0, 16, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            ArrayList<byte[]> out = new ArrayList<>();
            out.add(cipher.doFinal(inputData));
            out.add(cipher.getParameters().getEncoded());
            return out;
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException | IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Decrypts the given data to a byte[] given a shared secret, encoded data, and encoded params.
     * @param sharedSecret
     * @param encodedData
     * @param encodedParams
     * @return
     */
    public static byte[] Decrypt(byte[] sharedSecret, byte[] encodedData, byte[] encodedParams) {
        try {
            SecretKeySpec key = new SecretKeySpec(sharedSecret, 0, 16, "AES");
            AlgorithmParameters aesParams = AlgorithmParameters.getInstance("AES");
            aesParams.init(encodedParams);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key, aesParams);
            return cipher.doFinal(encodedData);
        } catch (InvalidAlgorithmParameterException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException | IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Converts a byte to hex digit and writes to the supplied buffer
     * From {@author Oracle)
     */
    private static void byte2hex(byte b, StringBuffer buf) {
        char[] hexChars = { '0', '1', '2', '3', '4', '5', '6', '7', '8',
                '9', 'A', 'B', 'C', 'D', 'E', 'F' };
        int high = ((b & 0xf0) >> 4);
        int low = (b & 0x0f);
        buf.append(hexChars[high]);
        buf.append(hexChars[low]);
    }

    public static String printSecretKey(byte[] block) {
        StringBuffer buf = new StringBuffer();
        int len = block.length;
        for (int i = 0; i < len; i++) {
            byte2hex(block[i], buf);
            if (i < len-1) {
                buf.append("-");
            }
        }
        return buf.toString();
    }

    public static String printSecretKey(Byte[] block) {
        return printSecretKey(FileUtils.convertBytes(block));
    }

    public static final int ALICE = 0, BOB = 1;
    public static final int KEYSIZE = 2048;

    public Handshake(int mode) {
        this.mode = mode;
        if (mode == ALICE)  initAlice();
    }

    public Handshake(byte[] pubKeyEnc) {
        mode = BOB;
        generateBob(pubKeyEnc);
    }

    public Handshake() {
        mode = ALICE;
        initAlice();
    }

    private int mode = -1;

    /**
     * -1 = failure
     * 0 = default status
     * 1 = success
     */
    private int status = 0;

    /**
     * Objects for ALICE
     */
    private byte[] pubKeyEnc;
    private KeyAgreement keyAgree;
    private byte[] sharedSecret;


    /**
     * Objects for BOB
     */
    private PublicKey publicKey;

    public byte[] getPubEncrypted() {
        if (mode == -1 || status == -1) return null;
        return pubKeyEnc;
    }

    public byte[] getSecret() {
        if (mode == -1 || status == -1) return null;
        return sharedSecret;
    }

    public boolean isComplete() {
        return status == 1;
    }

    public boolean isInProgress() {
        return status == 0 && mode != -1;
    }

    public boolean isFailure() {
        return status == -1 || mode == -1;
    }

    public boolean isAlice() {
        return mode == ALICE;
    }

    public boolean isBob() {
        return mode == BOB;
    }

    private void initAlice() {
        try {
            //generate key pair generator
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("DH");
            keyPairGen.initialize(KEYSIZE);
            KeyPair keyPair = keyPairGen.generateKeyPair();
            // Handshake creates and initializes her DH KeyAgreement object
            keyAgree = KeyAgreement.getInstance("DH");
            keyAgree.init(keyPair.getPrivate());
            // Handshake encodes her public key, and sends it over to Bob.
            pubKeyEnc = keyPair.getPublic().getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            status = -1;
        }
    }

    public byte[] generateAlice(byte[] pubKeyEnc) {
        if (status == -1) return null;
        try {
            KeyFactory keyFac = KeyFactory.getInstance("DH");
            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(pubKeyEnc);
            PublicKey publicKey = keyFac.generatePublic(x509KeySpec);
            keyAgree.doPhase(publicKey, true);
            status = 1;
            return sharedSecret = keyAgree.generateSecret();
        } catch (InvalidKeyException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            status = -1;
        }
        return null;
    }


    public void generateBob(byte[] pubKeyEnc) {
        try {
            KeyFactory keyFac = KeyFactory.getInstance("DH");
            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(pubKeyEnc);
            publicKey = keyFac.generatePublic(x509KeySpec);

            DHParameterSpec dhParamFromPubKey = ((DHPublicKey)publicKey).getParams();

            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("DH");
            keyPairGen.initialize(dhParamFromPubKey);
            KeyPair keyPair = keyPairGen.generateKeyPair();

            keyAgree = KeyAgreement.getInstance("DH");
            keyAgree.init(keyPair.getPrivate());
            this.pubKeyEnc = keyPair.getPublic().getEncoded();

            phase2Bob();
        } catch (InvalidKeyException | InvalidAlgorithmParameterException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            status = -1;
        }
    }

    private byte[] phase2Bob() {
        if (status == -1) return null;
        try {
            keyAgree.doPhase(publicKey, true);
            status = 1;
            return sharedSecret = keyAgree.generateSecret();
        } catch (InvalidKeyException ex) {
            status = -1;
        }
        return null;
    }
}
