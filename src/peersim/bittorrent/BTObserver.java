package peersim.bittorrent;/*
 * Copyright (c) 2007-2008 Fabrizio Frioli, Michele Pedrolli
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version 2 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * --
 *
 * Please send your questions/suggestions to:
 * {fabrizio.frioli, michele.pedrolli} at studenti dot unitn dot it
 *
 */

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.util.IncrementalFreq;
import peersim.util.IncrementalStats;

import java.io.IOException;
import java.util.Collection;

/**
 * This {@link Control} provides a way to keep track of some
 * parameters of the peersim.BitTorrent network.
 */
public class BTObserver implements Control {

    /**
     * The protocol to operate on.
     *
     * @config
     */
    private static final String PAR_PROT = "protocol";

    /**
     * Protocol identifier, obtained from config property
     */
    private final int pid;

    /**
     * The basic constructor that reads the configuration file.
     *
     * @param prefix the configuration prefix for this class
     */
    public BTObserver(String prefix) {
        pid = Configuration.getPid(prefix + "." + PAR_PROT);
    }

    /**
     * Prints information about the peersim.BitTorrent network
     * and the number of leechers and seeders.
     * Please refer to the code comments for more details.
     *
     * @return always false
     */
    public boolean execute() {
        IncrementalFreq nodeStatusStats = new IncrementalFreq();
        IncrementalStats neighborStats = new IncrementalStats();

        int numberOfNodes = Network.size();
        int numberOfCompletedPieces;

        // cycles from 1, since the node 0 is the tracker
        for (int i = 1; i < numberOfNodes; ++i) {

            BitTorrent nodeProtocol =((BitTorrent) (Network.get(i).getProtocol(pid)));

            // stats on number of leechers and seeders in the network
            // and consequently also on number of completed files in the network
            nodeStatusStats.add(nodeProtocol.getPeerStatus());

            // stats on number of neighbors per peer
            neighborStats.add(nodeProtocol.getNNodes());
        }

        // number of the pieces of the file, equal for every node, here 1 is chosen,
        // since 1 is the first "normal" node (0 is the tracker)
        int numberOfPieces = ((BitTorrent) (Network.get(1).getProtocol(pid))).nPieces;

        for (int i = 1; i < numberOfNodes; ++i) {
            numberOfCompletedPieces = 0;

            BitTorrent nodeProtocol = ((BitTorrent) (Network.get(i).getProtocol(pid)));

            // discovers the status of the current peer (leecher or seeder)
            int ps = nodeProtocol.getPeerStatus();
            String peerStatus;
            if (ps == 0) {
                peerStatus = "L"; //leecher
            } else {
                peerStatus = "S"; //seeder
            }


            if (Network.get(i) != null) {

                // counts the number of completed pieces for the i-th node
                for (int j = 0; j < numberOfPieces; j++) {
                    int fileStatus = nodeProtocol.getFileStatus()[j];
                    if (fileStatus == 16) {
                        numberOfCompletedPieces++;
                    }
                }

				/*
                 * Put here the output lines of the Observer. An example is provided with basic
                 * information and stats. CommonState.getTime() is used to print out time
                 * references (useful for graph plotting).
				 */

                Collection<Integer> valuesDown = nodeProtocol.nPiecesDown2.values();
                long nPiecesDown = valuesDown.stream().mapToInt(Integer::intValue).sum();

                Collection<Integer> valuesUp = nodeProtocol.nPiecesUp2.values();
                long nPiecesUp = valuesUp.stream().mapToInt(Integer::intValue).sum();

                if (nodeProtocol.nPiecesDown != nPiecesDown || nodeProtocol.nPiecesUp != nPiecesUp) {
                    System.out.println("OBS: node " + nodeProtocol.getThisNodeID() + "(" +
                            peerStatus + ")\t pieces completed: " + numberOfCompletedPieces + "\t" +
                            " \t down: " + nodeProtocol.nPiecesDown + "\t up: " +
                            nodeProtocol.nPiecesUp + " time: " + CommonState.getTime());
                    System.out.println("OBS: node " + nodeProtocol.getThisNodeID() + "(" +
                            peerStatus + ")\t pieces completed: " + numberOfCompletedPieces + "\t" +
                            " \t down: " + nPiecesDown + "\t up: " + nPiecesUp + " time: " +
                            CommonState.getTime());
                }

                BitNode node = ((BitNode) (Network.get(i)));

                //                    node.printResumedInteractions(DOWNLOAD);
                //                System.out.println("----------------------------");
                //                    node.printResumedInteractions(UPLOAD);
                ////                }

                //                System.out.println("Reputations");

                //                for (Neighbor neighbor : ((BitTorrent) (Network.get(i)
                // .getProtocol(pid)))
                //                        .getCache()) {
                //                    if (neighbor != null && neighbor.node != null && (
                // (BitTorrent) (Network.get
                //                            (i).getProtocol(pid))).alive(neighbor.node)) {
                ////
                //                        node.getPercentages(neighbor.node.getID());
                //
                //                    }
                //                }

                try {
                    node.file_requests.flush();
                    node.file_blocks.flush();
                    node.messagesFile.flush();
                    node.reputationFile.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else {
                //System.out.println("[OBS] t " + CommonState.getTime() + "\t
                // pc " + "0" + "\t n " + "0");
            }

        }

        // prints the frequency of 0 (leechers) and 1 (seeders)
        nodeStatusStats.printAll(System.out);

        // prints the average number of neighbors per peer
        System.out.println("Avg number of neighbors per peer: " + neighborStats.getAverage());

        if (nodeStatusStats.getFreq(0) == 0) {
            System.exit(0);
        }
        return false;
    }
}