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

package com.github.rumoel.pas.bittorrentspy.v3.dumper;

import java.util.Map;

import com.github.rumoel.pas.bittorrentspy.dht.PeerStats;
import com.github.rumoel.pas.bittorrentspy.v3.header.Header;
import com.github.rumoel.pas.bittorrentspy.v3.spring.BitSpySpringClient;
import com.github.rumoel.rumoel.libs.pas.torrents.peer.PeerCounter;
import com.github.rumoel.rumoel.libs.pas.torrents.peer.PeerInfo;
import com.github.rumoel.rumoel.libs.pas.torrents.report.ReportForTorrentPeer;
import com.github.rumoel.rumoel.libs.pas.torrents.torrent.TorrentInfo;

import bt.metainfo.TorrentId;
import bt.net.Peer;

public class StatsDumper {

	public void dumpStats(Map<TorrentId, PeerStats> aggregateStats) {
		try {
			// torrents
			for (Map.Entry<TorrentId, PeerStats> e : aggregateStats.entrySet()) {
				for (String ip : Header.getMyIp()) {
					Header.getConfig().getReporterInfo().getTrackerIp().addIfAbsent(ip);
				}
				ReportForTorrentPeer report = new ReportForTorrentPeer();
				TorrentInfo reportTorentInfo = new TorrentInfo();
				report.setReporter(Header.getConfig().getReporterInfo());

				TorrentId torrentId = e.getKey();
				PeerStats stats = e.getValue();

				reportTorentInfo.setHash(torrentId.toString());// SET TO REPORT

				// peer list with meta
				Map<Peer, PeerCounter> peerCounters = stats.getCounters();
				for (Map.Entry<Peer, PeerCounter> e2 : peerCounters.entrySet()) {
					Peer peer = e2.getKey();

					String testHost = peer.getInetAddress().getHostAddress();
					int testPort = peer.getPort();
					if (!itsMe(testHost)) {
						PeerInfo reportPeerInfo = new PeerInfo();

						reportPeerInfo.setTorrent(reportTorentInfo);

						reportPeerInfo.setHost(testHost);
						reportPeerInfo.setPort(testPort);

						PeerCounter counter = e2.getValue();
						reportPeerInfo.getPeerCounters().add(counter);

						ReportForTorrentPeer.correctAddtoPeerList(report.getPeerInfo(), reportPeerInfo);
					}
				}

				new Thread(() -> {
					try {
						BitSpySpringClient.sendResult(report);
						if (!report.getPeerInfo().isEmpty()) {
						}
					} catch (Exception e2) {
					}
				}, "reports sender").start();
			}

		} catch (Exception e) {
		}
	}

	private boolean itsMe(String testHost) {
		String testHostLc = testHost.toLowerCase();
		for (String myIp : Header.getMyIp()) {
			if (myIp.equalsIgnoreCase(testHostLc)) {
				return true;
			}
		}
		return false;
	}
}