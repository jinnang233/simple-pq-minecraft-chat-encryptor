package com.jn233.chatenc_mc;

import java.util.concurrent.Callable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

public class MessageSendingProgress implements Callable {
	private String session_id = "";
	private String sender = "";
	private int total = 0;
	private int current = 0;
	private int delay = 0;
	public void setTotal(int total) {
		this.total=total;
	}
	public void setCurrent(int current) {
		this.current=current;
	}
	public void setSender(String sender) {
		this.sender=sender;
	}
	public void setSessionId(String session_id) {
		this.session_id=session_id;
	}
	public void setDelay(int delay) {
		this.delay=delay;
	}
	public Object call() throws Exception {
		int current_progress = (int)(current*100/total);
		double remain_time = (total-current)*delay/1000;
		Text output_text = new LiteralText("[")
				.append(new LiteralText(session_id))
				.append(new LiteralText("]"))
				.append(new TranslatableText("general.jn233_mcchat_enc.sending"))
				.append(new LiteralText(":" + String.format("%d/%d %d%% (%f s)",current,total,current_progress,remain_time)));
		MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(output_text);

		return null;
	}

}
