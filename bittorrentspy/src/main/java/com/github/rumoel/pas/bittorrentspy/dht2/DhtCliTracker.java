package com.github.rumoel.pas.bittorrentspy.dht2;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.rumoel.pas.bittorrentspy.dht.MagnetLinkFileReader;
import com.github.rumoel.pas.bittorrentspy.dht.PeerStats;
import com.github.rumoel.pas.bittorrentspy.dht.StatsDumper;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

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

public class DhtCliTracker extends Thread {

	private static final Logger LOGGER = LoggerFactory.getLogger(DhtCliTracker.class);

	private static FileSystem FS;
	private static BtRuntime RUNTIME;
	private static Map<TorrentId, PeerStats> STATS;
	private static ScheduledExecutorService STATS_WRITER;

	private static final long STATS_DUMP_INTERVAL_SECONDS = 15;

	Object dumper2;

	CopyOnWriteArrayList<BtClient> btClients = new CopyOnWriteArrayList<>();

	public void init() {
		FS = Jimfs.newFileSystem(Configuration.unix());
		RUNTIME = createRuntime();
		STATS = new ConcurrentHashMap<>();
		STATS_WRITER = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread t = new Thread(r);
			t.setDaemon(true);
			return t;
		});
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

		Collection<MagnetUri> magnets = new MagnetLinkFileReader().readFromFile("FILE");
		for (MagnetUri magnetUri : magnets) {
			LOGGER.info("Creating client for info hash: {}", magnetUri.getTorrentId());
			attachPeerListener(RUNTIME, magnetUri.getTorrentId());
			btClients.add(createClient(RUNTIME, magnetUri));
		}

		StatsDumper dumper = new StatsDumper(System.currentTimeMillis());
		LOGGER.info("Scheduling stats dump every {} seconds...", STATS_DUMP_INTERVAL_SECONDS);
		STATS_WRITER.scheduleWithFixedDelay(() -> dumper.dumpStats(STATS), STATS_DUMP_INTERVAL_SECONDS,
				STATS_DUMP_INTERVAL_SECONDS, TimeUnit.SECONDS);

	}

	@Override
	public void run() {
		LOGGER.info("Starting clients...");
		CopyOnWriteArrayList<CompletableFuture<?>> futures = new CopyOnWriteArrayList<>();

		for (BtClient btClient : btClients) {
			futures.add(btClient.startAsync());
		}

		CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).join();
	}

	public void startDumper() {
		// FIXME
		((Thread) dumper2).start();
	}

	//
	private static BtClient createClient(BtRuntime runtime, MagnetUri magnetUri) {
		Storage storage = new FileSystemStorage(FS.getPath("/" + UUID.randomUUID()));
		return Bt.client(runtime).magnet(magnetUri).storage(storage).initEagerly().build();
	}

	private static void attachPeerListener(BtRuntime runtime, TorrentId torrentId) {
		PeerStats perTorrentStats = STATS.computeIfAbsent(torrentId, it -> new PeerStats());
		runtime.getEventSource().onPeerDiscovered(perTorrentStats::onPeerDiscovered)
				.onPeerConnected(perTorrentStats::onPeerConnected)
				.onPeerDisconnected(perTorrentStats::onPeerDisconnected)
				.onPeerBitfieldUpdated(perTorrentStats::onPeerBitfieldUpdated);
	}

	private static BtRuntime createRuntime() {
		final int MAX_PEER_CONNECTIONS = 5000;

		Config config2 = new Config();
		config2.setMaxConcurrentlyActivePeerConnectionsPerTorrent(0);

		Config config = new Config() {

			@Override
			public int getMaxPeerConnections() {
				return MAX_PEER_CONNECTIONS;
			}

			@Override
			public int getMaxPeerConnectionsPerTorrent() {
				return MAX_PEER_CONNECTIONS;
			}
		};

		DHTModule dhtModule = new DHTModule(new DHTConfig() {
			@Override
			public boolean shouldUseRouterBootstrap() {
				return true;
			}
		});

		return BtRuntime.builder(config).autoLoadModules().module(dhtModule).build();
	}
}
