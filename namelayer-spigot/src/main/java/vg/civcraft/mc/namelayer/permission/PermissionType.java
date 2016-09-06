package vg.civcraft.mc.namelayer.permission;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.bukkit.Bukkit;

import vg.civcraft.mc.namelayer.NameLayerPlugin;

/**
 * Allows creating and retrieving instances in a static way, while also
 * representing an instance of a single permission. A permission can be
 * identified by both it's id and it's name, so both have to be unique.
 * Permissions are handled as singleton each, you can assume that for a
 * PermissionType with a given name or id always maximum one instance will exist
 *
 */
public class PermissionType {

	private static Map<String, PermissionType> permissionByName;
	private static Map<Integer, PermissionType> permissionById;
	private static int maximumExistingId;

	public static void initialize() {
		permissionByName = new HashMap<String, PermissionType>();
		permissionById = new TreeMap<Integer, PermissionType>();
		maximumExistingId = 0;
		registerNameLayerPermissions();
	}

	/**
	 * Retrieves a permission by it's name
	 * 
	 * @param name
	 *            Name of the permission
	 * @return Permission with the given name or null if no such permission
	 *         exists
	 */
	public static PermissionType getPermission(String name) {
		return permissionByName.get(name);
	}

	/**
	 * Retrieves a permission by it's id
	 * 
	 * @param id
	 *            Id of the permission
	 * @return Permission with the given id or null if no such permission exists
	 */
	public static PermissionType getPermission(int id) {
		return permissionById.get(id);
	}

	/**
	 * Only used internally, specify your permissions with a description instead
	 */
	private static void registerPermission(String name, List<Integer> defaultPermLevels) {
		registerPermission(name, defaultPermLevels, null);
	}

	/**
	 * Allows external plugins to register their own permissions. Additionally
	 * to a name and description, they can specify a list of permission levels,
	 * which will get this permision by default, when a new group is created.
	 * This follows a static mapping: 1 = Admin, 2 = Mod, 3 = Member, 4 =
	 * DefaultNonMember, 5 = Blacklisted Owner with an id of 0 will
	 * automatically have the permission, as it does with all others
	 * 
	 * This will not be applied to already existing groups, as they might have a
	 * different structure than the one this is intended to be applied to.
	 * 
	 * If a permission with the given name was already registed, doing so again
	 * will fail without any further issues
	 * 
	 * @param name
	 * @param defaultPermLevels
	 * @param description
	 */
	public static void registerPermission(String name, List<Integer> defaultPermLevels, String description) {
		if (name == null) {
			Bukkit.getLogger().severe("Could not register permission, name was null");
			return;
		}
		if (permissionByName.get(name) != null) {
			Bukkit.getLogger().severe("Could not register permission " + name + ". It was already registered");
			return;
		}
		int id = -1;
		Map<Integer, String> dbRegisteredPerms = NameLayerPlugin.getGroupManagerDao().getPermissionMapping();
		for (Entry<Integer, String> perm : dbRegisteredPerms.entrySet()) {
			if (perm.getValue().equals(name)) {
				id = perm.getKey();
				break;
			}
		}
		PermissionType p;
		if (id == -1) {
			// not in db yet
			id = maximumExistingId + 1;
			while (dbRegisteredPerms.get(id) != null) {
				id++;
			}
			maximumExistingId = id;
			p = new PermissionType(name, id, defaultPermLevels, description);
			NameLayerPlugin.getGroupManagerDao().registerPermission(p);
		} else {
			// already in db, so use existing id
			p = new PermissionType(name, id, defaultPermLevels, description);
		}
		permissionByName.put(name, p);
		permissionById.put(id, p);
	}

