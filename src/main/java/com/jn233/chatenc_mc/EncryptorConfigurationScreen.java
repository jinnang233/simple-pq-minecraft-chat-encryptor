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

import net.minecraft.text.Text;



public class EncryptorConfigurationScreen  {
	public ConfigBuilder builder;
	public int message_delay = 1500;
	public int cut_limit = 200;
	public boolean encrypted_echo = false;
	
	
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
			} catch (IOException e) {
				ChatEnc.LOGGER.error(e.getMessage());
			}
			
		}
	}
	public Screen makeScreen(Screen parent) {
		MinecraftClient instance = MinecraftClient.getInstance();
		this.builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(Text.translatable("title.jn233_mcchat_enc.config"));
		ConfigCategory general = builder.getOrCreateCategory(Text.translatable("category.jn233_mcchat_enc.general"));
		ConfigEntryBuilder entryBuilder = this.builder.entryBuilder();
		
		AbstractConfigListEntry delayField = entryBuilder.startIntField(Text.translatable("option.jn233_mcchat_enc.message_delay"), message_delay)
				.setDefaultValue(1500)
				.setSaveConsumer(newValue -> message_delay = newValue)
				.build();
		AbstractConfigListEntry cutLimitField = entryBuilder.startIntField(Text.translatable("option.jn233_mcchat_enc.cut_limit"), cut_limit)
				.setDefaultValue(200)
				.setSaveConsumer(newValue -> cut_limit = newValue)
				.build();
		AbstractConfigListEntry encryptedEchoField = entryBuilder.startBooleanToggle(Text.translatable("option.jn233_mcchat_enc.encrypted_echo"), encrypted_echo)
				.setDefaultValue(false)
				.setSaveConsumer(newValue -> encrypted_echo = newValue)
				.build();

		AbstractConfigListEntry kemAlgShowField = entryBuilder.startStrField(Text.translatable("option.jn233_mcchat_enc.kem_alg"), PQParser.param.getName())
				.build();
		AbstractConfigListEntry sigAlgShowField = entryBuilder.startStrField(Text.translatable("option.jn233_mcchat_enc.sig_alg"), PQParser.sig_param)
				.build();
		
		List<String> pkList = PKStorageGlass.getAllPlayerNames(instance);
		AbstractConfigListEntry pkField = entryBuilder.startStrList(Text.translatable("option.jn233_mcchat_enc.pk_list"), pkList)
				.setDeleteButtonEnabled(false)
				.setInsertButtonEnabled(false)
				.setInsertInFront(false)
				.build();
				
		general.addEntry(delayField);
		general.addEntry(cutLimitField);
		general.addEntry(encryptedEchoField);
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
