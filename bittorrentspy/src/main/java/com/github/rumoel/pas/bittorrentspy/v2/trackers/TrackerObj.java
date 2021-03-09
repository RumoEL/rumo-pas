package com.github.rumoel.pas.bittorrentspy.v2.trackers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;

public class TrackerObj implements TrackerInterface {
	@Getter
	Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public void init() {
		logger.info("init");
	}

	@Override
	public boolean startTr() {
		logger.info("startTr");
		return false;
	}

	@Override
	public void dump() {
		logger.info("{}-dump", Thread.currentThread().getName());
	}

}
