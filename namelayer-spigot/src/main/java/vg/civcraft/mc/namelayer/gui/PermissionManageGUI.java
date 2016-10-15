package vg.civcraft.mc.namelayer.gui;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import vg.civcraft.mc.civmodcore.chatDialog.Dialog;
import vg.civcraft.mc.civmodcore.inventorygui.Clickable;
import vg.civcraft.mc.civmodcore.inventorygui.ClickableInventory;
import vg.civcraft.mc.civmodcore.inventorygui.DecorationStack;
import vg.civcraft.mc.civmodcore.inventorygui.IClickable;
import vg.civcraft.mc.civmodcore.inventorygui.MultiPageView;
import vg.civcraft.mc.civmodcore.itemHandling.ISUtils;
import vg.civcraft.mc.namelayer.NameLayerPlugin;
import vg.civcraft.mc.namelayer.command.commands.CreateGroup;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.permission.PermissionType;
import vg.civcraft.mc.namelayer.permission.PlayerType;
import vg.civcraft.mc.namelayer.permission.PlayerTypeHandler;

public class PermissionManageGUI extends AbstractGroupGUI {

	private MainGroupGUI parent;

	public PermissionManageGUI(Group g, Player p, MainGroupGUI parentGUI) {
		super(g, p);
		this.parent = parentGUI;
	}

	public void showScreen() {
		if (!validGroup()) {
			return;
		}
		List<IClickable> clicks = new LinkedList<IClickable>();
		PlayerTypeHandler handler = g.getPlayerTypeHandler();
		for (final PlayerType type : handler.getAllTypes()) {
			ItemStack is = MenuUtils.getHashedItem(type.getName().hashCode());
			ISUtils.setName(is, ChatColor.GOLD + type.getName());
			ISUtils.addLore(is, ChatColor.GREEN
					+ "Click to edit this rank, add child ranks and view or edit permissions for it");
			IClickable click = new Clickable(is) {

				@Override
				public void clicked(Player p) {
					showDetail(type);
				}
			};
			clicks.add(click);
		}
		MultiPageView view = new MultiPageView(p, clicks, g.getName(), true);
		ItemStack backStack = new ItemStack(Material.WOOD_DOOR);
		ISUtils.setName(backStack, ChatColor.GOLD + "Go back to member management");
		Clickable backClick = new Clickable(backStack) {

			@Override
			public void clicked(Player arg0) {
				parent.showScreen();
			}
		};
		view.setMenuSlot(backClick, 3);
		view.showScreen();
	}

