package vg.civcraft.mc.namelayer.command;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import vg.civcraft.mc.civmodcore.command.PlayerCommand;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameAPI;
import vg.civcraft.mc.namelayer.NameLayerPlugin;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.misc.Mercury;
import vg.civcraft.mc.namelayer.permission.PlayerType;

public abstract class PlayerCommandMiddle extends PlayerCommand{

	public PlayerCommandMiddle(String name) {
		super(name);
	}

	protected GroupManager gm = NameAPI.getGroupManager();
	
	protected boolean groupIsNull(CommandSender sender, String groupname, Group group) {
	    if (group == null) {
	        sender.sendMessage(String.format(
	                "%sThe group \"%s\" does not exist.", 
	                ChatColor.RED, groupname));
	        return true;
	    }
	    return false;
	}
	
	public void checkRecacheGroup(Group g){
		if (NameLayerPlugin.isMercuryEnabled()){
			String message = "recache " + g.getName();
			Mercury.invalidateGroup(message);
		}
	}
	
	protected void sendPlayerTypes(Group g, CommandSender sender, String entered) {
		StringBuilder sb = new StringBuilder();
		for(PlayerType type : g.getPlayerTypeHandler().getAllTypes()) {
			sb.append(type.getName());
			sb.append(", ");
		}
		sender.sendMessage(entered + " is not a valid type for this group, the existing ones are: " + sb.substring(0, sb.length()-2));
	}
	
	protected boolean canModifyRank(Group g, Player p, PlayerType rankToModify) {
		if (g == null || p == null || rankToModify == null) {
			return false;
		}
		PlayerType playersType = g.getPlayerType(p.getUniqueId());
		return playersType != null && g.getPlayerTypeHandler().isRelated(rankToModify, playersType);
	}
}
