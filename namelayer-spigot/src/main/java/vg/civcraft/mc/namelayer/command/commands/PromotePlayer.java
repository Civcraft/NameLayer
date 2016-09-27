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
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.misc.MercuryManager;
import vg.civcraft.mc.namelayer.permission.PermissionType;
import vg.civcraft.mc.namelayer.permission.PlayerType;
import vg.civcraft.mc.namelayer.permission.PlayerTypeHandler;
import vg.civcraft.mc.namelayer.command.NameLayerTabCompleter;
import vg.civcraft.mc.namelayer.events.PromotePlayerEvent;

public class PromotePlayer extends PlayerCommand {

	public PromotePlayer(String name) {
		super(name);
		setIdentifier("nlpp");
		setDescription("Promote/Demote a Player in a Group");
		setUsage("/nlpp <group> <player> <playertype>");
		setArguments(3,3);
	}

	@Override
	public boolean execute(CommandSender sender, String[] args) {
		if (!(sender instanceof Player)){
			sender.sendMessage("How about No?");
			return true;
		}
		
		Player p = (Player) sender;
		
		UUID executor = NameAPI.getUUID(p.getName());
		
		UUID promotee = NameAPI.getUUID(args[1]);
		
		if(promotee ==null){
			p.sendMessage(ChatColor.RED + "That player does not exist");
			return true;
		}
		
		if(promotee.equals(executor)){
			p.sendMessage(ChatColor.RED + "You can't promote yourself");
			return true;
		}
		
		Group group = GroupManager.getGroup(args[0]);
		if (group == null) {
			p.sendMessage(ChatColor.RED + "This group doesn't exist");
			return true;
		}
		if (group.isDisciplined()){
			p.sendMessage(ChatColor.RED + "This group is disciplined.");
			return true;
		}
		PlayerTypeHandler handler = group.getPlayerTypeHandler();
		PlayerType promoteecurrentType = group.getPlayerType(promotee);
		PlayerType targetType = handler.getType(args[2]);
		if(targetType == null){
			sender.sendMessage(ChatColor.RED + args [2] + " is not a valid player type for " + group.getName());
			return true;
		}
		
		GroupManager gm = NameAPI.getGroupManager();
		if (!gm.hasAccess(group, executor, PermissionType.getRemovePermission(promoteecurrentType.getId()))) {
			sender.sendMessage(ChatColor.RED + "You don't have permission to remove players from " + promoteecurrentType.getName());
			return true;
		}
		
		if (!gm.hasAccess(group, executor, PermissionType.getInvitePermission(targetType.getId()))) {
			sender.sendMessage(ChatColor.RED + "You don't have permission to add players to " + targetType.getName());
			return true;
		}
		
		if (!group.isMember(promotee)){ //can't edit a player who isn't in the group
			p.sendMessage(ChatColor.RED + NameAPI.getCurrentName(promotee) + " is not a member of this group.");
			return true;
		}
		
		if (group.isOwner(promotee)){
			p.sendMessage(ChatColor.RED + "That player owns the group, you can't demote the player.");
			return true;
		}
		PromotePlayerEvent event = new PromotePlayerEvent(promotee, group, promoteecurrentType, targetType);
		Bukkit.getPluginManager().callEvent(event);
		if(event.isCancelled()){
			return false;
		}
		Player promoted = Bukkit.getPlayer(NameAPI.getCurrentName(promotee));
		if(promoted != null){
			promoted.sendMessage(ChatColor.GREEN + "Your rank in " + group.getName() + " was changed from " 
					+ promoteecurrentType.getName() + " to " + targetType.getName() + " by " + p.getName());
		}
		else {
			MercuryManager.notifyPromote(group.getName(), promotee, executor, promoteecurrentType.getName(), targetType.getName());
		}
		group.removeFromTracking(promotee);
		group.addToTracking(promotee, targetType);
		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		if (!(sender instanceof Player)){
			sender.sendMessage(ChatColor.RED + "I'm sorry baby, please run this as a player :)");
			return null;
		}
		if (args.length < 2) {
			if (args.length == 0) {
				return NameLayerTabCompleter.completeGroupWithPermission(null, null, (Player) sender);
			}
			else {
				return NameLayerTabCompleter.completeGroupWithPermission(args[0], null, (Player)sender);
			}

		} else if (args.length == 2) {
			return NameLayerTabCompleter.completeOnlinePlayer(args[1]);
		}
		else if (args.length == 3)
			return NameLayerTabCompleter.completePlayerType(args [2], GroupManager.getGroup(args [0]));

		else return null;
	}

}