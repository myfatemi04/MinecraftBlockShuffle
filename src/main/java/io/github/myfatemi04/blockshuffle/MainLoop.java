package io.github.myfatemi04.blockshuffle;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class MainLoop extends BukkitRunnable {
	private final BlockShuffle plugin;
	
	public MainLoop(BlockShuffle plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public void run() {
		if (plugin.isPaused()) {
			return;
		}
		
		List<Player> searchingPlayers = plugin.getSearchingPlayers();
		List<Player> alivePlayers = plugin.getAlivePlayers();
		
		if (plugin.ticksLeft() <= 0) {
			// Time limit has been reached!
			
			if (alivePlayers.size() == searchingPlayers.size()) {
				// If nobody found their blocks, then nobody gets eliminated
				Bukkit.broadcastMessage(ChatColor.BOLD + "Nobody found their target blocks this round, so nobody is eliminated.");
				
				plugin.nextRound();
				
			// ----------------------------------------------------------------	
				
			} else {
				// If some players didn't find their blocks, then they get eliminated
				for (Player p : searchingPlayers) {
					plugin.eliminatePlayer(p);
					p.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "You have lost a life, " + plugin.getLives(p) + " lives left");
				}
				
				List<Player> safePlayers = plugin.getAlivePlayers();
				
				if (safePlayers.size() == 1) {
					Player lastPlayer = safePlayers.get(0);
					
					Bukkit.broadcastMessage(ChatColor.BOLD + "" + ChatColor.GOLD + lastPlayer.getName() + " is the winner!");
				
					plugin.endGame();
				} else {
					Bukkit.broadcastMessage(ChatColor.BOLD + "" + safePlayers.size() + " players remain!");
					Bukkit.broadcastMessage(ChatColor.BOLD + "---- Leaderboard ----");
					
					for (Player player : safePlayers) {
						Bukkit.broadcastMessage(player.getName() + " - " + plugin.getPoints(player) + ", " + plugin.getLives(player) + " lives");
					}
					
					plugin.nextRound();
				}
			}
			
		// -----------------------------------------------------------------
			
		} else {
			// Time limit has not been reached yet
			// Check for players that found their blocks
			
			int ticksLeft = plugin.ticksLeft();
			
			if (ticksLeft <= 20 * 10) {
				if (ticksLeft % 20 == 0) {
					int secondsLeft = ticksLeft / 20;
					Bukkit.broadcastMessage(secondsLeft + " seconds remaining!");
				}
			}
			
			// If all players find their blocks, we end the round early
			int foundBlocks = 0;
			for (Player p : searchingPlayers) {
				Material targetMaterial = (Material) p.getMetadata("blockshuffle_material").get(0).value();
				Material standingOn = p.getLocation().getBlock().getRelative(BlockFace.DOWN).getType();
				
				if (standingOn.equals(targetMaterial)) {
					Bukkit.broadcastMessage(ChatColor.BOLD + p.getName() + " found their block!");
					p.setMetadata("blockshuffle_foundblock", plugin.META_TRUE);
					plugin.setPoints(p, plugin.getPoints(p) + 1);
					foundBlocks += 1;
				}
				
			}
			
			if (foundBlocks == searchingPlayers.size()) {
				Bukkit.broadcastMessage(ChatColor.BOLD + "All players found their blocks");
				
				plugin.nextRound();
			}
		}
		
		plugin.nextTick();
	}
}
