package vg.civcraft.mc.namelayer.command.commands;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import vg.civcraft.mc.civmodcore.command.PlayerCommand;
import vg.civcraft.mc.namelayer.NameLayerPlugin;

public class GetDefaultGroup extends PlayerCommand {

	public GetDefaultGroup(String name) {
		super(name);
		setIdentifier("nlgdg");
		setDescription("Get your default group");
		setUsage("/nlgdg");
		setArguments(0,0);
	}

	@Override
	public boolean execute(CommandSender sender, String[] args) {
		if (!(sender instanceof Player)){
			sender.sendMessage("I don't think you need to do that.");
			return true;
		}
		Player p = (Player) sender;
		String x = NameLayerPlugin.getDefaultGroupHandler().getDefaultGroup(p);
		if(x == null){
			p.sendMessage(ChatColor.RED + "You do not currently have a default group use /nlsdg to set it");
		}
		else{
			p.sendMessage(ChatColor.GREEN + "Your current default group is " + x);
		}
		
		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		return null;
	}
}
