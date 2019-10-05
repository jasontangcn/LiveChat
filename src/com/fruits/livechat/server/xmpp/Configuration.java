package com.fruits.livechat.server.xmpp;

public class Configuration {
	private String hostname;
	private int port;

	public Configuration(String hostname, int port) {
		super();
		this.hostname = hostname;
		this.port = port;
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
}
