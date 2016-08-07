package vg.civcraft.mc.namelayer.permission;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import vg.civcraft.mc.namelayer.NameLayerPlugin;
import vg.civcraft.mc.namelayer.group.Group;

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
			this.perms = new ArrayList<PermissionType>(
					PermissionType.getAllPermissions());

		}
	}

	/**
	 * For loading existing types
	 */
	public PlayerType(String name, int id, PlayerType parent,
			List<PermissionType> perms, Group group) {
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

	public PlayerType getParent() {
		return parent;
	}

	public String getName() {
		return name;
	}

	void setName(String name) {
		this.name = name;
	}

	public List<PlayerType> getChildren() {
		// dont let them change the list itself
		return new ArrayList<PlayerType>(children);
	}

	public boolean isChildren(PlayerType type) {
		return children.contains(type);
	}

	public boolean addChild(PlayerType child) {
		children.add(child);
		return true;
	}

	public boolean removeChild(PlayerType child) {
		if (isChildren(child)) {
			children.remove(child);
			return true;
		} else {
			return false;
		}
	}

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
		List<PermissionType> permList = new LinkedList<PermissionType>();
		permList.add(perm);
		if (saveToDb) {
			NameLayerPlugin.getGroupManagerDao().addPermission(group.getName(),
					this, permList);
		}
		return true;
	}

	public List<PlayerType> getRecursiveChildren() {
		List<PlayerType> types = new LinkedList<PlayerType>();
		for (PlayerType child : children) {
			types.add(child);
			types.addAll(child.getRecursiveChildren());
		}
		return types;
	}

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
		// enforce consistent structure by updating children
		for (PlayerType type : getRecursiveChildren()) {
			if (type.hasPermission(perm)) {
				type.removePermission(perm, saveToDb);
			}
		}
		if (saveToDb) {
			NameLayerPlugin.getGroupManagerDao().removePermission(
					group.getName(), this, perm);
		}
		return true;
	}

	public boolean hasPermission(PermissionType perm) {
		return perms.contains(perm);
	}

	public List<PermissionType> getAllPermissions() {
		return new LinkedList<PermissionType>(perms);
	}

	public int getId() {
		return id;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof PlayerType)) {
			return false;
		}
		PlayerType comp = (PlayerType) o;
		return comp.getId() == this.getId()
				&& comp.getName().equals(this.getName());
	}

}
