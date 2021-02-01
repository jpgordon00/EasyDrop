package net;

import util.FileUtils;
import net.packet.FileSendPacket;
import net.packet.FileSendPacketSplit;

import java.io.File;
import java.util.*;

/**
 * Wrapper for FileSendPacket that can also store
 * 'FileSendPacketSplit' for larger files.
 * Can flush files to 'DIR_WORK_SPACE', which must be set explicitly.
 * Types of files to flush:
 * FileSendPacketWrap, FileSendPacket, FileSendPacketHeader, FileSendPacketSplit.
 */
public class FileSendPacketWrap {

    /**
     * List of wrap packets for 'BinClient' and 'BinServer'.
     * Clearing of this list can be implemented manually, however it
     * is cleared with 'flush()' and 'flushAll()'.
     * It can be added to manually, or with 'load()' or 'loadAll()'.
     */
    public static ArrayList<FileSendPacketWrap> wrapQue;

    /**
     * Returns the que containing 'FileSendPacketWrap', or creates one if null.
     * @return the created que.
     */
    public static ArrayList<FileSendPacketWrap> getQue() {
        if (wrapQue == null) wrapQue = new ArrayList<>();
        return wrapQue;
    }

    /**
     * File name of our catalog used to store list of qued files.
     */
    private static final String CATALOG_FILE_NAME = "catalog.txt";

    /**
     * Complete file path to our catalog given directory is setup.
     */
    private static String cd;

    /**
     * Set by invoking 'initReadWrite'.
     */
    private static String DIR_WORK_SPACE = null;

    /**
     * Given a HashMap(String, Object) replace all Byte array objects
     * with a String interpretation of a Byte array.
     * @param map
     * @return
     */
    public static boolean replaceByteArrInString(HashMap<String, Object> map) {
        for (String key: map.keySet()) {
            Object o = map.get(key);
            if (o instanceof Byte[]) {
                map.put(key, FileUtils.byteArrayToString((Byte[]) o));
            }
        }
        return true;
    }

    public static boolean replaceStringInByteArr(HashMap<String, Object> map) {
        for (String key: map.keySet()) {
            Object o = map.get(key);
            if (o instanceof String) {
                if (FileUtils.isStringByteArr(key)) map.put(key, FileUtils.stringToByteArray(key));
            }
        }
        return true;
    }

    /**
     * Checks & Creates Dirs if not existing, but sets the path for the
     * work space & therefore allows reading and writing.
     */
    public static void initReadWrite(String path) {
        DIR_WORK_SPACE = path;
        if (!DIR_WORK_SPACE.endsWith(FileUtils.getSeparator())) DIR_WORK_SPACE += FileUtils.getSeparator();
        if (!FileUtils.doesDirectoryExist(path)) FileUtils.createDir(DIR_WORK_SPACE);
    }

    /**
     * Returns true if this class is ready to read/write packets to the
     * working directory.
     * @return
     */
    public static boolean canReadWrite() {
        return DIR_WORK_SPACE != null;
    }

    /**
     * If 'setupReadWrite' has been invoked then return the
     * full path of our File Catalog.
     */
    public static String getCatalogPath() {
        if (cd == null) cd = canReadWrite() ? DIR_WORK_SPACE + CATALOG_FILE_NAME : null;
        return cd;
    }


    /**
     * File path to a given file uid and name,
     * @param uid
     * @return
     */
    private static final String PARAMS_FILE_PATH(String uid) {
        return String.format(DIR_WORK_SPACE + "%s_%s", uid, "params");
    }

