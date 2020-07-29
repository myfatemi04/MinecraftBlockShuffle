package io.github.myfatemi04.blockshuffle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class BlockShuffle extends JavaPlugin {
	private BukkitRunnable mainloop = null;
	private boolean running = false;
	private boolean paused = false;
	private int roundCount = 0;
	private int tickCount = 0;
	private int ticksPerRound = 5 * 60 * 20; // By default, 5 minutes per round
	
	public final FixedMetadataValue META_TRUE = new FixedMetadataValue(this, true);
	public final FixedMetadataValue META_FALSE = new FixedMetadataValue(this, false);
	
	public boolean isPaused() {
		return paused;
	}
	
	public void reset() {
		this.tickCount = 0;
	}
	
	public boolean timeLimitReached() {
		return this.tickCount > this.ticksPerRound;
	}
	
	public void nextRound() {
		this.tickCount = 0;
		this.roundCount += 1;
		
		for (Player player : getAlivePlayers()) {
			assignMaterial(player);
		}
		
		Bukkit.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Round " + this.roundCount + " is starting!");
	}
	
	public void nextTick() {
		this.tickCount += 1;
	}
	
	public int ticksLeft() {
		return this.ticksPerRound - this.tickCount;
	}
	
	/**
	 * Selects a random block that is not a command block, barrier, or end stone
	 * @return
	 */
	public Material getRandomMaterial() {
		Material[] materials = Material.values();
		Random random = new Random();
		
		// make sure it's not something basically impossible to get
		String[] skipExact = {
			"COMMAND_BLOCK",
			"BARRIER",
			
			// Extremely rare items
			"MYCELIUM",
			"SPONGE",
			"WET_SPONGE",
			
			"EMERALD_ORE",
			"BEACON",
		};
		
		String[] skipSubstrings = {
			"SHULKER",
			"CHORUS",
			"SPONGE",
			"END",
			"HEAD",
			"CORAL",
		};
		
		search: while (true) {
			Material material = materials[random.nextInt(materials.length)];
			
			if (!material.isBlock()) {
				continue;
			}
			
			if (!material.isSolid()) {
				continue;
			}
			
			for (String impossibleMaterial : skipExact) {
				if (material.toString().equals(impossibleMaterial)) {
					continue search;
				}
			}
			
			for (String impossibleClass : skipSubstrings) {
				if (material.toString().contains(impossibleClass)) {
					continue search;
				}
			}
			
			return material;
		}
	}
	
	public List<Player> getAlivePlayers() {
		ArrayList<Player> players = new ArrayList<Player>();
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (player.hasMetadata("blockshuffle_alive")) {
				if (player.getMetadata("blockshuffle_alive").get(0).asBoolean()) {
					players.add(player);
				}
			}
		}
		return players;
	}
	
	/**
	 * Gets a list of alive players that are not on their target block
	 * @return
	 */
	public List<Player> getSearchingPlayers() {
		ArrayList<Player> players = new ArrayList<Player>();
		for (Player player : getAlivePlayers()) {
			boolean foundTarget = player.getMetadata("blockshuffle_foundblock").get(0).asBoolean();
			if (!foundTarget) {
				players.add(player);
			}
		}
		
		return players;
	}
	
	public void eliminatePlayer(Player player) {
		player.setMetadata("blockshuffle_alive", META_FALSE);
		player.sendMessage(ChatColor.RED + "" +  ChatColor.BOLD + "You have been eliminated!");
	}
	
	public void reviveAll() {
		for (Player p : Bukkit.getOnlinePlayers()) {
			p.setMetadata("blockshuffle_alive", META_TRUE);
//			p.sendMessage("set blockshuffle_alive=true");
		}
	}
	
	public void resetFound() {
		for (Player player : getAlivePlayers()) {
			player.setMetadata("blockshuffle_foundblock", META_FALSE);
//			player.sendMessage("set blockshuffle_foundblock=false");
		}
	}
	
	public void startGame() {
		running = true;
		paused = false;
		mainloop = new MainLoop(this);
		mainloop.runTaskTimer(this, 0, 1);
		reviveAll();
		nextRound();
	}
	
	public void endGame() {
		running = false;
		paused = false;
		roundCount = 0;
		tickCount = 0;
		mainloop.cancel();
		Bukkit.broadcastMessage("Stopping Block Shuffle");
	}
	
	public void pauseGame() {
		paused = true;
	}
	
	public void unpauseGame() {
		paused = false;
	}
	
	/**
	 * Chooses a random material, sets it in the player metadata, and sends the player a message
	 * @param player
	 */
	public void assignMaterial(Player player) {
		// Choose a material
		Material target = getRandomMaterial();
		
		// Set the player's metadata
		player.setMetadata("blockshuffle_material", new FixedMetadataValue(this, target));
		player.setMetadata("blockshuffle_foundblock", META_FALSE);

//		player.sendMessage("set blockshuffle_material=" + target);
//		player.sendMessage("set blockshuffle_alive=false");
		
		// Get the name of the material, replace underscores, capitalize
		String targetName = target.toString().replace("_", " ");
		targetName = targetName.substring(0, 1).toUpperCase() + targetName.substring(1);
		
		// Send a chat message with their material
		player.sendMessage(ChatColor.GREEN + "Your material is: " + ChatColor.BOLD + targetName);
	}
	
	public String formatTickTime(int tickCount) {
		int rawSeconds = tickCount / 20;
		
		int minutes = rawSeconds / 60;
		int seconds = rawSeconds % 60;
		
		return minutes + "m" + seconds + "s";
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("blockshuffle")) {
			// starts a round of Block Shuffle
			
			if (args.length != 1) {
				return false;
			} else {
				if (args[0].equals("start")) {
					if (running) {
						sender.sendMessage("Block Shuffle is already started");
					} else {
						startGame();
					}
					
					return true;
				} else if (args[0].equals("stop")) {
					if (!running) {
						sender.sendMessage("Block Shuffle is already stopped");
					} else {
						endGame();
					}
					
					return true;
				} else {
					return false;
				}
			}
		} else if (cmd.getName().equalsIgnoreCase("blockshufflespeed")) {
			// sets the speed of each block shuffle round
			
			if (args.length == 0) {
				sender.sendMessage("Block Shuffle speed is set to " + formatTickTime(ticksPerRound));
				return true;
			} else if (args.length == 1) {
				try {
					double seconds = Double.parseDouble(args[0]);
					this.ticksPerRound = (int)(seconds * 20);
										
					sender.sendMessage("Block Shuffle speed successfully updated to " + formatTickTime(ticksPerRound));
					return true;
				} catch (NumberFormatException e) {
					return false;
				}
			}
		} else if (cmd.getName().equalsIgnoreCase("blockshuffletime")) {
			// gets the time left in this round
			
			if (args.length == 0) {
				if (this.running) {
					
					sender.sendMessage(formatTickTime(ticksPerRound - tickCount) + " until this round ends");
					
					return true;
				} else {
					sender.sendMessage("Block Shuffle is inactive. To start a round, run /blockshuffle start");
					
					return true;
				}
			} else if (args.length == 2) {
				if (args[1].equals("add")) {
					double seconds = Double.parseDouble(args[0]);
					int newTicks = (int)(seconds * 20);
					
					this.tickCount -= newTicks;
					
					Bukkit.broadcastMessage(sender.getName() + " has added " + formatTickTime(tickCount) + " to the round time.");
					Bukkit.broadcastMessage(ChatColor.BOLD + "New time left: " + formatTickTime(ticksPerRound - tickCount));
				} else if (args[1].equals("remove")) {
					double seconds = Double.parseDouble(args[0]);
					int newTicks = (int)(seconds * 20);
					
					this.tickCount += newTicks;
					
					Bukkit.broadcastMessage(sender.getName() + " has removed " + formatTickTime(tickCount) + " to the round time");
					Bukkit.broadcastMessage(ChatColor.BOLD + "New time left: " + formatTickTime(ticksPerRound - tickCount));
				}
			}
		}
		
		// no commands succeeded
		return false;
	}
}
