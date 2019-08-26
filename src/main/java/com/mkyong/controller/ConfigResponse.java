package com.mkyong.controller;

public class ConfigResponse {

	private int status;
	
	private ConfigContent data;

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public ConfigContent getData() {
		return data;
	}

	public void setData(ConfigContent data) {
		this.data = data;
	}

	@Override
	public String toString() {
		return "ConfigResponse [status=" + status + ", data=" + data + "]";
	}
	
	
}
