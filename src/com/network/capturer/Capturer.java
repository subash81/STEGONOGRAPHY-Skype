package com.network.capturer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jpcap.JpcapCaptor;
import jpcap.JpcapSender;
import jpcap.NetworkInterface;
import jpcap.PacketReceiver;
import jpcap.packet.Packet;
import jpcap.packet.TCPPacket;
import jpcap.packet.UDPPacket;

import org.apache.log4j.Logger;

import com.voip.steganography.packts.IpAddress;
import com.voip.steganography.packts.RtpUdpPacket;
import com.voip.steganography.transfer.Transport;

public class Capturer{

	private Logger logger = Logger.getLogger(Capturer.class);

	private InetAddress ownIP;

	Map<JpcapCaptor, Thread> captors = new HashMap<JpcapCaptor, Thread>();
	
	Map<JpcapCaptor, JpcapSender> captorSender = new HashMap<JpcapCaptor, JpcapSender>();
	
	public Capturer() {
		initCapturer();
	}
	
	public void initCapturer() {
		try {
			ownIP = InetAddress.getLocalHost();
			logger.info("ownIP:" + ownIP.getHostAddress());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		NetworkInterface[] devices = JpcapCaptor.getDeviceList();
		
		for (int i=0;i<devices.length;i++){
			try {
				JpcapCaptor captor = JpcapCaptor.openDevice(devices[i], 65535, false, 20);
				JpcapSender jpcapSender = captor.getJpcapSenderInstance();
				DeviceCapturer deviceCapturer = new DeviceCapturer(this, captor);
				Thread thread = new Thread(deviceCapturer);
				thread.start();
				this.captors.put(captor, thread);
				this.captorSender.put(captor, jpcapSender);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				continue;
			}
		}
	}
	
	private final List<PacketCapturerListener> listeners = new ArrayList<PacketCapturerListener>();
	
	public void addListener(PacketCapturerListener listener){
		logger.info("Captuer: One packet processor lister has been register");
		listeners.add(listener);
	}
	
	public void removeListener(PacketCapturerListener listener){
		listeners.remove(listener);
	}
	
	private void dispatchOutPacket(TPacket packet) {
		for (PacketCapturerListener listener : listeners){
			listener.processOutPacket(packet);
		}
	}
	
	private void dispatchInPacket(TPacket packet) {
		for (PacketCapturerListener listener : listeners){
			listener.processInPacket(packet);
		}
	}
	
	public synchronized void receivePacket(Packet p, JpcapCaptor captor) {
		JpcapSender sender = this.captorSender.get(captor);
		TPacket tPacket = new TPacket(p, sender);
		
		if (p instanceof TCPPacket) {
			TCPPacket tcpPacket = (TCPPacket) p;
			logger.info("Captuered one TCP Data! " + ":" 
					+ tcpPacket.src_ip + ":"
					+ tcpPacket.src_port + "-->"
					+ tcpPacket.dst_ip + ":"
					+ tcpPacket.dst_port + ", length:"
					+ tcpPacket.len
					);
			if (tcpPacket.src_ip.getHostAddress().equalsIgnoreCase(ownIP.getHostAddress())){
				dispatchOutPacket(tPacket);
			}
			if (tcpPacket.dst_ip.getHostAddress().equalsIgnoreCase(ownIP.getHostAddress())){
				dispatchInPacket(tPacket);
			}

		} else if (p instanceof UDPPacket) {
			UDPPacket udpPacket = (UDPPacket) p;
			logger.info("Captuered one UDP Data! " + ":" 
					+ udpPacket.src_ip + ":"
					+ udpPacket.src_port + "-->"
					+ udpPacket.dst_ip + ":"
					+ udpPacket.dst_port + ", length:"
					+ udpPacket.len 
					);
			if (udpPacket.src_ip.getHostAddress().equalsIgnoreCase(ownIP.getHostAddress())){
				dispatchOutPacket(tPacket);
			}
			if (udpPacket.dst_ip.getHostAddress().equalsIgnoreCase(ownIP.getHostAddress())){
				dispatchInPacket(tPacket);
			}
		} else {
			logger.info("Captuered one Data (not TCP or UDP)!");
		}
	}
	
	public class DeviceCapturer implements Runnable{
		
		private Capturer capturer;
		private JpcapCaptor captor;

		public DeviceCapturer(Capturer capturer, JpcapCaptor captor){
			this.capturer = capturer;
			this.captor = captor;
		}
		
		@Override
		public void run() {
			if (captor != null){
				captor.loopPacket(-1, new PacketHandler(capturer, captor));
			}
		}
	}

	private class PacketHandler implements PacketReceiver {
		private Capturer capturer;
		private JpcapCaptor captor;
		public PacketHandler(Capturer capturer, JpcapCaptor captor){
			this.capturer = capturer;
			this.captor = captor;
		}
		@Override
		public void receivePacket(Packet p) {
			this.capturer.receivePacket(p, captor);
		}
	}

	public void exit() {
		Iterator<JpcapCaptor> iter = captors.keySet().iterator();
		while (iter.hasNext()){
			JpcapCaptor item = iter.next();
			item.breakLoop();
			item.close();
			Thread thread = captors.get(item);
			thread.interrupt();
			try {
				thread.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public class TPacket{
		public Packet p;
		public JpcapSender sender;
		private int srcPort;
		private int desPort;
		private byte[] datas;
		
		public TPacket(Packet p, JpcapSender sender){
			this.p = p;
			this.sender = sender;
		} 
		
		public TPacket(Packet p, JpcapSender sender, int srcPort, int desPort){
			this.p = p;
			this.sender = sender;
			this.srcPort = srcPort;
			this.desPort = desPort;
		}
		
		public TPacket rebuild(byte[] dataBytes){
			this.p.data = dataBytes;
			this.datas = dataBytes;
			return this;
		}
		
		public RtpUdpPacket getRtpUdpPacket(){
			IpAddress ipAddress = null;
			if (p instanceof TCPPacket){
				ipAddress = new IpAddress(((TCPPacket)p).dst_ip.getHostAddress());
			}else if (p instanceof UDPPacket){
				ipAddress = new IpAddress(((UDPPacket)p).dst_ip.getHostAddress());
			}
			
			RtpUdpPacket newUdpPacket = new RtpUdpPacket(this.datas , this.datas.length, ipAddress, Transport.udpRecPort);
			
			return newUdpPacket; 
		}
	}
	
}
