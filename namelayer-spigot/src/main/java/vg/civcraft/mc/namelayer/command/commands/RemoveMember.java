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
import vg.civcraft.mc.namelayer.permission.PlayerType;

public class RemoveMember extends PlayerCommand {

	public RemoveMember(String name) {
		super(name);
		setIdentifier("nlrm");
		setDescription("Remove a member from a group.");
		setUsage("/nlrm <group> <member>");
		setArguments(2,2);
		
	}

	@Override
	public boolean execute(CommandSender sender, String[] args) {
		if (!(sender instanceof Player)){
			sender.sendMessage(ChatColor.RED + "I'm sorry baby, please run this as a player :)");
			return true;
		}
		Player p = (Player) sender;
		Group group = GroupManager.getGroup(args[0]);
		if (group == null) {
			p.sendMessage(ChatColor.RED + "This group doesn't exist");
			return true;
		}
		if (group.isDisciplined()){
			p.sendMessage(ChatColor.RED + "This group is disiplined.");
			return true;
		}
		UUID executor = NameAPI.getUUID(p.getName());
		UUID uuid = NameAPI.getUUID(args[1]);
		
		if (uuid == null){
			p.sendMessage(ChatColor.RED + "This player doesn't exist");
			return true;
		}
		//we need to ensure here that players cant extract who is on a group, based on the error message
		//so a player not being on a group has to give the same error message as one being on the group,
		//if you dont have the required permissions
		PlayerType currentType = group.getPlayerType(uuid);
		if (!group.isMember(uuid) || !NameAPI.getGroupManager().hasAccess(group, executor, currentType.getRemovalPermissionType())) {
			p.sendMessage(ChatColor.RED + "Either " + NameAPI.getCurrentName(uuid) + " is not on the group or you don't have permission to remove him");
			return true;
		}
		
		if (group.isOwner(uuid)){
			p.sendMessage(ChatColor.RED + "That player owns the group, you cannot "
					+ "remove the player.");
			return true;
		}
		
		p.sendMessage(ChatColor.GREEN + NameAPI.getCurrentName(uuid) + " has been removed from the group.");
		group.removeFromTracking(uuid);
		return true;
	}


	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		if (!(sender instanceof Player)){
			sender.sendMessage(ChatColor.RED + "I'm sorry baby, please run this as a player :)");
			return null;
		}
		if (args.length < 2) {
			if (args.length == 0) {
				return NameLayerTabCompleter.completeGroupWithPermission(null, null, (Player) sender);
			}
			else {
				return NameLayerTabCompleter.completeGroupWithPermission(args[0], null, (Player)sender);
			}

		} else if (args.length == 2) {
			return NameLayerTabCompleter.completeOnlinePlayer(args[1]);
		}
		return null;
	}

}