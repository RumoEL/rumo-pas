package com.github.rumoel.pas.bittorrentspy.v3.trackers;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;

public class TrackerHandler extends Thread {

	@Getter
	Logger logger = LoggerFactory.getLogger(getClass());

	CopyOnWriteArrayList<TrackerObj> trackers = new CopyOnWriteArrayList<>();

	ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);

	public void init() {
		for (TrackerObj tracker : trackers) {
			tracker.init();
		}
	}

	public void add(TrackerObj tracker) {
		if (!trackers.contains(tracker)) {
			trackers.add(tracker);
		}
	}

	@Override
	public void run() {
		for (TrackerObj tracker : trackers) {
			int statsDumpIntervalSecond = 10;
			executor.scheduleWithFixedDelay(() -> tracker.dump(), 0, statsDumpIntervalSecond, TimeUnit.SECONDS);
			tracker.startTr();
		}
		super.run();
	}
}
