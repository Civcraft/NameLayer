package vg.civcraft.mc.namelayer.permission;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import vg.civcraft.mc.namelayer.NameLayerPlugin;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.misc.MercuryManager;

public class PlayerType {

	private Group group;
	private String name;
	private int id;
	private List<PlayerType> children;
	private PlayerType parent;
	private List<PermissionType> perms;

	/**
	 * For creating completly new types
	 */
	public PlayerType(String name, int id, PlayerType parent, Group group) {
		this.name = name;
		this.parent = parent;
		this.id = id;
		this.group = group;
		this.children = new LinkedList<PlayerType>();
		if (parent != null) {
			// flat copy perms
			this.perms = new ArrayList<PermissionType>(parent.perms);
			parent.addChild(this);
		} else {
			// new root with all permissions
			this.perms = new ArrayList<PermissionType>(PermissionType.getAllPermissions());

		}
	}

	/**
	 * For loading existing types
	 */
	public PlayerType(String name, int id, PlayerType parent, List<PermissionType> perms, Group group) {
		this.name = name;
		this.parent = parent;
		this.id = id;
		this.group = group;
		this.children = new LinkedList<PlayerType>();
		this.perms = perms;
		if (parent != null) {
			parent.addChild(this);
		}
	}

	/**
	 * This gets the parent node of this instance. This must always be not null,
	 * except when this instance is an owner and the root node of the graph tree
	 * 
	 * @return Parent node or null if no parent exists
	 */
	public PlayerType getParent() {
		return parent;
	}

	public List<PlayerType> getAllParents() {
		List<PlayerType> types = new LinkedList<PlayerType>();
		if (parent != null) {
			types.add(parent);
			types.addAll(parent.getAllParents());
		}
		return types;
	}

	/**
	 * Each instance has a name, which can be modified dynamically and should
	 * only be used to present a player type to a player. For internal
	 * identifying, use ids
	 * 
	 * @return Name of this instance
	 */
	public String getName() {
		return name;
	}

	/**
	 * Updates the name of this instance
	 * 
	 * @param name
	 *            New name
	 */
	void setName(String name) {
		this.name = name;
	}

	/**
	 * Gets all children nodes of this instance based on the graph modelling the
	 * relation ship between player types for this instances group. The list
	 * returned will always be a copy of the one used internally
	 * 
	 * @param recursive
	 *            Whether children should be retrieved recursively (deep)
	 * @return All children
	 */
	public List<PlayerType> getChildren(boolean recursive) {
		if (recursive) {
			return getRecursiveChildren();
		}
		// dont let them change the list itself
		return new ArrayList<PlayerType>(children);
	}

	/**
	 * Utility method to recursively collect all children of a player type
	 */
	private List<PlayerType> getRecursiveChildren() {
		// deep search
		List<PlayerType> types = new LinkedList<PlayerType>();
		for (PlayerType child : children) {
			types.add(child);
			types.addAll(child.getRecursiveChildren());
		}
		return types;
	}

	/**
	 * Checks whether the given type is a direct child of this instance
	 * 
	 * @param type
	 *            Possible child
	 * @return True if the given player type is a direct child, false if not
	 */
	public boolean isChildren(PlayerType type) {
		return children.contains(type);
	}

	/**
	 * Adds the given PlayerType as child to this instance
	 * 
	 * @param child
	 *            PlayerType to add as child
	 * @return True if it was added successfully, false if not
	 */
	public boolean addChild(PlayerType child) {
		if (isChildren(child)) {
			return false;
		}
		children.add(child);
		return true;
	}

	/**
	 * Removes the given PlayerType as child from this instance
	 * 
	 * @param child
	 *            PlayerType to remove as child
	 * @return True if it was removed successfully, false if not
	 */
	public boolean removeChild(PlayerType child) {
		if (isChildren(child)) {
			children.remove(child);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Adds the given permission to this instance
	 * 
	 * @param perm
	 *            Permission to add
	 * @param saveToDb
	 *            Whether this action should be persisted to the db and
	 *            broadcasted via Mercury
	 * @return True if the permission was sucessfully added, false if not
	 */
	public boolean addPermission(PermissionType perm, boolean saveToDb) {
		if (perms.contains(perm)) {
			// already exists
			return false;
		}
		if (parent != null && !parent.hasPermission(perm)) {
			// would create inconsistent perm structure
			return false;
		}
		perms.add(perm);
		if (saveToDb) {
			MercuryManager.addPerm(group.getName(), getId(), perm.getName());
			NameLayerPlugin.getGroupManagerDao().addPermissionAsync(group, this, perm);
		}
		return true;
	}

	/**
	 * Removes the given permission from this instance
	 * 
	 * @param perm
	 *            Permission to remove
	 * @param saveToDb
	 *            Whether this action should be persisted to the db and
	 *            broadcasted via Mercury
	 * @return True if the permission was sucessfully removed, false if not
	 */
	public boolean removePermission(PermissionType perm, boolean saveToDb) {
		if (parent == null) {
			// is root and shouldnt be modified
			return false;
		}
		if (!perms.contains(perm)) {
			// doesn't exists
			return false;
		}
		perms.remove(perm);
		if (saveToDb) {
			MercuryManager.remPerm(group.getName(), getId(), perm.getName());
			NameLayerPlugin.getGroupManagerDao().removePermissionAsync(group, this, perm);
		}
		return true;
	}

	/**
	 * Checks whether this player type has the given permission
	 * 
	 * @param perm
	 *            Permission to check for
	 * @return True if this player type has the given permission, false if not
	 */
	public boolean hasPermission(PermissionType perm) {
		return perms.contains(perm);
	}

	/**
	 * @return Copy of the list containing all permissions this instance has
	 */
	public List<PermissionType> getAllPermissions() {
		return new LinkedList<PermissionType>(perms);
	}

	/**
	 * Each PlayerType has an id, which is unique for the group it is assigned
	 * to, but not unique for all groups, for example all owner root player
	 * types will always have id 0
	 * 
	 * @return Id of this instance
	 */
	public int getId() {
		return id;
	}

	/**
	 * @return The permission required to add/invite players to this rank
	 */
	public PermissionType getInvitePermissionType() {
		return PermissionType.getInvitePermission(getId());
	}

	/**
	 * @return The permission required to remove players from this rank
	 */
	public PermissionType getRemovalPermissionType() {
		return PermissionType.getRemovePermission(getId());
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof PlayerType)) {
			return false;
		}
		PlayerType comp = (PlayerType) o;
		return comp.getId() == this.getId() && comp.getName().equals(this.getName());
	}

}