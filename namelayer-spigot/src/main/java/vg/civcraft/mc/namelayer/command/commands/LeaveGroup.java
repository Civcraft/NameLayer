package vg.civcraft.mc.namelayer.command.commands;

import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import vg.civcraft.mc.civmodcore.command.PlayerCommand;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameAPI;
import vg.civcraft.mc.namelayer.command.NameLayerTabCompleter;
import vg.civcraft.mc.namelayer.group.Group;

public class LeaveGroup extends PlayerCommand {

	public LeaveGroup(String name) {
		super(name);
		setIdentifier("nlleg");
		setDescription("Leave a group");
		setUsage("/nlleg <group>");
		setArguments(1,1);
	}

	@Override
	public boolean execute(CommandSender sender, String[] args) {
		if (!(sender instanceof Player)){
			sender.sendMessage("Nope, be player");
			return true;
		}
		Player p = (Player) sender;
		Group g = GroupManager.getGroup(args[0]);
		if (g == null) {
			p.sendMessage(ChatColor.RED + "This group doesn't exist");
			return true;
		}
		if (g.isDisciplined()){
			p.sendMessage(ChatColor.RED + "This group is disciplined.");
			return true;
		}
		UUID uuid = NameAPI.getUUID(p.getName());
		if (!g.isMember(uuid)){
			if (g.isTracked(uuid)) {
				p.sendMessage(ChatColor.RED  + "You can't leave from being blacklisted");
			}
			else {
				p.sendMessage(ChatColor.RED + "You are not a member of this group.");
			}
			return true;
		}
		g.removeFromTracking(uuid);
		p.sendMessage(ChatColor.GREEN + "You have left " + g.getName());
		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		if (!(sender instanceof Player)) {
			return null;
		}
		if (args.length == 0)
			return NameLayerTabCompleter.completeGroupWithPermission(null, null, (Player) sender);
		else{
			return NameLayerTabCompleter.completeGroupWithPermission(args [0], null, (Player)sender);
		}
	}
}
