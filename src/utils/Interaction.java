package utils;

/**
 * This class belongs to the package utils and is for being use on Hyrax
 * Trust Peersim.
 */
public class Interaction {

    private Long time;
    private Long nodeID;
    private RESULT result;
    private TYPE type;
    private int blockID;

    public Interaction(Long time, Long nodeID, RESULT result, TYPE type, int blockID) {
        this.time = time;
        this.nodeID = nodeID;
        this.result = result;
        this.type = type;
        this.blockID = blockID;
    }

    public Long getTime() {
        return time;
    }

    public Long getNodeID() {
        return nodeID;
    }

    public RESULT getResult() {
        return result;
    }

    public boolean setResult(RESULT result) {
        this.result = result;
        return true;
    }

    public TYPE getType() {
        return type;
    }

    public int getBlockID() {
        return blockID;
    }

    public enum TYPE {DOWNLOAD, UPLOAD}

    public enum RESULT {SENT, NO_REPLY, GOOD, BAD}
}
