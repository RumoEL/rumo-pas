package com.github.rumoel.pas.bittorrentspy.v3.init;

import java.io.IOException;
import java.net.Socket;
import java.util.ServiceConfigurationError;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.rumoel.pas.bittorrentspy.config.PASBTSPConfig;
import com.github.rumoel.pas.bittorrentspy.v3.header.Header;
import com.github.rumoel.pas.bittorrentspy.v3.trackers.impl.TrackerDHT;

public class PBSInit {
	static Logger logger = LoggerFactory.getLogger(PBSInit.class);

	public static void main(String[] args) throws IOException {
		// debug option
		// java -jar file.jar socket host port
		// ######################## String int
		if (args != null && args.length == 3 && args[0].equalsIgnoreCase("socket")) {
			Header.setDebugSocket(new Socket(args[1], Integer.parseInt(args[2])));
		}

		readConfig();

		TrackerDHT trackerDHT = new TrackerDHT();
		Header.trackerHandler.add(trackerDHT);
		Header.trackerHandler.init();
		Header.trackerHandler.start();
	}

	private static void readConfig() throws IOException {
		logger.info("initConfig-start");
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
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
		Header.setConfig(mapper.readValue(Header.getConfigFile(), PASBTSPConfig.class));
		if (!Header.getConfig().isPrepare()) {
			throw new ServiceConfigurationError("please edit " + Header.getConfigFile().getAbsolutePath());
		}
		logger.info("initConfig-end");
	}

}
