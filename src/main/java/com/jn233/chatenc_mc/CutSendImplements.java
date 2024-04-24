package com.jn233.chatenc_mc;

public class CutSendImplements implements Runnable{

	private String message;
	private int cutLimit=120;
	private int delay=0;
	private EncCallback callback=null;
	
	private cutSendCallback on_sending = null;
	
	interface EncCallback {
		void onCallback();
	}
	interface cutSendCallback{
		void onSending(String message, int cutLimit, int delay);
	}
	
	
	public void setCallback(EncCallback callback) {
		this.callback=callback;
	}
	public void onSending(cutSendCallback callback) {
		this.on_sending=callback;
	}
	
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
		if(this.message!=null && this.on_sending!=null)
			this.on_sending.onSending(this.message,this.cutLimit,this.delay);
		if(this.callback!=null)this.callback.onCallback();
	}

}
