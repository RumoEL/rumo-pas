package com.github.rumoel.pas.bittorrentspy.header;

import java.io.File;

import com.github.rumoel.pas.bittorrentspy.config.PASBittorrentSpyConfig;
import com.github.rumoel.pas.bittorrentspy.init.PASBittorrentSpyInit;

import lombok.Getter;
import lombok.Setter;

public class PASBittorrentSpyHeader {
	private PASBittorrentSpyHeader() {
	}

	@Getter
	@Setter
	private static PASBittorrentSpyInit pasBittorrentSpyInit = new PASBittorrentSpyInit();

	@Getter
	public static final File ROOTDIR = new File(new File(new File("rumoel"), "pas"), "bittorrentspy");

	@Getter
	public static final File CONFIGFILE = new File(ROOTDIR, "config.yml");

	@Getter
	@Setter
	private static PASBittorrentSpyConfig config = new PASBittorrentSpyConfig();
}
