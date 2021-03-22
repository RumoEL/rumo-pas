package com.github.rumoel.pas.bittorrentspy.v3.trackers.impl;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.rumoel.pas.bittorrentspy.dht.MagnetLinkFileReader;
import com.github.rumoel.pas.bittorrentspy.dht.PeerStats;
import com.github.rumoel.pas.bittorrentspy.v3.dumper.StatsDumper;
import com.github.rumoel.pas.bittorrentspy.v3.header.Header;
import com.github.rumoel.pas.bittorrentspy.v3.trackers.TrackerObj;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.inject.Module;

import bt.Bt;
import bt.data.Storage;
import bt.data.file.FileSystemStorage;
import bt.dht.DHTConfig;
import bt.dht.DHTModule;
import bt.magnet.MagnetUri;
import bt.metainfo.TorrentId;
import bt.runtime.BtClient;
import bt.runtime.BtRuntime;
import bt.runtime.Config;
import lombok.Getter;
import lombok.Setter;

public class TrackerDHT extends TrackerObj {

	private static final FileSystem FS = Jimfs.newFileSystem(Configuration.unix());
	private static final BtRuntime RUNTIME = createRuntime();
	private static final Map<TorrentId, PeerStats> STATS = new ConcurrentHashMap<>();
	private static final ScheduledExecutorService STATS_WRITER = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread t = new Thread(r);
		t.setDaemon(true);
		return t;
	});

	StatsDumper dumper = new StatsDumper();

	public TrackerDHT() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				STATS_WRITER.shutdownNow();
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				FS.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}));
	}

	CopyOnWriteArrayList<MagnetUri> magnets = new CopyOnWriteArrayList<>();
	CopyOnWriteArrayList<BtClient> clients = new CopyOnWriteArrayList<>();

	@Override
	public void init() {
		Collection<MagnetUri> magnets2 = new MagnetLinkFileReader()
				.readFromFile(Header.getConfig().getTorrentsMagnets());
		magnets.addAll(magnets2);
		for (MagnetUri magnetUri : magnets) {
			getLogger().info("Creating client for info hash: {}", magnetUri.getTorrentId());
			attachPeerListener(RUNTIME, magnetUri.getTorrentId());
			clients.add(createClient(RUNTIME, magnetUri));
		}
	}

	@Getter
	@Setter
	boolean started;

	@Override
	public boolean startTr() {
		try {
			List<CompletableFuture<?>> futures = clients.stream().map(BtClient::startAsync)
					.collect(Collectors.toList());
			CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).join();
			setStarted(true);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public void dump() {
		dumper.dumpStats(STATS);
	}

	// BT
	private static BtRuntime createRuntime() {
		Config config = new Config() {
			final int MAX_PEER_CONNECTIONS = 5000;

			@Override
			public void setMaxConcurrentlyActivePeerConnectionsPerTorrent(
					int maxConcurrentlyActivePeerConnectionsPerTorrent) {
				throw new IllegalStateException("prevent downloading anything (tracker only) you value: "
						+ maxConcurrentlyActivePeerConnectionsPerTorrent);
			}

			@Override
			public int getMaxConcurrentlyActivePeerConnectionsPerTorrent() {
				return 0; // NC!!! prevent downloading anything
			}

			@Override
			public int getMaxPeerConnections() {
				return MAX_PEER_CONNECTIONS;
			}

			@Override
			public int getMaxPeerConnectionsPerTorrent() {
				return MAX_PEER_CONNECTIONS;
			}
		};

		Module dhtModule = new DHTModule(new DHTConfig() {
			@Override
			public boolean shouldUseRouterBootstrap() {
				return true;
			}
		});

		return BtRuntime.builder(config).autoLoadModules().module(dhtModule).build();
	}

	private static void attachPeerListener(BtRuntime runtime, TorrentId torrentId) {

		PeerStats perTorrentStats = STATS.computeIfAbsent(torrentId, new Function<TorrentId, PeerStats>() {
			@Override
			public PeerStats apply(TorrentId it) {
				return new PeerStats();
			}
		});

		runtime.getEventSource().onPeerDiscovered(perTorrentStats::onPeerDiscovered)
				.onPeerConnected(perTorrentStats::onPeerConnected)
				.onPeerDisconnected(perTorrentStats::onPeerDisconnected)
				.onPeerBitfieldUpdated(perTorrentStats::onPeerBitfieldUpdated);
	}

	private static BtClient createClient(BtRuntime runtime, MagnetUri magnetUri) {
		Path path = FS.getPath("FS/" + magnetUri.getTorrentId());
		Storage storage = new FileSystemStorage(path);
		return Bt.client(runtime).magnet(magnetUri).storage(storage).initEagerly().build();
	}
	// BT
}
