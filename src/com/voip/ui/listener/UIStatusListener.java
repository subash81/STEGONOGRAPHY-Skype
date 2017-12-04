
package com.voip.ui.listener;

import org.apache.log4j.Logger;

import com.skype.Call.Status;
import com.voip.ui.MainFrame;

public class UIStatusListener {
	private Logger logger = Logger.getLogger(UIStatusListener.class);
	public MainFrame mainFrame;
	public Status callStatus;
	
	public UIStatusListener(MainFrame mainFrame){
		this.mainFrame = mainFrame;
	}
	
	public void fireCallReceived(com.skype.Call.Status status, String callID){
		logger.info("fireCallReceived:" + callID + ":" + status.toString());
		if (status == Status.RINGING ){
			this.mainFrame.newCallRec(status.toString(), callID);
			notifyStartStatus(StartStatus.Waiting);
		}else if (status == Status.INPROGRESS){
			notifyStartStatus(StartStatus.Synchornizing);
			this.mainFrame.newCallRecInProgress(status.toString(), callID);
		}else{
			this.mainFrame.startStatus = StartStatus.Waiting;
			this.mainFrame.newCallRecFinish(status.toString(), callID);
		}
		this.callStatus = status;
	}
	
	public void fireCallStatusChanged(com.skype.Call.Status status, String callID){
		logger.info("fireCallStatusChanged:" + callID + ":" + status.toString());
		if (status == Status.MISSED || status == Status.FAILED || status == Status.FINISHED 
				|| status == Status.CANCELLED || status == Status.REFUSED ){
			this.mainFrame.startStatus = StartStatus.Waiting;
			this.mainFrame.newCallMakedDone(status.toString(), callID);
		}else if (status == Status.INPROGRESS || status == Status.RINGING){
			this.mainFrame.newCallMakedInProgress(status.toString(), callID);
		}else{
			this.mainFrame.startStatus = StartStatus.Waiting;
			this.mainFrame.newCallMakedFinish(status.toString(), callID);
		}
		if (status == Status.RINGING){
			notifyStartStatus(StartStatus.Waiting);
		}
		if (status == Status.INPROGRESS){
			notifyStartStatus(StartStatus.Synchornizing);
		} 
		
		this.callStatus = status;
	}
	
	public void receiveMsg(String recMsgStr){
		this.mainFrame.displayOtherMsg(recMsgStr);
	}
	
	public void notifyStartStatus(StartStatus status){
		logger.info("capture status changed:" + status.toString() );
		
		if (this.mainFrame.startStatus == StartStatus.Synchornizing && status == StartStatus.Done){
			this.mainFrame.sendButton.setEnabled(true);
			String msg = "Synchornized! Now you can send your secret messages!";
			logger.info(msg);
			this.mainFrame.displayAsHistoryMsg(msg);
			this.mainFrame.startStatus = status;
		}else if ( status == StartStatus.Waiting){
			String msg = "Connecting...";
			this.mainFrame.displayAsHistoryMsg(msg);
			this.mainFrame.startStatus = status;
			this.mainFrame.sendButton.setEnabled(false);
		}else if (this.mainFrame.startStatus == StartStatus.Waiting && status == StartStatus.Synchornizing){
			String msg = "Synchornizing...";
			this.mainFrame.displayAsHistoryMsg(msg);
			this.mainFrame.startStatus = status;
		}else{
			logger.info("Unknown StartStatus.");
		}
	}
	
}