	/**
	 * Gets the permission type required to invite or add players to the player
	 * type with the given id. This permission is independent from the group it
	 * is applied to, it will allow inviting to the player type with the given
	 * id for any group
	 * 
	 * @param id
	 *            ID of the player type to get the invite permission for
	 * @return Invite permission for the given id
	 */
	public static PermissionType getInvitePermission(int id) {
		String invitePermName = "invitePlayer#" + id;
		PermissionType invPerm = PermissionType.getPermission(invitePermName);
		if (invPerm == null) {
			// register type, because it was never used before, we do this with
			// the deprecated register without a description
			// because any further description is handled by the UI and
			// dependent on the current name of the rank
			registerPermission(invitePermName, new LinkedList<Integer>());
			invPerm = getPermission(invitePermName);
		}
		return invPerm;
	}

	/**
	 * Gets the permission type required to remove players from the player type
	 * with the given id. This permission is independent from the group it is
	 * applied to, it will allow removing from the player type with the given id
	 * for any group
	 * 
	 * @param id
	 *            ID of the player type to get the remove permission for
	 * @return Remove permission for the given id
	 */
	public static PermissionType getRemovePermission(int id) {
		String removePermName = "removePlayer#" + id;
		PermissionType removePerm = PermissionType.getPermission(removePermName);
		if (removePerm == null) {
			// register type, because it was never used before, we do this with
			// the deprecated register without a description
			// because any further description is handled by the UI and
			// dependent on the current name of the rank
			registerPermission(removePermName, new LinkedList<Integer>());
			removePerm = getPermission(removePermName);
		}
		return removePerm;
	}
	
	/**
	 * Gets the permission type required to list players for a player type
	 * with the given id. This permission is independent from the group it is
	 * applied to, it will allow listing members for the player type with the given id
	 * for any group
	 * 
	 * @param id
	 *            ID of the player type to get the list permission for
	 * @return List permission for the given id
	 */
	public static PermissionType getListPermission(int id) {
		String listPermName = "listPlayer#" + id;
		PermissionType listPerm = PermissionType.getPermission(listPermName);
		if (listPerm == null) {
			// register type, because it was never used before, we do this with
			// the deprecated register without a description
			// because any further description is handled by the UI and
			// dependent on the current name of the rank
			registerPermission(listPermName, new LinkedList<Integer>());
			listPerm = getPermission(listPermName);
		}
		return listPerm;
	}

	/**
	 * Checks if the given PermissionType is a player type removal permission
	 * and if it is, it returns the id for which this permission allows removing
	 * members. If it is not a removal permission -1 will be returned
	 * 
	 * @param perm
	 *            PermissionType to check
	 * @return removal id of the given perm or -1 if it isn't a removal perm
	 */
	public static int getRemovePermissionId(PermissionType perm) {
		if (perm.getDescription() != null) {
			return -1;
		}
		String [] parts = perm.getName().split("#");
		if (parts.length != 2) {
			return -1;
		}
		if (!parts [0].equals("removePlayer")) {
			return -1;
		}
		try {
			return Integer.parseInt(parts[1]);
		}
		catch(NumberFormatException e) {
			return -1;
		}
	}
	
	/**
	 * Checks if the given PermissionType is a player type invite permission
	 * and if it is, it returns the id for which this permission allows inviting
	 * members. If it is not an invite permission -1 will be returned
	 * 
	 * @param perm
	 *            PermissionType to check
	 * @return invite id of the given perm or -1 if it isn't a invitation perm
	 */
	public static int getInvitePermissionId(PermissionType perm) {
		if (perm.getDescription() != null) {
			return -1;
		}
		String [] parts = perm.getName().split("#");
		if (parts.length != 2) {
			return -1;
		}
		if (!parts [0].equals("invitePlayer")) {
			return -1;
		}
		try {
			return Integer.parseInt(parts[1]);
		}
		catch(NumberFormatException e) {
			return -1;
		}
	}

	/**
	 * Checks if the given PermissionType is a player type list permission
	 * and if it is, it returns the id for which this permission allows listing
	 * members. If it is not a list permission -1 will be returned
	 * 
	 * @param perm
	 *            PermissionType to check
	 * @return list id of the given perm or -1 if it isn't a listing perm
	 */
	public static int getListPermissionId(PermissionType perm) {
		if (perm.getDescription() != null) {
			return -1;
		}
		String [] parts = perm.getName().split("#");
		if (parts.length != 2) {
			return -1;
		}
		if (!parts [0].equals("listPlayer")) {
			return -1;
		}
		try {
			return Integer.parseInt(parts[1]);
		}
		catch(NumberFormatException e) {
			return -1;
		}
	}
	
