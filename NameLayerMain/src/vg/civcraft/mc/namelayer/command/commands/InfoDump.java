package vg.civcraft.mc.namelayer.command.commands;

import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameAPI;
import vg.civcraft.mc.namelayer.command.PlayerCommandMiddle;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.permission.PermissionType;
import vg.civcraft.mc.namelayer.permission.PlayerType;

public class InfoDump extends PlayerCommandMiddle
{
	
	public InfoDump(String name)
	{
		super(name);
		setIdentifier("nlid");
		setDescription("This command dumps group info for CitadelGUI.");
		setUsage("/nlid (page)");
		setArguments(0, 1);
	}
	
	@Override
	public boolean execute(CommandSender sender, String[] args)
	{
		if (!(sender instanceof Player)){
			sender.sendMessage(ChatColor.RED + "You are not a player?");
			return true;
		}
		
		Player player = (Player)sender;
		UUID playerUUID = NameAPI.getUUID(player.getName());
		
		List<String> groupNames = gm.getAllGroupNames(player.getUniqueId());
		
		if(args.length == 0)
		{
			player.sendMessage(ChatColor.GREEN + "[NLID]: " + groupNames.size());
			return true;
		}
		else
		{
			
			int page = 0;
			try
			{
				page = Integer.parseInt(args[0]);
			}
			catch(Exception e)
			{
				player.sendMessage(ChatColor.RED + "Please enter a valid number");
				return true;
			}

			Group group;
			try
			{
				group = GroupManager.getGroup(groupNames.get(page-1));
			}
			catch(Exception e)
			{
				player.sendMessage(ChatColor.RED + "No such Group");
				return true;
			}
			PlayerType playerRank = group.getPlayerType(playerUUID);
			StringBuilder outputBuilder = new StringBuilder();
			outputBuilder.append("[NLID] : [GROUPNAME] ");
			outputBuilder.append(group.getName());
			outputBuilder.append(" : [MEMBERSHIPLEVEL] ");
			outputBuilder.append(playerRank.getName());
			outputBuilder.append(" : [PERMS] ");
			for(PermissionType perm : playerRank.getAllPermissions()) {
				outputBuilder.append(perm.getName());
			}
			
			for(PlayerType pType : group.getPlayerTypeHandler().getAllTypes()) {
				outputBuilder.append(" : ["+ pType.getName() +"]");
				if (group.getPlayerTypeHandler().isRelated(pType, playerRank) || sender.hasPermission("namelayer.admin")) {
					for(UUID memberUUID : group.getAllMembers(pType))
					{
						outputBuilder.append(" " + NameAPI.getCurrentName(memberUUID));
					}
				}
			}

			if(gm.hasAccess(group, playerUUID, PermissionType.getPermission("LIST_PERMS")))
			{
				for(PlayerType pType : group.getPlayerTypeHandler().getAllTypes()) {
					outputBuilder.append(" : [" + pType.getName() +"-PERMS] ");
					for(PermissionType perm : pType.getAllPermissions()) {
						outputBuilder.append(perm.getName());
					}
				}
			}

			player.sendMessage(ChatColor.GREEN + outputBuilder.toString());
			return true;
		}
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) 
	{
		return null;
	}
}
