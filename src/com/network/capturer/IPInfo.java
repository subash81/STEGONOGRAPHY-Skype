package com.network.capturer;

public class IPInfo {
	private String ip;
	private int port;

	public IPInfo(String ip, int port){
		this.ip = ip;
		this.port = port;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
	
	public boolean isEqual(IPInfo newIP){
		if (newIP == null){
			return false;
		}
		if (this.ip.equalsIgnoreCase(newIP.getIp()) && this.port == newIP.getPort()){
			return true;
		}else{
			return false;
		}
	}
	
	public String toString(){
		return "ip:" + ip + ", port:" + port;
	}
}
