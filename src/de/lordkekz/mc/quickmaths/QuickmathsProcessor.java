package de.lordkekz.mc.quickmaths;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class QuickmathsProcessor {
	private String question;
	private String solution;
	private long askTime;
	private QuickmathsPlugin pl;
	private boolean enabled;

	public QuickmathsProcessor(QuickmathsPlugin pl) {
		this.pl = pl;
	}
	
	private void makeQuestion() {
		Random r = new Random();
		int numCount = r.nextInt(FileManager.getConfig().getInt("maxNumberCount")-FileManager.getConfig().getInt("minNumberCount"))+FileManager.getConfig().getInt("minNumberCount");
		StringBuilder sb = new StringBuilder();
		int lastNum = r.nextInt(2*FileManager.getConfig().getInt("maxNumberSize")-FileManager.getConfig().getInt("minNumberSize"))+FileManager.getConfig().getInt("minNumberSize")-FileManager.getConfig().getInt("maxNumberSize");
		sb.append(lastNum);
		int sol = lastNum;
		boolean multiplyable = true;
		for (int i=0;i<numCount; i++) {
			int num = r.nextInt(2*FileManager.getConfig().getInt("maxNumberSize")-FileManager.getConfig().getInt("minNumberSize"))+FileManager.getConfig().getInt("minNumberSize")-FileManager.getConfig().getInt("maxNumberSize");
			if (num!=0 && lastNum%num == 0) {
				sol -= lastNum;
				lastNum /= num;
				sol += lastNum;
				sb.append(" / ").append(num);
				multiplyable=false;
			} else if (r.nextBoolean() && multiplyable) {
				num = num/10+1;
				sol -= lastNum;
				sol += lastNum *= num;
				sb.append(" * ").append(num);
				multiplyable=true;
			} else {
				sol += lastNum = num;
				if (r.nextBoolean()) sb.append(" - ").append(-num);
				else sb.append(" + ").append(num);
				multiplyable=true;
			}
		}
		question = sb.toString();
		solution = Integer.toString(sol);
	}

	public boolean hasOpenQuestion() {
		return question!=null;
	}
	
	public boolean isEnabled() {
		return enabled;
	}

	public void processChatEvent(AsyncPlayerChatEvent event) {
		if(event.getMessage().trim().equalsIgnoreCase(solution)) {
			question = null;
			event.getPlayer().giveExpLevels(FileManager.getConfig().getInt("reward"));

			event.setCancelled(true);
			Bukkit.broadcastMessage(
					FileManager.getTexts().getString("messages.questionAnsweredMessage")
					.replaceAll("%player%", event.getPlayer().getDisplayName())
					.replaceAll("%solution%", solution)
					.replaceAll("%answer%", solution)
					.replaceAll("%answerTime%", Long.toString(TimeUnit.SECONDS.convert(System.currentTimeMillis()-askTime, TimeUnit.MILLISECONDS)))
					.replaceAll("%reward%", Integer.toString(FileManager.getConfig().getInt("reward"))));
		}
	}

	public void cancel() {
		enabled=false;
		question=solution=null;
		Bukkit.getScheduler().cancelTasks(pl);
	}

	public void schedule(long delay) {
		if (enabled) return;
		enabled=true;
		Bukkit.getScheduler().scheduleSyncRepeatingTask(pl, ()->{
			makeQuestion();
			ask(false);
		}, delay, FileManager.getConfig().getInt("waitingTime"));
	}
	
	public void ask(String question, String answer, int delay) {
		boolean wasEnabled = enabled;
		cancel();
		this.question = question;
		this.solution = answer;
		ask(wasEnabled);
	}
	
	public void askAuto() {
		boolean wasEnabled = enabled;
		cancel();
		makeQuestion();
		ask(wasEnabled);
	}

	private void ask(boolean reschedule) {
		askTime = System.currentTimeMillis();
		Bukkit.broadcastMessage(
				FileManager.getTexts().getString("messages.questionAskMessage")
				.replaceAll("%question%", question)
				.replaceAll("%answerTime%", FileManager.getConfig().getString("answerTime"))
				.replaceAll("%reward%", Integer.toString(FileManager.getConfig().getInt("reward"))));
		if (FileManager.getConfig().getInt("answerTime")<FileManager.getConfig().getInt("waitingTime")) {
			Bukkit.getScheduler().scheduleSyncDelayedTask(pl, ()->{
				if (hasOpenQuestion()) {
					Bukkit.broadcastMessage(
							FileManager.getTexts().getString("messages.questionTimeUpMessage")
							.replaceAll("%question%", question)
							.replaceAll("%solution%", solution)
							.replaceAll("%answer%", solution)
							.replaceAll("%reward%", Integer.toString(FileManager.getConfig().getInt("reward"))));
					question = solution = null;
				}
				if (reschedule) schedule(FileManager.getConfig().getInt("waitingTime"));
			}, FileManager.getConfig().getInt("answerTime"));
		}
	}
}