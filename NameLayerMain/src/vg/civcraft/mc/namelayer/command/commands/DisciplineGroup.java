package vg.civcraft.mc.namelayer.command.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.command.PlayerCommandMiddle;
import vg.civcraft.mc.namelayer.group.Group;

import java.util.List;

public class DisciplineGroup extends PlayerCommandMiddle{

	public DisciplineGroup(String name) {
		super(name);
		setIdentifier("nldig");
		setDescription("Disable a group from working.");
		setUsage("/nldig <group>");
		setArguments(1,1);
	}

	@Override
	public boolean execute(CommandSender sender, String[] args) {
		if (!(sender instanceof Player))
			sender.sendMessage(ChatColor.AQUA + "Meh, fine, just this one.");
		Player p = (Player) sender;
		Group g = GroupManager.getGroup(args[0]);
		if (groupIsNull(sender, args[0], g)) {
			return true;
		}
		if (!p.isOp() || !p.hasPermission("namelayer.admin")){
			p.sendMessage(ChatColor.RED + "You do not have permission for this op command.");
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
		checkRecacheGroup(g);
		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		return null;
	}
}
