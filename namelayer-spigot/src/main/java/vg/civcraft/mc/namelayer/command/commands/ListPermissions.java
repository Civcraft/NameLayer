package vg.civcraft.mc.namelayer.command.commands;

import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import vg.civcraft.mc.civmodcore.command.PlayerCommand;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameAPI;
import vg.civcraft.mc.namelayer.command.NameLayerTabCompleter;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.permission.PermissionType;
import vg.civcraft.mc.namelayer.permission.PlayerType;
import vg.civcraft.mc.namelayer.permission.PlayerTypeHandler;

public class ListPermissions extends PlayerCommand {

	public ListPermissions(String name) {
		super(name);
		setIdentifier("nllp");
		setDescription("Show permissions for a PlayerType in a specific group.");
		setUsage("/nllp <group> [PlayerType]");
		setArguments(1,2);
	}

	@Override
	public boolean execute(CommandSender sender, String[] args) {
		if (!(sender instanceof Player) && args.length == 1){
			sender.sendMessage(ChatColor.RED + "You need to specify an explicit type to list perms for");
			return true;
		}
		Group g = GroupManager.getGroup(args[0]);
		if (g == null) {
			sender.sendMessage(ChatColor.RED + "This group doesn't exist");
			return true;
		}
		PlayerType playerType = null;
		if (sender instanceof Player) {
			Player p = (Player) sender;
			UUID uuid = NameAPI.getUUID(p.getName());
			playerType = g.getPlayerType(uuid);
		}
		PlayerTypeHandler handler = g.getPlayerTypeHandler();
		if(args.length > 1){
			if (sender instanceof Player) {
				if (!NameAPI.getGroupManager().hasAccess(g, ((Player) sender).getUniqueId(), PermissionType.getPermission("LIST_PERMS"))){
						sender.sendMessage(ChatColor.RED + "You do not have permission to run this command for " + g.getName());
						return true;
				}
			}
			playerType = handler.getType(args [1]);
			if (playerType == null){
				sender.sendMessage(ChatColor.RED + args[1] + " is not a valid player type for " + g.getName());
				return true;
			}
		}
		StringBuilder sb = new StringBuilder();
		sb.append(ChatColor.GREEN + "Permissions for " + playerType.getName() + " are: ");
		for(PermissionType perm : playerType.getAllPermissions()) {
			sb.append(perm.getName());
			sb.append(" ");
		}
		sender.sendMessage(ChatColor.GREEN + sb.toString());
		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		if (args.length == 0 && (sender instanceof Player))
			return NameLayerTabCompleter.completeGroupWithPermission(null, null, (Player) sender);
		else if (args.length == 1 && (sender instanceof Player))
			return NameLayerTabCompleter.completeGroupWithPermission(args [0], null, (Player) sender);
		else if (args.length == 2)
			return NameLayerTabCompleter.completePlayerType(args[1], GroupManager.getGroup(args[0]));
		return  null;
	}

}
