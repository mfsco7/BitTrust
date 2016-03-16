package peersim.bittorrent;

import peersim.core.Node;

/**
 * This class belongs to the package peersim.bittorrent and is for being use on BitTrust.
 */
public class TimeOutEvent extends SimpleEvent {

    private int value;
    private Node sender;

    TimeOutEvent(long time, int blockID, Node sender) {
        this.type = 19;
        this.time = time;
        this.value = blockID;
        this.sender = sender;
    }

    public int getValue() {
        return value;
    }

    public Node getSender() {
        return sender;
    }
}
