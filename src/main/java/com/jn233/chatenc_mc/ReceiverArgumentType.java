package com.jn233.chatenc_mc;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

public class ReceiverArgumentType implements ArgumentType<String> {
	public static final DynamicCommandExceptionType INVALID_RECEIVER = new DynamicCommandExceptionType(o -> Text.literal("Invalid receiver: " + o));
	public static ReceiverArgumentType receiver() {
		return new ReceiverArgumentType();
	}
	public static <S> String getReceiver(CommandContext<S> context, String name) {
		return context.getArgument(name, String.class);
	}
	@Override
	public String parse(StringReader reader) throws CommandSyntaxException {
		MinecraftClient instance = MinecraftClient.getInstance();
		int argBeginning = reader.getCursor();
		if(!reader.canRead()) {
			reader.skip();
		}
		
	    while (reader.canRead() && (Character.isLetterOrDigit(reader.peek()) || reader.peek() == '-' || reader.peek()== '_')) {
	        reader.skip();
	    }
	    String receiver = reader.getString().substring(argBeginning, reader.getCursor());
	    
	    List<String> receiverList = PKStorageGlass.getAllPlayerNames(instance);
	    boolean receiver_existed = false;
	    for(String r : receiverList) {
	    	if(r.equals(receiver)) {
	    		receiver_existed=true;
	    	}
	    }
	    if(!receiver_existed)
	    	throw INVALID_RECEIVER.createWithContext(reader, "No such a receiver");
	    
	    return receiver;
	}
	
	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		MinecraftClient instance = MinecraftClient.getInstance();
		List<String> receiverList = PKStorageGlass.getAllPlayerNames(instance);
		return CommandSource.suggestMatching(receiverList,builder);
	}
}
