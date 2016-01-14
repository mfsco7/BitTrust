package peersim.bittorrent;

import peersim.config.Configuration;
import peersim.core.GeneralNode;
import peersim.util.IncrementalFreq;
import utils.Interaction;
import utils.Interaction.RESULT;
import utils.Interaction.TYPE;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static utils.Interaction.RESULT.*;

/**
 * This class belongs to the package ${PACKAGE_NAME} and is for being use on
 * Hyrax Trust Peersim.
 */
public class BitNode extends GeneralNode {

    private static final String PAR_PROT = "protocol";
    private final int pid;
    FileWriter file_interaction;
    FileWriter file_blocks;
    FileWriter file_requests;
    FileWriter messagesFile;
    private ArrayList<Interaction> interactions;
    private HashMap<Long, HashMap<Long, Integer>> nodeInteractions;

    //    /**
    //     * Used to construct the prototype node. This class currently does not
    //     * have specific configuration parameters and so the parameter
    //     * <code>prefix</code> is not used. It reads the protocol components
    //     * (components that have type {@value Node#PAR_PROT}) from
    //     * the configuration.
    //     *
    //     * @param prefix
    //     */
    public BitNode(String prefix) {
        super(prefix);
        pid = Configuration.getPid(prefix + "." + PAR_PROT);
        interactions = new ArrayList<>();
        nodeInteractions = new HashMap<>();
        try {
            file_interaction = new FileWriter
                    ("/home/aferreira/Documentos/Hyrax/BitTrust/csv/interaction_" + getID() + "" +
                    ".csv");
            file_blocks = new FileWriter("/home/aferreira/Documentos/Hyrax/BitTrust/csv/blocks_" +
                    getID() + "" +
                    ".csv");
            file_requests = new FileWriter
                    ("/home/aferreira/Documentos/Hyrax/BitTrust/csv/requests_" +
                    getID() + ".csv");
            messagesFile = new FileWriter("csv/messages_" + getID() + ".csv");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static HashMap<Long, Integer> sortByValues(HashMap<Long, Integer> map) {
        List list = new LinkedList<>(map.entrySet());
        // Defined Custom Comparator here
        Collections.sort(list, new Comparator() {
            public int compare(Object o1, Object o2) {
                return ((Comparable) ((Map.Entry) (o1)).getValue()).compareTo(((Map.Entry) (o2))
                        .getValue());
            }
        });

        // Here I am copying the sorted list in HashMap
        // using LinkedHashMap to preserve the insertion order
        HashMap<Long, Integer> sortedHashMap = new LinkedHashMap<>();
        for (Iterator it = list.iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            sortedHashMap.put((Long) entry.getKey(), (Integer) entry.getValue());
        }
        return sortedHashMap;
    }

    public boolean addInteraction(long time, long nodeID, RESULT result, TYPE type, int blockID) {
        Interaction interaction = new Interaction(time, nodeID, result, type, blockID);
        interactions.add(interaction);
        try {
            file_interaction.write(time + ";" + nodeID + ";" + result + ";" + type + ";" +
                    blockID + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public boolean turnGoodInteraction(Long time, Long nodeID, TYPE type) {
        Interaction interaction = getInteraction(time, nodeID, type);
        return interaction != null && interaction.setResult(GOOD);
    }

    public boolean turnGoodInteraction(Long nodeID, TYPE type, int blockID) {
        Interaction interaction = getInteraction(nodeID, type, blockID);
        return interaction != null && interaction.setResult(GOOD);
    }

    public boolean turnGoodInteraction(Long time, Long nodeID, TYPE type, int blockID) {
        Interaction interaction = getInteraction(time, nodeID, SENT, type, blockID);
        return interaction != null && interaction.setResult(GOOD);
    }

    public int getNumberInteraction(Long time, Long nodeID, TYPE type) {
        int count = 0;
        for (Interaction interaction : interactions) {
            if (interaction.getNodeID() == nodeID && interaction.getType() == type) {

                if (interaction.getTime() == time) {
                    count++;
                }
            }
        }

        return count;
    }

    public int getNumberInteraction(Long nodeID, TYPE type, int blockID) {
        int count = 0;
        for (Interaction interaction : interactions) {
            if (interaction.getNodeID() == nodeID && interaction.getType() == type && interaction
                    .getBlockID() == blockID) {

                count++;
            }
        }
        return count;
    }

    public int getNumberInteraction(Long time, Long nodeID, TYPE type, int blockID) {
        int count = 0;
        for (Interaction interaction : interactions) {
            if (interaction.getTime() == time && interaction.getNodeID() == nodeID &&
                    interaction.getType() == type && interaction.getBlockID() == blockID) {
                count++;
            }
        }
        return count;
    }

    public void printInteractions() {
        System.out.println("HyraxNode " + getID() + ": " + interactions.size() +
                " interactions");
        for (Interaction interaction : interactions) {
            System.out.println("HyraxNode" + getID() + ": (" + interaction.getTime() + "," +
                    interaction.getNodeID() + "," +
                    interaction.getResult() + "," + interaction.getType() +
                    ")");
        }
    }

    //    public void printInteractions(Long nodeID, Interaction.TYPE type) {
    //        IncrementalFreq freq = new IncrementalFreq();
    //        for (Interaction interaction : interactions) {
    //            if (interaction.getNodeID() == nodeID && interaction.getType() ==
    //                    type) {
    //                System.out.println("HyraxNode: (" +
    // interaction.getTime() +
    //                        "," +
    //                        interaction.getNodeID() + "," +
    // interaction.getResult
    //                        () +
    //                        "," + interaction.getType() + ")");
    //                freq.add( interaction.getResult());
    //            }
    //        }
    //
    //        if (freq.getN() > 0) System.out.println("HyraxNode " + getID() + ": " +
    //                interactions.size() + " interactions which " + freq.getN() +
    //                " is with node " + nodeID + " type " + type);
    //
    //        freq.printAll(System.out);
    //    }

    public void printInteraction(long time, long nodeID, TYPE type) {
        for (Interaction interaction : interactions) {
            if (interaction.getNodeID() == nodeID && interaction.getType() == type) {

                if (interaction.getTime() == time) {
                    System.out.println("found a interaction");
                }
            }
        }
    }

    public Interaction getInteraction(long time, long nodeID, TYPE type) {
        for (Interaction interaction : interactions) {
            if (interaction.getNodeID() == nodeID && interaction.getType() == type) {

                if (interaction.getTime() == time) {
                    return interaction;
                }
            }
        }
        return null;
    }

    public Interaction getInteraction(long time, long nodeID, TYPE type, int blockID) {
        for (Interaction interaction : interactions) {
            if (interaction.getNodeID() == nodeID && interaction.getType() == type &&
                    interaction.getTime() == time && interaction.getBlockID() == blockID) {
                return interaction;
            }
        }
        return null;
    }

    public Interaction getInteraction(long nodeID, TYPE type, int blockID) {
        for (Interaction interaction : interactions) {
            if (interaction.getNodeID() == nodeID && interaction.getType() == type && interaction
                    .getBlockID() == blockID) {

                return interaction;
            }
        }
        return null;
    }

    public Interaction getInteraction(long time, long nodeID, RESULT result, TYPE type, int
            blockID) {
        for (Interaction interaction : interactions) {
            if (interaction.getNodeID() == nodeID && interaction.getType() == type &&
                    interaction.getResult() == result &&
                    interaction.getTime() == time && interaction.getBlockID() == blockID) {
                return interaction;
            }
        }
        return null;
    }

    public Interaction getInteraction(long nodeID, RESULT result, TYPE type, int
            blockID) {
        for (Interaction interaction : interactions) {
            if (interaction.getNodeID() == nodeID && interaction.getType() == type &&
                    interaction.getResult() == result && interaction.getBlockID() == blockID) {
                return interaction;
            }
        }
        return null;
    }

    public void printInteraction(long nodeID, TYPE type, int blockID) {
        for (Interaction interaction : interactions) {
            if (interaction.getNodeID() == nodeID && interaction.getType() == type && interaction
                    .getBlockID() == blockID) {

                System.out.println("found a interaction");
            }
        }
    }

    //    public IncrementalFreq getInteractions(Long nodeID, Interaction.TYPE type) {
    //        IncrementalFreq freq = new IncrementalFreq();
    //        for (Interaction interaction : interactions) {
    //            if (interaction.getNodeID() == nodeID && interaction.getType() ==
    //                    type) {
    //                                System.out.println("HyraxNode: (" +
    //                 interaction.getTime() +
    //                                        "," +
    //                                        interaction.getNodeID() + "," +
    //                 interaction.getResult
    //                                        () +
    //                                        "," + interaction.getType() + ")");
    //                freq.add(interaction.getResult());
    //            }
    //        }

    //        if (freq.getN() > 0) System.out.println("HyraxNode " +
    // getID() + ": " +
    //                interactions.size() + " interactions which " + freq
    // .getN() +
    //                " is with node " + nodeID);

    //        freq.printAll(System.out);

    //        return freq;
    //    }

    public int getNumberInteractions(long nodeID, TYPE type) {
        int count = 0;
        for (Interaction interaction : interactions) {
            if (interaction.getNodeID() == nodeID && interaction.getType() == type) {
                count++;
            }
        }
        return count;
    }

    public int getNumberInteractions(long nodeID, TYPE type, RESULT result) {
        int count = 0;
        for (Interaction interaction : interactions) {
            if (interaction.getNodeID() == nodeID && interaction.getType() == type && interaction
                    .getResult() == result) {
                count++;
            }
        }
        return count;
    }

    public HashMap<Long, Integer> getSortedInteractions(TYPE type) {

        HashMap<Long, Integer> sortedInteractions = new HashMap<>();

        for (Neighbor neighbor : ((BitTorrent) (getProtocol(pid))).getCache()) {
            if (neighbor != null && neighbor.node != null) {
                sortedInteractions.put(neighbor.node.getID(), getNumberInteractions(neighbor.node
                        .getID(), type, GOOD));
            }
        }
        return sortByValues(sortedInteractions);
    }

    public void printResumedInteractions(TYPE type) {
        for (Neighbor neighbor : ((BitTorrent) (getProtocol(pid))).getCache()) {
            if (neighbor != null && neighbor.node != null /*&& (/*neighbor.node.getID() == 2 /*||
                    neighbor.node.getID() == 18 || getID() == 18)*/) {
                System.out.print(neighbor.node.getID() + ":\t");

                int[] stats = new int[4];

                for (Interaction interaction : interactions) {
                    if (interaction.getNodeID() == neighbor.node.getID() && interaction.getType()
                            == type) {

                        switch (interaction.getResult()) {

                            case SENT:
                                stats[0]++;
                                System.out.println(interaction.getTime() + "-" + interaction
                                        .getBlockID());
                                break;
                            case NO_REPLY:
                                stats[1]++;
                                break;
                            case GOOD:
                                stats[2]++;
                                break;
                            case BAD:
                                stats[3]++;
                                break;
                        }
                    }
                }

                for (int stat : stats) {
                    System.out.print(stat + "\t");
                }
                System.out.println();
            }
        }
    }

    @Override
    public Object clone() {
        BitNode result;
        result = (BitNode) super.clone();
        result.interactions = new ArrayList<>();
        result.nodeInteractions = new HashMap<>();
        try {
            result.file_interaction = new FileWriter("csv/interactions_" + result.getID() + ".csv");
            result.file_interaction.write("time;nodeID;result;type;blockID\n");

            result.file_blocks = new FileWriter("csv/blocks_" + result.getID() + ".csv");
            result.file_blocks.write("receiveTime;nodeId;value;requestTime\n");

            result.file_requests = new FileWriter("csv/requestsA_" + result.getID() + "" + ".csv");
            result.file_requests.write("requestTime;sender;blockID;receiveTime;receiver\n");

            result.messagesFile = new FileWriter("csv/messages_" + result.getID() + ".csv");
            result.messagesFile.write("Time;Sender;Type\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Add the interactions receive by {@code nodeID}
     *
     * @param nodeID
     * @param nodeInteractions
     */
    public void addNodeInteractions(long nodeID, HashMap<Long, Integer> nodeInteractions) {
        this.nodeInteractions.put(nodeID, nodeInteractions);
    }

    public boolean removeInteraction(long time, long nodeID, RESULT result, TYPE type, int
            blockID) {
        Interaction interaction = getInteraction(time, nodeID, result, type, blockID);
        return interactions.remove(interaction);
    }

    public boolean removeInteraction(long nodeID, RESULT result, TYPE type, int
            blockID) {
        Interaction interaction = getInteraction(nodeID, result, type, blockID);
        return interactions.remove(interaction);
    }

    public IncrementalFreq getNumberInteractionsByResult(long nodeID) {
        IncrementalFreq freq = new IncrementalFreq();

        for (Interaction interaction : interactions) {
            if (interaction.getNodeID() == nodeID) {
                freq.add(interaction.getResult().ordinal());
            }
        }

        return freq;
    }

    public double getDirectTrust(long nodeID) {
        //TODO remove the older interactions
        IncrementalFreq freq = getNumberInteractionsByResult(nodeID);
        int alpha = freq.getFreq(GOOD.ordinal());
        int beta = freq.getFreq(NO_REPLY.ordinal()) + freq.getFreq(BAD.ordinal());

        return (alpha + 1d) / (alpha + beta + 2);
    }
}
