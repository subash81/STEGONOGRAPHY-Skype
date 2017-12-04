package com.voip.steganography;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;

import org.apache.log4j.Logger;

import com.network.capturer.Capturer.TPacket;
import com.voip.steganography.packts.RtpAckPacket;
import com.voip.steganography.packts.RtpCheckPacket;
import com.voip.steganography.packts.RtpDataPacket;
import com.voip.steganography.packts.RtpRawPacket;
import com.voip.steganography.packts.RtpStartMsgPacket;
import com.voip.steganography.transfer.MsgTransfer;
import com.voip.steganography.transfer.RtpRecPacketHandler;
import com.voip.steganography.transfer.TransferStatusMachine;
import com.voip.steganography.transfer.Transport;

public class MsgReceiver {
	private Logger logger = Logger.getLogger(MsgReceiver.class);
	private Transport transport;
	private InPacketListener inPacketListener;
	private RtpRecPacketHandler recHandler;
	
	public static final String code = "UTF-8";
	
	public MsgReceiver(Transport transport){
		this.transport = transport;
//		inPacketListener = buildInPacketListener();
//		this.transport.registerInPacketListener(inPacketListener);
		
		recHandler = new RtpMsnRecPacketHandler();
		this.transport.registerInPacketListener(recHandler);
	}
	
	private class RtpMsnRecPacketHandler extends RtpRecPacketHandler{

		@Override
		public void handler(RtpRawPacket rtpPacket) {
			logger.info("RtpMsnRecPacketHandler:" + "ts:" + rtpPacket.getTimestamp() + ". sn: " + rtpPacket.getSequenceNumber());
			if (RtpStartMsgPacket.isStartPacket(rtpPacket.getTimestamp(),
					rtpPacket.getSequenceNumber())){
				logger.info("rec: start msg packet");
				RtpStartMsgPacket startMsgPacket = new RtpStartMsgPacket(rtpPacket);
				recMsgStartPacket(startMsgPacket);
				storePacketInfo(rtpPacket);
			}else if (RtpCheckPacket.isCheckPacket(rtpPacket.getTimestamp(),
					rtpPacket.getSequenceNumber())){
				logger.info("rec: check packet");
				RtpCheckPacket checkMsgPacket = new RtpCheckPacket(rtpPacket);
				recCheckPacket(checkMsgPacket);
				storePacketInfo(rtpPacket);
			}else if (RtpDataPacket.isDataPacket(rtpPacket.getTimestamp(), rtpPacket.getSequenceNumber())){
				logger.info("rec: data packet");
				RtpDataPacket dataPacket = new RtpDataPacket(rtpPacket);
				recDataPacket(dataPacket);
				storePacketInfo(rtpPacket);
			}else{
				logger.info("receive unknown packet: " + rtpPacket.getPacket());
			}
			
		}

		private void storePacketInfo(RtpRawPacket rtpPacket) {
			SimpleDateFormat   sDateFormat   =   new   SimpleDateFormat("yyyy-MM-dd   hh:mm:ss");     
			String   date   =   sDateFormat.format(new   java.util.Date());  
			Manager.getInstance().storeUserInPacket(date, rtpPacket.getPacket().length);
		}
		
	}
	
	private InPacketListener buildInPacketListener(){
		InPacketListener inPacketListener = new InPacketListener(){
			@Override
			public void fireInPacket(RtpRawPacket rtpPacket) {
//				RtpRawPacket rtpPacket = new RtpRawPacket(new byte[p.p.data.length],
//						p.p.data.length);
//				rtpPacket.setPacket(p.p.data);
				logger.info("RtpMsnRecPacketHandler:" + "ts:" + rtpPacket.getTimestamp() + ". sn: " + rtpPacket.getSequenceNumber());
				if (RtpStartMsgPacket.isStartPacket(rtpPacket.getTimestamp(),
						rtpPacket.getSequenceNumber())){
					logger.info("rec: start msg packet");
					RtpStartMsgPacket startMsgPacket = new RtpStartMsgPacket(rtpPacket);
					recMsgStartPacket(startMsgPacket);
				}else if (RtpCheckPacket.isCheckPacket(rtpPacket.getTimestamp(),
						rtpPacket.getSequenceNumber())){
					logger.info("rec: check packet");
					RtpCheckPacket checkMsgPacket = new RtpCheckPacket(rtpPacket);
					recCheckPacket(checkMsgPacket);
				}else if (RtpDataPacket.isDataPacket(rtpPacket.getTimestamp(), rtpPacket.getSequenceNumber())){
					logger.info("rec: data packet");
					RtpDataPacket dataPacket = new RtpDataPacket(rtpPacket);
					recDataPacket(dataPacket);
				}else{
					logger.info("receive unknown packet: " + rtpPacket.getPacket());
				}
			}
		};
		return inPacketListener; 
	}
	
