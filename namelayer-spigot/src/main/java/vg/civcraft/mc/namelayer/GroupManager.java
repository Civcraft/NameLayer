package vg.civcraft.mc.namelayer;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import vg.civcraft.mc.namelayer.database.GroupManagerDao;
import vg.civcraft.mc.namelayer.events.GroupCreateEvent;
import vg.civcraft.mc.namelayer.events.GroupDeleteEvent;
import vg.civcraft.mc.namelayer.events.GroupMergeEvent;
import vg.civcraft.mc.namelayer.events.GroupTransferEvent;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.misc.MercuryManager;
import vg.civcraft.mc.namelayer.permission.PermissionType;
import vg.civcraft.mc.namelayer.permission.PlayerType;
import vg.civcraft.mc.namelayer.permission.PlayerTypeHandler;

public class GroupManager{
	
	private static GroupManagerDao groupManagerDao;
	
	private static Map<String, Group> groupsByName;
	private static Map<Integer, Group> groupsById;
	
	public GroupManager(Map <Integer, Group> groupsById, Map <String,Group> groupsByName){
		GroupManager.groupsById = groupsById;
		GroupManager.groupsByName = groupsByName;
		groupManagerDao = NameLayerPlugin.getGroupManagerDao();
	}
	
	/**
	 * Saves the group into caching and saves it into the db. Also fires the GroupCreateEvent.
	 * @param The group to create to db.
	 */
	public int createGroup(Group group){
		return createGroup(group,true);
	}
	
	/**
	 * This will create a group asynchronously. Always saves to database. Pass in a Runnable of type RunnableOnGroup that 
	 * specifies what to run <i>synchronously</i> after the insertion of the group. Your runnable should handle the case where
	 * id = -1 (failure).
	 * 
	 * Note that internally, we setGroupId on the RunnableOnGroup; your run() method should use getGroupId() to retrieve it 
	 * and react to it.
	 * 
	 * @param group the Group placeholder to use in creating a group. Calls GroupCreateEvent synchronously, then insert the
	 *    group asynchronously, then calls the RunnableOnGroup synchronously.
	 * @param postCreate The RunnableOnGroup to run after insertion (whether successful or not!)
	 * @param checkBeforeCreate Checks if the group already exists (asynchronously) prior to creating it. Runs the CreateEvent
	 *    synchronously, then behaves as normal after that (running async create).
	 */
	public void createGroupAsync(final Group group, final RunnableOnGroup postCreate, boolean checkBeforeCreate) {
		if (group == null) {
			NameLayerPlugin.getInstance().getLogger().log(Level.INFO, "Group create failed, caller passed in null", new Exception());
			postCreate.setGroup(new Group(null, null, true, null, -1));
			Bukkit.getScheduler().runTask(NameLayerPlugin.getInstance(), postCreate);
		} else {
			if (checkBeforeCreate) {
				// Run check asynchronously. 
				Bukkit.getScheduler().runTaskAsynchronously(NameLayerPlugin.getInstance(), new Runnable() {
						@Override
						public void run() {
							// So.... when you `new Group` it makes a ton of DB calls and gets ids and members. If the group exists, ID at this point will be > -1...
							if (group.getGroupId() == -1) {// || getGroup(group.getName()) == null) {
								// group doesn't exist, so schedule create.
								Bukkit.getScheduler().runTask(NameLayerPlugin.getInstance(), new Runnable() {
										@Override
										public void run() {
											doCreateGroupAsync(group, postCreate);
										}
								});
							} else {
								// group does exist, so run postCreate with failure.
								NameLayerPlugin.getInstance().getLogger().log(Level.INFO, "Group create failed, group {0} already exists", group.getName());
								postCreate.setGroup(new Group(null, null, true, null, -1));
								Bukkit.getScheduler().runTask(NameLayerPlugin.getInstance(), postCreate);								
							}
						}
					});
				
			} else {
				doCreateGroupAsync(group, postCreate);
			}
		}
	}
	