    /**
     * Gets all file paths to file content with the given uid.
     * @param uid of the file to search for.
     * @return
     */
    private static final String[] CONTENT_FILE_PATHS(String uid) {
        if (!hasCatalog(uid)) return null;
        if (FileUtils.doesFileExist(DIR_WORK_SPACE + uid)) {
            //...is singular
            return new String[]{DIR_WORK_SPACE + uid};
        }
        //...is spit
        ArrayList<String> strings = new ArrayList<>();
        HashMap<String, Object> map = getParamsAsMap(uid);
        int length = (int) map.get("length");
        for (int i = 0; i < length; i++) {
            String str = DIR_WORK_SPACE + uid + "_" + i;
            if (!FileUtils.doesFileExist(str)) continue;
            strings.add(str);
        }
        return strings.toArray(new String[0]);
    }

    /**
     * Given a file is recorded and contains split data, remove
     * based on its series.
     * @param series index of split data.
     * @return true if removal was successful, false otherwise.
     */
    public static boolean removeSplit(String uid, int series) {
        if (!hasCatalog(uid)) return false;
        //if the length is 1 then do complete removal
        if (getLength(uid) == 1) {
            return removeCompletely(uid);
        }
        //see if split exits
        String pr = uid + "_" + series;
        File f = new File(DIR_WORK_SPACE + pr);
        if (!f.exists()) return false;
        //remove from params
        HashMap<String, Object> map = getParamsAsMap(uid);
        if (!map.containsKey("param_" + series)) return false;
        map.remove("param_" + series);
        replaceByteArrInString(map);
        FileUtils.writeFileAsKeyValueMap(PARAMS_FILE_PATH(uid), map);
        //remove content
        return new File(DIR_WORK_SPACE + pr).delete();
    }

    /**
     * If the item with the given UID exists,
     * remove it from the catalog
     * all its content files
     * and param file
     * @param uid
     * @return true if and only if all content was fully deleted.
     */
    public static boolean removeCompletely(String uid) {
        if (!hasCatalog(uid)) return false;
        //remove from catalog
        String[] data = FileUtils.readFileAsStrings(getCatalogPath());
        String[] nd = new String[data.length - 1];
        int i = 0;
        for (String str: data) {
            if (!str.equals(uid)) nd[i++] = str;
        }
        FileUtils.writeFileAsStrings(true, getCatalogPath(), nd);
        boolean f = true;
        //remove all content files
        for (String contentPath: CONTENT_FILE_PATHS(uid)) {
            if (!(new File(contentPath).delete())) f = false;
        }
        //remove param files
        if (!(new File(PARAMS_FILE_PATH(uid)).delete())) f = false;
        return f;
    }

    /**
     * Returns true if the given wrap has params in each
     * split packet or if only the first split.
     * @param uid of file to check.
     * @return
     */
    public static boolean isEncryptedEach(String uid) {
        if (!hasSplit(uid)) return false;
        return getContentParams(uid).keySet().size() > 1;
    }

    /**
     * Returns true if the file at the given uid is singular, with no split content.
     * False if the file isn't recorded or isn't singular.
     * @param uid of the file to check
     * @return
     */
    public static boolean hasSingular(String uid) {
        if (!hasCatalog(uid)) return false;
        if (FileUtils.doesFileExist(DIR_WORK_SPACE + uid)) return true;
        return false;
    }

    /**
     * Returns true if and only if the file is a split file.
     * @param uid
     * @return true if the recorded file has split content.
     */
    public static boolean hasSplit(String uid) {
        if (!hasCatalog(uid)) return false;
        if (FileUtils.doesFileExist(DIR_WORK_SPACE + uid)) return false;
        int l = getLength(uid);
        for (int i = 0; i < l; i++) {
            if (FileUtils.doesFileExist(DIR_WORK_SPACE + uid + "_" + i)) return true;
        }
        return false;
    }

    /**
     * Returns true if a file with split content exists with the given UID.
     * @param uid
     * @return
     */
    public static boolean hasSplit(String uid, int z) {
        if (!hasCatalog(uid)) return false;
        if (FileUtils.doesFileExist(DIR_WORK_SPACE + uid)) return false;
        return FileUtils.doesFileExist(DIR_WORK_SPACE + uid + "_" + z);

    }

