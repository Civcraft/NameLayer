package vg.civcraft.mc.namelayer.command.commands;

import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import vg.civcraft.mc.civmodcore.command.PlayerCommand;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameAPI;
import vg.civcraft.mc.namelayer.command.NameLayerTabCompleter;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.permission.PermissionType;
import vg.civcraft.mc.namelayer.permission.PlayerType;
import vg.civcraft.mc.namelayer.permission.PlayerTypeHandler;

public class ModifyPermissions extends PlayerCommand {

	public ModifyPermissions(String name) {
		super(name);
		setIdentifier("nlmp");
		setDescription("Modify the permissions of a group.");
		setUsage("/nlmp <group> <add/remove> <PlayerType> <PermissionType>");
		setArguments(4,4);
	}

	@Override
	public boolean execute(CommandSender sender, String[] args) {
		Group g = GroupManager.getGroup(args[0]);
		if (g == null) {
			sender.sendMessage(ChatColor.RED + "This group doesn't exist");
			return true;
		}
		if (g.isDisciplined() || !(sender instanceof ConsoleCommandSender || sender.hasPermission("namelayer.admin"))){
			sender.sendMessage(ChatColor.RED + "This group is currently disiplined.");
			return true;
		}
		if (sender instanceof Player) {
			//owner can always change perms
			UUID uuid = ((Player) sender).getUniqueId();
			if (!NameAPI.getGroupManager().hasAccess(g, uuid, PermissionType.getPermission("PERMS")) && !g.isOwner(uuid)){
				sender.sendMessage(ChatColor.RED + "You do not have permission to modify permissions for " + g.getName());
				return true;
			}
		}
		String info = args[1];
		PlayerTypeHandler handler = g.getPlayerTypeHandler();
		PlayerType playerType = handler.getType(args[2]);
		if (playerType == null){
			sender.sendMessage(ChatColor.RED + "The player type " + args [2] + " does not exist for " + g.getName());
			return true;
		}
		PermissionType perm = PermissionType.getPermission(args[3]);
		if (perm == null){
			StringBuilder sb = new StringBuilder();
			for(PermissionType pem : PermissionType.getAllPermissions()) {
				sb.append(pem.getName());
				sb.append(" ");
			}
			sender.sendMessage(ChatColor.RED 
						+ "That PermissionType does not exists.\n"
						+ "The current types are: " + sb.toString());
			return true;
		}
		if (info.equalsIgnoreCase("add")){
			if (playerType.hasPermission(perm)) {
				sender.sendMessage(ChatColor.RED + playerType.getName() + " already has the permission " + perm.getName());
			}
			else {
				playerType.addPermission(perm, true);
				sender.sendMessage(ChatColor.GREEN + perm.getName() + " was successfully given to " + playerType.getName());
			}
		}
		else if (info.equalsIgnoreCase("remove")){
			if (playerType.hasPermission(perm)){
				playerType.removePermission(perm, true);
				sender.sendMessage(ChatColor.GREEN + perm.getName() + " was successfully remove from " + playerType.getName());
			}
			else {
				sender.sendMessage(ChatColor.RED + playerType.getName() + " does not have the permission " + perm.getName());
			}
		}
		else{
			sender.sendMessage(ChatColor.RED + "Specify if you want to add or remove, " + info + " is not a valid action");
		}
		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		if (args.length == 0 && (sender instanceof Player))
			return NameLayerTabCompleter.completeGroupWithPermission(null, PermissionType.getPermission("PERMS"), (Player) sender);
		else if (args.length == 1 && (sender instanceof Player))
			return NameLayerTabCompleter.completeGroupWithPermission(args [0], PermissionType.getPermission("PERMS"), (Player) sender);
		else if (args.length == 2) {

			if (args[1].length() > 0) {
				if (args[1].charAt(0) == 'a') return java.util.Arrays.asList(new String[]{"add"});
				else if (args[1].charAt(0) == 'r') return java.util.Arrays.asList(new String[]{"remove"});
			} else {
				return java.util.Arrays.asList(new String[]{"add", "remove"});
			}

		} else if (args.length == 3) {
			return MemberTypeCompleter.complete(args[2]);
		} else if (args.length == 4) {
			return PermissionCompleter.complete(args[3]);
		}

		return  null;
	}
}
