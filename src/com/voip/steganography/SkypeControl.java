package com.voip.steganography;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.skype.Call;
import com.skype.Call.Status;
import com.skype.CallAdapter;
import com.skype.CallStatusChangedListener;
import com.skype.ContactList;
import com.skype.Friend;
import com.skype.Skype;
import com.skype.SkypeException;
import com.voip.steganography.transfer.Transport;
import com.voip.ui.listener.UIStatusListener;

public class SkypeControl {
	final public Logger logger = Logger.getLogger(SkypeControl.class);
	private Call skypeCaller = null;
	
	private long timeThread = 100;
	
	private UIStatusListener callStatChanged = null;
	private SynSender synSender;
	private Transport transport;
	private SynRec synRec; 
	
	public SkypeControl(Transport transport){
		this.transport = transport;
		initSkypeListener();
	}
	
	public void registerCallStatusChangedListener(UIStatusListener callStatus){
		this.callStatChanged = callStatus;
	}
	
	private void initSkypeListener(){
		try {
			Skype.addCallListener(new CallAdapter() {
			    @Override
			    public void callMaked(Call makedCall) throws SkypeException {
			    	final String callID = makedCall.getId();
			    	skypeCaller = makedCall;
			        makedCall.addCallStatusChangedListener(new CallStatusChangedListener() {
			        	@Override
						public void statusChanged(com.skype.Call.Status status)
								throws SkypeException {
							logger.info("MakedCall: The status of caller is changed as: " + status.toString());
							if (callStatChanged != null){
								callStatChanged.fireCallStatusChanged(status, callID);
							}else{
								logger.info("Error: there is no ui status listener");
							}
			            	if (status == Status.INPROGRESS){
			            		transport.notifyAnalysis(true);
			            		initSyner();
			            	}else{
			            		transport.notifyAnalysis(false);
			            		clearSyner();
			            	}
						}
			        });
			    }
			    
			    @Override
			    public void callReceived(Call receivedCall) throws SkypeException {
			    	final String callID = receivedCall.getId();
			    	skypeCaller = receivedCall;
			    	receivedCall.addCallStatusChangedListener(new CallStatusChangedListener(){

						@Override
						public void statusChanged(Status status)
								throws SkypeException {
							logger.info("One call received, from: " + callID);
			            	callStatChanged.fireCallReceived(status, callID);
			            	if (status == Status.INPROGRESS){
			            		transport.notifyAnalysis(true);
			            		initSyner();
			            	}else{
			            		transport.notifyAnalysis(false);
			            		clearSyner();
			            	}
						}
			    		
			    	});
			    }
			    
			});
		} catch (SkypeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public List<Friend> getAllFriends() throws SkypeException{
		ContactList list = Skype.getContactList();
        Friend fr[] = list.getAllFriends();
        List<Friend> friendsList = new ArrayList<Friend>();
        for(int i=0; i < fr.length; i++)
        {
              Friend f = fr[i];
              friendsList.add(f);
              sleep();
        }
        return friendsList;
	}
	
	private void sleep(){
		try {
			Thread.sleep(timeThread);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private void initSyner(){
		if (this.synSender == null){
			this.synSender = new SynSender(transport);
			Thread synSenderThread = new Thread(this.synSender, "com.voip.steganography.SkypeControl");
			synSenderThread.start();
	    }
		if (this.synRec == null){
			this.synRec = new SynRec(transport);
		}
	}
	
	private void clearSyner(){
		if (this.synSender != null){
			this.synSender.clear();
			this.synSender = null;
		}
		if (this.synRec != null){
			this.synRec.clear();
			this.synRec = null;
		}
	}
	
	public void callFriend(Friend f) throws SkypeException{
//		Status status = skypeCaller.getStatus();
//		if (status == Status.RINGING || status == Status.ONHOLD || status == Status.ROUTING || status == Status.INPROGRESS){
//			return;
//		}
		
		skypeCaller = f.call();
		File file = new File("output.wav");
		skypeCaller.setFileOutput(file);
		skypeCaller.setFileCaptureMic(new File("outputMic.wav"));
//		initSyner();
	}
	
	public void finishCall() throws SkypeException{
		if (skypeCaller != null){
			Status curStatus = skypeCaller.getStatus();
			if (curStatus != Status.FINISHED && curStatus != Status.CANCELLED){
				skypeCaller.finish();
				skypeCaller.clearFileCaptureMic();
				skypeCaller.clearFileOutput();
			}
		}
	}
	
	public void acceptRecCall() throws SkypeException{
		skypeCaller.answer();
//		initSyner();
	}
	
	public void rejectRecCall() throws SkypeException{
		skypeCaller.cancel();
//		clearSyner();
	}
	
	public void exit(){
		try {
			finishCall();
		} catch (SkypeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		clearSyner();
	}
	
}
