package com.skype.voip.protocol;

import com.voip.steganography.packts.RtpSynPacket;

public class RtpPacketTest {
	public RtpPacketTest(){
		
	}
	
	public static void main(String[] args){
		byte[] dataBytes = new byte[100];
		RtpSynPacket synPacket = new RtpSynPacket(dataBytes,
				dataBytes.length);
		synPacket = synPacket.packet();
		
		
		
		
//		TPacket tPacket = tPacket.rebuild(synPacket.getPacket().getPacket());
		
	}
}
