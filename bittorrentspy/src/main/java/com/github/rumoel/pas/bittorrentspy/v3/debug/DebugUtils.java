package com.github.rumoel.pas.bittorrentspy.v3.debug;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import com.github.rumoel.pas.bittorrentspy.v3.header.Header;

public class DebugUtils {
	private DebugUtils() {
	}

	public static void sendData(byte[] data) {
		try {
			Header.getDebugSocket().getOutputStream().write(data);
			Header.getDebugSocket().getOutputStream().flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static int getPort(SocketAddress address) {
		return ((InetSocketAddress) address).getPort();
	}

}
