package com.github.rumoel.pas.bittorrentspy.v3.trackers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;

public class TrackerObj implements TrackerInterface {
	@Getter
	Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public void init() {
		logger.warn("init");
	}

	@Override
	public boolean startTr() {
		logger.warn("startTr");
		return false;
	}

	@Override
	public void dump() {
		logger.warn("dump");
	}

}
