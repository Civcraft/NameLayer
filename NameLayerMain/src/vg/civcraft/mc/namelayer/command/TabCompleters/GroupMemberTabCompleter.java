package vg.civcraft.mc.namelayer.command.TabCompleters;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Player;

import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameAPI;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.permission.PlayerType;

public class GroupMemberTabCompleter {
	public static List<String> complete(String groupName, String playerName,
			Player sender) {
		Group g = GroupManager.getGroup(groupName);
		if (g != null && g.isMember(sender.getUniqueId())) {
				ArrayList<String> result = new ArrayList<String>();
				List<UUID> uuids = g.getMembersByName(playerName);
				PlayerType senderType = g.getPlayerType(sender.getUniqueId());
				for (UUID uuid : uuids) {
					if (g.getPlayerTypeHandler().isRelated(g.getPlayerType(uuid), senderType)) {
						result.add(NameAPI.getCurrentName(uuid));
					}
				}
				return result;
			}
		return null;
	}
}