    /**
     * If content exists, return false.
     * If the object is not recorded, then record.
     * Update param "isFinished & "contentParams" for FileSendPacketWrap & FileSendPacketSplit.
     * Add content files.
     * Accepted argument object types:
     * FileSendPacketWrap, FileSendPacket, FileSendPacketSplit
     *
     * @param o
     * @return
     */
    public static boolean flush(Object o) {
        if (!(o instanceof FileSendPacket || o instanceof FileSendPacketWrap || o instanceof FileSendPacketSplit)) return false;
        //record info if it doesnt exist
        if (!hasCatalog(getParamsAsStrings(o)[0])) {
            record(o);
        } else {
            //todo check if content file exists ten don't do work
        }
        //add content files
        if (o instanceof FileSendPacket) {
            FileSendPacket p = (FileSendPacket) o;
            FileUtils.writeFileAsBytes(DIR_WORK_SPACE + p.fileUID, FileUtils.convertBytes(p.content));
        } else if (o instanceof FileSendPacketSplit) {
            FileSendPacketSplit s = (FileSendPacketSplit) o;
            FileUtils.writeFileAsBytes(DIR_WORK_SPACE + s.fileUID + "_" + s.series, FileUtils.convertBytes(s.content));
        } else {
            FileSendPacketWrap w = (FileSendPacketWrap) o;
            if (w.isSingular()) {
                FileSendPacket p = w.packet;
                FileUtils.writeFileAsBytes(DIR_WORK_SPACE + p.fileUID, FileUtils.convertBytes(p.content));
            } else {
                for (FileSendPacketSplit s: w.packets) {
                    FileUtils.writeFileAsBytes(DIR_WORK_SPACE + s.fileUID + "_" + s.series, FileUtils.convertBytes(s.content));
                }
            }
        }
        //update 'isFinished' and 'contentParams' for FileSendPacketWrap & FileSendPacketSplit
        if (o instanceof FileSendPacketWrap || o instanceof FileSendPacketSplit) {
            HashMap<String, Object> map = getParamsAsMap(getParamsAsStrings(o)[0]);
            if (o instanceof FileSendPacketWrap) {
                FileSendPacketWrap w = (FileSendPacketWrap) o;
                //isFinished
                if (w.isComplete()) {
                    map.put("isFinished", true);
                }
                //contentParams
                if (w.isSingular()){
                    map.put("param_0", FileUtils.byteArrayToString(w.packet.contentParams));
                } else {
                    for (FileSendPacketSplit split : w.packets) {
                        if (split.contentParams == null) continue;
                        map.put("param_" + split.series,  FileUtils.byteArrayToString(split.contentParams));
                    }
                }
                //write updated map
                replaceByteArrInString(map);
                FileUtils.writeFileAsKeyValueMap(PARAMS_FILE_PATH(w.getUID()), map);
            } else {
                FileSendPacketSplit s = (FileSendPacketSplit) o;
                //isFinished
                if (s.finalPacket) {
                    System.out.println("changed is finished");
                    map.put("isFinished", true);
                }
                //content params if applicable
                if (s.contentParams != null) {
                    map.put("param_" + s.series,  FileUtils.byteArrayToString(s.contentParams));
                }
                //write updated map
                replaceByteArrInString(map);
                FileUtils.writeFileAsKeyValueMap(PARAMS_FILE_PATH(s.fileUID), map);
            }
        }
        return true;
    }



