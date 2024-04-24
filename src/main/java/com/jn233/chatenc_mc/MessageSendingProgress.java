package com.jn233.chatenc_mc;

import java.util.concurrent.Callable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;


public class MessageSendingProgress implements Callable<Object> {
	private String session_id = "";
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
		Text output_text = Text.literal("[")
				.append(Text.literal(session_id))
				.append(Text.literal("]"))
				.append(Text.translatable("general.jn233_mcchat_enc.sending"))
				.append(Text.literal(":" + String.format("%d/%d %d%% (%f s)",current,total,current_progress,remain_time)));
		MinecraftClient instance = MinecraftClient.getInstance();
		if(Configuration.progress_bar)instance.inGameHud.getChatHud().addMessage(output_text);
		return null;
	}
	public void finish() {
		//MinecraftClient instance = MinecraftClient.getInstance();
		//if(Configuration.progress_bar)instance.inGameHud.getChatHud().addMessage(Text.translatable("general.jn233_mcchat_enc.message_sent"));
	}

}
