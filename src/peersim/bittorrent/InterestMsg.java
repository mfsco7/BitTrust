package peersim.bittorrent;

import org.apache.commons.math3.util.Pair;
import peersim.core.Node;

/**
 * This class belongs to the package peersim.bittorrent and is for being use on BitTrust.
 */
public class InterestMsg extends IntMsg {

//    private final HashMap<Long, Integer> download;
//    private final HashMap<Long, Integer> upload;

    private final Pair<Long, Integer> download;
    private final Pair<Long, Integer> upload;

    public InterestMsg(Node sender, int value, long time, Pair<Long, Integer> download,
                       Pair<Long, Integer> upload) {
        super(4, sender, value, time);
                this.download = download;
        this.upload = upload;
    }
    //    public InterestMsg(Node sender, int value, long time, HashMap<Long, Integer> download,
//                       HashMap<Long, Integer>
//            upload) {
//        super(4, sender, value, time);
//        this.download = download;
//        this.upload = upload;
//    }



    public Pair<Long, Integer> getDownload() {
        return download;
    }

    public Pair<Long, Integer> getUpload() {
        return upload;
    }
}