    /**
     * If {@param overrride} & the object is recorded, then don't allow.
     * Given an acceptable argument for {@param o}, add a record of the object
     * to the catalog & create a params file.
     * @param o
     * @return
     */
    public static boolean record(Object o) {
        if (!(o instanceof FileSendPacket || o instanceof FileSendPacketWrap || o instanceof FileSendPacketSplit)) return false;
        if (hasCatalog(o)) return false;
        String[] params = getParamsAsStrings(o);
        //write to catalog first
        FileUtils.appendFileAsStrings(true, getCatalogPath(), getParamsAsStrings(o)[0]);
        //write params file
        HashMap<String, Object> map = new HashMap<>();
        int c = 0;
        map.put("uid", params[c++]);
        map.put("fileName", params[c++]);
        map.put("fileNameParams", params[c++]);
        map.put("fileSize", params[c++]);
        map.put("fileSizeParams", params[c++]);
        map.put("senderUID", params[c++]);
        map.put("isFinished", params[c++]);
        map.put("isZip", params[c++]);
        int l = Integer.parseInt(params[c]);
        map.put("length", params[c++]);
        //write params
        for (int i = 0; i < l; i++) {
            System.out.println("I: " + i);
            if (c + 1 >= params.length) break;
            String s = params[c++];
            map.put(FileUtils.getStringBefore(s, FileUtils.keyMapSep), FileUtils.getStringAfter(s, FileUtils.keyMapSep));
        }
        //store timestamp as string
        map.put("timestamp", System.currentTimeMillis());
        FileUtils.writeFileAsKeyValueMap(PARAMS_FILE_PATH(params[0]), map);
        System.out.println("continued");
        return true;
    }

    /**
     * Given a file existing with a given UID and its content not split,
     * read in memory as a 'FileSendPacket'.
     * @param uid of the file to generate
     * @return
     */
    public static FileSendPacket readSingle(String uid) {
        if (!hasCatalog(uid)) return null;
        if (getLength(uid) != 1) return null;
        FileSendPacket fsp = new FileSendPacket();
        //content
        fsp.content = FileUtils.convertBytes(FileUtils.readFileAsBytes(CONTENT_FILE_PATHS(uid)[0]));
        fsp.contentParams = getContentParams(uid).get(0);
        //params
        HashMap<String, Object> map = getParamsAsMap(uid);
        fsp.fileUID = uid;
        fsp.fileName = (Byte[]) map.get("fileName");
        fsp.fileNameParams = (Byte[]) map.get("fileNameParams");
        fsp.fileSize = (Byte[]) map.get("fileSize");
        fsp.fileSizeParams = (Byte[]) map.get("fileSizeParams");
        fsp.senderUID = (String) map.get("senderUID");
        fsp.isZip = (Boolean) map.get("isZip");
        return fsp;
    }

    /**
     * //TODO: size
     * Given a file existing with the given UID and series (content is split)
     * read a FileSendPacketSplit.
     * @param uid
     * @param series
     * @return
     */
    public static FileSendPacketSplit readSplit(String uid, int series) {
        if (!hasCatalog(uid)) return null;
        System.out.println("(" + uid + ") [" + series + "]" );
        //check if series exists in params
        HashMap<String, Object> map = getParamsAsMap(uid);
        //check if content exists
        if (!FileUtils.doesFileExist(DIR_WORK_SPACE + uid + "_" + series)) return null;
        FileSendPacketSplit split = new FileSendPacketSplit();
        split.fileUID = uid;
        split.fileName = (Byte[]) map.get("fileName");
        split.fileNameParams = (Byte[]) map.get("fileNameParams");
        split.fileSize = (Byte[]) map.get("fileSize");
        split.fileSizeParams = (Byte[]) map.get("fileSizeParams");
        split.senderUID = (String) map.get("senderUID");
        split.content = FileUtils.convertBytes(FileUtils.readFileAsBytes(DIR_WORK_SPACE + uid + "_" + series));
        split.contentParams = map.containsKey("param_" + series) ? (Byte[]) map.get("param_" + series) : null;
        split.series = series;
        split.length = (int) map.get("length");
        System.out.println("length from readSplit(): " + split.length);
        split.isZip = (boolean) map.get("isZip");
        split.finalPacket = (split.series + 1) == (split.length);
        return split;
    }

