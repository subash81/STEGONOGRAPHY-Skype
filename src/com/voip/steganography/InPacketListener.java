package com.voip.steganography;

import com.voip.steganography.packts.RtpRawPacket;

public abstract class InPacketListener{
	public InPacketListener(){
		
	}
	
	public abstract void fireInPacket(RtpRawPacket rtpPacket);
}
