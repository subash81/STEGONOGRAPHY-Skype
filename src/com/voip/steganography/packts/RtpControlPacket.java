
package com.voip.steganography.packts;

public abstract class RtpControlPacket {
	protected RtpRawPacket packet = null;
	long timeStamp;
	int sequenceNumber;
	protected final static int snCtl = 0;
	
	public RtpControlPacket(byte[] data, int length){
		this.packet = new RtpRawPacket(new byte[data.length], length);
		this.packet.setPacket(data);
		timeStamp = packet.getTimestamp();
		sequenceNumber = packet.getSequenceNumber();
	}
	
	public RtpControlPacket(RtpRawPacket rtpPacket){
		this.packet = rtpPacket;
		timeStamp = packet.getTimestamp();
		sequenceNumber = packet.getSequenceNumber();
	}
	
//	public abstract void packet();
	
	public RtpRawPacket getPacket(){
		return this.packet;
	}
	
	protected long setConvertRtpTimeStamp(long timeStamp, byte cmd) {
		long cmdLong = (long)cmd;
		long newTimeStamp = (timeStamp & (long)0xFFFFFF00) | cmdLong;
		return newTimeStamp;
	}

	protected byte buildMsgStartCMD(int numGroups, byte cmd) {
		byte fullCmd = (byte) (cmd | (byte)numGroups << 2);
		return fullCmd;
	}
}