    /**
     * Given a file existing with the uid then read the given data, singular
     * or split into a FileSendPacketWrap.
     * @param uid
     * @return
     */
    public static FileSendPacketWrap read(String uid) {
        if (!hasCatalog(uid)) return null;
        if (getLength(uid) == 1) return new FileSendPacketWrap(readSingle(uid));
        FileSendPacketWrap wrap = new FileSendPacketWrap();
        HashMap<String, Object> map = getParamsAsMap(uid);
        wrap.packets = new FileSendPacketSplit[(int) map.get("length")];
        for (int i = 0; i < wrap.packets.length; i++) {
            //read split checks our packets and will return null
            //if not recorded
            wrap.packets[i] = readSplit(uid, i);
        }
        return wrap;
    }


    /**
     * Gets all parameters associated with the given object as strings.
     * Parameters are position as follows:
     * uid, fileName, fileNameParams, isFinished, isZip, length
     * @param o
     * @return
     */
    public static String[] getParamsAsStrings(Object o) {
        if (!(o instanceof FileSendPacket || o instanceof FileSendPacketWrap || o instanceof FileSendPacketSplit)) return null;
        String uid, fileName, fileNameParams, size, sizeParams, senderUID;
        boolean isFinished, isZip;
        int length = 0;
        ArrayList<String> params = new ArrayList<>();
        if (o instanceof FileSendPacket) {
            FileSendPacket fsp = (FileSendPacket) o;
            uid = fsp.fileUID;
            fileName = FileUtils.byteArrayToString(fsp.fileName);
            fileNameParams = FileUtils.byteArrayToString(fsp.fileNameParams);
            size = FileUtils.byteArrayToString(fsp.fileSize);
            sizeParams = FileUtils.byteArrayToString(fsp.fileSizeParams);
            senderUID = fsp.senderUID;
            isFinished = true;
            isZip = fsp.isZip;
            length = 1;
            params.add("param_0" + FileUtils.keyMapSep + FileUtils.byteArrayToString(fsp.contentParams));
        } else if (o instanceof FileSendPacketWrap) {
            FileSendPacketWrap fsp = (FileSendPacketWrap) o;
            uid = fsp.getUID();
            fileName = FileUtils.byteArrayToString(fsp.getFileName());
            fileNameParams = FileUtils.byteArrayToString(fsp.getFileNameParams());
            size = FileUtils.byteArrayToString(fsp.isSingular() ? fsp.packet.fileSize : fsp.packets[0].fileSize);
            sizeParams = FileUtils.byteArrayToString(fsp.isSingular() ? fsp.packet.fileSizeParams : fsp.packets[0].fileSizeParams);
            senderUID = fsp.isSingular() ? fsp.packet.senderUID : fsp.packets[0].senderUID;
            isFinished = fsp.isFinished();
            isZip = fsp.isZip();
            length = fsp.isSingular() ?  1 : fsp.packets[0].length;
            if (fsp.isSingular()) {
                params.add("param_" + FileUtils.keyMapSep + FileUtils.byteArrayToString(fsp.packet.contentParams));
            } else {
                for (FileSendPacketSplit fsps: fsp.packets) {
                    if (fsps.contentParams == null) continue;
                    params.add("param_" + fsps.series + "" + FileUtils.keyMapSep + FileUtils.byteArrayToString(fsps.contentParams));
                }
            }
        } else {
            FileSendPacketSplit fsps = (FileSendPacketSplit) o;
            uid = fsps.fileUID;
            fileName = fsps.fileName == null ? null: FileUtils.byteArrayToString(fsps.fileName);
            fileNameParams = fsps.fileNameParams == null ? null : FileUtils.byteArrayToString(fsps.fileNameParams);
            size = fsps.fileSize == null ? null : FileUtils.byteArrayToString(fsps.fileSize);
            sizeParams = fsps.fileSizeParams == null ? null : FileUtils.byteArrayToString(fsps.fileSizeParams);
            senderUID = fsps.senderUID;
            isFinished = fsps.finalPacket;
            isZip = fsps.isZip;
            length = fsps.length;
            if (fsps.contentParams != null) params.add("param_" + fsps.series + "" + FileUtils.keyMapSep + FileUtils.byteArrayToString(fsps.contentParams));
        }
        String[] s = new String[]{uid, fileName, fileNameParams, size, sizeParams, senderUID, isFinished + "", isZip + "", length + ""};
        int sl = s.length;
        s = Arrays.copyOf(s, s.length + params.size());
        int z = 0;
        for (String str: params) {
            s[sl + z++] = str;
        }
        return s;
    }

