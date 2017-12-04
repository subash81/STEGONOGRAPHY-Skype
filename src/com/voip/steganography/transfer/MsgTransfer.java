
package com.voip.steganography.transfer;

import java.text.SimpleDateFormat;
import java.util.Arrays;

import org.apache.log4j.Logger;

import com.network.capturer.Capturer.TPacket;
import com.voip.steganography.InPacketListener;
import com.voip.steganography.Manager;
import com.voip.steganography.packts.RtpAckPacket;
import com.voip.steganography.packts.RtpCheckPacket;
import com.voip.steganography.packts.RtpDataPacket;
import com.voip.steganography.packts.RtpRawPacket;
import com.voip.steganography.packts.RtpStartMsgPacket;

public class MsgTransfer{
	private Logger logger = Logger.getLogger(MsgTransfer.class);
	
	public static final int numFramesInGroup = 10;
	
	public static final int validRtpSize = 1;
	
	public static final int sizeGroup = numFramesInGroup * validRtpSize;
	
	private static final int ackWaitingTime = 100;
	
	private TransferStatusMachine sendingStatus = new TransferStatusMachine();

	private Transport transport;
	
//	private LinkedList<TPacket> inFIFO = new LinkedList<TPacket>();
	
	private int waitFlag = 0;  // 0: for initial, 1: for waiting ack, 2: for done
	private RtpAckPacket ackPacket = null;
	
	private Object object = new Object();

	private InPacketListener inPacketListener;

	private RtpRecPacketHandler recHandler;
	
	public MsgTransfer(Transport transport) {
		this.transport = transport;
//		inPacketListener = buildInPacketListener();
//		this.transport.registerInPacketListener(inPacketListener);
		recHandler = new RtpMsnTransPacketHandler();
		this.transport.registerInPacketListener(recHandler);
	}
	
	private class RtpMsnTransPacketHandler extends RtpRecPacketHandler{

