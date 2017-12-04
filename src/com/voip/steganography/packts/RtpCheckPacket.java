
package com.voip.steganography.packts;

public class RtpCheckPacket extends RtpControlPacket {
	private static final byte cmd = 0x02;
		
	public RtpCheckPacket(byte[] data, int length) {
		super(data, length);
	}
	
	public RtpCheckPacket(RtpRawPacket rtpPacket) {
		super(rtpPacket);
	}
	
	public static boolean isCheckPacket(long timeStamp, int sn){
		byte tag = (byte)(timeStamp & (long)0x00000003);
		if (tag == cmd && snCtl == sn){
			return true;
		}else{
			return false;
		}
	}
	
	public RtpCheckPacket packet(int index) {
		byte cmd = buildMsgStartCMD(index, RtpCheckPacket.cmd);
		long newTimeStamp = setConvertRtpTimeStamp(timeStamp, cmd);
		packet.setSequenceNumber(snCtl);
		packet.setTimestamp(newTimeStamp);
		return this;
	}
	
	public int extractIndexGroup() {
		byte validData = (byte) (timeStamp & (long)0xFF);
		int index = (int) validData >> 2;
		return index;
	}
}
