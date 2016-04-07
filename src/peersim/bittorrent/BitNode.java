package peersim.bittorrent;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.rank.Median;
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

    private static final int CHOKE = 2;
    private static final int UNCHOKE = 3;

    private static final int maxToUnchoke = 3;

    private static final String PAR_PROT = "protocol";
    private static final String PAR_MAX_INTER = "max_interactions";
    private static final String PAR_DIRECT_WEIGHT = "direct_weight";
    private final int pid;
    private final int maxNumInteractionsPerPeer;
    private final float directWeight;

    FileWriter file_interaction;
    FileWriter file_blocks;
    FileWriter file_requests;
    FileWriter messagesFile;
    FileWriter reputationFile;
    private ArrayList<Interaction> interactions;
    private HashMap<Long, HashMap<Long, Double>> nodesDirectTrust;

    enum Behaviour {
        NORMAL, BAD_CHUNK, SLOW, FREE_RIDER}

    private Behaviour behaviour;


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
        interactions = new ArrayList<>();
        nodesDirectTrust = new HashMap<>();
        behaviour = Behaviour.NORMAL;
    }

    public Behaviour getBehaviour() {
        return behaviour;
    }

    public boolean isNormal() {
        return behaviour == Behaviour.NORMAL;
    }

    public void setBadChunk() {
        behaviour = Behaviour.BAD_CHUNK;
    }

    public void setSlow() {
        behaviour = Behaviour.SLOW;
    }

    public boolean isFreeRider() {
        return behaviour == Behaviour.FREE_RIDER;
    }

    public void setFreeRider() {
        behaviour = Behaviour.FREE_RIDER;
    }

