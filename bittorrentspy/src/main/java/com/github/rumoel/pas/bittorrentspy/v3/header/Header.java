package com.github.rumoel.pas.bittorrentspy.v3.header;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.web.client.RestTemplate;

import com.github.rumoel.pas.bittorrentspy.config.PASBTSPConfig;
import com.github.rumoel.pas.bittorrentspy.v3.trackers.TrackerHandler;

import bt.magnet.MagnetUri;
import lombok.Getter;
import lombok.Setter;

public final class Header {
	private Header() {
	}

	@Getter
	@Setter
	static Socket debugSocket;

	public static final TrackerHandler trackerHandler = new TrackerHandler();

	@Getter
	private static final CopyOnWriteArrayList<MagnetUri> magnetUris = new CopyOnWriteArrayList<>();
	@Getter
	public static final File ROOTDIR = new File(new File(new File("rumoel"), "pas"), "bittorrentspy");

	@Getter
	private static final File configFile = new File(ROOTDIR, "config.yml");

	@Getter
	@Setter
	private static PASBTSPConfig config = new PASBTSPConfig();

	@Getter
	@Setter
	private static CopyOnWriteArrayList<String> linkForMyIp = new CopyOnWriteArrayList<>();

	@Getter
	@Setter
	private static ScheduledExecutorService executorServiceGetterMyIp = Executors.newSingleThreadScheduledExecutor();
	@Getter
	@Setter
	private static CopyOnWriteArrayList<String> myIp = new CopyOnWriteArrayList<>();
	static {
		if (linkForMyIp.isEmpty()) {
			linkForMyIp.add("http://ident.me");
		}
		Runnable runnable = () -> {
			for (String string : linkForMyIp) {
				try {
					String ip = getDataFromUrl(string);
					if (ip != null) {
						if (myIp.isEmpty()) {
							myIp.add(ip);
						} else {
							if (!myIp.contains(ip)) {
								myIp.add(ip);
							}
						}
					}
				} catch (Exception e) {
					// IGNORE
				}
			}
		};
		executorServiceGetterMyIp.scheduleWithFixedDelay(runnable, 0, 10, TimeUnit.SECONDS);
	}

	private static String getDataFromUrl(String string) throws IOException {
		URL oracle = new URL(string);
		URLConnection yc = oracle.openConnection();
		try (BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()))) {
			return in.readLine();
		}
	}

	@Getter
	public static RestTemplate restTemplate3 = new RestTemplate();
}
