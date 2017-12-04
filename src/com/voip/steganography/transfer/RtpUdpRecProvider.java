package com.voip.steganography.transfer;

import java.io.IOException;

import com.voip.steganography.packts.RtpUdpPacket;

public class RtpUdpRecProvider implements Runnable{
	 public static final int BUFFER_SIZE = 65535;
	private RtpUdpSocket socket;
	private RtpUdpProviderListener listener;
	private boolean stop = false;
	
	public RtpUdpRecProvider(RtpUdpSocket socket, RtpUdpProviderListener provider){
		this.socket = socket;
		this.listener = provider;
	}
	
	@Override
	public void run() {
		byte[] buf = new byte[BUFFER_SIZE];
        RtpUdpPacket packet = new RtpUdpPacket(buf, buf.length);
		while (!stop) {
			try {
				socket.receive(packet);
				if (listener != null)
                    listener.onReceivedPacket(this, packet);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void stop(){
		this.stop = true;
	}

}
