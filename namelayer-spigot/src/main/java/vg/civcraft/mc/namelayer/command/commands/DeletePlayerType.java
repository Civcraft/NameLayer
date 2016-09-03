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
import vg.civcraft.mc.namelayer.permission.PlayerType;
import vg.civcraft.mc.namelayer.permission.PlayerTypeHandler;

public class DeletePlayerType extends PlayerCommand {

	public DeletePlayerType(String name) {
		super(name);
		setIdentifier("nldpt");
		setDescription("Deletes a player type from a specific group");
		setUsage("/nldpt <group> <playerType>");
		setArguments(2, 2);
	}

	@Override
	public boolean execute(CommandSender sender, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.MAGIC + "DONT YOU EVEN DARE");
			return true;
		}
		Player p = (Player) sender;
		Group group = GroupManager.getGroup(args[0]);
		if (group == null) {
			sender.sendMessage(ChatColor.RED + "That group doesn't exist");
			return true;
		}
		PlayerTypeHandler handler = group.getPlayerTypeHandler();
		PlayerType type = handler.getType(args[1]);
		if (type == null) {
			p.sendMessage(ChatColor.RED + "That player type doesn't exist");
			return true;
		}
		if (!NameAPI.getGroupManager().hasAccess(group, p.getUniqueId(),
				PermissionType.getPermission("DELETE_PLAYERTYPE"))) {
			p.sendMessage(ChatColor.RED
					+ "You don't have the required permissions to do this");
			return true;
		}
		if (group.getAllTrackedByType(type).size() != 0) {
			p.sendMessage(ChatColor.RED
					+ "You can't delete this type, because it still has members");
			return true;
		}
		List<PlayerType> children = type.getChildren(true);
		for (PlayerType child : children) {
			if (group.getAllTrackedByType(child).size() != 0) {
				p.sendMessage(ChatColor.RED
						+ "You can't delete this type, because the sub type "
						+ child.getName() + " still has members");
				return true;
			}
		}
		if (type == handler.getDefaultNonMemberType()
				|| type == handler.getOwnerType()) {
			p.sendMessage(ChatColor.RED + "You can't delete this type");
			return true;
		}
		handler.deleteType(type, true);
		p.sendMessage(ChatColor.GREEN + "The type " + type.getName()
				+ " was deleted from " + group.getName());
		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		if (!(sender instanceof Player)){
			return null;
		}
		if (args.length == 0)
			return NameLayerTabCompleter.completeGroupWithPermission(null, PermissionType.getPermission("DELETE_PLAYERTYPE"), (Player) sender);
		else if (args.length == 1)
			return NameLayerTabCompleter.completeGroupWithPermission(args[0], PermissionType.getPermission("DELETE_PLAYERTYPE"), (Player) sender);
		else if (args.length == 2)
			return NameLayerTabCompleter.completePlayerType(args[1], GroupManager.getGroup(args [0]));
		else 
			return null;
	}
}