	private byte[] recData = null;
	private int recNumGroups;  // the total group size
	private int countGroupData;  // the index packet in the group
	private int groupIndex;  // the index of the group data
	
	private TransferStatusMachine recStatus = new TransferStatusMachine();
	
	public void recMsgStartPacket(RtpStartMsgPacket startMsgPacket) {
		logger.info("recMsgStartPacket...");
		if (recStatus.getStatusID() == recStatus.CLOSED || recStatus.getStatusID() == recStatus.START) {
			recNumGroups = startMsgPacket.extractNumGroups();
			recData = new byte[recNumGroups * MsgTransfer.validRtpSize
					* MsgTransfer.sizeGroup];
			
			logger.info("recNumGroups: " + recNumGroups + ". data length:" + recData.length);
			
			countGroupData = 0;
			groupIndex = 0;
			if (recStatus.getStatusID() == recStatus.CLOSED){
				recStatus.touch();
			}
			sendAckPacket(recNumGroups);
		} else {
			logger.info("receive start msg packet when the status isn't closed:" + recStatus.getStatusID());
		}
	}

	public void recDataPacket(RtpDataPacket dataPacket) {
		logger.info("recDataPacket...");
		if (recStatus.getStatusID() == recStatus.START
				|| recStatus.getStatusID() == recStatus.START_SUBDATA) {
			int sn = dataPacket.extractSN();
			logger.info("sn:" + sn);
			if (sn <= MsgTransfer.sizeGroup && sn >= 1) {
				byte[] dataArray = dataPacket.extractValidData();
				byte data = dataArray[0];
				recData[(groupIndex * MsgTransfer.sizeGroup) + sn - 1] = data;
				countGroupData++;   // ??
			}
			if (recStatus.getStatusID() == recStatus.START) {
				recStatus.touch();
			}
		} else {
			logger.info("receive data packet when the status isn't started or subdata");
		}
	}
	
	public void recCheckPacket(RtpCheckPacket checkPacket) {
		logger.info("recCheckPacket...");
		if (recStatus.getStatusID() == recStatus.START_SUBDATA) {
			if (countGroupData == MsgTransfer.sizeGroup) {
				// receive all the group data, send the ack packet.
				groupIndex++;
				logger.info("group index:" + groupIndex);
				sendAckPacket(groupIndex);
				countGroupData = 0;
				if (groupIndex == recNumGroups){
					recStatus.touch();
					dispBytes(recData);
					String msgStr = convertBytesToString(recData);
					logger.info("receive all the group data: " + msgStr);
					Manager.getInstance().receiveMsg(msgStr);
				}
			} else {
				// did not receive all the group data, ask to re-send all the
				// group data.
				logger.info("did not receive all the group data, ask to re-send all the.");
				sendAckPacket(0);
				countGroupData = 0;
			}
		} else if (recStatus.getStatusID() == recStatus.CLOSED){
			logger.info("receive data check packet when the status is close");
			sendAckPacket(groupIndex);
		}else{
			logger.info("receive data check packet when the status isn't in subdata");
		}
	}

	private boolean sendAckPacket(int index) {
		logger.info("sendAckPacket, index:" + index);
		TPacket tPacket = null;
//		while (true) {
		tPacket = this.transport.pollOutPacket();
//			if (tPacket == null){
//				sleep(20);
//				continue;
//			}else{
//				break;
//			}
//		}
		if (tPacket == null){
			return false;
		}
		byte[] dataBytes = new byte[100];
		RtpAckPacket ackPacket = new RtpAckPacket(dataBytes, dataBytes.length);
		ackPacket = ackPacket.packet(index);
		try {
			logger.info("sendAckPacket, start send....");
			logger.info("ts:" + ackPacket.getPacket().getTimestamp() + ", sn:" + ackPacket.getPacket().getSequenceNumber());
			tPacket = tPacket.rebuild(ackPacket.getPacket().getPacket());
			transport.sendRtpPacket(tPacket);
		} catch (Exception e) {
			e.printStackTrace();
			logger.info("sendAckPacket...Fail.");
			return false;
		}
		logger.info("sendAckPacket...Done");
		return true;
	}	
	
	public void dispBytes(byte[] data){
		String s = "";
		for (int i=0;i<data.length;i++){
			s = s + data[i];
		}
		logger.info("bytes:" + s);
	}
	
	public String convertBytesToString(byte[] bytes){
		try {
			String str = new String(bytes, code);
			return str;
		} catch (UnsupportedEncodingException e) {
			logger.error("Error in converting bytes to string.");
			e.printStackTrace();
		}
		return null;
	}
	
	private void sleep(long time){
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void exit(){
//		this.transport.removeInPacketListener(inPacketListener);
		this.transport.removeInPacketListener(recHandler);

	}

	public void reInit() {
		recStatus = new TransferStatusMachine();
	}
}
