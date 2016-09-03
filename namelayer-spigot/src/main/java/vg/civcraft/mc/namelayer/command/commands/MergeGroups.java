package vg.civcraft.mc.namelayer.command.commands;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

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

public class MergeGroups extends PlayerCommand {

	private static boolean active = false;
	public MergeGroups(String name) {
		super(name);
		setIdentifier("nlmg");
		setDescription("Merge two groups together.");
		setUsage("/nlmg <The group left> <The group that will be gone>");
		setArguments(2,2);
	}

	@Override
	public boolean execute(CommandSender sender, String[] args) {
		if (!(sender instanceof Player)){
			sender.sendMessage(ChatColor.BLUE + "Fight me, bet you wont.\n Just back off you don't belong here.");
			return true;
		}
		final Player p = (Player) sender;
		if (active){
			p.sendMessage(ChatColor.RED + "Group merging is currently active. Please wait for this to finish.");
			return true;
		}
		final Group g = GroupManager.getGroup(args[0]);
		if (g == null) {
			p.sendMessage(ChatColor.RED + "This group doesn't exist");
			return true;
		}

		final Group toMerge = GroupManager.getGroup(args[1]);
		if (toMerge == null) {
			p.sendMessage(ChatColor.RED + "This group doesn't exist");
			return true;
		}
		
		if (g.isDisciplined() || toMerge.isDisciplined()){
			p.sendMessage(ChatColor.RED + "One of the groups is disiplined.");
			return true;
		}
		
		if(g == toMerge){
			p.sendMessage(ChatColor.RED + "You cannot merge a group into itself");
			return true;
		}
		final GroupManager gm = NameAPI.getGroupManager();
		UUID uuid = NameAPI.getUUID(p.getName());
		if (!gm.hasAccess(g, uuid, PermissionType.getPermission("MERGE"))){
			p.sendMessage(ChatColor.RED + "You don't have permission on group " + g.getName() + ".");
			return true;
		}
		if (!gm.hasAccess(toMerge, uuid, PermissionType.getPermission("MERGE"))){
			p.sendMessage(ChatColor.RED + "You don't have permission on group " + toMerge.getName() + ".");
			return true;
		}
		
		active = true;
		
		Bukkit.getScheduler().runTaskAsynchronously(NameLayerPlugin.getInstance(), new Runnable(){

			@Override
			public void run() {
				try {
					gm.mergeGroup(g, toMerge);
					p.sendMessage(ChatColor.GREEN + "Group merging is completed.");
				} catch (Exception e) {
					NameLayerPlugin.getInstance().getLogger().log(Level.SEVERE, "Group merging failed", e);
					p.sendMessage(ChatColor.GREEN + "Group merging may have failed.");
				}
				active = false;
				
			}
			
		});
		
		p.sendMessage(ChatColor.GREEN + "Group is under going merge.");
		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		if (!(sender instanceof Player))
			return null;

		if (args.length > 0)
			return NameLayerTabCompleter.completeGroupWithPermission(args[args.length - 1], PermissionType.getPermission("MERGE"), (Player) sender);
		else{
			return NameLayerTabCompleter.completeGroupWithPermission(null, PermissionType.getPermission("MERGE"),(Player)sender);
		}
	}
	
	public static boolean isActive() {
		return active;
	}
	
	public static void setActive(boolean active) {
		MergeGroups.active = active;
	}
}
