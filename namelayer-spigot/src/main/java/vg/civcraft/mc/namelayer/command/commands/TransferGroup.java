package vg.civcraft.mc.namelayer.command.commands;

import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import vg.civcraft.mc.civmodcore.command.PlayerCommand;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameAPI;
import vg.civcraft.mc.namelayer.NameLayerPlugin;
import vg.civcraft.mc.namelayer.command.NameLayerTabCompleter;
import vg.civcraft.mc.namelayer.group.Group;

public class TransferGroup extends PlayerCommand {

	public TransferGroup(String name) {
		super(name);
		setIdentifier("nltg");
		setDescription("Transfer one group to another person.");
		setUsage("/nltg <group> <player>");
		setArguments(2,2);
	}

	@Override
	public boolean execute(CommandSender sender, String[] args) {
		Group g = GroupManager.getGroup(args[0]);
		if (g == null) {
			sender.sendMessage(ChatColor.RED + "This group doesn't exist");
			return true;
		}
		UUID oPlayer = NameAPI.getUUID(args[1]); // uuid of the second player
		
		if (oPlayer == null) {
			sender.sendMessage(ChatColor.RED + "This player doesn't exist");
			return true;
		}
		return attemptTransfer(g, sender, oPlayer);
	}
	
	public static boolean attemptTransfer(Group g, CommandSender owner, UUID futureOwner) {
		GroupManager gm = NameAPI.getGroupManager();
		if (owner instanceof Player && !g.isOwner(((Player)owner).getUniqueId())) {
			owner.sendMessage(ChatColor.RED	+ "You don't own " + g.getName());
			return true;
		}
		if (g.isDisciplined()) {
			owner.sendMessage(ChatColor.RED	+ g.getName() + " is disciplined.");
			return true;
		}
		if (NameLayerPlugin.getInstance().getGroupLimit() < gm
				.countGroups(futureOwner) + 1) {
			owner.sendMessage(ChatColor.RED
					+ NameAPI.getCurrentName(futureOwner)
					+ " cannot receive the group! This player has already reached the group limit count.");
			return true;
		}
		if (!g.isMember(futureOwner)) {
			owner.sendMessage(ChatColor.RED
					+ NameAPI.getCurrentName(futureOwner)
					+ " is not a member of " + g.getName() + " and can't be made primary owner!");
			return true;
		}
		g.setOwner(futureOwner);
		owner.sendMessage(ChatColor.GREEN + NameAPI.getCurrentName(futureOwner)
				+ " has been given ownership of the group.");
		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		if(!(sender instanceof Player)) { 
			return null;
		}
		if (args.length == 0 ) {
			return NameLayerTabCompleter.completeGroupWithPermission(null, null, (Player) sender);
		}
		else {
			if (args.length == 1) {
				return NameLayerTabCompleter.completeGroupWithPermission(args [0], null, (Player) sender);
			}
			else {
				if (args.length == 2) {
					return NameLayerTabCompleter.completeOnlinePlayer(args[1]);
				}
			}
		}
		return null;
	}

}