    /**
     * Reads all param values as Strings from a given uid.
     * Exchanges the String returned for values that represent Byte[] to a Byte[].
     * @param uid
     * @return
     */
    public static HashMap<String, Object> getParamsAsMap(String uid) {
        if (!hasCatalog(uid)) {
            return null;
        }
        HashMap<String, Object> map = FileUtils.readKeyValueMap(FileUtils.readFileAsStrings(PARAMS_FILE_PATH(uid)));
        map.put("fileName", FileUtils.stringToByteArray((String) map.get("fileName")));
        map.put("fileNameParams", FileUtils.stringToByteArray((String) map.get("fileNameParams")));
        map.put("fileSize", FileUtils.stringToByteArray((String) map.get("fileSize")));
        map.put("fileSizeParams", FileUtils.stringToByteArray((String) map.get("fileSizeParams")));
        if (hasSingular(uid)) {
            map.put("param_0", FileUtils.stringToByteArray((String) map.get("param_0")));
        } else {
            int l = (Integer) map.get("length");
            for (int i = 0; i < l; i++) {
                if (map.containsKey("param_" + i)) map.put("param_" + i, FileUtils.stringToByteArray((String) map.get("param_" + i)));
            }
        }
        return map;
    }

    /**
     * Gets all of the content parameters for the given file if it exists.
     * @param uid
     * @return
     */
    public static HashMap<Integer, Byte[]> getContentParams(String uid) {
        if (!hasCatalog(uid)) return null;
        HashMap<String, Object> map = getParamsAsMap(uid);
        HashMap<Integer, Byte[]> bytes = new HashMap<>();
        int l = getLength(uid);
        if (l == -1) return null;
        if (l == 1) {
            bytes.put(0, (Byte[]) map.get("param_0"));
            return bytes;
        }
        for (int i = 0; i < l; i++) {
            if (!map.containsKey("param_" + i)) continue;
            bytes.put(i, (Byte[]) map.get("param_" + i));
        }
        return bytes;
    }

    /**
     * Returns true if the object is accounted for the in catalog.
     * @param o
     * @return
     */
    public static boolean hasCatalog(Object o) {
        if (o instanceof String) hasCatalog((String) o);
        if (!(o instanceof FileSendPacket || o instanceof FileSendPacketWrap || o instanceof FileSendPacketSplit)) return false;
        boolean has = false;
        if (o instanceof FileSendPacket) if (has(((FileSendPacket) o).fileUID)) has = true;
        if (o instanceof FileSendPacketWrap) if (has(((FileSendPacketWrap) o).getUID())) has = true;
        if (o instanceof FileSendPacketSplit) if (has(((FileSendPacketSplit) o).fileUID)) has = true;
        return has;
    }

    /**
     * Returns true if the catalog contains the given file.
     * @param uid of the file to check.
     * @return
     */
    public static boolean hasCatalog(String uid) {
        if (!canReadWrite()) return false;
        String[] strs = FileUtils.readFileAsStrings(getCatalogPath());
        if (strs == null) return false;
        for (int i = 0; i < strs.length; i++) {
            if (strs[i].equals(uid)) return true;
        }
        return false;
    }

