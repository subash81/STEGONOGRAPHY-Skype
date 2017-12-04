
package com.voip.steganography.packts;

import org.apache.log4j.Logger;


public class RtpStartMsgPacket extends RtpControlPacket {
	private final static byte cmd = 0x01;
	
	private final static int startSN = 0;
	final public Logger logger = Logger.getLogger(RtpStartMsgPacket.class);
	public int extractNumGroups() {
		byte validData = (byte) (timeStamp & (long)0xFF);
		int numGroup = (int) validData >> 2;
		return numGroup;
	}

	public RtpStartMsgPacket(byte[] data, int length){
		super(data, length);
	}
	
	public RtpStartMsgPacket(RtpRawPacket rtpPacket) {
		super(rtpPacket);
	}

	public RtpStartMsgPacket packet(int numGroups) {
		byte cmd = buildMsgStartCMD(numGroups, RtpStartMsgPacket.cmd);
		long newTimeStamp = setConvertRtpTimeStamp(timeStamp, cmd);
		packet.setSequenceNumber(startSN);
		packet.setTimestamp(newTimeStamp);
		logger.info("RtpStartMsgPacket packet: cmd: " + (int)cmd + ", time:" + newTimeStamp);
		return this;
	}
	
	public static boolean isStartPacket(long timeStamp, int l){
		byte tag = (byte)(timeStamp & (long)0x00000003);
		if (tag == RtpStartMsgPacket.cmd && startSN == l){
			return true;
		}else{
			return false;
		}
	}

}