	/**
	 * @return All existing permissions
	 */
	public static Collection<PermissionType> getAllPermissions() {
		return permissionByName.values();
	}

	/**
	 * Initializes all the permissions NameLayer uses internally
	 */
	private static void registerNameLayerPermissions() {
		LinkedList<Integer> modAndAbove = new LinkedList<Integer>();
		LinkedList<Integer> adminAndAbove = new LinkedList<Integer>();
		LinkedList<Integer> owner = new LinkedList<Integer>();
		LinkedList<Integer> all = new LinkedList<Integer>();
		modAndAbove.add(1);
		modAndAbove.add(2);
		adminAndAbove.add(1);
		all.add(1);
		all.add(2);
		all.add(3);
		// clone the list every time so changing the list of one perm later
		// doesn't affect other perms

		// allows adding/modifying a password for the group
		registerPermission("PASSWORD", (LinkedList<Integer>) adminAndAbove.clone(),
				"Allows viewing this groups password and changing or removing it");
		// allows to list the permissions for each permission group
		registerPermission("LIST_PERMS", (LinkedList<Integer>) adminAndAbove.clone(),
				"Allows viewing how permission for this group are set up");
		// allows to see general group stats
		registerPermission("GROUPSTATS", (LinkedList<Integer>) adminAndAbove.clone(),
				"Gives access to various group statistics such as member "
						+ "counts by permission type, who owns the group etc.");
		// allows to modify the permissions for different permissions groups
		registerPermission("PERMS", (LinkedList<Integer>) owner.clone(), "Allows modifying permissions for this group");
		// allows deleting the group
		registerPermission("DELETE", (LinkedList<Integer>) owner.clone(), "Allows deleting this group");
		// allows merging the group with another one
		registerPermission("MERGE", (LinkedList<Integer>) owner.clone(),
				"Allows merging this group into another or merging another group into this one");
		// allows creating player types
		registerPermission("CREATE_PLAYERTYPE", (LinkedList<Integer>) owner.clone(),
				"Allows creating new player types for this group");
		// allows deleting player types
		registerPermission("DELETE_PLAYERTYPE", (LinkedList<Integer>) owner.clone(),
				"Allows deleting player types for this group");
		// allows deleting player types
		registerPermission("RENAME_PLAYERTYPE", (LinkedList<Integer>) owner.clone(),
				"Allows renaming player types for this group");
		// allows opening the gui
		registerPermission("OPEN_GUI", (LinkedList<Integer>) all.clone(), "Allows opening the GUI for this group");
	}

	private String name;
	private List<Integer> defaultPermLevels;
	private int id;
	private String description;

	private PermissionType(String name, int id, List<Integer> defaultPermLevels, String description) {
		this.name = name;
		this.id = id;
		this.defaultPermLevels = defaultPermLevels;
		this.description = description;
	}

	/**
	 * @return Name of this permission
	 */
	public String getName() {
		return name;
	}

	/**
	 * List containing all player types, which will automatically get this
	 * permission when a new group is created. Player types are identified by id
	 * here: 1 = Admin, 2 = Mod, 3 = Member, 4 = DefaultNonMember, 5 =
	 * Blacklisted. 0, which is owner wont be in the list explicitly, but is
	 * implied to always have all permissions on group creation
	 * 
	 * @return All player type levels which get this permission by default
	 */
	public List<Integer> getDefaultPermLevels() {
		return defaultPermLevels;
	}

	/**
	 * @return Id of this permission
	 */
	public int getId() {
		return id;
	}

	/**
	 * @return Description of this permission, which is displayed to players
	 */
	public String getDescription() {
		return description;
	}
}