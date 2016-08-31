package vg.civcraft.mc.namelayer.command.commands;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import vg.civcraft.mc.civmodcore.command.PlayerCommand;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameAPI;
import vg.civcraft.mc.namelayer.command.NameLayerTabCompleter;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.permission.PermissionType;

public class SetPassword extends PlayerCommand {

	public SetPassword(String name) {
		super(name);
		setIdentifier("nlsp");
		setDescription("Set a password on a group.");
		setUsage("/nlsp <group> <password>");
		setArguments(1,2);
	}

	@Override
	public boolean execute(CommandSender sender, String[] args) {
		Group g = GroupManager.getGroup(args[0]);
		if (g == null) {
			sender.sendMessage(ChatColor.RED + "This group doesn't exist");
			return true;
		}		
		if (sender instanceof Player) {
			if (!NameAPI.getGroupManager().hasAccess(g, ((Player) sender).getUniqueId(), PermissionType.getPermission("PASSWORD"))){
				sender.sendMessage(ChatColor.RED + "You do not have permission to change the password for " + g.getName());
				return true;
			}
		}

		String password = null;
		if (args.length == 2) {
			password = args[1];
			sender.sendMessage(ChatColor.GREEN + "Password for " + g.getName() +  " has been successfully set to: " + password);
		}
		else {
			sender.sendMessage(ChatColor.GREEN + "Password was successfully removed from " + g.getName());
		}
		g.setPassword(password);
		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		if (!(sender instanceof Player)) {
			return null;
		}
		if (args.length == 0) {
			return NameLayerTabCompleter.completeGroupWithPermission(null, PermissionType.getPermission("PASSWORD"), (Player) sender);
		}
		else {
			return NameLayerTabCompleter.completeGroupWithPermission(args [0], PermissionType.getPermission("PASSWORD"), (Player) sender);
		}
	}
}
