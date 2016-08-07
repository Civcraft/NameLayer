package vg.civcraft.mc.namelayer.command.TabCompleters;

import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.permission.PlayerType;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by isaac on 2/2/2015.
 *
 * Used by tab completers to get a list of user types
 */
public class MemberTypeCompleter {

	public static List<String> complete(Group g, String lastArg) {
		List<String> type_strings = new LinkedList<>();
		List<String> result = new LinkedList<>();

		if (g == null) {
			return result;
		}

		for (PlayerType type : g.getPlayerTypeHandler().getAllTypes()) {
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
