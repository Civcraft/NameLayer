package vg.civcraft.mc.namelayer.command;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import vg.civcraft.mc.mercury.MercuryAPI;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameAPI;
import vg.civcraft.mc.namelayer.NameLayerPlugin;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.listeners.PlayerListener;
import vg.civcraft.mc.namelayer.permission.PermissionType;
import vg.civcraft.mc.namelayer.permission.PlayerType;

public class NameLayerTabCompleter {

	public static List<String> completeGroupWithPermission(String lastArg, PermissionType accessLevel, Player sender) {
		UUID uuid = NameAPI.getUUID(sender.getName());
		GroupManager gm = NameAPI.getGroupManager();
		List<String> groups = gm.getAllGroupNames(uuid);
		List<String> fitting_groups = new LinkedList<>();
		List<String> result = new LinkedList<>();

		if (lastArg != null) {
			for (String group : groups) {
				if (group.toLowerCase().startsWith(lastArg.toLowerCase())) {
					fitting_groups.add(group);
				} else {
				}
			}
		} else {
			fitting_groups = groups;
		}

		if (accessLevel == null) {
			return fitting_groups;
		} else {
			for (String group_name : fitting_groups) {
				if (gm.hasAccess(group_name, uuid, accessLevel))
					result.add(group_name);
			}
		}
		return result;
	}

	public static List<String> completeOpenGroupInvite(String lastArg, Player sender) {
		UUID uuid = sender.getUniqueId();
		List<Group> groups = PlayerListener.getNotifications(uuid);
		List<String> result = new LinkedList<>();

		if (groups == null)
			return new ArrayList<>();

		for (Group group : groups) {
			if (lastArg == null || group.getName().toLowerCase().startsWith(lastArg.toLowerCase())) {
				result.add(group.getName());
			}
		}

		return result;
	}

	public static List<String> completePlayerType(String lastArg, Group g) {
		List<String> result = new LinkedList<>();
		if (g == null) {
			return result;
		}
		List<PlayerType> types = g.getPlayerTypeHandler().getAllTypes();
		List<String> type_strings = new LinkedList<>();

		for (PlayerType type : types) {
			type_strings.add(type.getName());
		}

		if (lastArg != null) {
			for (String type : type_strings) {
				if (type.toLowerCase().startsWith(lastArg.toLowerCase()))
					result.add(type);
			}
		} else {
			result = type_strings;
		}

		return result;
	}

	public static List<String> completePermission(String lastArg) {
		List<String> type_strings = new LinkedList<>();
		List<String> result = new LinkedList<>();

		for (PermissionType type : PermissionType.getAllPermissions()) {
			type_strings.add(type.getName());
		}

		if (lastArg != null) {
			for (String type : type_strings) {
				if (type.toLowerCase().startsWith(lastArg.toLowerCase()))
					result.add(type);
			}
		} else {
			result = type_strings;
		}

		return result;
	}
	
	public static List<String> completeOnlinePlayer(String lastArg) {
		List<String> namesToReturn = new ArrayList<String>();
		if (NameLayerPlugin.isMercuryEnabled()) {
			Set<String> players = MercuryAPI.getAllPlayers();
			for (String x: players) {
				if (x.toLowerCase().startsWith(lastArg.toLowerCase()))  {
					namesToReturn.add(x);
				}
			}
		}
		else {
			for (Player p: Bukkit.getOnlinePlayers()) {
				if (p.getName().toLowerCase().startsWith(lastArg.toLowerCase())) {
					namesToReturn.add(p.getName());
				}
			}
		}
		return namesToReturn;
	}
}
