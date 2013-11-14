/**
 * 
 */
package netutils.files;

import java.io.IOException;

import netutils.NetUtilsException;
import netutils.utils.ByteUtils;

/**
 * @author <a href="mailto:osadeh@vasonanetworks.com">Oren Sadeh</a>
 * 
 */
public class CaptureFileUtility {

    private static final int L3_OFFSET_ETHERNET = 14;

    private static final int L3_OFFSET_ETHERNET_VLAN = 18;

    /**
     * 
     */
    private final String filename;

    /**
     * Index of the last-read packet.
     */
    private int lastReadIndex = 0;

    /**
     * Array of hops between selected packets indices
     */
    private final int[] hops;

    /**
     * Index of the next packet index
     */
    private int nextIndex = 0;

    /**
     * 
     */
    private final CaptureIterator itr;

    public CaptureFileUtility(String filename) throws IOException, NetUtilsException {
        this(filename, null);
    }

    /**
     * @param filename
     * @param l3Offset
     * @param hops
     * @throws NetUtilsException
     * @throws IOException
     */
    public CaptureFileUtility(String filename, int[] indices) throws IOException, NetUtilsException {
        super();
        this.filename = filename;
        if (filename == null) {
            throw new RuntimeException("Got null filename");
        }

        if ((indices != null) && indices.length > 0) {

            if (indices[0] < 1) {
                throw new RuntimeException("Indices are 1-based. Values lower than 1 are not allowed");
            }

            hops = new int[indices.length];
            hops[0] = indices[0] - 1;
            // Make sure hops are unique and sorted. If not - throw an error
            for (int i = 1; i < indices.length; i++) {
                if (indices[i - 1] >= indices[i]) {
                    throw new RuntimeException("Indices must be unique and sorted");
                }
                hops[i] = indices[i] - indices[i - 1] - 1;
            }
        } else {
            // We'll iterate through all packets
            this.hops = null;
        }

        itr = new CaptureIterator(CaptureFileFactory.createCaptureFileReader(filename));
    }

    /**
     * @return the filename
     */
    public String getFilename() {
        return filename;
    }

    /**
     * @return the index of the last-read packet
     */
    public int getLastPacketIndex() {
        return lastReadIndex;
    }

    /**
     * @return time stamp of the last-read packet
     */
    public long getLastTimestamp() {
        return itr.getLastTimestamp();
    }

    /**
     * Move to the next requested packet and return its data bytes
     * 
     * @return the next packet bytes, or null when EOF
     */
    public byte[] getNextPacket() {
        // No more packets
        if (!itr.hasNext()) {
            return null;
        }

        // Not using hops - apply simple packet iteration
        if (hops == null) {
            lastReadIndex++;
            return internalGetNextPacketData();
        }

        // No more hops left - return null to mark EOF
        if (nextIndex >= hops.length) {
            return null;
        }

        while (hops[nextIndex] > 0) {
            if (!itr.hasNext()) {
                return null;
            }
            lastReadIndex++;
            itr.next();
            hops[nextIndex]--;
        }

        byte[] data = internalGetNextPacketData();
        lastReadIndex++;
        nextIndex++;

        return data;
    }

    private byte[] internalGetNextPacketData() {

        byte data[] = itr.next();
        itr.getLastTimestamp();
        int tag = ByteUtils.getByteNetOrderTo_uint16(data, 12);
        int offset = L3_OFFSET_ETHERNET;
        if (tag == 0x8100) {
            offset = L3_OFFSET_ETHERNET_VLAN;
        }
        byte[] packet = new byte[data.length - offset];
        System.arraycopy(data, offset, packet, 0, packet.length);

        return packet;
    }
}
