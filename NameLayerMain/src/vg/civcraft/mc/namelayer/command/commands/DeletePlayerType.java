package vg.civcraft.mc.namelayer.command.commands;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.command.PlayerCommandMiddle;
import vg.civcraft.mc.namelayer.command.TabCompleters.GroupTabCompleter;
import vg.civcraft.mc.namelayer.command.TabCompleters.MemberTypeCompleter;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.permission.PermissionType;
import vg.civcraft.mc.namelayer.permission.PlayerType;
import vg.civcraft.mc.namelayer.permission.PlayerTypeHandler;

public class DeletePlayerType extends PlayerCommandMiddle {

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
		if (!gm.hasAccess(group, p.getUniqueId(),
				PermissionType.getPermission("PLAYERTYPES"))) {
			p.sendMessage(ChatColor.RED
					+ "You don't have the required permissions to do this");
			return true;
		}
		if (group.getAllMembers(type).size() != 0) {
			p.sendMessage(ChatColor.RED
					+ "You can't delete this type, because it still has members");
			return true;
		}
		List<PlayerType> children = type.getRecursiveChildren();
		for (PlayerType child : children) {
			if (group.getAllMembers(child).size() != 0) {
				p.sendMessage(ChatColor.RED
						+ "You can't delete this type, because the sub type "
						+ child.getName() + " still has members");
				return true;
			}
		}
		if (type.equals(handler.getBlacklistedType())
				|| type.equals(handler.getOwnerType())
				|| type.equals(handler.getDefaultNonMemberType())) {
			p.sendMessage(ChatColor.RED + "You can't delete this type");
		}
		PlayerType parent = type.getParent();
		parent.removeChild(type);
		for (PlayerType ty : children) {
			handler.deleteType(ty, true);
			p.sendMessage(ChatColor.GREEN + "The type " + ty.getName()
					+ " was deleted from " + group.getName());
		}
		handler.deleteType(type, true);
		p.sendMessage(ChatColor.GREEN + "The type " + type.getName()
				+ " was deleted from " + group.getName());
		checkRecacheGroup(group);
		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		if (!(sender instanceof Player)) {
			return null;
		}
		Player p = (Player) sender;
		if (args.length == 0) {
			return GroupTabCompleter.complete(null,
					PermissionType.getPermission("PLAYERTYPES"), p);
		}
		if (args.length == 1) {
			return GroupTabCompleter.complete(args[0],
					PermissionType.getPermission("PLAYERTYPES"), p);
		}
		if (args.length == 2) {
			return MemberTypeCompleter.complete(GroupManager.getGroup(args[0]),
					args[1]);
		}
		return null;
	}
}
