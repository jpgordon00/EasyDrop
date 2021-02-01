package net;

import net.packet.FileSendPacketSplit;
import java.util.Comparator;

/**
 * Comparator for sorting 'FileSendPacketSplits' by their series,
 * from lowest to highest.
 */
public class SplitComparator implements Comparator<FileSendPacketSplit> {

    @Override
    public int compare(FileSendPacketSplit o1, FileSendPacketSplit o2) {
        if (o1.series.equals(o2.series)) return 0;
        return o1.series < o2.series ? -1 : 1;
    }
}