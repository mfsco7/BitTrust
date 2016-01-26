package peersim.bittorrent;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import peersim.config.Configuration;
import peersim.core.GeneralNode;
import peersim.edsim.EDSimulator;
import peersim.transport.Transport;
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

    private static final int UNCHOKE = 3;

    private static final int maxToUnchoke = 4;

    private static final String PAR_PROT = "protocol";
    private static final String PAR_MAX_INTER = "max_interactions";
    private static final String PAR_DIRECT_WEIGHT = "direct_weight";
    private static final String PAR_THRESH_ITP = "itp_thresh";
    private static final String PAR_THRESH_CRP = "crp_thresh";
    private final int pid;
    private final int maxNumInteractionsPerPeer;
    private final float directWeight;
    private final float itp_threshold;
    private final float crp_threshold;


    FileWriter file_interaction;
    FileWriter file_blocks;
    FileWriter file_requests;
    FileWriter messagesFile;
    FileWriter reputationFile;
    private ArrayList<Interaction> interactions;
    private HashMap<Long, HashMap<Long, Double>> nodesDirectTrust;

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
        maxNumInteractionsPerPeer = Configuration.getInt(prefix + "." + PAR_MAX_INTER, 624);
        directWeight = Configuration.getInt(prefix + "." + PAR_DIRECT_WEIGHT, 60) / 100f;
        itp_threshold = Configuration.getInt(prefix + "." + PAR_THRESH_ITP, 25) / 100f;//25 -> 2xMAD
        crp_threshold = Configuration.getInt(prefix + "." + PAR_THRESH_CRP, 25) / 100f;//25 -> 2xMAD
        interactions = new ArrayList<>();
        nodesDirectTrust = new HashMap<>();
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

    public boolean turnGoodInteraction(Long time, Long nodeID, TYPE type, int blockID) {
        Interaction interaction = getInteraction(time, nodeID, SENT, type, blockID);
        return interaction != null && interaction.setResult(GOOD);
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

    public Interaction getInteraction(long nodeID, RESULT result, TYPE type, int blockID) {
        for (Interaction interaction : interactions) {
            if (interaction.getNodeID() == nodeID && interaction.getType() == type &&
                    interaction.getResult() == result && interaction.getBlockID() == blockID) {
                return interaction;
            }
        }
        return null;
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
        result.nodesDirectTrust = new HashMap<>();
        try {
            result.file_interaction = new FileWriter("csv/interactions_" + result.getID() + ".csv");
            result.file_interaction.write("time;nodeID;result;type;blockID\n");

            result.file_blocks = new FileWriter("csv/blocks_" + result.getID() + ".csv");
            result.file_blocks.write("receiveTime;nodeId;value;requestTime\n");

            result.file_requests = new FileWriter("csv/requestsA_" + result.getID() + "" + ".csv");
            result.file_requests.write("requestTime;sender;blockID;receiveTime;receiver\n");

            result.messagesFile = new FileWriter("csv/messages_" + result.getID() + ".csv");
            result.messagesFile.write("Time;Sender;Type\n");

            result.reputationFile = new FileWriter("csv/reputation_" + result.getID() + ".csv");
            result.reputationFile.write("Node;DTP;RP;ITP;CRP;TTP;TRP\n");
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
    public void addNodeInteractions(long nodeID, HashMap<Long, Double> nodeInteractions) {
        this.nodesDirectTrust.put(nodeID, nodeInteractions);
    }

    public boolean removeInteraction(long time, long nodeID, RESULT result, TYPE type, int
            blockID) {
        Interaction interaction = getInteraction(time, nodeID, result, type, blockID);
        return interactions.remove(interaction);
    }

    public boolean removeInteraction(long nodeID, RESULT result, TYPE type, int blockID) {
        Interaction interaction = getInteraction(nodeID, result, type, blockID);
        return interactions.remove(interaction);
    }

    public IncrementalFreq getNumberInteractionsByResult(long nodeID, TYPE type) {
        IncrementalFreq freq = new IncrementalFreq();

        for (Interaction interaction : interactions) {
            if (interaction.getNodeID() == nodeID && interaction.getType() == type) {
                freq.add(interaction.getResult().ordinal());
            }
        }

        return freq;
    }

    public double getDirectTrust(long nodeID) {
        //TODO remove the older interactions
        IncrementalFreq freq = getNumberInteractionsByResult(nodeID, TYPE.DOWNLOAD);
        int alpha = freq.getFreq(GOOD.ordinal());
        int beta = freq.getFreq(NO_REPLY.ordinal()) + freq.getFreq(BAD.ordinal());

        return (alpha + 1d) / (alpha + beta + 2);
    }

    public double getReputation(long nodeID) {
        IncrementalFreq freq = getNumberInteractionsByResult(nodeID, TYPE.DOWNLOAD);
        return ((double) freq.getN()) / maxNumInteractionsPerPeer;
    }

    public double[] getDirectPercentages(long nodeID) {
        //TODO remove the older interactions
        IncrementalFreq freq = getNumberInteractionsByResult(nodeID, TYPE.DOWNLOAD);
        int alpha = freq.getFreq(GOOD.ordinal());
        int beta = freq.getFreq(NO_REPLY.ordinal()) + freq.getFreq(BAD.ordinal());

        double DTP = (alpha + 1d) / (alpha + beta + 2);
        //TODO reanalyse double cast
        double RP = (alpha + beta) / (double) maxNumInteractionsPerPeer;
        return new double[]{DTP, RP};
    }

    /**
     * Obtain indirect trust from others relative to the specific node
     *
     * @param nodeID
     * @return
     */
    public double[] getIndirectPercentages(long nodeID) {
        DescriptiveStatistics stats = new DescriptiveStatistics();
        DescriptiveStatistics stats2 = new DescriptiveStatistics();
        HashMap<Long, double[]> percentage = new HashMap<>();

        for (Neighbor neighbor : ((BitTorrent) (getProtocol(pid))).getCache()) {
            if (neighbor.node != null && neighbor.node.getID() != nodeID) {
                double[] percentages = neighbor.node.getDirectPercentages(nodeID);
                stats.addValue(percentages[0]);
                stats2.addValue(percentages[1]);
                percentage.put(neighbor.node.getID(), percentages);
            }
        }

        double medianDTP = stats.getPercentile(50);
        double medianRP = stats2.getPercentile(50);

        DescriptiveStatistics statsITP = new DescriptiveStatistics();
        DescriptiveStatistics statsCRP = new DescriptiveStatistics();

        for (Neighbor neighbor : ((BitTorrent) (getProtocol(pid))).getCache()) {
            if (neighbor.node != null && neighbor.node.getID() != nodeID) {
                double[] percentages = percentage.get(neighbor.node.getID());
                if (Math.abs(medianDTP - percentages[0]) <= itp_threshold) {
                    statsITP.addValue(percentages[0]);
                }
                if (Math.abs(medianRP - percentages[1]) <= crp_threshold) {
                    statsCRP.addValue(percentages[1]);
                }
            }
        }
        return new double[]{statsITP.getMean(), statsCRP.getMean()};
    }

    public double getTTP(double DTP, double ITP) {
        return (directWeight * DTP) + ((1 - directWeight) * ITP);
    }

    public double getTRP(double RP, double CRP) {
        return (directWeight * RP) + ((1 - directWeight) * CRP);
    }

    public double[] getPercentages(long nodeID) {
        double[] directPercentages = getDirectPercentages(nodeID);
        double[] indirectPercentages = getIndirectPercentages(nodeID);

        double DTP = directPercentages[0];
        double RP = directPercentages[1];

        double ITP = indirectPercentages[0];
        double CRP = indirectPercentages[1];

        double TTP = getTTP(DTP, ITP);
        double TRP = getTRP(RP, CRP);

        try {
            reputationFile.write(nodeID + ";" + DTP + ";" + RP + ";" + ITP + ";" + CRP + ";" + TTP +
                    ";" + TRP + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new double[]{TTP, TRP};
    }

    public void unchokingAlgorithm() {

        final float exTrust = 0.91f;
        final float vgTrust = 0.81f;
        final float gdTrust = 0.71f;
        final float satTrust = 0.61f;
        final float poorTrust = 0.5f;

        final float exRate = 0.81f;
        final float vgRate = 0.61f;
        final float gdRate = 0.41f;
        final float satRate = 0.21f;
        final float poorRate = 0;

        Integer nUnchoked = 0;

        HashMap<Neighbor, double[]> nodePercentages = new HashMap<>();

        float[][] quality = {//1st quality control for TTP, others for TRP
                /* Phase I   */ {exTrust, exRate}, {exTrust, vgRate}, {exTrust, gdRate},
                /*           */ {vgTrust, exRate}, {vgTrust, vgRate}, {vgTrust, gdRate},
                /*           */ {gdTrust, exRate}, {gdTrust, vgRate}, {gdTrust, gdRate},
                /* Phase II  */ {exTrust, satRate}, {exTrust, poorRate},
                /*           */ {vgTrust, satRate}, {vgTrust, poorRate},
                /*           */ {gdTrust, satRate}, {gdTrust, poorRate},
                /* Phase III */ {satTrust, exRate}, {satTrust, vgRate}, {satTrust, gdRate},
                /*           */ {poorTrust, exRate}, {poorTrust, vgRate}, {poorTrust, gdRate},
                /* Phase IV  */ {satTrust, satRate}, {satTrust, poorRate},
                /*           */ {poorTrust, satRate}, {poorTrust, poorRate}};

        //Create BitPeerList
        for (Neighbor neighbor : ((BitTorrent) (getProtocol(pid))).getCache()) {
            double[] percentages = getPercentages(neighbor.node.getID());
            nodePercentages.put(neighbor, percentages);
        }

        //Unchoke nodes for each quality control
        for (int i = 0; i < quality.length && nUnchoked < maxToUnchoke; i++) {
            qualityControl(nodePercentages, nUnchoked, quality[i]);
        }
    }

    int qualityControl(HashMap<Neighbor, double[]> nodePercentages, Integer nUnchoked, float[]
            quality) {
        for (Map.Entry<Neighbor, double[]> entry : nodePercentages.entrySet()) {
            Neighbor neighbor = entry.getKey();
            double[] percentages = entry.getValue();

            double TTP = percentages[0];
            double TRP = percentages[1];

            if (TTP >= quality[0] && TRP >= quality[1]) {
                unchoke(neighbor);
                nodePercentages.remove(neighbor); //BitPeerList.Remove

                if (++nUnchoked >= maxToUnchoke) {
                    break;
                }
            }
        }
        return nUnchoked;
    }

    void unchoke(Neighbor neighbor) {
        neighbor.status = 1;
        Object msg = new SimpleMsg(UNCHOKE, this);
        int tid = ((BitTorrent) getProtocol(pid)).tid;
        long latency = ((Transport) getProtocol(tid)).getLatency(this, neighbor.node);
        EDSimulator.add(latency, msg, neighbor.node, pid);
        neighbor.justSent();
    }
}
