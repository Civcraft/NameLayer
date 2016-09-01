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

	public PlayerTypeHandler(PlayerType root, Group group) {
		this.root = root;
		this.group = group;
		this.typesByName = new HashMap<String, PlayerType>();
		this.typesById = new TreeMap<Integer, PlayerType>();
		typesByName.put(root.getName(), root);
		typesById.put(root.getId(), root);
		for (PlayerType type : root.getChildren(true)) {
			typesByName.put(type.getName(), type);
			typesById.put(type.getId(), type);
		}
	}

	/**
	 * Checks whether this instance has a player type with the given name
	 * 
	 * @param name
	 *            Name to check for
	 * @return True if such a player type exists, false if not
	 */
	public boolean doesTypeExist(String name) {
		return typesByName.get(name) != null;
	}

	/**
	 * @return Highest unused id available for this instance or -1 if no id is
	 *         available
	 */
	public int getUnusedId() {
		for (int i = 0; i < MAXIMUM_TYPE_COUNT; i++) {
			if (typesById.get(i) == null) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Retrieves a PlayerType by it's id
	 * 
	 * @param id
	 * @return PlayerType with that id or null if no such player type exists
	 */
	public PlayerType getType(int id) {
		return typesById.get(id);
	}

	/**
	 * Retrieves a PlayerType by it's name
	 * 
	 * @param name
	 * @return PlayerType with that id or null if no such player type exists
	 */
	public PlayerType getType(String name) {
		return typesByName.get(name);
	}

	/**
	 * @return All player types this instance is tracking
	 */
	public List<PlayerType> getAllTypes() {
		return new ArrayList<PlayerType>(typesByName.values());
	}

	/**
	 * Each instance has an undeleteable player type, which is initially called
	 * "Owner" and will always be the root of the tree graph representing this
	 * instance's permission hierarchy. This player type will additionally
	 * always have the id 0.
	 * 
	 * @return Owner player type
	 */
	public PlayerType getOwnerType() {
		return typesById.get(0);
	}

	/**
	 * Additionally to the owner-root type, there is a second non-deleteable
	 * type, the non-member type. By default any player will have this player
	 * type for any group, unless the player is a member of the group or
	 * explicitly blacklisted. This type is always a child node of the owner
	 * type and always has the id 4
	 * 
	 * @return Default-NonMember Player type
	 */
	public PlayerType getDefaultNonMemberType() {
		return typesById.get(4);
	}

	/**
	 * When inviting new players to a group, the inviting player may chose to
	 * not explicitly specify a playertype as which the invitee is invited. If
	 * this is the case, the player will be invited as this player type. If this
	 * player type is not specified, inviting without naming a specific player
	 * type will not work
	 * 
	 * @return Default player type for invitation
	 */
	public PlayerType getDefaultInvitationType() {
		return defaultInvitationType;
	}

	/**
	 * If a player joins a group by password, no specific player type can be
	 * specified for him in particular, so anyone joining will be assigned this
	 * player type. If this player type is not specified, joining a group with a
	 * password is not possible
	 * 
	 * @return Default player type for anyone joining a group via password
	 */
	public PlayerType getDefaultPasswordJoinType() {
		return defaultPasswordJoinType;
	}

	/**
	 * Deletes the given player type from this instance. If this player type
	 * still has any children, they will all be deleted recursively
	 * 
	 * @param type
	 *            Player type to delete
	 * @param saveToD
	 *            Whether the action should be persisted to the database and
	 *            broadcasted via Mercury
	 */
	public void deleteType(PlayerType type, boolean saveToD) {
		List<PlayerType> types = type.getChildren(true);
		// retrieving children deep is implemented as deep search, so deleting
		// nodes
		// in reverse is guaranteed to respect the tree structure and clean up
		// everything below the parent node
		for (int i = types.size() - 1; i >= 0; i--) {
			deleteType(types.get(i), saveToD);
		}
		if (type.getParent() != null) {
			type.getParent().removeChild(type);
		}
		typesByName.remove(type.getName());
		typesById.remove(type.getId());
		if (saveToD) {
			NameLayerPlugin.getGroupManagerDao().removePlayerType(group, type);
		}
	}

	/**
	 * Registers the given player type for this instance
	 * 
	 * @param type
	 *            Player type to add
	 * @param saveToD
	 *            Whether the action should be persisted to the database and
	 *            broadcasted via Mercury
	 */
	public boolean registerType(PlayerType type, boolean saveToDb) {
		//we can always assume that the register type has a parent here, because the root is created a different way and 
		//all other nodes should have a parent
		if (type == null || type.getParent() == null || doesTypeExist(type.getName())
				|| !doesTypeExist(type.getParent().getName())) {
			return false;
		}
		typesByName.put(type.getName(), type);
		typesById.put(type.getId(), type);
		if (saveToDb) {
			NameLayerPlugin.getGroupManagerDao().registerPlayerType(group, type);
		}
		return true;
	}

	/**
	 * Checks whether the given PlayerTypes are related in the sense that the
	 * first one is an (indirect) child node of the second one. Important to
	 * avoid cycles.
	 * 
	 * @param child
	 *            Child node in the relation to check for
	 * @param parent
	 *            Parent node in the relation to check for
	 * @return True if the first parameter is a child of the second one, false
	 *         in all other cases
	 */
	public boolean isRelated(PlayerType child, PlayerType parent) {
		PlayerType currentParent = child.getParent();
		while (currentParent != null) {
			if (currentParent.equals(parent)) {
				return true;
			}
			currentParent = currentParent.getParent();
		}
		return false;
	}

	/**
	 * Renames the given player type and updates it's name to the given one
	 * 
	 * @param type
	 *            Player type to update
	 * @param name
	 *            New name for the player type
	 * @param writeToDb
	 *            Whether the action should be persisted to the database and
	 *            broadcasted via Mercury
	 */
	public void renameType(PlayerType type, String name, boolean writeToDb) {
		typesByName.remove(type.getName());
		type.setName(name);
		typesByName.put(name, type);
		if (writeToDb) {

		}
	}

	/**
	 * Creates a set of standard permissions, which are assigned to any group
	 * when it is initially created. This roughly follows the permission schema
	 * NameLayer used to have when it's player types were completly static
	 * 
	 * @param g
	 *            Group for which permissions should be created
	 * @return Completly initialized PlayerTypeHandler for new group
	 */
	public static PlayerTypeHandler createStandardTypes(Group g) {
		PlayerType owner = new PlayerType("OWNER", 0, null, g);
		PlayerTypeHandler handler = new PlayerTypeHandler(owner, g);
		for (PermissionType perm : PermissionType.getAllPermissions()) {
			owner.addPermission(perm, true);
		}
		PlayerType admin = new PlayerType("ADMINS", 1, owner, g);
		handler.registerType(admin, true);
		for (PermissionType perm : PermissionType.getAllPermissions()) {
			if (perm.getDefaultPermLevels().contains(1)) {
				admin.addPermission(perm, true);
			}
		}
		PlayerType mod = new PlayerType("MODS", 2, admin, g);
		handler.registerType(mod, true);
		for (PermissionType perm : PermissionType.getAllPermissions()) {
			if (perm.getDefaultPermLevels().contains(2)) {
				mod.addPermission(perm, true);
			}
		}
		PlayerType member = new PlayerType("MEMBERS", 3, mod, g);
		handler.registerType(member, true);
		for (PermissionType perm : PermissionType.getAllPermissions()) {
			if (perm.getDefaultPermLevels().contains(3)) {
				member.addPermission(perm, true);
			}
		}
		PlayerType defaultNonMember = new PlayerType("DEFAULT", 4, owner, g);
		handler.registerType(defaultNonMember, true);
		for (PermissionType perm : PermissionType.getAllPermissions()) {
			if (perm.getDefaultPermLevels().contains(4)) {
				defaultNonMember.addPermission(perm, true);
			}
		}
		PlayerType blacklisted = new PlayerType("BLACKLISTED", 5, defaultNonMember, g);
		handler.registerType(blacklisted, true);
		for (PermissionType perm : PermissionType.getAllPermissions()) {
			if (perm.getDefaultPermLevels().contains(5)) {
				blacklisted.addPermission(perm, true);
			}
		}
		handler.defaultInvitationType = member;
		handler.defaultPasswordJoinType = member;
		return handler;
	}
}