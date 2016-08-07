package vg.civcraft.mc.namelayer.command.commands;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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

public class RenamePlayerType extends PlayerCommandMiddle {

	public RenamePlayerType(String name) {
		super(name);
		setIdentifier("nlrpt");
		setDescription("Renames a player type for a specific group");
		setUsage("/nlrpt <group> <playerType> <newName>");
		setArguments(3, 3);
	}

	@Override
	public boolean execute(CommandSender sender, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.MAGIC + "BACK OFF");
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
		String name = args[2];
		// enforce regulations on the name
		if (name.length() > 32) {
			p.sendMessage(ChatColor.RED
					+ "The player type name is not allowed to contain more than 32 characters");
			return true;
		}
		Charset latin1 = StandardCharsets.ISO_8859_1;
		boolean invalidChars = false;
		if (!latin1.newEncoder().canEncode(name)) {
			invalidChars = true;
		}

		for (char c : name.toCharArray()) {
			if (Character.isISOControl(c)) {
				invalidChars = true;
			}
		}

		if (invalidChars) {
			p.sendMessage(ChatColor.RED
					+ "You used characters, which are not allowed");
			return true;
		}
		if (handler.getType(name) != null) {
			p.sendMessage(ChatColor.RED + "A type with this name already exists!");
			return true;
		}
		String oldName = type.getName();
		handler.renameType(type, name);
		p.sendMessage(ChatColor.GREEN +"Changed name of player type " + oldName + " to " + name);
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
