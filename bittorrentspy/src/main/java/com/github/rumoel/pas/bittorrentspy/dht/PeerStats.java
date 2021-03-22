/*
 * Copyright (c) 2016—2017 Andrei Tomashpolskiy and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.rumoel.pas.bittorrentspy.dht;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.github.rumoel.rumoel.libs.pas.torrents.peer.PeerCounter;

import bt.event.PeerBitfieldUpdatedEvent;
import bt.event.PeerConnectedEvent;
import bt.event.PeerDisconnectedEvent;
import bt.event.PeerDiscoveredEvent;
import bt.net.Peer;
import lombok.Getter;

public class PeerStats {

	@Getter
	private final Map<Peer, PeerCounter> counters = new ConcurrentHashMap<>(5000);

	public void onPeerDiscovered(PeerDiscoveredEvent event) {
		getCounter(event.getPeer()).incrementDiscovered();
	}

	public void onPeerConnected(PeerConnectedEvent event) {
		getCounter(event.getPeer()).incrementConnected();
	}

	public void onPeerDisconnected(PeerDisconnectedEvent event) {
		getCounter(event.getPeer()).incrementDisconnected();
	}

	public void onPeerBitfieldUpdated(PeerBitfieldUpdatedEvent event) {
		PeerCounter counter = getCounter(event.getPeer());
		counter.setPiecesCompleted(event.getBitfield().getPiecesComplete());
		counter.setPiecesRemaining(event.getBitfield().getPiecesRemaining());
	}

	private PeerCounter getCounter(Peer peer) {
		synchronized (counters) {
			PeerCounter counter;
			if (counters.isEmpty()) {
				counter = new PeerCounter();
				counters.putIfAbsent(peer, counter);
				return counter;
			} else {
				if (peerIFCounted(counters, peer)) {
					return counters.get(getPeerByPeerInMap(counters, peer));
				} else {
					counter = new PeerCounter();
					counters.putIfAbsent(peer, counter);
					return counter;
				}
			}
		}
	}

	public static Peer getPeerByPeerInMap(Map<Peer, PeerCounter> map, Peer peer) {
		for (Map.Entry<Peer, PeerCounter> mapEntry : map.entrySet()) {
			Peer peerForRet = mapEntry.getKey();
			if (peerIsPeer(peerForRet, peer)) {
				return peerForRet;
			}
		}
		return null;
	}

	public static boolean peerIFCounted(Map<Peer, PeerCounter> map, Peer peerForCheck) {
		for (Map.Entry<Peer, PeerCounter> mapEntry : map.entrySet()) {
			Peer mapPeer = mapEntry.getKey();
			if (peerIsPeer(peerForCheck, mapPeer)) {
				return true;
			}
		}
		return false;
	}

	public static boolean peerIsPeer(Peer peer1, Peer peer2) {
		String peer2Host = peer2.getInetAddress().getHostAddress();
		int peer2Port = peer2.getPort();
		String peer1Host = peer1.getInetAddress().getHostAddress();
		int peer1Port = peer1.getPort();

		if (peer2Host.equalsIgnoreCase(peer1Host) && peer2Port == peer1Port) {
			return true;
		}
		return false;
	}

}