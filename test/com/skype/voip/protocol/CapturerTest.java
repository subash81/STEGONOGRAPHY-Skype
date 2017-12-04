package com.skype.voip.protocol;

import org.apache.log4j.Logger;

import com.network.capturer.Capturer;
import com.network.capturer.Capturer.TPacket;
import com.network.capturer.PacketCapturerListener;
import com.voip.steganography.packts.RtpAckPacket;
import com.voip.steganography.packts.RtpRawPacket;

public class CapturerTest {
	private static Logger logger = Logger.getLogger(CapturerTest.class);
	
	public static void main(String[] args){
		byte[] dataBytes = new byte[100];
//		RtpSynPacket synPacket = new RtpSynPacket(dataBytes,
//				dataBytes.length);
//		synPacket = synPacket.packet();
//		final RtpSynPacket synPacket1 = synPacket;
//		RtpAckPacket ackPacket = new RtpAckPacket(dataBytes, dataBytes.length);
//		ackPacket = ackPacket.packet(0);
//		final RtpAckPacket packet = ackPacket;
//		RtpDataPacket dataPacket = new RtpDataPacket(dataBytes,
//				dataBytes.length);
//		byte[] sendingData = new byte[1];
//		sendingData[0] = 0x11;
//		dataPacket = dataPacket.packet(groupData[i], i + 1);
//		dataPacket = dataPacket.packet(sendingData, 1);
		
//		
//		
//		RtpStartMsgPacket ctlPacket = new RtpStartMsgPacket(dataBytes,
//				dataBytes.length);
//		ctlPacket = ctlPacket.packet(1);
		
		RtpAckPacket ackPacket = new RtpAckPacket(dataBytes, dataBytes.length);
		ackPacket = ackPacket.packet(0);
		
		final RtpAckPacket packet = ackPacket;
		
		Capturer caputer = new Capturer();
		
		PacketCapturerListener inPacketListener = new PacketCapturerListener(){

			@Override
			public void processOutPacket(TPacket p) {
				p.p.data = packet.getPacket().getPacket();
//				p.p.len = p.p.data.length;
				RtpRawPacket rtpPacket = new RtpRawPacket(new byte[p.p.data.length],
						p.p.data.length);
				rtpPacket.setPacket(p.p.data);
				
				logger.info("Is syn packet: " + RtpAckPacket.isAckPacket(rtpPacket.getTimestamp(),rtpPacket.getSequenceNumber()));
				logger.info("ts:"+ rtpPacket.getTimestamp() + ", sn:" + rtpPacket.getSequenceNumber());
				
				RtpAckPacket dataPacket = new RtpAckPacket( rtpPacket );
				int grouds = dataPacket.extractValue();
				logger.info("grouds:" + grouds );
//				logger.info("Ori ts:"+ synPacket1.getPacket().getTimestamp() + ", sn:" + synPacket1.getPacket().getSequenceNumber());
				logger.info("new out packet: " + p.p.toString());
				StringBuffer sb = new StringBuffer();
				for (int i=0; i < p.p.data.length;i++){
					sb.append(p.p.data.toString());
				}
				logger.info("the data lenagth: " + p.p.data.length + ", data: "+ sb.toString());
				
				byte[] data = new byte[20];
				for (int i=0;i<20;i++){
					data[i] = 0;
				}
				p.p.data = data;
				p.sender.sendPacket(p.p);
			}

			@Override
			public void processInPacket(TPacket p) {
				// TODO Auto-generated method stub
				
			}

			
		};
		caputer.addListener(inPacketListener);
	}
}
