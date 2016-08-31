package vg.civcraft.mc.namelayer.command.commands;

import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import vg.civcraft.mc.civmodcore.command.PlayerCommand;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameAPI;
import vg.civcraft.mc.namelayer.NameLayerPlugin;
import vg.civcraft.mc.namelayer.command.NameLayerTabCompleter;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.permission.PermissionType;
import vg.civcraft.mc.namelayer.permission.PlayerType;

public class GroupStats extends PlayerCommand {

	public GroupStats(String name) {
		super(name);
		setIdentifier("nlgs");
		setDescription("Get stats about a group.");
		setUsage("/nlgs <group>");
		setArguments(1, 1);
	}

	@Override
	public boolean execute(CommandSender sender, String[] args) {
		Group g = GroupManager.getGroup(args[0]);
		if (g == null) {
			sender.sendMessage(ChatColor.RED + "This group doesn't exist");
			return true;
		}
		if (sender instanceof Player) {
			Player p = (Player) sender;
			
			UUID uuid = NameAPI.getUUID(p.getName());	
			if (!NameAPI.getGroupManager().hasAccess(g, uuid, PermissionType.getPermission("GROUPSTATS"))){
				p.sendMessage(ChatColor.RED + "You do not have permission to run that command.");
				return true;
			}
		}
		
		Bukkit.getScheduler().runTaskAsynchronously(NameLayerPlugin.getInstance(), new StatsMessage(sender, g));
		
		return true;
	}


	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		if (!(sender instanceof Player))
			return null;
		if (args.length > 0)
			return NameLayerTabCompleter.completeGroupWithPermission(args[0], PermissionType.getPermission("GROUPSTATS"), (Player) sender);
		else {
			return NameLayerTabCompleter.completeGroupWithPermission(null, PermissionType.getPermission("GROUPSTATS"), (Player) sender);
		}
	}

	public class StatsMessage implements Runnable{

		private final CommandSender sender;
		private final Group g;
		public StatsMessage(CommandSender sender, Group g){
			this.sender = sender;
			this.g = g;
		}
		@Override
		public void run() {
			String message = ChatColor.GREEN + "This group is: " + g.getName() + ".\n";
			for (PlayerType type: g.getPlayerTypeHandler().getAllTypes()){
				String names = "";
				for (UUID uu: g.getAllTrackedByType(type))
					names += NameAPI.getCurrentName(uu) + ", ";
				if (!names.equals("")){
					names = names.substring(0, names.length()-2);
					names += ".";
					message += "The members for PlayerType " + type.getName() + " are: " + names + "\n";
				}
				else
					message += "No members for the PlayerType " + type.getName() + ".\n";
			}
			message += "That makes " + g.getAllTracked().size() + " tracked total.";
			sender.sendMessage(message);
		}
	}
}
