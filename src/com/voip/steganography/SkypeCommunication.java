package com.voip.steganography;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import jpcap.packet.Packet;
import jpcap.packet.TCPPacket;
import jpcap.packet.UDPPacket;

import org.apache.log4j.Logger;

import com.network.capturer.IPInfo;

public class SkypeCommunication {
	
	private Logger logger = Logger.getLogger(SkypeCommunication.class);
	
	private boolean inFlag = false;
	
	private boolean outFlag = false;
	
	private int sizeThre = 100;
	
	private int maxSizeThre = 700;
	
	private int timeThre = 10;
	
	private Map<PacketAddr, Integer> inCands = new HashMap<PacketAddr, Integer>();
	
	private Map<PacketAddr, Integer> outCands = new HashMap<PacketAddr, Integer>();
	
	private PacketAddr inPacket = null;
	
	private PacketAddr outPacket = null;
	
	
	public SkypeCommunication(){
		
	}
	
//	public boolean newInRtpUdpPacket(Packet packet){
//		if (packet == null){
//			return false;
//		}
//		IPInfo ipInfoSrc = null;
//		if (packet instanceof UDPPacket ){
//			UDPPacket udpPacket = (UDPPacket) packet;
//			ipInfoSrc = new IPInfo(udpPacket.src_ip.getHostAddress(), udpPacket.src_port);
//			if (inFlag && ipInfoSrc.getIp().equalsIgnoreCase(inPacket.srcIP.getIp()) && ipInfoSrc.getPort() == this.outPort){
//				return true;
//			}
//		}
//		return false;
//	}
	
	public boolean newInPacket(Packet packet){
		if (packet == null){
			return false;
		}
		IPInfo ipInfoSrc = null;
		IPInfo ipInfoDes = null;
		if (packet instanceof UDPPacket ){
			UDPPacket udpPacket = (UDPPacket) packet;
			ipInfoSrc = new IPInfo(udpPacket.src_ip.getHostAddress(), udpPacket.src_port);
			ipInfoDes = new IPInfo(udpPacket.dst_ip.getHostAddress(), udpPacket.dst_port);
		}else if (packet instanceof TCPPacket ){
//			TCPPacket tcpPacket = (TCPPacket) packet;
//			ipInfoSrc = new IPInfo(tcpPacket.src_ip.getHostAddress(), tcpPacket.src_port);
//			ipInfoDes = new IPInfo(tcpPacket.dst_ip.getHostAddress(), tcpPacket.dst_port);
			return false;
		}
		PacketAddr newPacket = new PacketAddr(ipInfoSrc, ipInfoDes);
		
		if (outFlag && inFlag && newPacket.isEqual(inPacket)){
			return true;
		}else if (!inFlag ){
			if (packet.len > sizeThre && packet.len < maxSizeThre){
				Integer time = addInPacketTimes(newPacket);
				logger.info("The time of new inpacket:" + time + ", srcip:" + newPacket.srcIP.getIp());
				if (time > timeThre){
					inFlag = true;
					inPacket = newPacket;
					logger.info("The in packet address is detected: srcIp: " + ipInfoSrc.getIp() + "  port:" + ipInfoSrc.getPort() + " ,desIp:" + ipInfoDes.getIp() + "  port:" + ipInfoDes.getPort());
					checkFlag();
//					return flag;
					return false;
				}
			}
		}
		
		return false;
	}
	
	public boolean newOutPacket(Packet packet){
		if (packet == null){
			return false;
		}
		IPInfo ipInfoSrc = null;
		IPInfo ipInfoDes = null;
		if (packet instanceof UDPPacket ){
			UDPPacket udpPacket = (UDPPacket) packet;
			ipInfoSrc = new IPInfo(udpPacket.src_ip.getHostAddress(), udpPacket.src_port);
			ipInfoDes = new IPInfo(udpPacket.dst_ip.getHostAddress(), udpPacket.dst_port);
		}else if (packet instanceof TCPPacket ){
//			TCPPacket tcpPacket = (TCPPacket) packet;
//			ipInfoSrc = new IPInfo(tcpPacket.src_ip.getHostAddress(), tcpPacket.src_port);
//			ipInfoDes = new IPInfo(tcpPacket.dst_ip.getHostAddress(), tcpPacket.dst_port);
			return false;
		}
		PacketAddr newPacket = new PacketAddr(ipInfoSrc, ipInfoDes);
		
		if (inFlag && outFlag && newPacket.isEqual(outPacket)){
			return true;
		}else if (!outFlag ){
			if (packet.len > sizeThre && packet.len < maxSizeThre){
				Integer time = addOutPacketTimes(newPacket);
				logger.info("The time of new outpacket:" + time + ", desip:" + newPacket.desIP.getIp());
				if (time > timeThre){
					outFlag = true;
					outPacket = newPacket;
					checkFlag();
					logger.info("The out packet address is detected: srcIp: " + ipInfoSrc.getIp() + "  port:" + ipInfoSrc.getPort() + " ,desIp:" + ipInfoDes.getIp() + "  port:" + ipInfoDes.getPort());
					return false;
				}
			}
		}
		return false;
	}
	
	private boolean checkFlag() {
		if (outPacket == null || inPacket == null){
			return true;
		}
		if (!outPacket.srcIP.isEqual(inPacket.desIP) || !outPacket.desIP.isEqual(inPacket.srcIP)){
			clearInfo();
			return false;
		}else{
			return true;
		}
		
	}

	private Integer addInPacketTimes(PacketAddr newPacket){
		Iterator<PacketAddr> iter = inCands.keySet().iterator();
		while (iter.hasNext()){
			PacketAddr ip = iter.next();
			if (ip.isEqual(newPacket)){
				Integer time = inCands.get(ip);
				time = time + 1;
				inCands.put(ip, time);
				return time;
			}
		}
		Integer time = 1;
		inCands.put(newPacket, time);
		return time;
	}
	
	private Integer addOutPacketTimes(PacketAddr newPacket){
		Iterator<PacketAddr> iter = outCands.keySet().iterator();
		while (iter.hasNext()){
			PacketAddr ip = iter.next();
			if (ip.isEqual(newPacket)){
				Integer time = outCands.get(ip);
				time = time + 1;
				outCands.put(ip, time);
				return time;
			}
		}
		Integer time = 1;
		outCands.put(newPacket, time);
		return time;
	}
	
	public void clearInfo(){
		inFlag = false;
		
		outFlag = false;
		
		inCands = new HashMap<PacketAddr, Integer>();
		
		outCands = new HashMap<PacketAddr, Integer>();
		
		inPacket = null;
		
		outPacket = null;
	}
	
	public class PacketAddr{
		private IPInfo srcIP;
		private IPInfo desIP;
		
		public PacketAddr(IPInfo srcIP, IPInfo desIP){
			this.srcIP = srcIP;
			this.desIP = desIP;
		}
		
		public boolean isEqual(PacketAddr packet){
			if (packet == null){
				return false;
			}
			if (packet.srcIP.isEqual(srcIP) && packet.desIP.isEqual(desIP)){
				return true;
			}
			return false;
		}
	}
	
	
	
}
