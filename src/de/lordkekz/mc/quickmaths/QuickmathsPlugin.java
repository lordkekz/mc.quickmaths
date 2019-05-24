package de.lordkekz.mc.quickmaths;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class QuickmathsPlugin extends JavaPlugin implements Listener {
	private QuickmathsProcessor processor;
	private boolean overrideOpenQuestion;
	
	@Override
	public void onEnable() {
		getLogger().fine("Registering listener...");
	    getServer().getPluginManager().registerEvents(this, this);
	    
		getLogger().fine("Setting up files...");
		FileManager.setup(this);

		processor = new QuickmathsProcessor(this);
		if (FileManager.getConfig().getBoolean("enable")) {
			getLogger().fine("Enabling quickmaths...");
			processor.schedule(FileManager.getConfig().getInt("waitingTime"));
		} else {
			getLogger().info("Quickmaths is disabled in config.");
			getLogger().info("To enable quickmaths, execute /quickmaths enable");
		}
	    
		getLogger().info("Quickmaths Plugin enabled.");
	}

	@Override
	public void onDisable() {
		
	}
	
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (processor!=null && processor.hasOpenQuestion()) {
            processor.processChatEvent(event);
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
		List<String> result = null;
    	if (cmd.getName().equalsIgnoreCase("quickmaths")) {
    		if (args.length==1) return Arrays.asList("enable", "disable", "ask");
    		
    		result = new ArrayList<String>();
			if (args[0].equals("ask") && args.length==2) result.add(FileManager.getTexts().getString("tabComplete.question"));
    		if (args[0].equals("ask") && args[args.length-2].endsWith("\"")) result.add(FileManager.getTexts().getString("tabComplete.answer"));
    	}
    	if (result==null) result = super.onTabComplete(sender, cmd, alias, args);
		while (result.contains(null)) result.remove(null);
		return result;
    }
    
    public boolean onCommand(CommandSender sender, Command cmd, String cmdLabel, String[] args) {
    	if (cmd.getName().equalsIgnoreCase("quickmaths") && args.length>0) {
	    	if ((args[0].equalsIgnoreCase("enable"))) {
	    		if (processor.isEnabled()) {
	        		sender.sendMessage(ChatColor.YELLOW+FileManager.getTexts().getString("command.alreadyEnabled"));
	    		} else {
	    			processor.schedule(0);
	    			FileManager.getConfig().set("enable", true);
	    			FileManager.saveConfig();
	        		sender.sendMessage(ChatColor.GREEN+FileManager.getTexts().getString("command.enabled"));
	    		}
	    		return true;
	    	} else if ((args[0].equalsIgnoreCase("disable"))) {
	    		if (!processor.isEnabled()) {
	        		sender.sendMessage(ChatColor.YELLOW+FileManager.getTexts().getString("command.alreadyDisabled"));
	    		} else {
	    			processor.cancel();
	    			FileManager.getConfig().set("enable", false);
	    			FileManager.saveConfig();
	        		sender.sendMessage(ChatColor.GREEN+FileManager.getTexts().getString("command.disabled"));
	    		}
	    		return true;
	    	} else if ((args[0].equalsIgnoreCase("ask"))) {
	    		if (processor.hasOpenQuestion() && !overrideOpenQuestion) {
	        		sender.sendMessage(ChatColor.RED+FileManager.getTexts().getString("command.hasOpenQuestion"));
	        		overrideOpenQuestion=true;
	    		} else if (args.length==1) {
	        		overrideOpenQuestion=false;
	    			processor.askAuto();
	    		} else if (args.length>=3) {
	        		overrideOpenQuestion=false;
	        		System.out.println(Arrays.toString(args));
	        		System.out.println("tsts");
	        		int i=1;
	        		if (args[i].charAt(0)!='"') return false;
	        		StringBuilder qu = new StringBuilder(args[i]).deleteCharAt(0);
	        		if (args[i].charAt(args[i].length()-1)=='"') {
	        			qu.deleteCharAt(qu.length()-1);
	        			i++;
	        		} else for (i++; i<args.length; i++) {
	        			System.out.println(i);
	        			qu.append(' ').append(args[i]);
	        			if (args[i].charAt(args[i].length()-1)=='"') {
	        				qu.deleteCharAt(qu.length()-1);
	        				i++;
	        				break;
	        			}
	        		}
	        		System.out.println(qu);

	        		if (args[i].charAt(0)!='"') return false;
	        		StringBuilder ans = new StringBuilder(args[i]).deleteCharAt(0);
	        		if (args[i].charAt(args[i].length()-1)=='"') {
	        			ans.deleteCharAt(ans.length()-1);
	        			i++;
	        		} else for (i++; i<args.length; i++) {
	        			System.out.println(i);
	        			ans.append(' ').append(args[i]);
	        			if (args[i].charAt(args[i].length()-1)=='"') {
	        				ans.deleteCharAt(ans.length()-1);
	        				i++;
	        				break;
	        			}
	        		}
	        		System.out.println(ans);
	    			processor.ask(qu.toString().trim(), ans.toString().trim(), 0);
	    		} else {
	    			return false;
	    		}
	    		return true;
	    	}
    	}
    	return false;
    }
}