		@Override
		public void handler(RtpRawPacket rtpPacket) {
			logger.info("MsgTransfer:" + "ts:" + rtpPacket.getTimestamp() + ". sn: " + rtpPacket.getSequenceNumber() + ", flag: " + waitFlag);
			if (RtpAckPacket.isAckPacket(rtpPacket.getTimestamp(),
					rtpPacket.getSequenceNumber())) {
				synchronized (object){
					if (waitFlag == 1){
						setWaitFlag(2,  new RtpAckPacket(rtpPacket));
					}
				}
				storePacketInfo(rtpPacket);
				logger.info("Receive one ACK packet");
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
//				inFIFO.add(p);
//				RtpRawPacket rtpPacket = new RtpRawPacket(new byte[p.p.data.length],
//						p.p.data.length);
//				rtpPacket.setPacket(p.p.data);
				logger.info("MsgTransfer:" + "ts:" + rtpPacket.getTimestamp() + ". sn: " + rtpPacket.getSequenceNumber() + ", flag: " + waitFlag);
				if (RtpAckPacket.isAckPacket(rtpPacket.getTimestamp(),
						rtpPacket.getSequenceNumber())) {
					synchronized (object){
						if (waitFlag == 1){
							setWaitFlag(2,  new RtpAckPacket(rtpPacket));
						}
					}
					logger.info("Receive one ACK packet");
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
	
	public int calcNumGroups(String msgStr){
		int oriLen = msgStr.length();
		int numGroups = (int) Math.ceil((double)(oriLen) / (double)sizeGroup);	
		return numGroups;
	}
	
	public byte[] getSubGroupData(String msgStr, int index){
		if (index >= calcNumGroups(msgStr)){
			return null;
		}
		byte[] oriData = msgStr.getBytes();
		int oriLength = oriData.length;
		int startIndex = index*sizeGroup;
		int endIndex = (index+1) * sizeGroup-1;
		if (endIndex > oriLength-1){
			endIndex = oriLength-1;
		}
		byte[] code = new byte[sizeGroup];
		Arrays.fill(code, (byte)0);
		System.arraycopy(oriData, startIndex, code, 0, endIndex-startIndex+1);
		return code;
	}
	
	
	public boolean sendMsnData(String msgStr) {
		logger.info("sendMsnData, ready to send data:" );
		int numGroups = calcNumGroups(msgStr);
		logger.info("numGroups:" + numGroups);
		if (!sendStartMsgCtlPacket(numGroups, msgStr)){
			return false;
		}
		for (int i=0;i<numGroups;i++){
			byte[] groupData = getSubGroupData(msgStr, i);
			if (!sendGroupData(groupData, i+1)){
				return false;
			}
		}
		if (!sendCompleteCtlPakcet()){
			return false;
		}
		return true;
	}

	private boolean sendCompleteCtlPakcet() {
		logger.info("sendCompleteCtlPakcet...");
		if (sendingStatus.getStatusID() == sendingStatus.START_SUBDATA) {
			sendingStatus.touch();
			logger.info("sendCompleteCtlPakcet...Done");
			return true;
		}
		return false;
	}

	private boolean sendGroupData(byte[] groupData, int index) {
		logger.info("sendGroupData...");
		if (sendingStatus.getStatusID() == sendingStatus.START
				|| sendingStatus.getStatusID() == sendingStatus.START_SUBDATA) {
			if (!sendMsgSubDataPacket(groupData, index)) {
				logger.error("Sending group data package error!");
				return false;
			}
			if (sendingStatus.getStatusID() == sendingStatus.START) {
				sendingStatus.touch();
			}
			logger.info("sendGroupData...Done");
			return true;
		}
		logger.error("Unknown status when sending group data!");
		return false;
	}
	
	private boolean sendMsgSubDataPacket(byte[] groupData, int index) {
		// data, sending these package until receiving ack package.
		logger.info("sendMsgSubDataPacket..., index group:" + index);
		while (true) {
			for (int i = 0; i < groupData.length/validRtpSize; i=i+1) {
				logger.info("sendMsgSubDataPacket..., index packet:" + i);
				TPacket tPacket = null;
				while (true) {
					tPacket = transport.pollOutPacket();
					if (tPacket == null){
						continue;
					}else{
						break;
					}
				}
				byte[] dataBytes = new byte[100];
				RtpDataPacket dataPacket = new RtpDataPacket(dataBytes,
						dataBytes.length);
				byte[] sendingData = new byte[1];
				sendingData[0] = groupData[i];
//				dataPacket = dataPacket.packet(groupData[i], i + 1);
				dataPacket = dataPacket.packet(sendingData, i + 1);
				tPacket = tPacket.rebuild(dataPacket.getPacket().getPacket());
				transport.sendRtpPacket(tPacket);
				// UDPPacket udpPacket = this.capturer.removeUDPPacket();
				// byte[] dataBytes = udpPacket.data;
				
			}
			logger.info("sendMsgSubDataPacket..., send check packet.");
			RtpAckPacket ackPacket = sendCtlPacket(index);
			if (ackPacket.isPerfect()) {
				logger.info("sendMsgSubDataPacket..., The ack packet is perfect.");
				break;
			} else {
				logger.info("The ack packet is not the perfect, continue send the sub group data.");
				continue; // need add for counting resent time.
			}
		}
		logger.info("sendMsgSubDataPacket...Done");
		return true;
	}
	
	private RtpAckPacket sendCtlPacket(int index) {
		logger.info("sendCtlPacket...");
		RtpAckPacket packet = null;
		while (true) {
			TPacket tPacket = transport.pollOutPacket();
			if (tPacket == null){
				continue;
			}
			byte[] dataBytes = new byte[100];
			RtpCheckPacket ctlPacket = new RtpCheckPacket(dataBytes,
					dataBytes.length);
			ctlPacket = ctlPacket.packet(index);
			tPacket = tPacket.rebuild(ctlPacket.getPacket().getPacket());
			transport.sendRtpPacket(tPacket);
			logger.info("sendCtlPacket..., receive ack...");
			packet = waitToReceiveAck(ackWaitingTime);
			if (packet == null)  {
				logger.info("sendCtlPacket..., cannot receive ack, resend check packet");
				continue;
			} else {
				logger.info("sendCtlPacket..., receive ack");
				break;
			}
		}
		logger.info("sendCtlPacket...Done");
		return packet;
	}

	private boolean sendStartMsgCtlPacket(int numGroups, String msgStr) {
		logger.info("sendStartMsgCtlPacket...");
		if (sendingStatus.getStatusID() != sendingStatus.CLOSED) {
			logger.info("Error: Unknown status when sending start control data!");
			return false;
		}
		RtpAckPacket ackPacket = sendMsgStartPacket(numGroups);
		if (ackPacket == null) {
			logger.error("Sending start package error!");
			return false;
		}
		if (ackPacket.isPerfect()) {
			sendingStatus.touch();
			logger.info("sendStartMsgCtlPacket...Done");
			return true;
		} else {
			return false;
		}
	}
	
	private RtpAckPacket sendMsgStartPacket(int numGroups) {
		// data type and group number, sending it until receiving ack packge.
		logger.info("sendMsgStartPacket...");
		RtpAckPacket packet = null;
		while (true) {
			TPacket tPacket = transport.pollOutPacket();
			if (tPacket == null){
				sleep(20);
				continue;
			}
			logger.info("build packet..");
			byte[] dataBytes = new byte[100];
			RtpStartMsgPacket ctlPacket = new RtpStartMsgPacket(dataBytes,
					dataBytes.length);
			ctlPacket = ctlPacket.packet(numGroups);
			tPacket = tPacket.rebuild(ctlPacket.getPacket().getPacket());
			transport.sendRtpPacket(tPacket);
			packet = waitToReceiveAck(ackWaitingTime);
			if (packet == null) {
				continue;
			} else {
				break;
			}
		}
		logger.info("sendMsgStartPacket...Done");
		return packet;
	}
	
	private RtpAckPacket waitToReceiveAck(int waitTime) {
		long currentTime = System.currentTimeMillis();
		RtpAckPacket packet = null;
		this.setWaitFlag(1, null);
		while (true) {
			logger.info("MsgTransfer: waitToReceiveAck... flag:" + this.waitFlag);
			synchronized (object){
				if (this.waitFlag == 2) {
					packet = this.ackPacket;
					this.setWaitFlag(0, null);
					break;
				}
			}
			
			long time = System.currentTimeMillis() - currentTime;
			if (time >= waitTime) {
				logger.info("MsgTransfer: Cannot wait one ack packet from the receiver.");
				return null;
			}
			sleep(20);
		}
		return packet;
	}
	
	private void sleep(long time){
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void exit(){
//		this.transport.removeInPacketListener(inPacketListener);
		this.transport.removeInPacketListener(recHandler);
	}

	public void reInit() {
		sendingStatus = new TransferStatusMachine();
		this.setWaitFlag(0, null);
	}

}
