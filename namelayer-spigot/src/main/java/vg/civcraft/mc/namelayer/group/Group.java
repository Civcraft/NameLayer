package vg.civcraft.mc.namelayer.group;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameAPI;
import vg.civcraft.mc.namelayer.NameLayerPlugin;
import vg.civcraft.mc.namelayer.database.GroupManagerDao;
import vg.civcraft.mc.namelayer.misc.Mercury;
import vg.civcraft.mc.namelayer.permission.PlayerType;
import vg.civcraft.mc.namelayer.permission.PlayerTypeHandler;

public class Group {

	private static GroupManagerDao db;

	private String name;
	private String password;
	private UUID owner;
	private boolean isDisciplined; // if true, prevents any interactions with
									// this group
	private boolean isValid = true; // if false, then group has recently been
									// deleted and is invalid
	private int id;
	private Set<Integer> ids = Sets.<Integer> newConcurrentHashSet();

	private Map<UUID, PlayerType> players = Maps.<UUID, PlayerType> newHashMap();
	private Map<UUID, PlayerType> invites = Maps.<UUID, PlayerType> newHashMap();

	private PlayerTypeHandler playerTypeHandler;

	public Group(String name, UUID owner, boolean disciplined, String password, int id) {
		if (db == null) {
			db = NameLayerPlugin.getGroupManagerDao();
		}

		this.name = name;
		this.password = password;
		this.owner = owner;
		this.isDisciplined = disciplined;

		// TODO Gut this and have one id
		// This returns list of ids w/ id holding largest # of players at top.
		List<Integer> allIds = db.getAllIDs(name);
		if (allIds != null && allIds.size() > 0) {
			this.ids.addAll(allIds);
			this.id = allIds.get(0); // default "root" id is the one with the
										// players.
		} else {
			this.ids.add(id);
			this.id = id; // otherwise just use what we're given
		}
	}

	/**
	 * Gets the uuids of all players, who are tracked by this group. This
	 * doesn't only include members of the group, but also blacklisted players.
	 * Anyone not in this list will have the default non member player type as
	 * specified in the player type handler of this group
	 * 
	 * @return All tracked players
	 */
	public List<UUID> getAllTracked() {
		return Lists.newArrayList(players.keySet());
	}

	/**
	 * Checks whether the given uuid is tracked by this group, either as member
	 * or blacklisted
	 * 
	 * @param uuid
	 *            UUID to check for
	 * @return True if the uuid is tracked, false if not
	 */
	public boolean isTracked(UUID uuid) {
		return players.containsKey(uuid);
	}

	/**
	 * Gets the uuids of all players, who are tracked with the given player
	 * type. This will not work when being called with the default non member
	 * type
	 * 
	 * @param type
	 *            PlayerType to retrieve tracked players for
	 * @return All players tracked in this group with the given player type
	 */
	public List<UUID> getAllTrackedByType(PlayerType type) {
		List<UUID> uuids = Lists.newArrayList();
		;
		for (Map.Entry<UUID, PlayerType> entry : players.entrySet()) {
			if (entry.getValue() == type) {
				uuids.add(entry.getKey());
			}
		}
		return uuids;
	}

	/**
	 * Gives the uuids of the members whose name starts with the given String,
	 * this is not case-sensitive
	 * 
	 * @param prefix
	 *            start of the players name
	 * @return list of all players whose name starts with the given string
	 */
	public List<UUID> getMembersByName(String prefix) {
		List<UUID> uuids = Lists.newArrayList();
		List<UUID> members = getAllTracked();

		prefix = prefix.toLowerCase();
		for (UUID member : members) {
			String name = NameAPI.getCurrentName(member);
			if (name.toLowerCase().startsWith(prefix)) {
				uuids.add(member);
			}
		}
		return uuids;
	}

	/**
	 * Gives the uuids of players who are in this group and whos name is within
	 * the given range.
	 * 
	 * @param lowerLimit
	 *            lexicographically lowest acceptable name
	 * @param upperLimit
	 *            lexicographically highest acceptable name
	 * @return list of uuids of all players in the group whose name is within
	 *         the given range
	 */
	public List<UUID> getMembersInNameRange(String lowerLimit, String upperLimit) {
		List<UUID> uuids = Lists.newArrayList();
		List<UUID> members = getAllTracked();

		for (UUID member : members) {
			String name = NameAPI.getCurrentName(member);
			if (name.compareToIgnoreCase(lowerLimit) >= 0 && name.compareToIgnoreCase(upperLimit) <= 0) {
				uuids.add(member);
			}
		}
		return uuids;
	}

