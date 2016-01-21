package peersim.bittorrent;

import peersim.core.Node;

import java.util.HashMap;

/**
 * This class belongs to the package peersim.bittorrent and is for being use
 * on Hyrax Trust Peersim.
 */
public class RequestMsg extends IntMsg {

    private HashMap<Long, Double> download;
    private HashMap<Long, Double> upload;

    /**
     * Constructor of a RequestMsg
     * @param sender
     * @param value
     * @param time
     * @param download
     * @param upload
     */
    public RequestMsg(Node sender, int value, long time,
                      HashMap<Long, Double> download, HashMap<Long, Double>
                              upload) {
        super(8, sender, value, time);
        this.download = download;
        this.upload = upload;
    }

    public HashMap<Long, Double> getDownload() {
        return download;
    }

    public HashMap<Long, Double> getUpload() {
        return upload;
    }
}
