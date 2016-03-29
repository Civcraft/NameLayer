package vg.civcraft.mc.namelayer.permission;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.google.common.collect.Maps;

/*
 * To add or remove perms add them to this list.
 * Then either modify or leave the default perms given to players
 * in the GroupManager class method initiateDefaultPerms()
 * 
 * Also remember that you need to modify the database to
 * add the new permission type to the owners so people can
 * actually modify the groups.
 */
public class PermissionType {
	
	private static Map<String, PermissionType> permissions = Maps.newHashMap();
	
	public static String getStringOfTypes() {
		StringBuilder perms = new StringBuilder();
		for (PermissionType perm: permissions.values()) {
			perms.append(perm);
			perms.append(" ");
		}
		return perms.toString();
	}
	
	public static void displayPermissionTypes(Player p) {
		p.sendMessage(ChatColor.RED 
				+ "That PermissionType does not exists.\n"
				+ "The current types are: " + getStringOfTypes());
	}
	
	public static void registerPermission(PermissionType perm) {
		permissions.put(perm.getName(), perm);
	}
	
	public static Collection<PermissionType> values() {
		return permissions.values();
	}
	
	/**
	 * 
	 * @param name The name of the permissions. Not the PermissionFormat.
	 * @return Returns the PermissionType.
	 */
	public static PermissionType getType(String name) {
		return permissions.get(name);
	}
	
	private String name;
	private String permFormat;
	/**
	 * 
	 * @param name The name of the permission.
	 */
	public PermissionType(String name) {
		this.name = name;
		permFormat = "namelayer.custom.permissions." + name;
	}
	
	public String getName() {
		return name;
	}
	
	/**
	 * 
	 * @return Returns in permission format such as 'namelayer.custom.permissions.DOORS'.
	 */
	public String getPermissionFormat() {
		return permFormat;
	}
}
