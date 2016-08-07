package vg.civcraft.mc.namelayer.command.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.command.PlayerCommandMiddle;
import vg.civcraft.mc.namelayer.command.TabCompleters.GroupTabCompleter;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.permission.PermissionType;
import vg.civcraft.mc.namelayer.permission.PlayerType;

import java.util.List;

public class ListPlayerTypes extends PlayerCommandMiddle{

	public ListPlayerTypes(String name) {
		super(name);
		setIdentifier("nllpt");
		setDescription("List PlayerTypes for a group");
		setUsage("/nllpt <group>");
		setArguments(1,1);
	}

	@Override
	public boolean execute(CommandSender sender, String[] args) {
		if (!(sender instanceof Player)){
			sender.sendMessage("wereyjukqswedfrgyjukl.");
			return true;
		}
		Group g = GroupManager.getGroup(args [0]);
		if (!gm.hasAccess(g, ((Player) sender).getUniqueId(), PermissionType.getPermission("GROUPSTATS"))) {
			sender.sendMessage(ChatColor.RED  + "You don't have permission to do this");
		}
		StringBuilder sb = new StringBuilder();
		for(PlayerType type : g.getPlayerTypeHandler().getAllTypes()) {
			sb.append(type.getName());
			sb.append(", ");
		}
		sender.sendMessage("The existing player types for " + g.getName() + " are: " + sb.substring(0, sb.length()-2));
		return true;
	}

	public List<String> tabComplete(CommandSender sender, String[] args) {
		if (!(sender instanceof Player)) {
			return null;
		}
		if (args.length == 0) {
			return GroupTabCompleter.complete(null, null, (Player) sender);
		}
		return GroupTabCompleter.complete(args [0], null, (Player) sender);
	}
}
