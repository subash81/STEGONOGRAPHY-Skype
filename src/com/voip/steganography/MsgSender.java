package com.voip.steganography;

import java.util.LinkedList;

import org.apache.log4j.Logger;

import com.voip.steganography.transfer.MsgTransfer;
import com.voip.steganography.transfer.Transport;

public class MsgSender {
	private Logger logger = Logger.getLogger(MsgSender.class);
	private LinkedList<String> msgFIFO = new LinkedList<String>();
	private MsgDispatcher disPatcher;
	private MsgTransfer msgTransfer;
	private Thread msgDispatcherThread;
	
	public MsgSender(Transport transport){
		msgTransfer = new MsgTransfer(transport);
		disPatcher = new MsgDispatcher();
		msgDispatcherThread = new Thread(disPatcher, "com.voip.steganography.Protocol.MsgDispatcher");
        msgDispatcherThread.start();
	}
	
	public void sendMsg(String msgStr){
		synchronized (msgFIFO)
        {
			msgFIFO.addFirst(msgStr);
			msgFIFO.notify();
		}
		logger.info("New msg has been put into the FIFO list: " + msgStr);
	}
	
	private class MsgDispatcher extends Thread
    {
		boolean isRunning = true;
        
        public MsgDispatcher(){
		}
		
        public void run()
        {
            while(isRunning)
            {
                synchronized (msgFIFO) {
                    if(msgFIFO.isEmpty())
                        try {
                        	msgFIFO.wait();
                        }
                        catch (InterruptedException ex) {
                        	logger.info("Error: MsgDispatcher thread Exit");
                            return;
                        }
                }
//                if(msgFIFO.isEmpty())
//                    continue;
                
                String msgStr = msgFIFO.pollFirst();
                logger.info("Dispatcher: poll one data :" + msgStr);
                if (!msgTransfer.sendMsnData(msgStr)){
                	msgFIFO.addLast(msgStr);
                	msgFIFO.notify();
                	logger.info("Send msg fail: add to the FIFO again. FIFO size:" + msgFIFO.size());
                }else{
                	logger.info("Send msg success: " + msgStr);
                }
            }
        }
    }
	
	public void exit(){
		msgTransfer.exit();
		disPatcher.isRunning = false;
	}

	public void reInit() {
		synchronized (msgFIFO)
        {
			msgFIFO.clear();
		}
		
		msgTransfer.reInit();
	}
}
