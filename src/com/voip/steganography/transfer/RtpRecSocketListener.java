
package com.voip.steganography.transfer;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.voip.steganography.packts.RtpRawPacket;
import com.voip.steganography.packts.RtpUdpPacket;

class RtpRecSocketListener implements RtpUdpProviderListener{
	private Log logger = LogFactory.getLog(RtpRecSocketListener.class);
	
	public List<RtpRecPacketHandler> handlers = new ArrayList<RtpRecPacketHandler>();
	
	public void registerHandler(RtpRecPacketHandler handler){
		this.handlers.add(handler);
	}
	
	public void removeHandler(RtpRecPacketHandler handler){
		this.handlers.remove(handler);
	}
	
	
	@Override
	public void onReceivedPacket(RtpUdpRecProvider udp, RtpUdpPacket packet) {
		RtpRawPacket rtpPacket = new RtpRawPacket(new byte[packet.getData().length],
				packet.getData().length);
		rtpPacket.setPacket(packet.getData());
		logger.info("onReceivedPacket: receive one pakcet : " + this.handlers.size());
		int i = 0;
		for (RtpRecPacketHandler handler : this.handlers){
			i = i + 1;
			logger.info("onReceivedPacket: receive one pakcet : " + i + "," + handler.toString());
			handler.handler(rtpPacket);
		}
	}
	
	@Override
	public void onServiceTerminated(RtpUdpRecProvider udp, Exception error) {
		// TODO Auto-generated method stub

	}

}
