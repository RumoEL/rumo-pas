package com.github.rumoel.pas.bittorrentspy.dht2;

public class Main {

	public static void main(String[] args) {
		DhtCliTracker dhtCliTracker = new DhtCliTracker();
		dhtCliTracker.init();
		dhtCliTracker.start();
		dhtCliTracker.startDumper();
	}

}
