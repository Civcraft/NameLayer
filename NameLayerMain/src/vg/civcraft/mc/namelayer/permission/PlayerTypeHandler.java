package vg.civcraft.mc.namelayer.permission;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import vg.civcraft.mc.namelayer.NameLayerPlugin;
import vg.civcraft.mc.namelayer.group.Group;
/**
 * The different ranks players can have in a group. Ranks can dynamically be
 * register, deleted and renamed
 */
public class PlayerTypeHandler {

	private Group group;
	private PlayerType root;
	private PlayerType defaultInvitationType;
	private PlayerType defaultPasswordJoinType;
	private Map<String, PlayerType> typesByName;
	private Map<Integer, PlayerType> typesById;
	private final static int MAXIMUM_TYPE_COUNT = 27;
	
	public PlayerTypeHandler(PlayerType root, Group group, boolean saveToDb) {
		this.root = root;
		this.group = group;
		this.typesByName = new HashMap<String, PlayerType>();
		this.typesById = new TreeMap<Integer, PlayerType>();
		typesByName.put(root.getName(), root);
		typesById.put(root.getId(), root);
		if (saveToDb) {
			NameLayerPlugin.getGroupManagerDao().addPlayerType(group, root);
		}
	}

	public boolean doesTypeExist(String name) {
		return typesByName.get(name) != null;
	}
	
	public int getUnusedId() {
		for(int i = 0; i < MAXIMUM_TYPE_COUNT; i++) {
			if (typesById.get(i) == null) {
				return i;
			}
		}
		return -1;
	}
	
	public PlayerType getType(int id) {
		return typesById.get(id);
	}
	
	public PlayerType getType(String name) {
		return typesByName.get(name);
	}

	public List<PlayerType> getAllTypes() {
		return new ArrayList<PlayerType>(typesByName.values());
	}
	
	public PlayerType getBlacklistedType() {
		return typesById.get(5);
	}
	
	public PlayerType getOwnerType() {
		return typesById.get(0);
	}
	
	public PlayerType getDefaultNonMemberType() {
		return typesById.get(4);
	}
	
	public PlayerType getDefaultInvitationType() {
		return defaultInvitationType;
	}
	
	public PlayerType getDefaultPasswordJoinType() {
		return defaultPasswordJoinType;
	}

	public void deleteType(PlayerType type, boolean saveToD) {
		typesByName.remove(type.getName());
		typesById.remove(type.getId());
		if (saveToD) {
			NameLayerPlugin.getGroupManagerDao().removePlayerType(group, type);
		}
	}
	
	public boolean registerType(PlayerType type, boolean saveToDb) {
		if (type == null || type.getParent() == null || doesTypeExist(type.getName()) || !doesTypeExist(type.getParent().getName())) {
			return false;
		}
		typesByName.put(type.getName(), type);
		typesById.put(type.getId(), type);
		if (saveToDb) {
			NameLayerPlugin.getGroupManagerDao().addPlayerType(group, type);
		}
		return true;
	}
	
	public boolean isRelated(PlayerType child, PlayerType parent) {
		PlayerType currentParent = child.getParent();
		while(currentParent != null) {
			if (currentParent.equals(parent)) {
				return true;
			}
			currentParent = currentParent.getParent();
		}
		return false;
	}
	
	public void renameType(PlayerType type, String name) {
		typesByName.remove(type.getName());
		type.setName(name);
		typesByName.put(name, type);
		NameLayerPlugin.getGroupManagerDao().updatePlayerTypeName(group, type);
	}
	
	public static PlayerTypeHandler createStandardTypes(Group g) {
		PlayerType owner = new PlayerType("OWNER", 0, null, g);
		PlayerTypeHandler handler = new PlayerTypeHandler(owner, g, true);
		for(PermissionType perm : PermissionType.getAllPermissions()) {
			owner.addPermission(perm, true);
		}
		PlayerType admin = new PlayerType("ADMINS", 1, owner, g);
		handler.registerType(admin, true);
		for(PermissionType perm : PermissionType.getAllPermissions()) {
			if (perm.getDefaultPermLevels().contains(1)) {
				admin.addPermission(perm, true);
			}
		}
		PlayerType mod = new PlayerType("MODS", 2, admin, g);
		handler.registerType(mod, true);
		for(PermissionType perm : PermissionType.getAllPermissions()) {
			if (perm.getDefaultPermLevels().contains(2)) {
				mod.addPermission(perm, true);
			}
		}
		PlayerType member = new PlayerType("MEMBERS", 3, mod, g);
		handler.registerType(member, true);
		for(PermissionType perm : PermissionType.getAllPermissions()) {
			if (perm.getDefaultPermLevels().contains(3)) {
				member.addPermission(perm, true);
			}
		}
		PlayerType defaultNonMember = new PlayerType("DEFAULT", 4, owner, g);
		handler.registerType(defaultNonMember, true);
		for(PermissionType perm : PermissionType.getAllPermissions()) {
			if (perm.getDefaultPermLevels().contains(4)) {
				defaultNonMember.addPermission(perm, true);
			}
		}
		PlayerType blacklisted = new PlayerType("BLACKLISTED", 5, defaultNonMember, g);
		handler.registerType(blacklisted, true);
		for(PermissionType perm : PermissionType.getAllPermissions()) {
			if (perm.getDefaultPermLevels().contains(5)) {
				blacklisted.addPermission(perm, true);
			}
		}
		handler.defaultInvitationType = member;
		handler.defaultPasswordJoinType = member;
		return handler; 
	}
}
