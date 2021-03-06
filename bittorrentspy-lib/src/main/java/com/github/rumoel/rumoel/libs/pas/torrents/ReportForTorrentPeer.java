package com.github.rumoel.rumoel.libs.pas.torrents;

import java.io.Serializable;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

public class ReportForTorrentPeer implements Serializable {
	private static final long serialVersionUID = 5375481922314433060L;
	@Getter
	@Setter
	private String reportId = UUID.randomUUID().toString();
	@Getter
	@Setter
	private long time;

	@Getter
	@Setter
	private String torrentHash;
	@Getter
	@Setter
	private String peerHash;
	@Getter
	@Setter
	private String ip;
	@Getter
	@Setter
	private int port;

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ReportForTorrentPeer [reportId=");
		builder.append(reportId);
		builder.append(", time=");
		builder.append(time);
		builder.append(", torrentHash=");
		builder.append(torrentHash);
		builder.append(", peerHash=");
		builder.append(peerHash);
		builder.append(", ip=");
		builder.append(ip);
		builder.append(", port=");
		builder.append(port);
		builder.append("]");
		return builder.toString();
	}

}
