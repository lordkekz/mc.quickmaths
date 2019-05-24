package de.lordkekz.mc.quickmaths;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class FileManager {
	private static FileConfiguration config;
	private static File configFile;
	private static FileConfiguration texts;
	private static File textsFile;
	private static String initializedLang;
	
	public static void setup(QuickmathsPlugin pl) {
		// load config
		configFile = new File("plugins/Quickmaths", "config.yml");
		configFile.getParentFile().mkdirs();
		config = YamlConfiguration.loadConfiguration(configFile);
		config.addDefault("language", "en");
		config.addDefault("enable", true);
		config.addDefault("reward", 10);
		config.addDefault("waitingTime", 20*60*20);
		config.addDefault("answerTime", 20*60*1);
		config.addDefault("maxNumberSize", 100);
		config.addDefault("minNumberSize", 0);
		config.addDefault("maxNumberCount", 4);
		config.addDefault("minNumberCount", 2);
		
		// load texts
		textsFile = new File("plugins/Quickmaths", config.getString("language")+".yml");
		if (!textsFile.exists()) {
			try {
				InputStream in = FileManager.class.getResource(config.getString("language")+".yml").openStream();
				Files.copy(in, textsFile.toPath());
			} catch (IOException e) {
				pl.getLogger().severe("Can't find and/or copy texts file for language: "+config.getString("language"));
				e.printStackTrace();
			}
		}
		texts = YamlConfiguration.loadConfiguration(textsFile);
		initializedLang = config.getString("language");
		
		// save config
		config.options().copyDefaults(true);
		saveConfig();
	}
	
    public static void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            Bukkit.getServer().getLogger().severe("Could not save file: config.yml");
        }
    }
    
    public static void saveAll() {
    	saveConfig();
    }
    
    public static void reloadConfig() {
		config = YamlConfiguration.loadConfiguration(configFile);
    }
    
    public static void reloadTexts() {
    	if (!initializedLang.equalsIgnoreCase(config.getString("language"))) {
    		textsFile = new File("plugins/Quickmaths", config.getString("language")+".yml");
    	}
		texts = YamlConfiguration.loadConfiguration(textsFile);
    }
    
    public static void reloadAll() {
    	reloadConfig();
    	reloadTexts();
    }
	
	public static FileConfiguration getConfig() {
		return config;
	}
	
	public static FileConfiguration getTexts() {
		return texts;
	}
}