
package com.voip.steganography.packts;

public class RtpAckPacket extends RtpControlPacket {
	private static final byte cmd = 0x03;

	public RtpAckPacket(byte[] data, int length) {
		super(data, length);
	}
	
	public RtpAckPacket(RtpRawPacket rtpPacket) {
		super(rtpPacket);
	}
	
	public static boolean isAckPacket(long timeStamp, int sn){
		byte tag = (byte)(timeStamp & (long)0x00000003);
		if (tag == RtpAckPacket.cmd && snCtl == sn){
			return true;
		}else{
			return false;
		}
	}
	
	//value = numgroupsize for start packet, value = index of group for subdata packet.
	public RtpAckPacket packet(int value){
		byte cmd = buildMsgStartCMD(value, RtpAckPacket.cmd);
		long newTimeStamp = setConvertRtpTimeStamp(timeStamp, cmd);
		packet.setSequenceNumber(snCtl);
		packet.setTimestamp(newTimeStamp);
		return this;
	}
	
	/*
	 * the value for the ack type. For example, if the ack msg to re-sent the whole group data, it will be 0.
	 */
	public int extractValue(){
		byte validData = (byte) (timeStamp & (long)0xFF);
		int value = (int) validData >> 2;
		return value;
	}
	
	/*
	 * if value =0, that means it is reject for start packet, or it asks to re-send for sub-data packet.
	 */
	public boolean isPerfect(){
		int value = extractValue();
		if (value != 0){
			return true;
		}else{
			return false;
		}
	}

}
