package vg.civcraft.mc.namelayer.command.commands;

import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import vg.civcraft.mc.civmodcore.command.PlayerCommand;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameAPI;
import vg.civcraft.mc.namelayer.NameLayerPlugin;
import vg.civcraft.mc.namelayer.command.NameLayerTabCompleter;
import vg.civcraft.mc.namelayer.group.DefaultGroupHandler;
import vg.civcraft.mc.namelayer.group.Group;

public class SetDefaultGroup extends PlayerCommand {
	
	private DefaultGroupHandler handler;

	public SetDefaultGroup(String name) {
		super(name);
		setIdentifier("nlsdg");
		setDescription("Set or change a default group");
		setUsage("/nlsdg <group>");
		setArguments(1,1);
		handler = NameLayerPlugin.getDefaultGroupHandler();
	}

	@Override
	public boolean execute(CommandSender sender, String[] args) {
		if (!(sender instanceof Player)){
			sender.sendMessage("I don't think you need to do that.");
			return true;
		}
		Player p = (Player) sender;
		UUID uuid = NameAPI.getUUID(p.getName());
		Group g = GroupManager.getGroup(args[0]);
		if (g == null) {
			p.sendMessage(ChatColor.RED + "This group doesn't exist");
			return true;
		}
		String x = handler.getDefaultGroup(uuid);
		if(x == null){
			handler.setDefaultGroup(uuid, g);
			p.sendMessage(ChatColor.GREEN + "You have set your default group to " + g.getName());
		}
		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		if (!(sender instanceof Player))
			return null;
		if (args.length > 0)
			return NameLayerTabCompleter.completeGroupWithPermission(args[0], null, (Player) sender);
		else {
			return NameLayerTabCompleter.completeGroupWithPermission(null, null, (Player) sender);
		}
	}
}
