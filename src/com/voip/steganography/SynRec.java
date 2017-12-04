package com.voip.steganography;

import java.text.SimpleDateFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.network.capturer.Capturer.TPacket;
import com.voip.steganography.packts.RtpAckPacket;
import com.voip.steganography.packts.RtpRawPacket;
import com.voip.steganography.packts.RtpSynPacket;
import com.voip.steganography.transfer.RtpRecPacketHandler;
import com.voip.steganography.transfer.Transport;

public class SynRec{
	private Log logger = LogFactory.getLog(SynRec.class);
	
	private Transport transport;

	private InPacketListener inPacketListener;

	private RtpSynRecPacketHandler recHandler;
	
	public SynRec(Transport transport) {
		this.transport = transport;
		inPacketListener = buildInPacketListener();
		this.transport.registerInPacketListener(inPacketListener);
//		recHandler = new RtpSynRecPacketHandler();
//		this.transport.registerInPacketListener(recHandler);
	}
	
	public void clear(){
		this.transport.removeInPacketListener(inPacketListener);
//		this.transport.removeInPacketListener(recHandler);
		Manager.getInstance().clearSynRec();
	}
	
	private class RtpSynRecPacketHandler extends RtpRecPacketHandler{

		@Override
		public void handler(RtpRawPacket rtpPacket) {
			logger.info("RtpSynRecPacketHandler: receive rtp packet. time:" + rtpPacket.getTimestamp() + ", sn:" + rtpPacket.getSequenceNumber());
			if (RtpSynPacket.isSynPacket(rtpPacket.getTimestamp(),rtpPacket.getSequenceNumber())){
				logger.info("RtpSynRecPacketHandler: receive syn packet.");
				sendAckPacket(0);
				Manager.getInstance().completeSynRec();
				storePacketInfo(rtpPacket);
			}
			
		}
		
	}
	
	private void storePacketInfo(RtpRawPacket rtpPacket) {
		SimpleDateFormat   sDateFormat   =   new   SimpleDateFormat("yyyy-MM-dd   hh:mm:ss");     
		String   date   =   sDateFormat.format(new   java.util.Date());  
		Manager.getInstance().storeUserInPacket(date, rtpPacket.getPacket().length);
	}
	
	private InPacketListener buildInPacketListener(){
		InPacketListener inPacketListener = new InPacketListener(){

			@Override
			public void fireInPacket(RtpRawPacket rtpPacket) {
//				RtpRawPacket rtpPacket = new RtpRawPacket(new byte[p.p.data.length],
//						p.p.data.length);
//				rtpPacket.setPacket(p.p.data);
				logger.info("RtpSynRecPacketHandler: receive rtp packet. time:" + rtpPacket.getTimestamp() + ", sn:" + rtpPacket.getSequenceNumber());
				if (RtpSynPacket.isSynPacket(rtpPacket.getTimestamp(),rtpPacket.getSequenceNumber())){
					logger.info("RtpSynRecPacketHandler: receive syn packet.");
					sendAckPacket(0);
					Manager.getInstance().completeSynRec();
				}
			}
		};
		return inPacketListener; 
	}
	
	private boolean sendAckPacket(int index) {
		logger.info("sendAckPacket, index:" + index);
		TPacket tPacket = null;
		while (true) {
			tPacket = this.transport.pollOutPacket();
			if (tPacket == null){
//				sleep(20);
				return true;
//				continue;
			}else{
				break;
			}
		}
		byte[] dataBytes = new byte[100];
		RtpAckPacket ackPacket = new RtpAckPacket(dataBytes, dataBytes.length);
		ackPacket = ackPacket.packet(index);
		try {
			logger.info("sendAckPacket, start send....:" + RtpAckPacket.isAckPacket(ackPacket.getPacket().getTimestamp(), ackPacket.getPacket().getSequenceNumber()));
			tPacket = tPacket.rebuild(ackPacket.getPacket().getPacket());
			logger.info("ts:" + ackPacket.getPacket().getTimestamp() +  ", sn:" + ackPacket.getPacket().getSequenceNumber());
			transport.sendPacket(tPacket);
		} catch (Exception e) {
			e.printStackTrace();
			logger.info("sendAckPacket...Fail.");
			return false;
		}
		logger.info("sendAckPacket...Done");
		return true;
	}
	
//	private void sleep(long time){
//		try {
//			Thread.sleep(time);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
}
