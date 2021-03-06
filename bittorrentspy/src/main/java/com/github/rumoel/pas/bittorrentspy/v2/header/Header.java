package com.github.rumoel.pas.bittorrentspy.v2.header;

import java.io.File;
import java.util.concurrent.CopyOnWriteArrayList;

import com.github.rumoel.pas.bittorrentspy.config.PASBittorrentSpyConfig;
import com.github.rumoel.pas.bittorrentspy.v2.object.PASBit;
import com.github.rumoel.pas.bittorrentspy.v2.trackers.TrackerHandler;

import bt.magnet.MagnetUri;
import lombok.Getter;
import lombok.Setter;

public final class Header {
	private Header() {
	}

	@Getter
	private static final PASBit pasBit = new PASBit();

	public static final TrackerHandler trackerHandler = new TrackerHandler();

	@Getter
	private static final CopyOnWriteArrayList<MagnetUri> magnetUris = new CopyOnWriteArrayList<>();
	@Getter
	public static final File ROOTDIR = new File(new File(new File("rumoel"), "pas"), "bittorrentspy");

	@Getter
	private static final File configFile = new File(ROOTDIR, "config.yml");

	@Getter
	@Setter
	private static PASBittorrentSpyConfig config = new PASBittorrentSpyConfig();

}
