package vg.civcraft.mc.namelayer.command.commands;

import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import vg.civcraft.mc.mercury.MercuryAPI;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameAPI;
import vg.civcraft.mc.namelayer.NameLayerPlugin;
import vg.civcraft.mc.namelayer.command.PlayerCommandMiddle;
import vg.civcraft.mc.namelayer.command.TabCompleters.GroupTabCompleter;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.listeners.PlayerListener;
import vg.civcraft.mc.namelayer.permission.PlayerType;

public class RevokeInvite extends PlayerCommandMiddle{

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
		if (groupIsNull(sender, args[0], group)) {
			return true;
		}
		if (group.isDisciplined()){
			p.sendMessage(ChatColor.RED + "This group is disiplined.");
			return true;
		}
		UUID uuid = NameAPI.getUUID(args[1]);
		
		if (uuid == null){
			p.sendMessage(ChatColor.RED + "The player has never played before.");
			return true;
		}
		
		//check invitee has invite
		if(group.getInvite(uuid) == null){
			if(group.isMember(uuid)){
				p.sendMessage(ChatColor.RED + NameAPI.getCurrentName(uuid) + " is already part of that group, "
						+ "use /remove to remove them.");
				return true;
			}
			p.sendMessage(ChatColor.RED + NameAPI.getCurrentName(uuid) + " does not have an invite to that group.");
			return true;
		}
		
		//get invitee PlayerType
		PlayerType pType = group.getInvite(uuid);
		if (!group.isMember(p.getUniqueId())) {
			p.sendMessage(ChatColor.RED + "You are not on that group.");
			return true;
		}
		boolean allowed = canModifyRank(group, p, pType);
		if (!allowed){
			p.sendMessage(ChatColor.RED + "You do not have permissions to modify this group.");
			return true;
		}
		
		group.removeInvite(uuid, true);
		PlayerListener.removeNotification(uuid, group);
		
		if(NameLayerPlugin.isMercuryEnabled()){
			MercuryAPI.sendGlobalMessage("removeInvitation " + group.getGroupId() + " " + uuid, "namelayer");
		}
		
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
			if (args.length == 0)
				return GroupTabCompleter.complete(null, null, (Player) sender);
			else
				return GroupTabCompleter.complete(args[0], null, (Player)sender);

		} else if (args.length == 2)
			return null;

		else return null;
	}
}
