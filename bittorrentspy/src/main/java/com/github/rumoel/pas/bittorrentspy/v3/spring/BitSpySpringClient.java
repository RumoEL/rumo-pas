package com.github.rumoel.pas.bittorrentspy.v3.spring;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.github.rumoel.pas.bittorrentspy.v3.header.Header;
import com.github.rumoel.rumoel.libs.pas.torrents.report.ReportForTorrentPeer;

public class BitSpySpringClient {
	public static void sendResult(ReportForTorrentPeer report) {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Accept", MediaType.APPLICATION_JSON_VALUE);
		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<ReportForTorrentPeer> requestBody = new HttpEntity<>(report, headers);

		ResponseEntity<ReportForTorrentPeer> result = Header.restTemplate3.postForEntity(
				//
				Header.getConfig().getApiAddr(),
				//
				requestBody,
				//
				ReportForTorrentPeer.class);

		System.err.println(result.getStatusCode());
		// Code = 200.
		if (result.getStatusCode() == HttpStatus.OK) {
			ReportForTorrentPeer resultBody = result.getBody();
			System.out.println("(Client Side) Employee Created: {}" + resultBody);
		}
	}
}
