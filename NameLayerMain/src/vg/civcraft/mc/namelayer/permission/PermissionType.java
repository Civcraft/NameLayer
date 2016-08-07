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


public class PermissionType {
	
	private static Map <String, PermissionType> permissionByName;
	private static Map <Integer, PermissionType> permissionById;
	private static int maximumExistingId;
	
	public static void initialize() {
		permissionByName = new HashMap<String, PermissionType>();
		permissionById = new TreeMap<Integer, PermissionType>();
		maximumExistingId = 0;
		registerNameLayerPermissions();
	}
	
	public static PermissionType getPermission(String name) {
		return permissionByName.get(name);
	}
	
	public static PermissionType getPermission(int id) {
		return permissionById.get(id);
	}
	
	public static void registerPermission(String name, List <Integer> defaultPermLevels) {
		registerPermission(name, defaultPermLevels, null);
	}
	
	/**
	 * Allows external plugins to register their own permissions. Additionally to a name and description, they can specify a list of permission levels, which will
	 * get this permision by default, when a new group is created. This follows a static mapping: 1 = Admin, 2 = Mod, 3 = Member, 4 = DefaultNonMember, 5 = Blacklisted
	 * Owner with an id of 0 will automatically have the permission, as it does with all others
	 * 
	 * This will not be applied to already existing groups, as they might have a different structure than the one this is intended to be applied to
	 * 
	 * @param name
	 * @param defaultPermLevels
	 * @param description
	 */
	public static void registerPermission(String name, List <Integer> defaultPermLevels, String description) {
		if (name == null ) {
			Bukkit.getLogger().severe("Could not register permission, name was null");
			return;
		}
		if (permissionByName.get(name) != null) {
			Bukkit.getLogger().severe("Could not register permission " + name + ". It was already registered");
			return;
		}
		int id = -1;
		Map <Integer,String> dbRegisteredPerms = NameLayerPlugin.getGroupManagerDao().getPermissionMapping();
		for(Entry <Integer,String> perm : dbRegisteredPerms.entrySet()) {
			if (perm.getValue().equals(name)) {
				id = perm.getKey();
				break;
			}
		}
		PermissionType p;
		if (id == -1) {
			//not in db yet
			id = maximumExistingId + 1;
			while(dbRegisteredPerms.get(id) != null) {
				id++;
			}
			maximumExistingId = id;
			p = new PermissionType(name, id, defaultPermLevels, description);
			NameLayerPlugin.getGroupManagerDao().registerPermission(p);
		}
		else {
			//already in db, so use existing id
			p = new PermissionType(name, id, defaultPermLevels, description);
		}
		permissionByName.put(name, p);
		permissionById.put(id, p);
	}
	
	public static Collection<PermissionType> getAllPermissions() {
		return permissionByName.values();
	}
	
	private static void registerNameLayerPermissions() {
		LinkedList <Integer> modAndAbove = new LinkedList<Integer>();
		LinkedList <Integer> adminAndAbove = new LinkedList<Integer>();
		LinkedList <Integer> owner = new LinkedList<Integer>();
		LinkedList <Integer> all = new LinkedList <Integer>();
		modAndAbove.add(1);
		modAndAbove.add(2);
		adminAndAbove.add(1);
		all.add(1);
		all.add(2);
		all.add(3);
		//clone the list every time so changing the list of one perm later doesn't affect other perms
		
		//allows adding/removing members
		registerPermission("MEMBERS", (LinkedList <Integer>)modAndAbove.clone(), "Allows inviting new members and removing existing members");
		//allows blacklisting/unblacklisting players and viewing the blacklist
		registerPermission("BLACKLIST", (LinkedList <Integer>)modAndAbove.clone(), "Allows viewing this group's blacklist, adding players to the blacklist "
				+ "and removing players from the blacklist");
		//allows adding/removing mods
		registerPermission("MODS", (LinkedList <Integer>)adminAndAbove.clone(), "Allows inviting new mods and removing existing mods");
		//allows adding/modifying a password for the group
		registerPermission("PASSWORD", (LinkedList <Integer>)adminAndAbove.clone(), "Allows viewing this groups password and changing or removing it");
		//allows to list the permissions for each permission group
		registerPermission("LIST_PERMS", (LinkedList <Integer>)adminAndAbove.clone(), "Allows viewing how permission for this group are set up");
		//allows to see general group stats
		registerPermission("GROUPSTATS", (LinkedList <Integer>)adminAndAbove.clone(), "Gives access to various group statistics such as member "
				+ "counts by permission type, who owns the group etc.");
		//allows to add/remove admins
		registerPermission("ADMINS", (LinkedList <Integer>)owner.clone(), "Allows inviting new admins and removing existing admins");
		//allows to add/remove owners
		registerPermission("OWNER", (LinkedList <Integer>)owner.clone(), "Allows inviting new owners and removing existing owners");
		//allows to modify the permissions for different permissions groups
		registerPermission("PERMS", (LinkedList <Integer>)owner.clone(), "Allows modifying permissions for this group");
		//allows deleting the group
		registerPermission("DELETE", (LinkedList <Integer>)owner.clone(), "Allows deleting this group");
		//allows merging the group with another one
		registerPermission("MERGE", (LinkedList <Integer>)owner.clone(), "Allows merging this group into another or merging another group into this one");
		//allows linking this group to another
		registerPermission("LINKING", (LinkedList <Integer>)owner.clone(), "Allows linking this group to another group as a supergroup or a subgroup");
		//allows creating, deleting and renaming player types
		registerPermission("PLAYERTYPES", (LinkedList <Integer>)owner.clone(), "Allows creating, renaming and deleting player types for this group");
		//allows opening the gui
		registerPermission("OPEN_GUI", (LinkedList <Integer>)all.clone(), "Allows opening the GUI for this group");
	}
	
	private String name;
	private List <Integer> defaultPermLevels;
	private int id;
	private String description;
	
	private PermissionType(String name, int id, List <Integer> defaultPermLevels, String description) {
		this.name = name;
		this.id = id;
		this.defaultPermLevels = defaultPermLevels;
		this.description = description;
	}
	
	public String getName() {
		return name;
	}
	
	public List <Integer> getDefaultPermLevels() {
		return defaultPermLevels;
	}
	
	public int getId() {
		return id;
	}
	
	public String getDescription() {
		return description;
	}
}
