package vg.civcraft.mc.namelayer.command.commands;

import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import vg.civcraft.mc.civmodcore.command.PlayerCommand;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameAPI;
import vg.civcraft.mc.namelayer.command.NameLayerTabCompleter;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.permission.PlayerType;

public class JoinGroup extends PlayerCommand {

	public JoinGroup(String name) {
		super(name);
		setIdentifier("nljg");
		setDescription("Join a password protected group.");
		setUsage("/nljg <group> <password>");
		setArguments(2,2);
	}

	@Override
	public boolean execute(CommandSender sender, String[] args) {
		if (!(sender instanceof Player)){
			sender.sendMessage(ChatColor.RED + "How would this even work. Seriously my reddit account is rourke750, explain to me why " +
					"you would ever want to do this from console and I will remove this check.");
			return true;
		}
		Player p = (Player) sender;
		Group g = GroupManager.getGroup(args[0]);
		if (g == null) {
			p.sendMessage(ChatColor.RED + "This group doesn't exist");
			return true;
		}
		if (g.isDisciplined()){
			p.sendMessage(ChatColor.RED + "This group is disciplined.");
			return true;
		}
		if (g.getPassword() == null){
			p.sendMessage(ChatColor.GREEN + "This group does not have a password, so you can't join it.");
			return true;
		}
		if (!g.getPassword().equals(args[1])){
			p.sendMessage(ChatColor.RED + "That password is incorrect");
			return true;
		}
		UUID uuid = NameAPI.getUUID(p.getName());
		PlayerType pType = g.getPlayerTypeHandler().getDefaultPasswordJoinType();
		if (pType == null) {
			p.sendMessage(ChatColor.RED + "No default type for players joining with a password is set for this group. You can't join while this is the case");
			return true;
		}
		if (g.isTracked(uuid)) {
			PlayerType currentType = g.getPlayerType(uuid);
			if (g.getPlayerTypeHandler().isMemberType(currentType)) {
				p.sendMessage(ChatColor.RED + "You are already a member of this group");
			}
			else {
				p.sendMessage(ChatColor.RED + "You are blacklisted on this group");
			}
			return true;
		}
		g.addToTracking(uuid, pType);
		p.sendMessage(ChatColor.GREEN + "You have successfully joined " + g.getName() + " as " + pType.getName());
		return true;
	}

	public List<String> tabComplete(CommandSender sender, String[] args) {
		if (args.length == 0) {
			return NameLayerTabCompleter.completeGroupWithPermission(null, null, (Player) sender);
		}
		else {
			return NameLayerTabCompleter.completeGroupWithPermission(args[0], null, (Player)sender);
		}
	}
}