	private void doCreateGroupAsync(final Group group, final RunnableOnGroup postCreate) {
		GroupCreateEvent event = new GroupCreateEvent(
				group.getName(), group.getOwner(),
				group.getPassword());
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()){
			NameLayerPlugin.getInstance().info("Group create was cancelled for group: " + group.getName());
			postCreate.setGroup(new Group(group.getName(), group.getOwner(), true, group.getPassword(), -1));
			Bukkit.getScheduler().runTask(NameLayerPlugin.getInstance(), postCreate);
		}
		final String name = event.getGroupName();
		final UUID owner = event.getOwner();
		final String password = event.getPassword();
		Bukkit.getScheduler().runTaskAsynchronously(NameLayerPlugin.getInstance(), new Runnable() {
			@Override
			public void run() {
				int id = internalCreateGroup(group, true, name, owner, password);
				NameLayerPlugin.getInstance().getLogger().log(Level.INFO, "Async group create finished for group {0}, id assigned: {1}",
						new Object[]{name, id});
				Group g = GroupManager.getGroup(id);
				postCreate.setGroup(g);
				Bukkit.getScheduler().runTask(NameLayerPlugin.getInstance(), postCreate);
			}
		});
	}
	
	public int createGroup(Group group, boolean savetodb){
		if (group == null) {
			NameLayerPlugin.getInstance().getLogger().log(Level.INFO, "Group create failed, caller passed in null", new Exception());
			return -1;
		}
		GroupCreateEvent event = new GroupCreateEvent(
				group.getName(), group.getOwner(),
				group.getPassword());
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()){
			NameLayerPlugin.getInstance().info("Group create was cancelled for group: " + group.getName());
			return -1;
		}
		return internalCreateGroup(group, savetodb, event.getGroupName(), event.getOwner(), event.getPassword());
	}
	
	private int internalCreateGroup(Group group, boolean savetodb, String name, UUID owner, String password) {
		int id;
		if (savetodb){
			id = groupManagerDao.createGroup(group.getName(), owner,password);
			group.setPlayerTypeHandler(PlayerTypeHandler.createStandardTypes(group));
			MercuryManager.createGroup(group, id);
		} else {
			id = group.getGroupId();
			group.setPlayerTypeHandler(groupManagerDao.getPlayerTypes(group));
		}
		if (id > -1 && savetodb) {
			GroupManager.getGroup(id);
		}
		return id;
	}
	
	public boolean deleteGroup(String groupName){
		return deleteGroup(groupName,true);
	}
	
	public boolean deleteGroup(String groupName, boolean savetodb){
		if (groupName == null) {
			NameLayerPlugin.getInstance().getLogger().log(Level.INFO, "Group delete failed, caller passed in null", new Exception());
			return false;
		}
		groupName = groupName.toLowerCase();
		Group group = getGroup(groupName);
		if (group == null) {
			NameLayerPlugin.getInstance().getLogger().log(Level.INFO, "Group delete failed, failed to find group " + groupName);
			return false;
		}
		
		// Call once w/ finished false to allow cancellation.
		GroupDeleteEvent event = new GroupDeleteEvent(group, false);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) {
			NameLayerPlugin.getInstance().getLogger().log(Level.INFO, "Group delete was cancelled for "+ groupName);
			return false;
		}
		groupsByName.remove(group.getName());
		for (int id : group.getSecondaryIds()) {
			groupsById.remove(id);
		}
		
		// Call after actual delete to alert listeners that we're done.
		event = new GroupDeleteEvent(group, true);
		Bukkit.getPluginManager().callEvent(event);
		
		group.setDisciplined(true);
		group.setValid(false);
		if (savetodb){
			groupManagerDao.deleteGroup(group);
			MercuryManager.deleteGroup(groupName);
		}
		return true;
	}
	
	public void transferGroup(Group g, UUID uuid){
		transferGroup(g,uuid,true);
	}
	
	public void transferGroup(Group g, UUID uuid, boolean savetodb){
		if (g == null || uuid == null) {
			NameLayerPlugin.getInstance().getLogger().log(Level.INFO, "Group transfer failed, caller passed in null", new Exception());
			return;
		}

		GroupTransferEvent event = new GroupTransferEvent(g, uuid);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()){
			NameLayerPlugin.getInstance().info("Group transfer event was cancelled for group: " + g.getName());
			return;
		}
		if (savetodb){
			g.addToTracking(uuid, g.getPlayerTypeHandler().getOwnerType());
			g.setOwner(uuid);
			MercuryManager.transferGroup(g, uuid);
		} else {
			g.addToTracking(uuid, g.getPlayerTypeHandler().getOwnerType(), false);
			g.setOwner(uuid, false);
		}
	}

	/**
	 * Merging is initiated asynchronously on the shard the player currently inhabits. On initiation and post initial checks,
	 * a Mercury message "merge|" is sent, indicating the beginning of the merging process. All shards receive this and
	 * immediately discipline the groups involved to prevent desynchronization.
	 * 
	 * When the host shard is _done_, a second mercury message is sent, which signals the end of the process.
	 * Due to the complexity of keeping the cache consistent, we're whiffing on this one a bit and
	 * _for now_ simply invalidating the cache on servers.
	 *
	 * Eventually, we'll need to go line-by-line through the db code and just replicate in cache. That day is not today.
	 */
	public void doneMergeGroup(Group group, Group toMerge) {
		if (group == null || toMerge == null) {
			NameLayerPlugin.getInstance().getLogger().log(Level.INFO, "Group merge failed, caller passed in null", new Exception());
			return;
		}

		GroupMergeEvent event = new GroupMergeEvent(group, toMerge, true);
		Bukkit.getPluginManager().callEvent(event);

		// Then invalidate. Updating the cache was proving unreliable; we'll address it later.
		GroupManager.invalidateCache(group.getName());
		GroupManager.invalidateCache(toMerge.getName());
	}

	public void mergeGroup(Group group, Group to){
		mergeGroup(group,to,true);
	}
	
	public void mergeGroup(final Group group, final Group toMerge, boolean savetodb){
		if (group == null || toMerge == null) {
			NameLayerPlugin.getInstance().getLogger().log(Level.INFO, "Group merge failed, caller passed in null", new Exception());
			return;
		} else if (group == toMerge || group.getName().equalsIgnoreCase(toMerge.getName())) {
			NameLayerPlugin.getInstance().getLogger().log(Level.INFO, "Group merge failed, can't merge the same group into itself", new Exception());
			return;
		}
		Set <String> playerTypesAfterMerge = new HashSet<String>();
		for(PlayerType type : group.getPlayerTypeHandler().getAllTypes()) {
			playerTypesAfterMerge.add(type.getName().toLowerCase());
		}
		for(PlayerType type : toMerge.getPlayerTypeHandler().getAllTypes()) {
			playerTypesAfterMerge.add(type.getName().toLowerCase());
		}
		if (playerTypesAfterMerge.size() > PlayerTypeHandler.getMaximumTypeCount()) {
			NameLayerPlugin.getInstance().info("Group merge failed for groups: " +
					group.getName() + " and " + toMerge.getName() + ". Merge would exceed maximum player type count");
		}
		GroupMergeEvent event = new GroupMergeEvent(group, toMerge, false);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()){
			NameLayerPlugin.getInstance().info("Group merge event was cancelled for groups: " +
					group.getName() + " and " + toMerge.getName());
			return;
		}
		group.setDisciplined(true, false);
		toMerge.setDisciplined(true, false);
		//owners always become owners, unless they already have another rank
		for(UUID uuid : toMerge.getAllTrackedByType(toMerge.getPlayerTypeHandler().getOwnerType())) {
			if (!group.isTracked(uuid)) {
				group.addToTracking(uuid, group.getPlayerTypeHandler().getOwnerType(), savetodb);
			}
		}
		
		
		for(PlayerType type : toMerge.getPlayerTypeHandler().getAllTypes()) {
			if (type == toMerge.getPlayerTypeHandler().getOwnerType()) {
				continue;
			}
			PlayerType existingType = group.getPlayerTypeHandler().getType(type.getName());
			if (existingType != null) {
				//a type with the same name exists, so we transfer all players over to this same type
				for(UUID uuid : toMerge.getAllTrackedByType(type)) {
					if (!group.isTracked(uuid)) {
						group.addToTracking(uuid, existingType, savetodb);
					}
				}
			}
			else {
				//this type doesnt exist, so we copy it over
				PlayerType parent;
				if (toMerge.getPlayerTypeHandler().isBlackListedType(type)) {
					parent = toMerge.getPlayerTypeHandler().getDefaultNonMemberType();
				}
				else {
					parent = toMerge.getPlayerTypeHandler().getOwnerType();
				}
				int id = toMerge.getPlayerTypeHandler().getUnusedId();
				if (id == -1) {
					//something went very wrong
					NameLayerPlugin.getInstance().getLogger().log(Level.INFO, "Group merge failed, could not retrieve id for new player type");
					continue;
				}
				//create a copy of the player type and transfer members over
				PlayerType copy = new PlayerType(type.getName(), id, parent, type.getAllPermissions(), group);
				group.getPlayerTypeHandler().registerType(copy, savetodb);
				for(UUID uuid : toMerge.getAllTrackedByType(type)) {
					group.addToTracking(uuid, copy, savetodb);
				}
			}
		}
		
		if (savetodb){
			MercuryManager.mergeGroup(group.getName(), toMerge.getName());
			// This basically just fires starting events and disciplines groups on target server.
			// They then wait for merge to complete. Botched merges will lock groups, basically. :shrug:

			NameLayerPlugin.getInstance().getServer().getScheduler().runTaskAsynchronously(
					NameLayerPlugin.getInstance(), new Runnable(){

				@Override
				public void run() {
					groupManagerDao.mergeGroup(group, toMerge);
					// At this point, at the DB level all non-overlap members are in target group, name is reset to target,
					// unique group header record is removed, and faction_id all point to new name.

					doneMergeGroup(group, toMerge);

					// Now we are done the merging process probably, so tell everyone to invalidate their caches for these
					// two groups and perform any other cleanup (subgroup links,etc.)
					MercuryManager.doneMergeGroup(group.getName(), toMerge.getName()); 
				}
			});
		}
	}
	
	/**
	 * Attempts to load the group with the given name from the cache. If no group with such a name exists
	 *  or the existing group is invalid, the group will be attempted to be loaded from the database. 
	 *  
	 * @param name Name of the group to load
	 * @return The group with the given name if a valid version was found in the cache or a new instance could be 
	 * loaded from the database or null if neither of those cases was fulfilled
	 */
	public static Group getGroup(String name){
		if (name == null) {
			NameLayerPlugin.getInstance().getLogger().log(Level.INFO, "getGroup failed, caller passed in null", new Exception());
			return null;
		}
		
		String lower = name.toLowerCase();
		Group retrieved = groupsByName.get(lower);
		if (retrieved != null && retrieved.isValid()) {
			return retrieved;
		} else { 
			Group group = groupManagerDao.getGroup(name);
			if (group != null) {
				groupsByName.put(lower, group);
				for (int j : group.getSecondaryIds()){
					groupsById.put(j, group);
				}
				groupsById.put(group.getGroupId(), group);
			} else {
				NameLayerPlugin.getInstance().getLogger().log(Level.INFO, "getGroup by Name failed, unable to find the group " + name);
			}
			return group;
		}
	}
		
	/**
	 * Attempts to load the group with the given id from the cache. If no group with such an id exists
	 * or the existing group is invalid, the group will be attempted to be loaded from the database. Be aware 
	 * that while each group has one "main" id, many other ids might point to the same group as the result of group
	 * merging. Those ids are the "secondary ids" each group has.
	 *  
	 * @param groupId Id of the group to load
	 * 
	 * @return The group with the given id if a valid version was found in the cache or a new instance could be 
	 * loaded from the database or null if neither of those cases was fulfilled
	 */
	public static Group getGroup(int groupId){
		Group retrieved = groupsById.get(groupId);
		if (retrieved != null && retrieved.isValid()) {
			return retrieved;
		} else { 
			if (retrieved != null) {
				//set id to retrieve with to main id of that group, the one looked up in faction_id
				groupId = retrieved.getGroupId();
			}
			Group group = groupManagerDao.getGroup(groupId);
			if (group != null) {
				groupsByName.put(group.getName().toLowerCase(), group);
				for (int j : group.getSecondaryIds()){
					groupsById.put(j, group);
				}
				groupsById.put(group.getGroupId(), group);
			} else {
				NameLayerPlugin.getInstance().getLogger().log(Level.INFO, "getGroup by ID failed, unable to find the group " + groupId);
			}
			return group;
		}
	}
	
	public static boolean hasGroup(String groupName) {
		if (groupName == null) {
			NameLayerPlugin.getInstance().getLogger().log(Level.INFO, "HasGroup Name failed, name was null ", new Exception());
			return false;
		}

		if (!groupsByName.containsKey(groupName.toLowerCase())) {
			return (getGroup(groupName.toLowerCase()) != null);
		} else {
			return true;
		}
	}
		
	public boolean hasAccess(String groupname, UUID player, PermissionType perm) {
		if (groupname == null) {
			NameLayerPlugin.getInstance().getLogger().log(Level.INFO, "hasAccess failed (access denied), could not find group " + groupname);
			return false;
		}
		return hasAccess(getGroup(groupname), player, perm);
	}
	
	public boolean hasAccess(Group group, UUID player, PermissionType ... perms) {
		Player p = Bukkit.getPlayer(player);
		if (p != null && (p.isOp() || p.hasPermission("namelayer.admin"))) {
			return true;
		}
		if (group == null || player == null || perms == null) {
			NameLayerPlugin.getInstance().getLogger().log(Level.INFO, "hasAccess failed, caller passed in null", new Exception());
			return false;
		}
		if (!group.isValid()) {
			group = getGroup(group.getName());
			if (group == null) {
				//what happened? who knows?
				return false;
			}
		}
		PlayerType type = group.getPlayerType(player);
		for(PermissionType perm : perms) {
			if (type.hasPermission(perm)) {
				return true;
			}
		}
		return false;
	}
	
	public List<String> getAllGroupNames(UUID uuid){
		if (uuid == null) {
			NameLayerPlugin.getInstance().getLogger().log(Level.INFO, "getAllGroupNames failed, caller passed in null", new Exception());
			return new ArrayList<String>();
		}
		return groupManagerDao.getGroupNames(uuid);
	}

	/**
	 * Invalidates a group from cache.
	 * @param group
	 */
	public static void invalidateCache(String group){
		if (group == null) {
			NameLayerPlugin.getInstance().getLogger().log(Level.INFO, "invalidateCache failed, caller passed in null", new Exception());
			return;
		}

		Group g = groupsByName.get(group.toLowerCase());
		if (g != null) {
			g.setValid(false);
			Collection<Integer>k = new LinkedList<Integer> (g.getSecondaryIds());
			k.add(g.getGroupId());
			groupsByName.remove(group.toLowerCase());
			
			boolean fail = true;
			// You have a freaking hashmap, use it.
			for (int j : k) {
				if (groupsById.remove(j) != null) {
					fail = false;
				}
			}
			//this is a weird leftover. It might be unneeded, but I'd rather not remove it for now
			
			// FALLBACK is hardloop
			if (fail) { // can't find ID or cache is wrong.
				for (Group x: groupsById.values()) {
					if (x.getName().equals(g.getName())) {
						groupsById.remove(x.getGroupId());
					}
				}
			}
		} else {
			NameLayerPlugin.getInstance().getLogger().log(Level.INFO, "Invalidate cache by name failed, unable to find the group " + group);			
		}
	}
	
	public int countGroups(UUID uuid){
		if (uuid == null) {
			NameLayerPlugin.getInstance().getLogger().log(Level.INFO, "countGroups failed, caller passed in null", new Exception());
			return 0;
		}
		return groupManagerDao.countGroups(uuid);
	}
	
	public Timestamp getTimestamp(String group){
		if (group == null) {
			NameLayerPlugin.getInstance().getLogger().log(Level.INFO, "getTimeStamp failed, caller passed in null", new Exception());
			return null; 
		}

		return groupManagerDao.getTimestamp(group);
	}
}