	/**
	 * Adds the player to be allowed to join a group into a specific PlayerType.
	 * 
	 * @param uuid
	 *            - The UUID of the player.
	 * @param type
	 *            - The PlayerType they will be joining.
	 */
	public void addInvite(UUID uuid, PlayerType type) {
		addInvite(uuid, type, true);
	}

	/**
	 * Adds the player to be allowed to join a group into a specific PlayerType.
	 * 
	 * @param uuid
	 *            - The UUID of the player.
	 * @param type
	 *            - The PlayerType they will be joining.
	 * @param saveToDB
	 *            - save the invitation to the DB.
	 */
	public void addInvite(UUID uuid, PlayerType type, boolean saveToDB) {
		invites.put(uuid, type);
		if (saveToDB) {
			db.addGroupInvitationAsync(uuid, this, type);
		}
	}

	/**
	 * Get's the PlayerType of an invited Player.
	 * 
	 * @param uuid
	 *            - The UUID of the player.
	 * @return Returns the PlayerType or null.
	 */
	public PlayerType getInvite(UUID uuid) {
		if (!invites.containsKey(uuid)) {
			db.loadGroupInvitation(uuid, this);
		}
		return invites.get(uuid);
	}

	/**
	 * Removes the invite of a Player
	 * 
	 * @param uuid
	 *            - The UUID of the player.
	 * @param saveToDB
	 *            - remove the invitation from the DB.
	 */
	public void removeInvite(UUID uuid) {
		removeInvite(uuid, true);
	}

	/**
	 * Removes the invite of a Player
	 * 
	 * @param uuid
	 *            - The UUID of the player.
	 * @param saveToDB
	 *            - remove the invitation from the DB.
	 */
	public void removeInvite(UUID uuid, boolean saveToDB) {
		invites.remove(uuid);
		if (saveToDB) {
			db.removeGroupInvitationAsync(uuid, this);
		}
	}

	/**
	 * Checks if the player is a group member or not.
	 * 
	 * @param uuid
	 *            - The UUID of the player.
	 * @return Returns true if the player is a member, false otherwise.
	 */
	public boolean isMember(UUID uuid) {
		PlayerType pType = players.get(uuid);
		if (pType != null) {
			// if the type is not a child node of the non member type, it is not
			// a blacklisted type, so the player is a member
			return !playerTypeHandler.isRelated(pType, playerTypeHandler.getDefaultNonMemberType());
		}
		return false;
	}

	/**
	 * Checks if the player is in the Group's PlayerType or not.
	 * 
	 * @param uuid
	 *            - The UUID of the player.
	 * @param type
	 *            - The PlayerType wanted.
	 * @return Returns true if the player is a member of the specific
	 *         playertype, otherwise false.
	 */
	public boolean isTracked(UUID uuid, PlayerType type) {
		PlayerType pType = players.get(uuid);
		if (pType != null && pType.equals(type)) {
			return true;
		}
		return false;
	}

	/**
	 * @param uuid
	 *            - The UUID of the player.
	 * @return Returns the PlayerType of a UUID.
	 */
	public PlayerType getPlayerType(UUID uuid) {
		PlayerType member = players.get(uuid);
		if (member != null) {
			return member;
		}
		// not tracked, so default
		return playerTypeHandler.getDefaultNonMemberType();
	}

	/**
	 * Adds a member to a group.
	 * 
	 * @param uuid
	 *            - The uuid of the player.
	 * @param type
	 *            - The PlayerType to add. If a preexisting PlayerType is found,
	 *            it will be overwritten.
	 */

	public void addToTracking(UUID uuid, PlayerType type) {
		addToTracking(uuid, type, true);
	}

	public void addToTracking(UUID uuid, PlayerType type, boolean savetodb) {
		if (type == playerTypeHandler.getDefaultNonMemberType() && isTracked(uuid)) {
			return;
		}
		if (savetodb) {
			db.addMember(uuid, this, type);
			Mercury.addMember(this.name, uuid.toString(), type.toString());
		}
		players.put(uuid, type);
	}

	/**
	 * Removes the Player from the Group.
	 * 
	 * @param uuid
	 *            - The UUID of the Player.
	 */
	public void removeFromTracking(UUID uuid) {
		removeFromTracking(uuid, true);
	}

