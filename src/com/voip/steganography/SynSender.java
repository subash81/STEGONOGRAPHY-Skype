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

public class SynSender implements Runnable {
	private static final long ackWaitingTime = 1000;
	private Log logger = LogFactory.getLog(SynSender.class);
	
	private Transport transport;
	private int waitFlag = 0;  // 0: for initial, 1: for waiting ack, 2: for done
	private RtpAckPacket ackPacket = null;
	private Object object = new Object();
	private InPacketListener inPacketListener;
	private RtpSynSenderPacketHandler recHandler;
	
	public SynSender(Transport transport) {
		this.transport = transport;
		inPacketListener = buildInPacketListener();
		this.transport.registerInPacketListener(inPacketListener);
//		recHandler = new RtpSynSenderPacketHandler();
//		this.transport.registerInPacketListener(recHandler);
	}
	
	public void clear(){
		this.transport.removeInPacketListener(inPacketListener);
//		this.transport.removeInPacketListener(recHandler);
		Manager.getInstance().clearSynSender();
	}
	
	private class RtpSynSenderPacketHandler extends RtpRecPacketHandler{

		@Override
		public void handler(RtpRawPacket rtpPacket) {
			logger.info("RtpMsnRecPacketHandler:" + "ts:" + rtpPacket.getTimestamp() + ". sn: " + rtpPacket.getSequenceNumber());
			if (RtpAckPacket.isAckPacket(rtpPacket.getTimestamp(),
					rtpPacket.getSequenceNumber())) {
				logger.info("Receive one ACK packet: flag:" + waitFlag);
				synchronized (object){
					if (waitFlag == 1){
						setWaitFlag(2,  new RtpAckPacket(rtpPacket));
					}
				}
				Manager.getInstance().completeSynSender();
				storePacketInfo(rtpPacket);
				logger.info("2:");
			}else{
				logger.info("is not ACK packet");
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
				
				if (RtpAckPacket.isAckPacket(rtpPacket.getTimestamp(),
						rtpPacket.getSequenceNumber())) {
					if (waitFlag == 1){
						setWaitFlag(2,  new RtpAckPacket(rtpPacket));
					}
					Manager.getInstance().completeSynSender();
					logger.info("Receive one ACK packet: flag:" + waitFlag);
				}else{
					logger.info("is not ACK packet");
				}
			}
		};
		return inPacketListener; 
	}
	
	private void setWaitFlag(int flag, RtpAckPacket ackPacket){
		synchronized (object) {
			this.waitFlag = flag;
			this.ackPacket = ackPacket;
		}
	}
	
	
	private void sleep(long time){
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		logger.info("RtpSynSender Running...");
		TPacket tPacket = null;
		while (true){
			while (true) {
				tPacket = this.transport.pollOutPacket();
				if (tPacket == null){
					sleep(20);
					continue;
				}else{
					break;
				}
			}
			byte[] dataBytes = new byte[100];
			RtpSynPacket synPacket = new RtpSynPacket(dataBytes,
					dataBytes.length);
			synPacket = synPacket.packet();
			try {
				logger.info("RtpSynPacket, start send....");
				tPacket = tPacket.rebuild(synPacket.getPacket().getPacket());
//				tPacket = tPacket.rebuild(dataBytes);
				transport.sendPacket(tPacket);
				
				boolean falg = waitToReceiveSyn(ackWaitingTime);
				if (!falg) {
					logger.info("Fail in receive syn");
					continue;
				}else{
					logger.info("Receive syn");
					break;
				}
				
			} catch (Exception e) {
				e.printStackTrace();
				logger.info("sendAckPacket...Fail.");
			}
			logger.info("Syn Done");
		}
		Manager.getInstance().clearSynSender();
	}
	
	
	private boolean waitToReceiveSyn(long waitTime) {
		logger.info("waitToReceiveSyn...");
		long currentTime = System.currentTimeMillis();
		this.setWaitFlag(1, null);
		while (true){
			logger.info("waitToReceiveAck... flag:" + this.waitFlag);
			synchronized (object){
				if (this.waitFlag == 2) {
					this.setWaitFlag(0, null);
					return true;
				}
			}
			long time = System.currentTimeMillis() - currentTime;
			if (time >= waitTime) {
				logger.info("Cannot wait one ack packet from the receiver.");
				return false;
			}
			sleep(20);
		}
	}
	
}
