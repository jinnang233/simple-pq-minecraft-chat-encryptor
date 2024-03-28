package com.jn233.chatenc_mc;

public class CutSendImplements implements Runnable{

	private String message;
	private int cutLimit=120;
	private int delay=0;
	
	public void setMessage(String message) {
		this.message=message;
	}
	public void setCutLimit(int cutLimit) {
		this.cutLimit=cutLimit;
	}
	public void setDelay(int delay) {
		this.delay=delay;
	}
	public void run() {
		if(this.message!=null)
			ChatHandler.cutSend(this.message,this.cutLimit,this.delay);
	}

}