	public void removeFromTracking(UUID uuid, boolean savetodb) {
		if (savetodb) {
			db.removeMember(uuid, this);
			Mercury.remMember(this.name, uuid.toString());
		}
		players.remove(uuid);
	}

	/**
	 * @return Returns the group name.
	 */
	public String getName() {
		return name;
	}

	public String getPassword() {
		return password;
	}

	/**
	 * Checks if a string equals the password of a group.
	 * 
	 * @param password
	 *            - The password to compare.
	 * @return Returns true if they equal, otherwise false.
	 */
	public boolean isPassword(String password) {
		return this.password.equals(password);
	}

	/**
	 * @return The UUID of the owner of the group.
	 */
	public UUID getOwner() {
		return owner;
	}

	/**
	 * @param uuid
	 * @return true if the UUID belongs to the owner of the group, false
	 *         otherwise.
	 */
	public boolean isOwner(UUID uuid) {
		return owner.equals(uuid);
	}

	public boolean isDisciplined() {
		return isDisciplined;
	}

	public boolean isValid() {
		return isValid;
	}

	/**
	 * Gets the id for a group.
	 * <p>
	 * <b>Note:</b> Keep in mind though if you are trying to get a group_id from
	 * a GroupCreateEvent event it will not be accurate. You must have a delay
	 * for 1 tick for it to work correctly.
	 * <p>
	 * Also calling the GroupManager.getGroup(int) will return a group that
	 * either has that group id or the object associated with that id. As such
	 * if a group is previously called and which didn't have the same id as the
	 * one called now you could get a different group id. Example would be
	 * System.out.println(GroupManager.getGroup(1).getGroupId()) and that could
	 * equal something like 2.
	 * 
	 * @return the group id for a group.
	 */
	public int getGroupId() {
		return id;
	}

	/**
	 * Addresses issue above somewhat. Allows implementations that need the
	 * whole list of Ids associated with this groupname to get them.
	 * 
	 * @return list of ids paired with this group name.
	 */
	public List<Integer> getGroupIds() {
		return new ArrayList<Integer>(this.ids);
	}

	// == SETTERS
	// =========================================================================
	// //

	/**
	 * Sets the password for a group. Set the parameter as null to remove the
	 * password.
	 * 
	 * @param password
	 *            - The password of the group.
	 */
	public void setPassword(String password) {
		setPassword(password, true);
	}

	public void setPassword(String password, boolean savetodb) {
		this.password = password;
		if (savetodb) {
			db.updatePassword(name, password);
			Mercury.setPassword(this.name, password);
		}
	}

	/**
	 * Sets the owner of the group.
	 * 
	 * @param uuid
	 *            - The UUID of the Player.
	 */
	public void setOwner(UUID uuid) {
		setOwner(uuid, true);
	}

	public void setOwner(UUID uuid, boolean savetodb) {
		this.owner = uuid;
		if (savetodb) {
			db.setFounder(uuid, this);
			Mercury.setFounder(this.name, uuid.toString());
		}
	}

	public void setDisciplined(boolean value) {
		setDisciplined(value, true);
	}

	public void setDisciplined(boolean value, boolean savetodb) {
		this.isDisciplined = value;
		if (savetodb) {
			db.setDisciplined(this, value);
			Mercury.setDisciplined(this.name, this.isDisciplined);
		}
	}

	public void setValid(boolean valid) {
		this.isValid = valid;
	}

	// acts as replace
	public void setGroupId(int id) {
		this.ids.remove(this.id);
		this.id = id;
		if (!ids.contains(this.id)) {
			this.ids.add(this.id);
		}
	}

	/**
	 * Updates/replaces the group id list with a new one. Clears the old one,
	 * adds these, and ensures that the "main" id is added to the list as well.
	 */
	public void setGroupIds(List<Integer> ids) {
		this.ids.clear();
		if (ids != null) {
			this.ids.addAll(ids);
		}
		if (!ids.contains(this.id)) {
			this.ids.add(this.id);
		}
	}

	public PlayerTypeHandler getPlayerTypeHandler() {
		return playerTypeHandler;
	}
	
	public void setPlayerTypeHandler(PlayerTypeHandler handler) {
		this.playerTypeHandler = handler;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Group))
			return false;
		Group g = (Group) obj;
		return g.getName().equals(this.getName()); // If they have the same name
													// they are equal.
	}
}