    /**
     * Returns true if the given object is fully accounted for in the catalog, has a params file and
     * all appropriate content files.
     * ACCEPTABLE OBJECTS: FileSendPacket, FileSendPacketWrap, FIleSendPacketSplit
     * @param o object to check if its been recorded.
     * @return true if fully accounted for.
     */
    public static boolean has(Object o) {
        if (o instanceof String) return hasCatalog(o);
        if (!(o instanceof FileSendPacket || o instanceof FileSendPacketWrap || o instanceof FileSendPacketSplit)) return false;
        if (!hasCatalog(o)) return false;
        if (o instanceof FileSendPacket) {
            FileSendPacket fsp = (FileSendPacket) o;
            //check params
            if (!FileUtils.doesFileExist(PARAMS_FILE_PATH(fsp.fileUID))) return false;
            //check content
            if (!FileUtils.doesFileExist(CONTENT_FILE_PATHS(fsp.fileUID)[0])) return false;
            return true;
        } else if (o instanceof FileSendPacketSplit) {
            FileSendPacketSplit fsps = (FileSendPacketSplit) o;
            //check params
            if (!FileUtils.doesFileExist(PARAMS_FILE_PATH(fsps.fileUID))) return false;
            //check content
            if (!FileUtils.doesFileExist(DIR_WORK_SPACE + "" + fsps.fileUID + "_" + fsps.series)) return false;
            return true;
        }
        FileSendPacketWrap wrap = (FileSendPacketWrap) o;
        //check params
        if (!FileUtils.doesFileExist(PARAMS_FILE_PATH(wrap.getUID()))) return false;
        //check content
        if (wrap.isSingular()) if (!FileUtils.doesFileExist(DIR_WORK_SPACE + wrap.getUID())) return false;
        for (int i = 0; i < wrap.packets.length; i++) {
            if (!FileUtils.doesFileExist(DIR_WORK_SPACE + wrap.getUID() + "_" + wrap.packets[i].series)) return false;
        }
        return true;
    }

    /**
     * Returns the timestamp of the given file's creation in milliseconds, or -1 if non existent.
     * @param uid of the file to get the timestamp of.
     * @return the timestamp of the given file or -1.
     */
    public static long getFileTimestamp(String uid) {
        if (!hasCatalog(uid)) return -1;
        return ((Double) getParamsAsMap(uid).get("timestamp")).longValue();
    }

    /**
     * Gets all UIDs from the catalog.
     * @return
     */
    public static String[] getAllUIDs() {
        if (!canReadWrite()) return new String[0];
        if (FileUtils.isFileEmpty(getCatalogPath())) return new String[0];
        return FileUtils.readFileAsStrings(getCatalogPath());
    }

    /**
     * If the recorded uid exists & is a split, gets the amount of packets that
     * are currently recorded.
     * If if singular, return 1.
     * @param uid of recorded file.
     * @return
     */
    public static int getLength(String uid) {
        if (!hasCatalog(uid)) return -1;
        if (FileUtils.doesFileExist(DIR_WORK_SPACE + uid)) return 1;
        //is split
        HashMap<String, Object> map = getParamsAsMap(uid);
        return (int) map.get("length");
    }

    /**
     * Counts the length of existing content files for the given file.
     * @param uid of the file to check the length of.
     * @return
     */
    public static int getRealLength(String uid) {
        if (FileUtils.doesFileExist(DIR_WORK_SPACE + uid)) return 1;
        int c = 0;
        int l = getLength(uid);
        for (int i = 0; i < l; i++) {
            if (FileUtils.doesFileExist(DIR_WORK_SPACE + uid + "_" + i)) c++;
        }
        return c;
    }

    /**
     * Returns true if the file associated with the uid is complete.
     * FileSendPacket's are complete, FileSendPacketSplit's are complete if all data is recorded.
     * @param uid
     * @return
     */
    public static boolean isFinished(String uid) {
        if (!hasCatalog(uid)) return false;
        HashMap<String, Object> map = FileUtils.readKeyValueMap(FileUtils.readFileAsStrings(PARAMS_FILE_PATH(uid)));
        return (boolean) map.get("isFinished");
    }

