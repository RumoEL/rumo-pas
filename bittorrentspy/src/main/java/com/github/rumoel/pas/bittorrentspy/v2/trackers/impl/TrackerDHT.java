package com.github.rumoel.pas.bittorrentspy.v2.trackers.impl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.FileSystem;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.github.rumoel.pas.bittorrentspy.dht.MagnetLinkFileReader;
import com.github.rumoel.pas.bittorrentspy.dht.PeerStats;
import com.github.rumoel.pas.bittorrentspy.dht.StatsDumper;
import com.github.rumoel.pas.bittorrentspy.v2.header.Header;
import com.github.rumoel.pas.bittorrentspy.v2.trackers.TrackerObj;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

import bt.Bt;
import bt.data.Storage;
import bt.data.file.FileSystemStorage;
import bt.dht.DHTConfig;
import bt.dht.DHTModule;
import bt.magnet.MagnetUri;
import bt.metainfo.TorrentId;
import bt.net.InetPeerAddress;
import bt.runtime.BtClient;
import bt.runtime.BtRuntime;
import bt.runtime.Config;

public class TrackerDHT extends TrackerObj {
	private static final FileSystem FS = Jimfs.newFileSystem(Configuration.unix());
	private static final BtRuntime RUNTIME = createRuntime();
	private final ConcurrentHashMap<TorrentId, PeerStats> STATS = new ConcurrentHashMap<>();

	CopyOnWriteArrayList<BtClient> btClients = new CopyOnWriteArrayList<>();
	StatsDumper dumper = new StatsDumper(System.currentTimeMillis());

	@Override
	public void init() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				FS.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}));

		readTorrents();
		readMagnets();

	}

	private void readMagnets() {
		try {
			if (!Header.getConfig().getTorrentsMagnets().exists()) {
				Header.getConfig().getTorrentsMagnets().getParentFile().mkdirs();
			}
			getLogger().info("magnets file[{}] is created:{}",
					Header.getConfig().getTorrentsMagnets().getAbsolutePath(),
					Header.getConfig().getTorrentsMagnets().createNewFile());

			Header.getMagnetUris().addAll(
					new MagnetLinkFileReader().readFromFile(Header.getConfig().getTorrentsMagnets().getAbsolutePath()));
		} catch (Exception e2) {
			getLogger().error("read", e2);
		}
		for (MagnetUri magnetUri : Header.getMagnetUris()) {
			getLogger().info("Creating client for info hash: {}", magnetUri.getTorrentId());
			attachPeerListener(RUNTIME, magnetUri.getTorrentId());
			btClients.add(createClient(RUNTIME, magnetUri));
		}
	}

	private void readTorrents() {
		if (!Header.getConfig().getTorrentsDir().exists()) {
			getLogger().info("torrent dir {} is created:{}", Header.getConfig().getTorrentsDir(),
					Header.getConfig().getTorrentsDir().mkdirs());
		}

	}

	@Override
	public void dump() {
		dumper.dumpStats(STATS);
		STATS.clear();
	}

	@Override
	public boolean startTr() {
		getLogger().info("Starting clients...");
		CopyOnWriteArrayList<CompletableFuture<?>> futures = new CopyOnWriteArrayList<>();

		for (BtClient btClient : btClients) {
			futures.add(btClient.startAsync());
		}

		CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).join();

		return true;
	}

	private void attachPeerListener(BtRuntime runtime, TorrentId torrentId) {
		PeerStats perTorrentStats = STATS.computeIfAbsent(torrentId, it -> new PeerStats());
		runtime.getEventSource().onPeerDiscovered(perTorrentStats::onPeerDiscovered)
				.onPeerConnected(perTorrentStats::onPeerConnected)
				.onPeerDisconnected(perTorrentStats::onPeerDisconnected)
				.onPeerBitfieldUpdated(perTorrentStats::onPeerBitfieldUpdated);
	}

	private static BtClient createClient(BtRuntime runtime, MagnetUri magnetUri) {
		Storage storage = new FileSystemStorage(FS.getPath("/" + UUID.randomUUID()));
		return Bt.client(runtime).magnet(magnetUri).storage(storage).initEagerly().build();
	}

	private static BtRuntime createRuntime() {

		Config mainConfig = new Config() {

			@Override
			public int getAcceptorPort() {
				return Header.getConfig().getUdpTcpTrackerPort();
			}

			@Override
			public int getMaxPeerConnections() {
				return Header.getConfig().getMaxPeerConnections();
			}

			@Override
			public int getMaxPeerConnectionsPerTorrent() {
				return Header.getConfig().getMaxPeerConnections();
			}
		};

		CopyOnWriteArrayList<InetPeerAddress> bootstrapNodes = castTypes(Header.getConfig().getBootstrapNodes());

		DHTConfig dhtConfig = new DHTConfig() {
			@Override
			public Collection<InetPeerAddress> getBootstrapNodes() {
				return bootstrapNodes;
			}

			@Override
			public int getListeningPort() {
				return Header.getConfig().getDhtTrackerPort();
			}

			@Override
			public boolean shouldUseRouterBootstrap() {
				return true;
			}
		};

		DHTModule dhtModule = new DHTModule(dhtConfig);

		return BtRuntime.builder(mainConfig).autoLoadModules().module(dhtModule).build();
	}

	private static CopyOnWriteArrayList<InetPeerAddress> castTypes(CopyOnWriteArrayList<InetSocketAddress> input) {
		CopyOnWriteArrayList<InetPeerAddress> ret = new CopyOnWriteArrayList<>();
		if (input == null) {
			return ret;
		}
		for (InetSocketAddress inetSocketAddress : input) {
			ret.add(new InetPeerAddress(inetSocketAddress.getHostString(), inetSocketAddress.getPort()));
		}
		return ret;
	}

}
