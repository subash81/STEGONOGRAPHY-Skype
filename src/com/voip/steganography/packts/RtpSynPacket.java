package com.voip.steganography.packts;

public class RtpSynPacket extends RtpControlPacket {
	private static final long cmd = (long)0x00;
	
	public RtpSynPacket(byte[] data, int length) {
		super(data, length);
	}
	
	
	public RtpSynPacket(RtpRawPacket rtpPacket) {
		super(rtpPacket);
	}
	
	
	public static boolean isSynPacket(long timeStamp, int sn){
		byte tag = (byte)(timeStamp & (long)0x00000003);
		if (tag == cmd && snCtl == sn){
			return true;
		}else{
			return false;
		}
	}
	
	public RtpSynPacket packet(){
		packet.setSequenceNumber(snCtl);
		packet.setTimestamp(cmd);
		System.out.println("ts:" + packet.getTimestamp() + ". sn:" + packet.getSequenceNumber());
		return this;
	}
}
