
package com.voip.steganography.packts;

import com.voip.steganography.transfer.MsgTransfer;

public class RtpDataPacket extends RtpControlPacket {
	private static final long cmd = (long)0xFFFFFFFF;
	
	public RtpDataPacket(byte[] data, int length){
		super(data, length);
	}
	
	public RtpDataPacket(RtpRawPacket rtpPacket) {
		super(rtpPacket);
	}
	
	public RtpDataPacket packet(byte[] validData, int index){
//		long timeStamp = packet.getTimestamp();
//		long newTimeStamp = (timeStamp & (long)0xFFFFFF00) | (long)validData;
//		packet.setTimestamp(newTimeStamp);
		packet.setSequenceNumber(index);
		packet.setTimestamp(cmd);
		packet.setPayload(validData, validData.length);
		return this;
	}
	
	public byte[] extractValidData(){
//		long timeStamp = packet.getTimestamp();
//		byte validData = (byte)(timeStamp & (long)0x000000FF);
//		return validData;
		return packet.getPayload();
	}
	
	public int extractSN(){
		return this.packet.getSequenceNumber();
	}
	
	public static boolean isDataPacket(long time, int sn){
//		if (snCtl != sn){
//			return true;
//		}else{
//			return false;
//		}
		byte tag = (byte)(time & (long)0x000000FF);
		if (tag == cmd && (sn <= MsgTransfer.sizeGroup && sn > 0)){
			return true;
		}else{
			return false;
		}
	}

}
