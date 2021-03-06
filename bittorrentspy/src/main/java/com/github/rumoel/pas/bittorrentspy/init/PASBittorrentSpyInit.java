package com.github.rumoel.pas.bittorrentspy.init;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Map;
import java.util.ServiceConfigurationError;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.rumoel.pas.bittorrentspy.config.PASBittorrentSpyConfig;
import com.github.rumoel.pas.bittorrentspy.header.PASBittorrentSpyHeader;
import com.github.rumoel.rumoel.libs.pas.torrents.ReportForTorrentPeer;
import com.turn.ttorrent.tracker.TrackedPeer;
import com.turn.ttorrent.tracker.TrackedTorrent;
import com.turn.ttorrent.tracker.Tracker;

public class PASBittorrentSpyInit extends Thread {

	Logger logger = LoggerFactory.getLogger(getClass());
	Tracker trackerHttp;

	public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
		PASBittorrentSpyHeader.getPasBittorrentSpyInit().readConfig();
		PASBittorrentSpyHeader.getPasBittorrentSpyInit().initTracker();
		PASBittorrentSpyHeader.getPasBittorrentSpyInit().readTorrents();
		PASBittorrentSpyHeader.getPasBittorrentSpyInit().startTracker();
	}

	@Override
	public void run() {
		do {
			if (trackerHttp != null) {
				Collection<TrackedTorrent> trackedTorrents = trackerHttp.getTrackedTorrents();
				for (TrackedTorrent trackedTorrent : trackedTorrents) {
					Map<String, TrackedPeer> trackedPeers = trackedTorrent.getPeers();
					for (Map.Entry<String, TrackedPeer> trackedPeerEntry : trackedPeers.entrySet()) {
						String id = trackedPeerEntry.getKey();
						String torrentHash = id;// .getTorrentHash();

						TrackedPeer trackedPeer = trackedPeerEntry.getValue();
						ReportForTorrentPeer report = new ReportForTorrentPeer();

						report.setTime(System.currentTimeMillis() / 1000);
						report.setTorrentHash(torrentHash);
						report.setPeerHash(trackedPeer.getHexPeerId());
						report.setIp(trackedPeer.getIp());
						report.setPort(trackedPeer.getPort());
						logger.info("{}", report);
					}
				}
			}
			try {
				sleep(300);
			} catch (Exception e) {
				logger.error(getName(), e);
			}
		} while (true);

	}

	private void startTracker() throws IOException {
		trackerHttp.start();
		start();
	}

	private void initTracker() throws IOException {
		// HTTP
		InetSocketAddress address = new InetSocketAddress(PASBittorrentSpyHeader.getConfig().getHttpTrackerPort());
		trackerHttp = new Tracker(address);
		// HTTP
		// DHT
		// DHT
	}

	private void readTorrents() throws IOException, NoSuchAlgorithmException {
		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".torrent");
			}
		};
		if (!PASBittorrentSpyHeader.getConfig().getTorrentsDir().exists()) {
			logger.info("dir {} is created:{}", PASBittorrentSpyHeader.getConfig().getTorrentsDir().getAbsolutePath(),
					PASBittorrentSpyHeader.getConfig().getTorrentsDir().mkdirs());
		}

		for (File f : PASBittorrentSpyHeader.getConfig().getTorrentsDir().listFiles(filter)) {
			logger.info("_____________________________________");
			trackerHttp.announce(TrackedTorrent.load(f));
			logger.info("torrent: {} is loaded", f.getName());
			logger.info("_____________________________________");
		}

	}

	private void readConfig() throws IOException {
		logger.info("readConfig-start");
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		if (!PASBittorrentSpyHeader.getCONFIGFILE().exists()) {
			if (!PASBittorrentSpyHeader.getCONFIGFILE().getParentFile().exists()) {
				logger.info("dir {} is created ?:{}",
						PASBittorrentSpyHeader.getCONFIGFILE().getParentFile().getAbsolutePath(),
						PASBittorrentSpyHeader.getCONFIGFILE().getParentFile().mkdirs());
			}
			if (PASBittorrentSpyHeader.getCONFIGFILE().createNewFile()) {
				mapper.writeValue(PASBittorrentSpyHeader.getCONFIGFILE(), PASBittorrentSpyHeader.getConfig());
				throw new ServiceConfigurationError(
						"please edit " + PASBittorrentSpyHeader.getCONFIGFILE().getAbsolutePath());
			}
		}
		PASBittorrentSpyHeader
				.setConfig(mapper.readValue(PASBittorrentSpyHeader.getCONFIGFILE(), PASBittorrentSpyConfig.class));
		if (!PASBittorrentSpyHeader.getConfig().isPrepare()) {
			throw new ServiceConfigurationError(
					"please edit " + PASBittorrentSpyHeader.getCONFIGFILE().getAbsolutePath());
		}
		logger.info("readConfig-end");

	}

}
