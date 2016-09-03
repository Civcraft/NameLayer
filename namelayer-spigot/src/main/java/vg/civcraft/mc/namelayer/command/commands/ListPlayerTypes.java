package vg.civcraft.mc.namelayer.command.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import vg.civcraft.mc.civmodcore.command.PlayerCommand;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameLayerPlugin;
import vg.civcraft.mc.namelayer.command.NameLayerTabCompleter;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.permission.PermissionType;
import vg.civcraft.mc.namelayer.permission.PlayerType;

import java.util.List;

public class ListPlayerTypes extends PlayerCommand {

	public ListPlayerTypes(String name) {
		super(name);
		setIdentifier("nllpt");
		setDescription("List PlayerTypes.");
		setUsage("/nllpt [group]");
		setArguments(0, 1);
	}

	@Override
	public boolean execute(CommandSender sender, String[] args) {
		if (args.length == 0 && !(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED
					+ "You dont have a default group, so you need to spefiy for which group you want to see player types");
			return true;
		}
		Group group;
		if (args.length == 1)  {
			group = GroupManager.getGroup(args [0]);
		}
		else {
			group = GroupManager.getGroup(NameLayerPlugin.getDefaultGroupHandler().getDefaultGroup(((Player) sender)));
		}
		if (group == null) {
			sender.sendMessage(ChatColor.RED + "This group doesn't exist");
			return true;
		}
		StringBuilder sb = new StringBuilder();
		sb.append(ChatColor.GREEN + "Player types for " + group.getName() + " are: ");
		for(PlayerType type: group.getPlayerTypeHandler().getAllTypes()) {
			sb.append(type.getName());
			sb.append(" ");
		}
		sender.sendMessage(sb.toString());
		return true;
	}

	public List<String> tabComplete(CommandSender sender, String[] args) {
		if(!(sender instanceof Player)) { 
			return null;
		}
		if (args.length == 0) {
			return NameLayerTabCompleter.completeGroupWithPermission(null, PermissionType.getPermission("LIST_PLAYERTYPES"), (Player) sender);
		}
		else {
			return NameLayerTabCompleter.completeGroupWithPermission(args [0], PermissionType.getPermission("LIST_PLAYERTYPES"), (Player) sender);
		}
	}
}
