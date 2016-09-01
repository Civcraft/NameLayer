package vg.civcraft.mc.namelayer.command.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import vg.civcraft.mc.civmodcore.command.PlayerCommand;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.group.Group;

import java.util.List;

public class DisciplineGroup extends PlayerCommand{

	public DisciplineGroup(String name) {
		super(name);
		setIdentifier("nldig");
		setDescription("Disable a group from working.");
		setUsage("/nldig <group>");
		setArguments(1,1);
	}

	@Override
	public boolean execute(CommandSender sender, String[] args) {
		//permission handling for this group is done by plugin.yml
		Group g = GroupManager.getGroup(args[0]);
		if (g == null) {
			sender.sendMessage(ChatColor.RED + "This group doesn't exist");
			return true;
		}
		if (g.isDisciplined()){
			g.setDisciplined(false);
			sender.sendMessage(ChatColor.GREEN + "Group has been enabled.");
		}
		else{
			g.setDisciplined(true);
			sender.sendMessage(ChatColor.GREEN + "Group has been disabled.");
		}
		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		return null;
	}
}