	public void showDetail(final PlayerType type) {
		ClickableInventory ci = new ClickableInventory(27, g.getName());
		// change name
		ItemStack renameStack = new ItemStack(Material.ANVIL);
		ISUtils.setName(renameStack, ChatColor.GOLD + "Rename " + type.getName());
		IClickable renameClick;
		if (gm.hasAccess(g, p.getUniqueId(), PermissionType.getPermission("RENAME_PLAYERTYPE"))) {
			ISUtils.addLore(renameStack, ChatColor.GREEN + "Click to change the name of this rank");
			renameClick = new Clickable(renameStack) {

				@Override
				public void clicked(final Player p) {
					ClickableInventory.forceCloseInventory(p);
					p.sendMessage(ChatColor.GOLD + "Please enter the new name for the rank " + type.getName());
					new Dialog(p, NameLayerPlugin.getInstance()) {

						@Override
						public List<String> onTabComplete(String wordCompleted, String[] fullMessage) {
							return new LinkedList<String>();
						}

						@Override
						public void onReply(String[] message) {
							PlayerTypeHandler handler = g.getPlayerTypeHandler();
							if (!gm.hasAccess(g, p.getUniqueId(), PermissionType.getPermission("RENAME_PLAYERTYPE"))) {
								p.sendMessage(ChatColor.RED + "You lost permission to do this");
								showDetail(type);
								return;
							}
							if (message.length == 0) {
								p.sendMessage(ChatColor.RED + "You have to enter something!");
								showDetail(type);
								return;
							}
							if (message.length > 1) {
								p.sendMessage(ChatColor.RED + "Player type names may not contain spaces");
								showDetail(type);
								return;
							}
							String name = message[0];
							if (!CreateGroup.isConformName(name)) {
								p.sendMessage(ChatColor.RED + "You used characters, which are not allowed");
								showDetail(type);
								return;
							}
							if (name.length() > 32) {
								p.sendMessage(ChatColor.RED
										+ "The player type name is not allowed to contain more than 32 characters");
								showDetail(type);
								return;
							}
							if (handler.getType(name) != null) {
								p.sendMessage(ChatColor.RED + "A type with this name already exists!");
								showDetail(type);
								return;
							}
							String oldName = type.getName();
							handler.renameType(type, name, true);
							p.sendMessage(ChatColor.GREEN + "Changed name of player type " + oldName + " to " + name);
							showDetail(type);
						}
					};

				}
			};
		} else {
			ISUtils.addLore(renameStack, ChatColor.RED + "You don't have permission to do this");
			renameClick = new DecorationStack(renameStack);
		}
		ci.setSlot(renameClick, 10);
		// edit permissions
		ItemStack is = new ItemStack(Material.CHEST);
		Clickable c;
		ISUtils.setName(is, ChatColor.GOLD + "View and edit permissions");
		if (!gm.hasAccess(g, p.getUniqueId(), PermissionType.getPermission("LIST_PERMS"))) {
			ISUtils.addLore(is, ChatColor.RED + "You are not allowed to list", ChatColor.RED
					+ "permissions for this group");
			c = new DecorationStack(is);
		} else {
			c = new Clickable(is) {

				@Override
				public void clicked(Player arg0) {
					showPermissionEditing(type);
				}
			};
		}
		ci.setSlot(c, 12);
		// add child
		ItemStack childStack = new ItemStack(Material.APPLE);
		ISUtils.addLore(childStack, ChatColor.GOLD + "Add child rank below " + type.getName());
		if (type.getChildren(false).size() != 0) {
			ISUtils.addLore(childStack, ChatColor.AQUA + "Current children:");
			for (PlayerType child : type.getChildren(false)) {
				ISUtils.addLore(childStack, ChatColor.AQUA + "  " + child.getName());
			}
		} else {
			ISUtils.addLore(childStack, ChatColor.AQUA + "Currently no existing children");
		}
		Clickable childClick;
		if (!gm.hasAccess(g, p.getUniqueId(), PermissionType.getPermission("CREATE_PLAYERTYPE"))) {
			ISUtils.addLore(childStack, ChatColor.RED + "You dont have permission to do this");
			childClick = new DecorationStack(childStack);
		} else {
			childClick = new Clickable(childStack) {
				@Override
				public void clicked(final Player p) {
					ClickableInventory.forceCloseInventory(p);
					p.sendMessage(ChatColor.GOLD + "Please enter a name for the new rank");
					new Dialog(p, NameLayerPlugin.getInstance()) {

						@Override
						public List<String> onTabComplete(String wordCompleted, String[] fullMessage) {
							return new LinkedList<String>();
						}

						@Override
						public void onReply(String[] message) {
							PlayerTypeHandler handler = g.getPlayerTypeHandler();
							if (!gm.hasAccess(g, p.getUniqueId(), PermissionType.getPermission("CREATE_PLAYERTYPE"))) {
								p.sendMessage(ChatColor.RED + "You lost permission to do this");
								showDetail(type);
								return;
							}
							if (message.length == 0) {
								p.sendMessage(ChatColor.RED + "You have to enter something!");
								showDetail(type);
								return;
							}
							if (message.length > 1) {
								p.sendMessage(ChatColor.RED + "Player type names may not contain spaces");
								showDetail(type);
								return;
							}
							String name = message[0];
							if (!CreateGroup.isConformName(name)) {
								p.sendMessage(ChatColor.RED + "You used characters, which are not allowed");
								showDetail(type);
								return;
							}
							if (name.length() > 32) {
								p.sendMessage(ChatColor.RED
										+ "The player type name is not allowed to contain more than 32 characters");
								showDetail(type);
								return;
							}
							if (handler.getType(name) != null) {
								p.sendMessage(ChatColor.RED + "A type with this name already exists!");
								showDetail(type);
								return;
							}
							int id = handler.getUnusedId();
							if (id == -1) {
								p.sendMessage(ChatColor.RED
										+ "You have reached the allowed limit of player types, you'll have to delete some before creating new ones");
							}
							PlayerType added = new PlayerType(name, id, type, g);
							handler.registerType(added, true);
							p.sendMessage(ChatColor.GREEN + "You successfully added " + added.getName()
									+ " as player type to " + g.getName());
							showDetail(type);
						}
					};

				}
			};
		}
		ci.setSlot(childClick, 14);
		// delete type
		ItemStack deleteStack = new ItemStack(Material.BARRIER);
		ISUtils.setName(deleteStack, ChatColor.GOLD + "Delete " + type.getName());
		Clickable deleteClick = null;
		final PlayerTypeHandler handler = g.getPlayerTypeHandler();
		if (!gm.hasAccess(g, p.getUniqueId(), PermissionType.getPermission("DELETE_PLAYERTYPE"))) {
			ISUtils.addLore(deleteStack, ChatColor.RED + "You don't have permission to do this");
			deleteClick = new DecorationStack(deleteStack);
		} else {
			if (type == handler.getDefaultNonMemberType() || type == handler.getOwnerType()) {
				ISUtils.addLore(deleteStack, ChatColor.RED + "This rank is undeleteable");
				deleteClick = new DecorationStack(deleteStack);
			} else {
				if (g.getAllTrackedByType(type).size() != 0) {
					ISUtils.addLore(deleteStack, ChatColor.RED
							+ "You can't delete this type, because it still has members");
					deleteClick = new DecorationStack(deleteStack);
				} else {
					for (PlayerType child : type.getChildren(true)) {
						if (g.getAllTrackedByType(child).size() != 0) {
							ISUtils.addLore(deleteStack, ChatColor.RED
									+ "You can't delete this type, because the sub type " + child.getName()
									+ " still has members");
							deleteClick = new DecorationStack(deleteStack);
							break;
						}
					}
					if (deleteClick == null) {
						ISUtils.addLore(deleteStack, ChatColor.GREEN + "Click to delete " + type.getName()
								+ " and all of it's sub ranks");
						deleteClick = new Clickable(deleteStack) {
								@Override
							public void clicked(Player p) {
								if (!gm.hasAccess(g, p.getUniqueId(),
										PermissionType.getPermission("DELETE_PLAYERTYPE"))) {
									p.sendMessage(ChatColor.RED
											+ "You don't have the required permissions to do this");
									showDetail(type);
									return;
								}
								if (g.getAllTrackedByType(type).size() != 0) {
									p.sendMessage(ChatColor.RED
											+ "You can't delete this type, because it still has members");
									showDetail(type);
									return;
								}
								List<PlayerType> children = type.getChildren(true);
								for (PlayerType child : children) {
									if (g.getAllTrackedByType(child).size() != 0) {
										p.sendMessage(ChatColor.RED
												+ "You can't delete this type, because the sub type "
												+ child.getName() + " still has members");
										showDetail(type);
										return;
									}
								}
								if (type == handler.getDefaultNonMemberType() || type == handler.getOwnerType()) {
									p.sendMessage(ChatColor.RED + "You can't delete this type");
									showDetail(type);
									return;
								}
								handler.deleteType(type, true);
								p.sendMessage(ChatColor.GREEN + "The type " + type.getName() + " was deleted from "
										+ g.getName());
								showScreen();
							}
						};
					}
				}
			}
		}
		ci.setSlot(deleteClick, 16);
		ItemStack backToOverview = new ItemStack(Material.WOOD_DOOR);
		ISUtils.setName(backToOverview, ChatColor.GOLD + "Go back");
		ci.setSlot(new Clickable(backToOverview) {

			@Override
			public void clicked(Player arg0) {
				showScreen();
			}
		}, 22);
		ci.showInventory(p);
	}

