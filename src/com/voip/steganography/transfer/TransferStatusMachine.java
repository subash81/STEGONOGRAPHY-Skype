
package com.voip.steganography.transfer;

import org.apache.log4j.Logger;

public final class TransferStatusMachine {
	private Logger logger = Logger.getLogger(TransferStatusMachine.class);
	public final int INIT = 0;
	public final int CLOSED = -1;
	public final int START = -2;
	public final int START_SUBDATA = -3;  
	
	private int state = CLOSED;
	
	public void touch(){
		switch (state){
		case INIT:
			setstate(CLOSED);
			break;
		case CLOSED:
			setstate(START);
			break;
		case START:
			setstate(START_SUBDATA);
			break;
		case START_SUBDATA:
			setstate(CLOSED);
			break;
		}
		logger.info("status has been changed to:" + getStatus());
	}

	private void setstate(int state) {
		this.state = state;
	}
	
	public String getStatus(){
		switch (state)  
        {  
            case START:  
                return "START_HEAD";  
            case START_SUBDATA:  
                return "START_SUBDATA"; 
            case CLOSED:  
                return "CLOSED";
            default:  
                return "INIT";  
        } 
	}
	
	public int getStatusID(){
		return state;
	}
}
