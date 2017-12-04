package com.voip.steganography.transfer;

import com.voip.steganography.packts.RtpRawPacket;

public abstract class RtpRecPacketHandler {
	
	public abstract void handler(RtpRawPacket rtpPacket);
}