	private void showPermissionEditing(final PlayerType pType) {
		if (!validGroup()) {
			return;
		}
		if (!gm.hasAccess(g, p.getUniqueId(), PermissionType.getPermission("LIST_PERMS"))) {
			p.sendMessage(ChatColor.RED + "You are not allowed to list permissions for this group");
			showScreen();
			return;
		}
		final List<IClickable> clicks = new ArrayList<IClickable>();
		boolean canEdit = gm.hasAccess(g, p.getUniqueId(), PermissionType.getPermission("PERMS"));
		for (final PermissionType perm : PermissionType.getAllPermissions()) {
			ItemStack is;
			Clickable c;
			final boolean hasPerm = pType.hasPermission(perm);
			if (hasPerm) {
				is = new ItemStack(Material.INK_SACK, 1, (short) 10); // green
																		// dye
				ISUtils.addLore(is, ChatColor.DARK_AQUA + pType.getName() + " currently has", ChatColor.DARK_AQUA
						+ "this permission");
			} else {
				is = new ItemStack(Material.INK_SACK, 1, (short) 1); // red dye
				ISUtils.addLore(is, ChatColor.DARK_AQUA + pType.getName() + " currently doesn't have",
						ChatColor.DARK_AQUA + "this permission");
			}
			ISUtils.setName(is, perm.getName());
			String desc = perm.getDescription();
			if (desc != null) {
				ISUtils.addLore(is, ChatColor.GREEN + desc);
			}
			if (canEdit) {
				ISUtils.addLore(is, ChatColor.AQUA + "Click to toggle");
				c = new Clickable(is) {

					@Override
					public void clicked(Player arg0) {
						if (hasPerm == pType.hasPermission(perm)) { // recheck
							if (gm.hasAccess(g, p.getUniqueId(), PermissionType.getPermission("PERMS"))) {
								NameLayerPlugin.getInstance().info(p.getName() + (hasPerm ? " removed " : " added ")
										+ "the permission " + perm.toString() + "for player type" + pType.toString()
										+ " for " + g.getName() + " via gui");
								if (hasPerm) {
									pType.removePermission(perm, true);
								} else {
									pType.addPermission(perm, true);
								}
							}
						} else {
							p.sendMessage(ChatColor.RED
									+ "Something changed while you were modifying permissions, so cancelled the process");
						}
						showPermissionEditing(pType);
					}
				};
			} else {
				c = new DecorationStack(is);
			}
			clicks.add(c);
		}

		MultiPageView view = new MultiPageView(p, clicks, g.getName(), true);

		ItemStack backToOverview = new ItemStack(Material.WOOD_DOOR);
		ISUtils.setName(backToOverview, ChatColor.GOLD + "Go back");
		view.setMenuSlot(new Clickable(backToOverview) {

			@Override
			public void clicked(Player arg0) {
				showScreen();
			}
		}, 3);
		view.showScreen();
	}

}
