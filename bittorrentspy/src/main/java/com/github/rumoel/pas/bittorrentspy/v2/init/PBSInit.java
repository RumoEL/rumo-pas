package com.github.rumoel.pas.bittorrentspy.v2.init;

import java.io.IOException;
import java.util.ServiceConfigurationError;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.rumoel.pas.bittorrentspy.config.PASBittorrentSpyConfig;
import com.github.rumoel.pas.bittorrentspy.v2.header.Header;
import com.github.rumoel.pas.bittorrentspy.v2.trackers.impl.TrackerDHT;
import com.github.rumoel.pas.bittorrentspy.v2.trackers.impl.TrackerHttp;
import com.github.rumoel.pas.bittorrentspy.v2.trackers.impl.TrackerUDP;

public class PBSInit {
	static Logger logger = LoggerFactory.getLogger(PBSInit.class);

	public static void main(String[] args) throws IOException {
		readConfig();

		Header.getPasBit().initNetwork();
		Header.getPasBit().connect();

		TrackerHttp trackerHttp = new TrackerHttp();
		TrackerUDP trackerUDP = new TrackerUDP();
		TrackerDHT trackerDHT = new TrackerDHT();

		Header.trackerHandler.add(trackerHttp);
		Header.trackerHandler.add(trackerUDP);
		Header.trackerHandler.add(trackerDHT);

		Header.trackerHandler.init();
		Header.trackerHandler.start();
	}

	private static void readConfig() throws IOException {
		logger.info("initConfig-start");
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		// CHECK and create

		if (!Header.getConfigFile().getParentFile().exists()) {
			Header.getConfigFile().getParentFile().mkdirs();
		}
		if (!Header.getConfigFile().exists()) {
			logger.info("File {} is created:{}", Header.getConfigFile().getAbsolutePath(),
					Header.getConfigFile().createNewFile());

			mapper.writeValue(Header.getConfigFile(), Header.getConfig());
			throw new ServiceConfigurationError("please edit " + Header.getConfigFile().getAbsolutePath());
		}
		// READ
		Header.setConfig(mapper.readValue(Header.getConfigFile(), PASBittorrentSpyConfig.class));
		if (!Header.getConfig().isPrepare()) {
			throw new ServiceConfigurationError("please edit " + Header.getConfigFile().getAbsolutePath());
		}
		logger.info("initConfig-end");
	}

}
