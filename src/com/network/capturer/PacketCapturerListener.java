package com.network.capturer;

import com.network.capturer.Capturer.TPacket;

public abstract class PacketCapturerListener {
	
	public abstract void processOutPacket(TPacket p);
	
	public abstract void processInPacket(TPacket p);
}
