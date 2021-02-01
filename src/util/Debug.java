package util;
import com.esotericsoftware.kryonet.Listener;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;


public class Debug extends Listener {

    public static void main3(String[] args) {}

    public static void main(String[] args) {
        //floodMap();
        Random random = new Random();
        byte[] bytes = new byte[(int) FileUtils.BYTES_IN_MB];
        random.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(bytes[i]);
        }
        FileUtils.writeFileAsBytes(FileUtils.getDesktopDirectory() + "bytes_raw.txt", bytes);
        FileUtils.writeFileAsBytes(FileUtils.getDesktopDirectory() + "bytes_converted.txt", sb.toString().getBytes());
    }

    public static void floodMap() {
        String file = FileUtils.getDesktopDirectory() + "flood";
        File f = new File(file);
        if (f.exists()) f.delete();
        try {
            f.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        long start = System.currentTimeMillis();
        Random r = new Random();
        int target = 30000000;
        int eachSize = 2000000;
        int amnt = (int) Math.ceil(target / eachSize);
        for (int z = 0; z < amnt; z++) {
            HashMap<String, Object> map = new HashMap<>();
            for (int i = 0; i < eachSize; i++) map.put(PINUtils.gen(5), PINUtils.gen(10));
            for (int i = 0; i < eachSize; i++) map.put(PINUtils.gen(5), r.nextInt(1000));
            FileUtils.appendFileAsKeyValueMap(file, map);
        }
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("done? " + elapsed);
    }

    public static float problem1(float n) {
        if (n <= 0) return 1;
        return -3 * problem1(n - 1) + 4 * problem1(n - 2);
    }

    public static void main2(String[] args) {
        Handshake a = new Handshake();
        Handshake b = new Handshake(a.getPubEncrypted());
        a.generateAlice(b.getPubEncrypted());

        String inputFile = FileUtils.getDesktopDirectory() + "rope.jar";
        String tempDir = FileUtils.getDesktopDirectory() + "temp" + FileUtils.getSeparator();
        String fileName = "rope";
        String downloadDir = FileUtils.getDesktopDirectory();
        try {
            //encrypt
            String s1 = tempDir + fileName + ".zip";
            ZipFile zf = new ZipFile(s1);
            getFilesFromFolder(zf, new File(inputFile));
            ArrayList<byte[]> encOut = Handshake.Encrypt(a.getSecret(), FileUtils.readFileAsBytes(s1));
            //TODO: encOut[0] = encData, encOut[1] = encParams
            //DELETE file @ s1
            new File(s1).delete();

            //decrypt
            String s2 = tempDir + fileName + "_enc";
            String s3 = downloadDir + fileName + "_new";
            FileUtils.writeFileAsBytes(s2, Handshake.Decrypt(b.getSecret(), encOut.get(0), encOut.get(1)));
            new ZipFile(s2).extractAll(s3);
            //DELETE file @ s2
            new File(s2).delete();
        } catch (ZipException e) {
            e.printStackTrace();
        }
    }

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
}