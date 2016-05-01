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
import peersim.core.Node;
import peersim.edsim.EDSimulator;
import peersim.transport.Transport;
import peersim.util.IncrementalFreq;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

/**
 * This {@link Control} ...
 */
public class NetworkInitializer implements Control {
	/**
	* The protocol to operate on.
	*
	* @config
	*/
	private static final String PAR_PROT="protocol";

	private static final String PAR_TRANSPORT="transport";

	private static final String PAR_N_BAD_CHUNK="nBadChunk";

	private static final String PAR_N_SLOW="nSlow";

	private static final String PAR_N_FREE_RIDER="nFreeRider";

	private static final int TRACKER = 11;

	private static final int CHOKE_TIME = 13;

	private static final int OPTUNCHK_TIME = 14;

	private static final int ANTISNUB_TIME = 15;

	private static final int CHECKALIVE_TIME = 16;

	private static final int TRACKERALIVE_TIME = 17;

	/** Protocol identifier, obtained from config property */
	private final int pid;
	private final int tid;
	private NodeInitializer init;

	private final int nBadChunk;
	private final int nSlow;
	private static int nFreeRider;

	private Random rnd;

	public NetworkInitializer(String prefix) {
		System.out.println("net init");
		pid = Configuration.getPid(prefix+"."+PAR_PROT);
		tid = Configuration.getPid(prefix+"."+PAR_TRANSPORT);
		init = new NodeInitializer(prefix);

		nBadChunk = Configuration.getInt(prefix + "." + PAR_N_BAD_CHUNK);
		nSlow = Configuration.getInt(prefix + "." + PAR_N_SLOW);
//		nFreeRider = Configuration.getInt(prefix + "." + PAR_N_FREE_RIDER);
		nFreeRider = ((BitNode) Network.get(1)).getnFreeRider();
	}

	public boolean execute() {
		int completed;
		Node tracker = Network.get(0);

		// manca l'inizializzazione del tracker;

		((BitTorrent)Network.get(0).getProtocol(pid)).initializeTracker();


		for(int i=1; i<Network.size(); i++){
			System.err.println("chiamate ad addNeighbor " + i);
			((BitTorrent)Network.get(0).getProtocol(pid)).addNeighbor(
					(BitNode) Network.get(i));
			init.initialize(Network.get(i));
		}

		int badChunkLeft = nBadChunk;
		while (badChunkLeft > 0) {
			int random = CommonState.r.nextInt(29) + 1;
			BitNode node =((BitNode) Network.get(random));
			if (node.isNormal()) {
				node.setBadChunk();
				badChunkLeft--;
			}
		}

		//TODO timeout did not work
		int slowLeft = nSlow;
		while (slowLeft > 0) {
			int random = CommonState.r.nextInt(29)+1;
			BitNode node =((BitNode) Network.get(random));
			if (node.isNormal()) {
				node.setSlow();
				slowLeft--;
			}
		}

		int nSeeders = getPeerStatus().getFreq(1);

        int freeRidersLeft = Math.min(nFreeRider, Network.size() - 1 - nSeeders);

        if (nFreeRider > Network.size() - nSeeders - 1) {
            System.err.println("Number of FreeRiders settled is more than the nodes available," +
                    " only " + freeRidersLeft + " nodes were configured as " + "FreeRiders");
        }
        while (freeRidersLeft > 0) {
			int random = CommonState.r.nextInt(Network.size()-1)+1;
			BitNode node =((BitNode) Network.get(random));
			BitTorrent bitTorrent = (BitTorrent) node.getProtocol(pid);
			if (node.isNormal() && bitTorrent.getPeerStatus() == 0) {
				node.setFreeRider();
				freeRidersLeft--;
			}
		}

		String unchokingAlgorithm = String.valueOf(((BitTorrent) Network.get(0).getProtocol(pid))
				.getUnchokingAlgorithm());
		String seed = String.valueOf(CommonState.r.getLastSeed());

		Path file_path = Paths.get("log", String.valueOf(Network.size()), unchokingAlgorithm,
				String.valueOf(getnFreeRider()), seed, "NodeTypes.csv");
		System.out.println(file_path + " it should be this");

		Path folder_path = Paths.get("log", Integer.toString(Network.size()), unchokingAlgorithm, Integer.toString
				(NetworkInitializer.getnFreeRider()), String.valueOf(CommonState.r.getLastSeed()));

//		Path folder_path = Paths.get("log", Integer.toString(Network.size()), ((BitTorrent)
//				getProtocol(pid)).getUnchokingAlgorithm().toString(), Integer.toString
//				(NetworkInitializer.getnFreeRider()), String.valueOf(CommonState.r.getLastSeed()));

		try {
			Files.createDirectories(folder_path);
		} catch (IOException e) {
			e.printStackTrace();
		}

//		result.file_interaction = new FileWriter(folder_path + File.separator +
//				"interactions_" + result.getID() + ".csv");
		String file_path2 = folder_path + File.separator + "NodeTypes.csv";
		String file_path3 = folder_path + File.separator + "DownTimes.csv";
		File dir = new File(folder_path.toString());
		boolean exist = dir.exists();
		File file = new File(file_path2);
		File file2 = new File(file_path3);
		try {
			file.createNewFile();
			file2.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try (FileWriter fileWriter = new FileWriter(file_path2); FileWriter fileWriter1 = new
				FileWriter(file_path3)
		) {
            fileWriter.write("NodeID;NodeType\n");
            for (int i = 1; i < Network.size(); i++) {
                BitNode n = (BitNode) Network.get(i);
                long latency = ((Transport) n.getProtocol(tid)).getLatency(n, tracker);
                Object ev = new SimpleMsg(TRACKER, n);
                EDSimulator.add(latency, ev, tracker, pid);
                ev = new SimpleEvent(CHOKE_TIME);
                EDSimulator.add(10000, ev, n, pid);
                ev = new SimpleEvent(OPTUNCHK_TIME);
                EDSimulator.add(30000, ev, n, pid);
                ev = new SimpleEvent(ANTISNUB_TIME);
                EDSimulator.add(60000, ev, n, pid);
                ev = new SimpleEvent(CHECKALIVE_TIME);
                EDSimulator.add(120000, ev, n, pid);
                ev = new SimpleEvent(TRACKERALIVE_TIME);
                EDSimulator.add(1800000, ev, n, pid);

                System.out.println("Node " + n.getID() + " is " + n.getBehaviour());
                fileWriter.write(n.getID() + ";" + n.getBehaviour()+ "\n");
            }
			fileWriter1.write("NodeID;NodeType;DownloadTime\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
		return true;
	}

	/**
	 * Number of leechers and seeders on network
	 */
	IncrementalFreq getPeerStatus() {
		IncrementalFreq nodeStatusStats = new IncrementalFreq();

		// cycles from 1, since the node 0 is the tracker
		for (int i = 1; i < Network.size(); ++i) {

			// stats on number of leechers and seeders in the network
			// and consequently also on number of completed files in the network
			nodeStatusStats.add(((BitTorrent) (Network.get(i).getProtocol
					(pid))).getPeerStatus());

		}

		return nodeStatusStats;
	}


	public int getnBadChunk() {
		return nBadChunk;
	}

	public int getnSlow() {
		return nSlow;
	}

	public static int getnFreeRider() {
		return nFreeRider;
	}
	}