//    private static HashMap<Long, Integer> sortByValues(HashMap<Long, Integer> map) {
//        List list = new LinkedList<>(map.entrySet());
//        // Defined Custom Comparator here
//        Collections.sort(list, (o1, o2) -> ((Comparable) ((Map.Entry) (o1)).getValue()).compareTo(((Map.Entry) (o2))
//                .getValue()));
//
//        // Here I am copying the sorted list in HashMap
//        // using LinkedHashMap to preserve the insertion order
//        HashMap<Long, Integer> sortedHashMap = new LinkedHashMap<>();
//        for (Iterator it = list.iterator(); it.hasNext(); ) {
//            Map.Entry entry = (Map.Entry) it.next();
//            sortedHashMap.put((Long) entry.getKey(), (Integer) entry.getValue());
//        }
//        return sortedHashMap;
//    }

    public boolean addInteraction(long time, long nodeID, RESULT result, TYPE type, int blockID) {
        Interaction interaction = new Interaction(time, nodeID, result, type, blockID);
        removeOnLimit(nodeID, type);
        interactions.add(interaction);
        try {
            file_interaction.write(time + ";" + nodeID + ";" + result + ";" + type + ";" +
                    blockID + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public void removeOnLimit(long nodeID, TYPE type) {

        int count = 0;
        for (Interaction interaction : interactions) {
            if (interaction.getNodeID() == nodeID && interaction.getType() == type) {
                count++;
            }
        }
        int toRemove = Math.max(0, count - maxNumInteractionsPerPeer);
        Iterator<Interaction> iterator = interactions.iterator();
        while (toRemove > 0 && iterator.hasNext()) {

            Interaction interaction = iterator.next();
            if (interaction.getNodeID() == nodeID && interaction.getType() == type) {
                iterator.remove();
                toRemove--;
            }
        }
    }

    public boolean turnGoodInteraction(Long time, Long nodeID, TYPE type, int blockID) {
        Interaction interaction = getInteraction(time, nodeID, SENT, type, blockID);
        return interaction != null && interaction.setResult(GOOD);
    }

    public boolean turnBadNOREPLYInteraction(Long time, Long nodeID, TYPE type, int blockID) {
        Interaction interaction = getInteraction(time, nodeID, SENT, type, blockID);
        return interaction != null && interaction.setResult(NO_REPLY);
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

//    public HashMap<Long, Integer> getSortedInteractions(TYPE type) {
//
//        HashMap<Long, Integer> sortedInteractions = new HashMap<>();
//
//        for (Neighbor neighbor : ((BitTorrent) (getProtocol(pid))).getCache()) {
//            if (neighbor != null && neighbor.node != null) {
//                sortedInteractions.put(neighbor.node.getID(), getNumberInteractions(neighbor.node
//                        .getID(), type, GOOD));
//            }
//        }
//        return sortByValues(sortedInteractions);
//    }

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
        //TODO RP is greater than 1
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

        double madDTP = getMAD(stats.getValues(), medianDTP);
        double madRP = getMAD(stats2.getValues(), medianRP);

        DescriptiveStatistics statsITP = new DescriptiveStatistics();
        DescriptiveStatistics statsCRP = new DescriptiveStatistics();

        for (Neighbor neighbor : ((BitTorrent) (getProtocol(pid))).getCache()) {
            if (neighbor.node != null && neighbor.node.getID() != nodeID) {
                double[] percentages = percentage.get(neighbor.node.getID());
                if (Math.abs(medianDTP - percentages[0]) <= madDTP) {
                    statsITP.addValue(percentages[0]);
                }
                if (Math.abs(medianRP - percentages[1]) <= madRP) {
                    statsCRP.addValue(percentages[1]);
                }
            }
        }
        return new double[]{statsITP.getMean(), statsCRP.getMean()};
    }

    /**
     * Compute the median absolute deviation of a array.
     * @param values array of double
     * @return the median absolute deviation of a array
     */
    public double getMAD(double [] values, double medianValue){
        double [] temp = new double[values.length];
        Median m = new Median();
        for(int i=0 ; i<values.length ;i++){
            temp[i] = Math.abs(values[i] - medianValue);
        }
        return m.evaluate(temp); //return the median of temp
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

        float[][] quality = { //1st quality control for TTP, 2nd for TRP: {TTP, TRP}
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
            if (neighbor != null && neighbor.node != null) {
                double[] percentages = getPercentages(neighbor.node.getID());
                nodePercentages.put(neighbor, percentages);
            }
        }

        //Unchoke nodes for each quality control
        for (int i = 0; i < quality.length && nUnchoked < maxToUnchoke; i++) {
            qualityControl(nodePercentages, nUnchoked, quality[i]);
        }

        //Choke the remaining neighbours
        for (Neighbor neighbor : nodePercentages.keySet()) {
            if (neighbor != null && neighbor.node != null) {
                choke(neighbor);
            }
        }
    }

    int qualityControl(HashMap<Neighbor, double[]> nodePercentages, Integer nUnchoked, float[]
            quality) {
        Iterator<Neighbor> iterator = nodePercentages.keySet().iterator();
        while (iterator.hasNext()) {
            Neighbor neighbor = iterator.next();
            double[] percentages = nodePercentages.get(neighbor);

            double TTP = percentages[0];
            double TRP = percentages[1];

            if (TTP >= quality[0] && TRP >= quality[1]) {
                unchoke(neighbor);
//                nodePercentages.remove(neighbor); //BitPeerList.Remove
                iterator.remove();

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

    void choke(Neighbor neighbor) {
        neighbor.status = 0;
        Object ev = new SimpleMsg(CHOKE, this);
        int tid = ((BitTorrent) getProtocol(pid)).tid;
        long latency = ((Transport) getProtocol(tid)).getLatency(this,
                neighbor.node);
        EDSimulator.add(latency, ev, neighbor.node, pid);
        neighbor.justSent();
    }

    public void unchokingAlgorithm2() {
        final float trustThreshold = 0.5f;

        HashMap<Neighbor, Double> neighborRates = new HashMap<>(maxToUnchoke);
        ArrayList<Neighbor> neighborsToChoke = new ArrayList<>();

        //Create BitPeerList
        for (Neighbor neighbor : ((BitTorrent) (getProtocol(pid))).getCache()) {
            if (neighbor != null && neighbor.node != null) {
                double[] trustAndRate = getPercentages(neighbor.node.getID());

                if (trustAndRate[0] >= trustThreshold) {
//                    if (neighborRates.size() < maxToUnchoke) {
//                        neighborRates.put(neighbor, trustAndRate[1]);
//                    } else {
//
//                        Map.Entry<Neighbor, Double> worse = neighborRates.entrySet().stream()
//                                .min((neighbor1, neighbor2) -> Double.compare(neighbor1.getValue(),
//                                                                            neighbor2.getValue()))
//                                .get();
//
//                        if (worse != null && worse.getValue() < trustAndRate[1]) {
//                            neighborRates.remove(worse.getKey());
//                            neighborsToChoke.add(worse.getKey());
//                            neighborRates.put(neighbor, trustAndRate[1]);
//
//                        }
//
//                    }
                    neighborRates.put(neighbor, trustAndRate[1]);
                } else neighborsToChoke.add(neighbor);
            }
        }

        for (int i = 0; i < maxToUnchoke; i++) {
            Map.Entry<Neighbor, Double> better = null;
            for (Map.Entry<Neighbor, Double> entry : neighborRates.entrySet()) {
                if (better == null || (better.getValue() < entry.getValue()) && (((BitTorrent)entry
                        .getKey().node.getProtocol(pid)).getPeerStatus() == 0 )){
                    better = entry;
                }
            }
            if (better != null) {
                neighborRates.remove(better.getKey());

                unchoke(better.getKey());
            }
        }

//        neighborRates.keySet().forEach(this::unchoke);
//        for (Neighbor neighbor : neighborRates.keySet()) {
//            unchoke(neighbor);
//        }
        neighborsToChoke.addAll(neighborRates.keySet());
//        neighborsToChoke.forEach(this::choke);
        for (Neighbor neighbor : neighborsToChoke) {
            choke(neighbor);
        }

    }
}
