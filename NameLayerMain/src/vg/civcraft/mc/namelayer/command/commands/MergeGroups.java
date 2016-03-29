package vg.civcraft.mc.namelayer.command.commands;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import vg.civcraft.mc.namelayer.GroupManager.PlayerType;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameAPI;
import vg.civcraft.mc.namelayer.NameLayerPlugin;
import vg.civcraft.mc.namelayer.command.PlayerCommandMiddle;
import vg.civcraft.mc.namelayer.command.TabCompleters.GroupTabCompleter;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.permission.GroupPermission;
import vg.civcraft.mc.namelayer.permission.PermissionType;

public class MergeGroups extends PlayerCommandMiddle{

	private boolean active = false;
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
		final Group g = gm.getGroup(args[0]);
		if (groupIsNull(sender, args[0], g)) {
			return true;
		}

		final Group toMerge = gm.getGroup(args[1]);
		if (groupIsNull(sender, args[1], toMerge)) {
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
		
		GroupPermission gPerm = gm.getPermissionforGroup(g);
		GroupPermission mPerm = gm.getPermissionforGroup(toMerge);
		
		UUID uuid = NameAPI.getUUID(p.getName());
		PlayerType p1 = g.getPlayerType(uuid);
		PlayerType p2 = toMerge.getPlayerType(uuid);
		
		if (p1 == null || p2 == null){
			p.sendMessage(ChatColor.RED + "You don't have access for one of the groups.");
			return true;
		}
		
		if (!gPerm.isAccessible(p1, PermissionType.getType("MERGE"))){
			p.sendMessage(ChatColor.RED + "You don't have permission on group " + g.getName() + ".");
			return true;
		}
		if (!mPerm.isAccessible(p2, PermissionType.getType("MERGE"))){
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
			return GroupTabCompleter.complete(args[args.length - 1], PermissionType.getType("MERGE"), (Player) sender);
		else{
			return  GroupTabCompleter.complete(null, PermissionType.getType("MERGE"),(Player)sender);
		}
	}
}
