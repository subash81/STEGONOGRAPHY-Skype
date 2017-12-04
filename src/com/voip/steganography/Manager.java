package com.voip.steganography;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.network.capturer.Capturer;
import com.skype.Friend;
import com.skype.SkypeException;
import com.voip.steganography.transfer.Transport;
import com.voip.ui.listener.StartStatus;
import com.voip.ui.listener.UIStatusListener;

public class Manager {
	
	private Capturer capturer;
	private Transport transport;
	private UIStatusListener uiListener;
	
	private SkypeControl skypeControl;
	
	private List<String> msgHist = new ArrayList<String>();
	
	private static Manager instance = null;
	
	private String inFileNameStr = "inPacketData.txt";
	private String outFileNameStr = "outPacketData.txt";
	
	private Logger logger = Logger.getLogger(Manager.class);
	private MsgSender msgSender;
	
	private MsgReceiver msgReceiver;
	private boolean synRecComplete = false;
	private boolean synSenderComplete = false;
	
	public static Manager getInstance(){
		if (instance == null){
			instance = new Manager();
		}
		return instance; 
	}
	
	public Manager(){
		initCapturer();
		initTransport();
		initSkypeControl();
	}
	
	private void initCapturer() {
		capturer = new Capturer();
	}

	private void initSkypeControl() {
		skypeControl = new SkypeControl(transport);
	}

	private void initTransport() {
		transport = new Transport(capturer);
	}
	
	public void setUIListener(UIStatusListener uiListener){
		this.uiListener = uiListener;
		skypeControl.registerCallStatusChangedListener(uiListener);
	}
	
	public List<Friend> getFriends(){
		try {
			return this.skypeControl.getAllFriends();
		} catch (SkypeException e) {
			logger.info("Error when geting friends' information: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}
		
	public void callFriend(Friend f){
		try {
			this.skypeControl.callFriend(f);
		} catch (SkypeException e) {
			e.printStackTrace();
			logger.info("Error when calling friend: " + e.getMessage());
		}
	}
	
	public void finishCall(){
		try {
			this.skypeControl.finishCall();
		} catch (SkypeException e) {
			logger.info("Error when fininshing calling friend: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	public void sendMsg(String msgStr){
		logger.info("Manager: new msg will be sent:" + msgStr);
		msgHist.add(msgStr);
		msgSender.sendMsg(msgStr);
	}
	
	public void receiveMsg(String msgStr){
		logger.info("Manager: receive msg:" + msgStr);
		uiListener.receiveMsg(msgStr);
	}
	
	public void clearSynSender(){
		this.synSenderComplete = false;
//		clearMsgSender();
	}
	
	public void clearSynRec(){
		this.synRecComplete = false;
//		clearMsgRec();
	}
	
	public void completeSynSender(){
		this.synSenderComplete = true;
		initMsgSender();
		
		uiListener.notifyStartStatus(StartStatus.Done);
	}
	
	public void completeSynRec(){
		this.synRecComplete = true;
		initMsgRec();
	}
	
	private void initMsgSender() {
		if (msgSender == null){
			msgSender = new MsgSender(transport);
		}else{
			msgSender.reInit();
		}
		
	}
	
	private void initMsgRec() {
		if (msgReceiver == null){
			msgReceiver = new MsgReceiver(transport);
		}else{
			msgReceiver.reInit();
		}
		
	}
	
	private void clearMsgSender(){
		if (msgSender != null){
			msgSender.exit();
		}
	}
	
	private void clearMsgRec(){
		if (msgSender != null){
			msgReceiver.exit();
		}
	}
	
	public void exit(){
		storeInfo();
		if (msgSender != null){
			msgSender.exit();
		}
		if (msgReceiver != null){
			msgReceiver.exit();
		}
		this.skypeControl.exit();
		this.capturer.exit();
	}
	
	private void storeInfo() {
		File file = new File(this.inFileNameStr);
		
		StringBuffer sb = new StringBuffer();
		if (inPacket != null){
			for (PacketInfo pI : inPacket){
				sb.append(pI.getType());
				sb.append(", ");
				sb.append(pI.getDate());
				sb.append(", ");
				sb.append(pI.size);
				sb.append("\n");
			}
			try {
				BufferedWriter output = new BufferedWriter(new FileWriter(file));
				String s = sb.toString();
				output.write(s);
				output.close();
			} catch (IOException e) {
				logger.info("Manager: write the in packet info error;" + e.getMessage());
				e.printStackTrace();
			}
		}
				
		File outFile = new File(this.outFileNameStr);
		
		if (outPacket != null){
			sb = new StringBuffer();
			for (PacketInfo pI : outPacket){
				sb.append(pI.getType());
				sb.append(", ");
				sb.append(pI.getDate());
				sb.append(", ");
				sb.append(pI.size);
				sb.append("\n");
			}
			try {
				BufferedWriter output = new BufferedWriter(new FileWriter(outFile));
				String s = sb.toString();
				output.write(s);
				output.close();
			} catch (IOException e) {
				logger.info("Manager: write the out packet info error;" + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	List<PacketInfo> outPacket = null;
	List<PacketInfo> inPacket = null;
	public void storeOutPacket(String date, int size) {
		if (outPacket == null){
			outPacket = new ArrayList<PacketInfo>();
		}
		PacketInfo packetInfo = new PacketInfo(size, 0, date);
		outPacket.add(packetInfo);
	}
	
	public void storeInPacket(String date, int size) {
		if (inPacket == null){
			inPacket = new ArrayList<PacketInfo>();
		}
		PacketInfo packetInfo = new PacketInfo(size, 0, date);
		inPacket.add(packetInfo);
	}
	
	public void storeUserOutPacket(String date, int size){
		if (outPacket == null){
			outPacket = new ArrayList<PacketInfo>();
		}
		PacketInfo packetInfo = new PacketInfo(size, 1, date);
		outPacket.add(packetInfo);
	}
	
	public void storeUserInPacket(String date, int size) {
		if (inPacket == null){
			inPacket = new ArrayList<PacketInfo>();
		}
		PacketInfo packetInfo = new PacketInfo(size, 1, date);
		inPacket.add(packetInfo);
	}
	
	private class PacketInfo{
		private int size;
		private int type;
		private String date;
		public int getSize() {
			return size;
		}
		public void setSize(int size) {
			this.size = size;
		}
		public int getType() {
			return type;
		}
		public void setType(int type) {
			this.type = type;
		}
		public String getDate() {
			return date;
		}
		public void setDate(String date) {
			this.date = date;
		}
		public PacketInfo(int size, int type, String date){
			this.size = size;
			this.type = type;
			this.date = date;
		}
	}
}
