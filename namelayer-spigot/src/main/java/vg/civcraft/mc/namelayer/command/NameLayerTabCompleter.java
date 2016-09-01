package vg.civcraft.mc.namelayer.command;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameAPI;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.listeners.PlayerListener;
import vg.civcraft.mc.namelayer.permission.PermissionType;
import vg.civcraft.mc.namelayer.permission.PlayerType;

public class NameLayerTabCompleter {

	public static List<String> complete(String groupName, String playerName, CommandSender sender) {
		Group g = GroupManager.getGroup(groupName);
		if (g != null || !(sender instanceof Player)) {
			NameAPI.getGroupManager().
			if (NameAPI.getGroupManager().hasAccess(groupName, sender.getUniqueId(),
					PermissionType.getPermission("MEMBERS"))) {
				ArrayList<String> result = new ArrayList<String>();
				List<UUID> uuids = g.getMembersByName(playerName);
				for (UUID uuid : uuids) {
					result.add(NameAPI.getCurrentName(uuid));
				}
				return result;
			}
		}
		return null;
	}

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
}
