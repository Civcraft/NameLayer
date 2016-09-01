package vg.civcraft.mc.namelayer.command.commands;

import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.google.common.collect.Lists;

import vg.civcraft.mc.civmodcore.command.PlayerCommand;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameAPI;
import vg.civcraft.mc.namelayer.command.NameLayerTabCompleter;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.permission.PermissionType;
import vg.civcraft.mc.namelayer.permission.PlayerType;

public class ListMembers extends PlayerCommand {

	public ListMembers(String name) {
		super(name);
		setIdentifier("nllm");
		setDescription("List the members in a group");
		setUsage("/nllm <group> [PlayerType]");
		setArguments(1,2);
	}

	@Override
	public boolean execute(CommandSender sender, String[] args) {		
		String groupname = args[0];
		Group group = GroupManager.getGroup(groupname);
		if (group == null) {
			sender.sendMessage(ChatColor.RED + "This group doesn't exist");
			return true;
		}
		if (sender instanceof Player) {
		GroupManager gm = NameAPI.getGroupManager();
		Player p = (Player) sender;
		UUID uuid = NameAPI.getUUID(p.getName());
			if (!gm.hasAccess(group, uuid, PermissionType.getPermission("GROUPSTATS"))) {
				p.sendMessage(ChatColor.RED 
						+ "You don't have permission to run that command.");
				return true;
			}
		}
		List<UUID> uuids = null;
		if (args.length == 3) {
			String nameMin = args[1], nameMax = args[2];
			
			List<UUID> members = group.getAllTracked();
			uuids = Lists.newArrayList();
			
			for (UUID member : members) {
				String name = NameAPI.getCurrentName(member);
				if (name.compareToIgnoreCase(nameMin) >=0 
						&& name.compareToIgnoreCase(nameMax) <= 0) {
					uuids.add(member);
				}
			}
		} else if (args.length == 2) {
			String playerRank = args[1];
			PlayerType filterType = group.getPlayerTypeHandler().getType(playerRank);
			
			if (filterType == null) {
				sender.sendMessage(ChatColor.RED + "The player type you entered was not valid");
				return false;
			}
			
			uuids = group.getAllTrackedByType(filterType);
		} else {
			uuids = group.getAllTracked();
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append(ChatColor.GREEN);
		sb.append("Members are as follows:\n");
		for (UUID uu: uuids){
			sb.append(NameAPI.getCurrentName(uu));
			sb.append(" (");
			sb.append(group.getPlayerType(uu));
			sb.append(")\n");
		}
		
		sender.sendMessage(sb.toString());
		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		if (!(sender instanceof Player)) {
			return null;
		}	
		if (args.length == 0)
			return NameLayerTabCompleter.completeGroupWithPermission(null, PermissionType.getPermission("GROUPSTATS"), (Player) sender);
		else if (args.length == 1)
			return NameLayerTabCompleter.completeGroupWithPermission(args [0], PermissionType.getPermission("GROUPSTATS"), (Player) sender);
		else if (args.length == 2)
			return NameLayerTabCompleter.completeOnlinePlayer(args[1]);

		return null;
	}
}