    /**
     * Finds a 'FileSendPacketWrap' given a UID associated with the packet.
     * Searches both the singular and split packets.
     * @param uid of a packet to find.
     * @return the packet associated with it, or null.
     */
    public static FileSendPacketWrap getWrap(String uid) {
        if (wrapQue == null) return null;
        for (FileSendPacketWrap fsp: wrapQue) {
            if (fsp.getUID().equals(uid)) return fsp;
        }
        return null;
    }

    /**
     * Removes a wrap from the que if the UID given it exists.
     * @param uid
     * @return
     */
    public static boolean removeWrap(String uid) {
        if (wrapQue == null) return false;
        Iterator<FileSendPacketWrap> it = wrapQue.iterator();
        while (it.hasNext()) {
            if (it.next().getUID().equals(uid)) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    /**
     * Adds a wrap to the que given a wrap.
     * @param p
     * @return
     */
    public static boolean addWrap(FileSendPacketWrap p) {
        if (wrapQue == null) return false;
        for (FileSendPacketWrap wrap: wrapQue) {
            if (wrap.getUID().equals(p.getUID())) return false;
        }
        wrapQue.add(p);
        return true;
    }


    public FileSendPacket packet;
    public FileSendPacketSplit[] packets;

    public FileSendPacketWrap(FileSendPacket packet) {
        this.packet = packet;
    }

    public FileSendPacketWrap(FileSendPacketSplit ... packets) {
        this.packets = packets;
    }

    public FileSendPacketWrap() {
    }

    public boolean hasContent() {
        if (packet != null || (packets != null)) return true;
        return false;
    }

    public boolean isSingular() {
        return packet != null;
    }

    public String getUID() {
        return isSingular() ? packet.fileUID: packets[0].fileUID;
    }

    public Byte[] getFileName() {
        return isSingular() ? packet.fileName : packets[0].fileName;
    }

    public Byte[] getFileNameParams() {
        return isSingular() ? packet.fileNameParams : packets[0].fileNameParams;
    }

    public boolean isFinished() {
        return isSingular() ? true : packets[packets.length - 1].finalPacket;
    }

    public boolean isZip() {
        return isSingular() ? packet.isZip : packets[0].isZip;
    }

    public Object getPacket() {
        return isSingular() ? packet : packets[0];
    }

    public FileSendPacket concatenate() {
        if (isSingular()) return null;
        if (!isFinished()) return null;
        //sort splits
        ArrayList<FileSendPacketSplit> arr = FileUtils.arrayToArrayList(packets);
        Collections.sort(arr, new SplitComparator());
        packets = arr.toArray(new FileSendPacketSplit[0]);
        int l = 0;
        for (FileSendPacketSplit s: packets) {
            l += s.content.length;
        }
        Byte[] data = new Byte[l];
        int c = 0;
        //combine
        for (FileSendPacketSplit s: packets) {
            for (Byte b: s.content) data[c++] = b;
        }
        //setup packet
        FileSendPacket fsp = new FileSendPacket();
        fsp.content = data;
        fsp.contentParams = packets[0].contentParams;
        fsp.fileUID = packets[0].fileUID;
        fsp.fileName = packets[0].fileName;
        fsp.fileNameParams = packets[0].fileNameParams;
        fsp.fileSize = packets[0].fileSize;
        fsp.fileSizeParams = packets[0].fileSizeParams;
        fsp.isZip = packets[0].isZip;
        return fsp;
    }

    public void addPacket(FileSendPacketSplit packet) {
        if (isSingular()) return;
        if (packets == null) {
            packets = new FileSendPacketSplit[1];
            packets[0] = packet;
            return;
        }
        packets = Arrays.copyOf(packets, packets.length + 1);
        packets[packets.length - 1] = packet;
    }


    public boolean isComplete() {
        return isSingular() ? false : packets[packets.length - 1].finalPacket;
    }


    public int getSize() {
        if (isSingular()) return -1;
        return packets.length;
    }
}