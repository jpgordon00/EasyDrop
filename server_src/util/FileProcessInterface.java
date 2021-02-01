package util;

/**
 * This interface is used for file processing a File in Util/FileUtils.java
 * The given file is read chunk-for-chunk with each resulting chunk resulting
 * in 'processed' invocation for each processed chunk.
 */
public interface FileProcessInterface {

    /**
     * Invoked to read the current chunk of data from a file.
     * @param chunk current chunk of data to read.
     * @param perc percent of total bytes read so far.
     */
    void processed(byte[] chunk, double perc);
}
