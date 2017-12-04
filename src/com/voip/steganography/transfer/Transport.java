package com.voip.steganography.transfer;

import java.io.IOException;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.network.capturer.Capturer;
import com.network.capturer.Capturer.TPacket;
import com.network.capturer.PacketCapturerListener;
import com.voip.steganography.InPacketListener;
import com.voip.steganography.Manager;
import com.voip.steganography.SkypeCommunication;
import com.voip.steganography.packts.RtpRawPacket;

public class Transport {
	final public Logger logger = Logger.getLogger(Transport.class);
	private Capturer capturer = null;
	private PacketCapturerListener listener;
	private SkypeCommunication skypeComm;
	private LinkedList<TPacket> inFIFO = new LinkedList<TPacket>();
	private LinkedList<TPacket> outFIFO = new LinkedList<TPacket>();
	private List<InPacketListener> inPacketListener = new ArrayList<InPacketListener>();
	private boolean startFlag = false;
	
	RtpUdpSocket sendSocket  = null;
	RtpUdpSocket recSocket  = null;
	public final static int udpRecPort = 12346;
	public final static int udpSendPort = 12345;
	
	private RtpUdpSocket rtpSendSocket = null;
	private RtpUdpSocket rtpRecSocket = null;
	private RtpRecSocketListener rtpRecListener;

	public Transport(Capturer capturer){
		this.capturer = capturer;
		initTransport();
	}

	private void initTransport() {
		skypeComm = new SkypeCommunication();
		listener = new PacketCapturerListener(){

			@Override
			public void processOutPacket(TPacket p) {
				if (!startFlag){
					return;
				}
			
				if (skypeComm.newOutPacket(p.p)){
					// detected out packet;
//					if (outFIFO.size() > 4){
//						outFIFO.removeFirst();
//					}
					outFIFO.add(p);
					SimpleDateFormat   sDateFormat   =   new   SimpleDateFormat("yyyy-MM-dd   hh:mm:ss");     
					String   date   =   sDateFormat.format(new   java.util.Date());  
					Manager.getInstance().storeOutPacket(date, p.p.len);
				}
			}

			@Override
			public void processInPacket(TPacket p) {
				if (!startFlag){
					return;
				}
				if (skypeComm.newInPacket(p.p)){
					// detected in packet;
					inFIFO.add(p);
					
					SimpleDateFormat   sDateFormat   =   new   SimpleDateFormat("yyyy-MM-dd   hh:mm:ss");     
					String   date   =   sDateFormat.format(new   java.util.Date());  
					Manager.getInstance().storeInPacket(date, p.p.len);
					
					RtpRawPacket rtpPacket = new RtpRawPacket(new byte[p.p.data.length],
							p.p.data.length);
					rtpPacket.setPacket(p.p.data);
					
					for (InPacketListener listener : inPacketListener){
						listener.fireInPacket(rtpPacket);
					}
				}
//				if (skypeComm.newInRtpUdpPacket(p.p)){
//					for (InPacketListener listener : inPacketListener){
//						listener.fireInPacket(p);
//					}
//				}
			}
		};
		
		this.capturer.addListener(listener);
		
		try {
			rtpSendSocket = new RtpUdpSocket(udpSendPort);
			rtpRecSocket = new RtpUdpSocket(udpRecPort);
			rtpRecListener = new RtpRecSocketListener();
			RtpUdpRecProvider rtpUdpRecProvider = new RtpUdpRecProvider(rtpRecSocket, rtpRecListener);
			Thread rtpUdpRecProviderThread = new Thread(rtpUdpRecProvider);
			rtpUdpRecProviderThread.start();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	public void reInit(){
		skypeComm = new SkypeCommunication();
		startFlag  = false;
		inFIFO.clear();
		outFIFO.clear();
	}
	
	public synchronized void sendPacket(TPacket p){
		if (p.sender == null){
			logger.info("error: send packet.");
			return;
		}
		p.sender.sendPacket(p.p);
//		sendRtpPacket(p);
		
		logger.info("send packet success: " + p.p.len);
	}
	
	public synchronized void sendRtpPacket(TPacket p) {
		try {
			this.rtpSendSocket.send(p.getRtpUdpPacket());
		} catch (IOException e) {
			logger.info("sendRtpPacket fail " );
			e.printStackTrace();
		}
		SimpleDateFormat   sDateFormat   =   new   SimpleDateFormat("yyyy-MM-dd   hh:mm:ss");     
		String   date   =   sDateFormat.format(new   java.util.Date());  
		Manager.getInstance().storeUserOutPacket(date, p.p.data.length);
		logger.info("send packet success: " + p.p.len);
	}

	public TPacket pollInPacket(){
		TPacket packet = inFIFO.pollFirst();
		if (packet != null){
			logger.info("Poll one packet from inFIFO");
		}else{
			logger.info("Cannot poll packet: INFIFO is empty.");
		}
		return packet;
	}
	
	public void clearInFIFO(){
		inFIFO.clear();
	}
	
	public void clearOutFIFO(){
		outFIFO.clear();
	}
	
	public synchronized TPacket pollOutPacket(){
		TPacket packet = outFIFO.pollFirst(); 
//		TPacket packet = outFIFO.getFirst();
		if (packet != null){
			logger.info("Poll one packet from outFIFO");
		}else{
			logger.info("Cannot poll packet: outFIFO is empty.");
		}
		return packet;
	}
	
	public synchronized void registerInPacketListener(InPacketListener listener){
		this.inPacketListener.add(listener);
	}
	
	public synchronized void removeInPacketListener(InPacketListener listener){
		this.inPacketListener.remove(listener);
	}
	
	public synchronized void notifyAnalysis(boolean flag){
		this.startFlag = flag;
		if (!flag){
			skypeComm.clearInfo();
			clearInFIFO();
			clearOutFIFO();
		}
	}

	public synchronized void registerInPacketListener(RtpRecPacketHandler recHandler) {
		if (rtpRecListener != null){
			this.rtpRecListener.registerHandler(recHandler);
			logger.info("register listener success:" + this.rtpRecListener.handlers.size() + ", " + recHandler.toString());
		}else{
			logger.info("register listener error:" + this.rtpRecListener.handlers.size() + ", " + recHandler.toString());
		}
	}

	public synchronized void removeInPacketListener(RtpRecPacketHandler recHandler) {
		if (rtpRecListener != null){
			logger.info("register listener removed:" + this.rtpRecListener.handlers.size() + ", " + recHandler.toString());
			this.rtpRecListener.removeHandler(recHandler);
		}
	}
	
//	private boolean initRecvSocket() {
//		try {
//			recSocket = new RtpUdpSocket(inPort);
//		} catch (SocketException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			return false;
//		}
//		return true;
//	}
//	
//	private boolean intiSenderSocket() {
//		try {
//			sendSocket = new RtpUdpSocket(outPort);
//		} catch (SocketException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			return false;
//		}
//		return true;
//		
//	}
//	
//	public void clearRecvSenderSocket(){
//		recSocket.close();
//		sendSocket.close();
//	}

}
