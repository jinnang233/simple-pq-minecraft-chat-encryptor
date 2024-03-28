package com.jn233.chatenc_mc;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.gradle.internal.serialize.InputStreamBackedDecoder;
import org.yaml.snakeyaml.Yaml;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.EntryListWidget;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;


public class EncryptorConfigurationScreen  {
	public ConfigBuilder builder;
	public int message_delay = 1500;
	public int cut_limit = 200;
	public boolean encrypted_echo = false;
	public final static String chat_regex_default = "\\<(.*)\\>(.*)";
	public String chat_regex = chat_regex_default; 
	
	
	public String configFileName = "config.yaml";
	

	
	public EncryptorConfigurationScreen() {
		readConfiguration();
		
	}
	private void storeConfiguration() {
		Yaml yaml = new Yaml();
		
		
		Configuration conf = new Configuration();
		conf.message_delay=this.message_delay;
		conf.cut_limit=this.cut_limit;
		conf.encrypted_echo=this.encrypted_echo;
		conf.chat_regex=this.chat_regex;
		
		MinecraftClient instance = MinecraftClient.getInstance();
		
		File configFile = PKStorageGlass.getFileObject(instance, configFileName);
		byte[] data = yaml.dumpAsMap(conf).getBytes();
		
		try {
			Files.write(Paths.get(configFile.getAbsolutePath()), data);
		} catch (IOException e) {
			ChatEnc.LOGGER.error(e.toString());
		}
		
		
	}
	
	
	private void readConfiguration() {
		Yaml yaml = new Yaml();
		
		
		MinecraftClient instance = MinecraftClient.getInstance();
		File configFile = PKStorageGlass.getFileObject(instance, configFileName);
		
		if(configFile.exists() && configFile.canRead()) {
			try {
				FileInputStream config = new FileInputStream(configFile.getAbsolutePath());
				Configuration conf = yaml.loadAs(config, Configuration.class);
				
				this.message_delay=conf.message_delay;
				this.cut_limit=conf.cut_limit;
				this.encrypted_echo=conf.encrypted_echo;
				this.chat_regex=conf.chat_regex;
			} catch (IOException e) {
				ChatEnc.LOGGER.error(e.getMessage());
			}
			
		}
	}
	public Screen makeScreen(Screen parent) {
		MinecraftClient instance = MinecraftClient.getInstance();
		this.builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(new TranslatableText("title.jn233_mcchat_enc.config"));
		ConfigCategory general = builder.getOrCreateCategory(new TranslatableText("category.jn233_mcchat_enc.general"));
		ConfigEntryBuilder entryBuilder = this.builder.entryBuilder();
		
		AbstractConfigListEntry delayField = entryBuilder.startIntField(new TranslatableText("option.jn233_mcchat_enc.message_delay"), message_delay)
				.setDefaultValue(1500)
				.setSaveConsumer(newValue -> message_delay = newValue)
				.build();
		AbstractConfigListEntry cutLimitField = entryBuilder.startIntField(new TranslatableText("option.jn233_mcchat_enc.cut_limit"), cut_limit)
				.setDefaultValue(200)
				.setSaveConsumer(newValue -> cut_limit = newValue)
				.build();
		AbstractConfigListEntry encryptedEchoField = entryBuilder.startBooleanToggle(new TranslatableText("option.jn233_mcchat_enc.encrypted_echo"), encrypted_echo)
				.setDefaultValue(false)
				.setSaveConsumer(newValue -> encrypted_echo = newValue)
				.build();
		AbstractConfigListEntry chatPatternField = entryBuilder.startStrField(new TranslatableText("option.jn233_mcchat_enc.chat_regex"), chat_regex)
				.setDefaultValue(chat_regex_default)
				.setSaveConsumer(newValue -> chat_regex = newValue)
				.build();
		AbstractConfigListEntry kemAlgShowField = entryBuilder.startStrField(new TranslatableText("option.jn233_mcchat_enc.kem_alg"), PQParser.param.getName())
				.build();
		AbstractConfigListEntry sigAlgShowField = entryBuilder.startStrField(new TranslatableText("option.jn233_mcchat_enc.sig_alg"), PQParser.sig_param)
				.build();
		
		List<String> pkList = PKStorageGlass.getAllPlayerNames(instance);
		AbstractConfigListEntry pkField = entryBuilder.startStrList(new TranslatableText("option.jn233_mcchat_enc.pk_list"), pkList)
				.setDeleteButtonEnabled(false)
				.setInsertButtonEnabled(false)
				.setInsertInFront(false)
				.build();
				
		general.addEntry(delayField);
		general.addEntry(cutLimitField);
		general.addEntry(encryptedEchoField);
		general.addEntry(chatPatternField);
		general.addEntry(kemAlgShowField);
		general.addEntry(sigAlgShowField);
		general.addEntry(pkField);
		builder.setSavingRunnable(()->{
			storeConfiguration();
		});
		Screen configScreen = builder.build();
		
		return configScreen;
	}
}
