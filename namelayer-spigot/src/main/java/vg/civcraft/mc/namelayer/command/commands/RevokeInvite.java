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
import vg.civcraft.mc.namelayer.listeners.PlayerListener;
import vg.civcraft.mc.namelayer.misc.MercuryManager;
import vg.civcraft.mc.namelayer.permission.PlayerType;

public class RevokeInvite extends PlayerCommand {

	public RevokeInvite(String name) {
		super(name);
		setIdentifier("nlri");
		setDescription("Revoke an Invite.");
		setUsage("/nlri <group> <player>");
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
			p.sendMessage(ChatColor.RED + "The player has never played before.");
			return true;
		}
		
		//check invitee has invite
		if(group.getInvite(uuid) == null){
			p.sendMessage(ChatColor.RED + NameAPI.getCurrentName(uuid) + " does not have an invite to that group or is already a member of it");
			return true;
		}
		PlayerType type = group.getPlayerType(uuid);
		if (!NameAPI.getGroupManager().hasAccess(group, executor, type.getInvitePermissionType())) {
			p.sendMessage(ChatColor.RED + "You dont have permissions to revoke this invite");
			return true;
		}
		
		group.removeInvite(uuid, true);
		PlayerListener.removeNotification(uuid, group);
		MercuryManager.remInvite(group.getGroupId(), uuid);
		
		p.sendMessage(ChatColor.GREEN + NameAPI.getCurrentName(uuid) + "'s invitation has been revoked.");
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
