package vg.civcraft.mc.namelayer.command.commands;

import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameAPI;
import vg.civcraft.mc.namelayer.command.PlayerCommandMiddle;
import vg.civcraft.mc.namelayer.command.TabCompleters.GroupTabCompleter;
import vg.civcraft.mc.namelayer.command.TabCompleters.MemberTypeCompleter;
import vg.civcraft.mc.namelayer.command.TabCompleters.PermissionCompleter;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.permission.PermissionType;
import vg.civcraft.mc.namelayer.permission.PlayerType;

public class ModifyPermissions extends PlayerCommandMiddle{

	public ModifyPermissions(String name) {
		super(name);
		setIdentifier("nlmp");
		setDescription("Modify the permissions of a group.");
		setUsage("/nlmp <group> <add/remove> <PlayerType> <PermissionType>");
		setArguments(4,4);
	}

	@Override
	public boolean execute(CommandSender sender, String[] args) {
		if (!(sender instanceof Player)){
			sender.sendMessage(ChatColor.RED + "You must be a player. Nuf said.");
			return true;
		}
		Player p = (Player) sender;
		Group g = GroupManager.getGroup(args[0]);
		if (groupIsNull(sender, args[0], g)) {
			return true;
		}
		UUID uuid = NameAPI.getUUID(p.getName());
		if (g.isDisciplined()){
			p.sendMessage(ChatColor.RED + "This group is currently disiplined.");
			return true;
		}
		if (!gm.hasAccess(g, uuid, PermissionType.getPermission("PERMS")) && !g.isOwner(uuid) && !(p.isOp() || p.hasPermission("namelayer.admin"))){
			p.sendMessage(ChatColor.RED + "You do not have permission for this command.");
			return true;
		}
		String info = args[1];
		PlayerType playerType = g.getPlayerTypeHandler().getType((args[2]));
		if (playerType == null){
			sendPlayerTypes(g, sender,args[2]);
			return true;
		}
		PermissionType pType = PermissionType.getPermission(args[3]);
		if (pType == null){
			StringBuilder sb = new StringBuilder();
			for(PermissionType perm : PermissionType.getAllPermissions()) {
				sb.append(perm.getName());
				sb.append(" ");
			}
			p.sendMessage(ChatColor.RED 
						+ "That PermissionType does not exists.\n"
						+ "The current types are: " + sb.toString());
			return true;
		}
		if (info.equalsIgnoreCase("add")){
			if (playerType.hasPermission(pType))
				sender.sendMessage(ChatColor.RED + "This PlayerType already has the PermissionType: " + pType.getName());
			else {
				playerType.addPermission(pType, true);
				sender.sendMessage(ChatColor.GREEN + "The PermissionType: " + pType.getName() + " was successfully added to the PlayerType: " +
				playerType.getName());
				checkRecacheGroup(g);
			}
		}
		else if (info.equalsIgnoreCase("remove")){
			if (playerType.hasPermission(pType)){
				playerType.removePermission(pType, true);
				sender.sendMessage(ChatColor.GREEN + "The PermissionType: " + pType.getName() + " was successfully removed from" +
						" the PlayerType: " + playerType.getName());
				checkRecacheGroup(g);
			}
			else
				sender.sendMessage(ChatColor.RED + "This PlayerType does not have the PermissionType: " + pType.getName());
		}
		else{
			p.sendMessage(ChatColor.RED + "Specify if you want to add or remove.");
		}
		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		if (!(sender instanceof Player))
			return null;

		if (args.length == 0)
			return GroupTabCompleter.complete(null, PermissionType.getPermission("PERMS"), (Player) sender);
		else if (args.length == 1)
			return GroupTabCompleter.complete(args[0], PermissionType.getPermission("PERMS"), (Player)sender);
		else if (args.length == 2) {

			if (args[1].length() > 0) {
				if (args[1].charAt(0) == 'a') return java.util.Arrays.asList(new String[]{"add"});
				else if (args[1].charAt(0) == 'r') return java.util.Arrays.asList(new String[]{"remove"});
			} else {
				return java.util.Arrays.asList(new String[]{"add", "remove"});
			}

		} else if (args.length == 3) {
			Group g = GroupManager.getGroup(args [0]);
			if (g == null) {
				return null;
			}
			return MemberTypeCompleter.complete(g, args[2]);
		} else if (args.length == 4) {
			return PermissionCompleter.complete(args[3]);
		}

		return  null;
	}
}
