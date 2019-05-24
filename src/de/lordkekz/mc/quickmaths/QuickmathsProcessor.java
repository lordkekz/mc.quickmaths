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
	
	/**
	 * Generates an question-answer pair.
	 */
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

	/**
	 * @return whether there is an question that can be answered
	 */
	public boolean hasOpenQuestion() {
		return question!=null;
	}
	
	/**
	 * @return whether the QuickmathsProcessor will ask questions regularly
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Processes an event. If the message is the correct answer to the active question the player will be rewarded.
	 * @param event the event to be processed
	 */
	public void processChatEvent(AsyncPlayerChatEvent event) {
		if(hasOpenQuestion() && event.getMessage().trim().equalsIgnoreCase(solution)) {
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

	/**
	 * Disables the QuickmathsProcessor.
	 */
	public void cancel() {
		enabled=false;
		question=solution=null;
		Bukkit.getScheduler().cancelTasks(pl);
	}

	/**
	 * Enables  the QuickmathsProcessor and schedules regular broadcasts.
	 * @param delay the delay of the first broadcast (in ticks)
	 */
	public void schedule(long delay) {
		if (enabled) return;
		enabled=true;
		Bukkit.getScheduler().scheduleSyncRepeatingTask(pl, ()->{
			makeQuestion();
			ask(false);
		}, delay, FileManager.getConfig().getInt("waitingTime"));
	}
	
	/**
	 * Cancels the regular questions and asks given question.
	 * After the question is answered or the time is up it re-schedules the regular cycle (if it was scheduled before).
	 * @param question the qustion
	 * @param answer the answer
	 * @param delay the delay before asking
	 */
	public void ask(String question, String answer, int delay) {
		boolean wasEnabled = enabled;
		cancel();
		this.question = question;
		this.solution = answer;
		ask(wasEnabled);
	}
	
	/**
	 * Cancels the regular questions and immediately asks a question.
	 * After the question is answered or the time is up it re-schedules the regular cycle (if it was scheduled before).
	 */
	public void askAuto() {
		boolean wasEnabled = enabled;
		cancel();
		makeQuestion();
		ask(wasEnabled);
	}

	/**
	 * Broadcasts the question. If the time is up before someone correctly answers the question it will also broadcast that.
	 * @param reschedule whether or not to re-schedule the regular cycle; relevant for manually initiated questions.
	 */
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