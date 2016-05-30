package peersim.bittorrent;

import peersim.core.Node;

import java.util.HashMap;

/**
 * This class belongs to the package peersim.bittorrent and is for being use on BitTrust.
 */
public class InterestMsg extends IntMsg {

    private final HashMap<Long, Integer> download;
    private final HashMap<Long, Integer> upload;

    public InterestMsg(Node sender, int value, long time, HashMap<Long, Integer> download,
                       HashMap<Long, Integer>
            upload) {
        super(4, sender, value, time);
        this.download = download;
        this.upload = upload;
    }

    public HashMap<Long, Integer> getDownload() {
        return download;
    }

    public HashMap<Long, Integer> getUpload() {
        return upload;
    }
}
