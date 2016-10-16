package vg.civcraft.mc.namelayer.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import vg.civcraft.mc.civmodcore.chatDialog.Dialog;
import vg.civcraft.mc.civmodcore.inventorygui.Clickable;
import vg.civcraft.mc.civmodcore.inventorygui.ClickableInventory;
import vg.civcraft.mc.civmodcore.inventorygui.DecorationStack;
import vg.civcraft.mc.civmodcore.inventorygui.IClickable;
import vg.civcraft.mc.civmodcore.inventorygui.MultiPageView;
import vg.civcraft.mc.civmodcore.itemHandling.ISUtils;
import vg.civcraft.mc.namelayer.NameAPI;
import vg.civcraft.mc.namelayer.NameLayerPlugin;
import vg.civcraft.mc.namelayer.events.PromotePlayerEvent;
import vg.civcraft.mc.namelayer.group.DefaultGroupHandler;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.listeners.PlayerListener;
import vg.civcraft.mc.namelayer.misc.MercuryManager;
import vg.civcraft.mc.namelayer.permission.PermissionType;
import vg.civcraft.mc.namelayer.permission.PlayerType;
import vg.civcraft.mc.namelayer.permission.PlayerTypeHandler;

public class MainGroupGUI extends AbstractGroupGUI {

	private Collection<PlayerType> shownTypes;
	private boolean showInvites;

	private int currentPage;

	public MainGroupGUI(Player p, Group g) {
		super(g, p);
		shownTypes = new HashSet<PlayerType>();
		for(PlayerType type : g.getPlayerTypeHandler().getAllTypes()) {
			if (gm.hasAccess(g, p.getUniqueId(), type.getListPermissionType())) {
				shownTypes.add(type);
			}
		}
		showInvites = false;
		currentPage = 0;
		showScreen();
	}

	/**
	 * Shows the main gui overview for a specific group based on the properties
	 * of this class
	 */
	public void showScreen() {
		if (!validGroup()) {
			return;
		}
		ClickableInventory ci = new ClickableInventory(54, g.getName());
		final List<IClickable> clicks = constructClickables();
		if (clicks.size() < 45 * currentPage) {
			// would show an empty page, so go to previous
			currentPage--;
			showScreen();
		}
		// fill gui
		for (int i = 36 * currentPage; i < 36 * (currentPage + 1) && i < clicks.size(); i++) {
			ci.setSlot(clicks.get(i), 9 + i - (36 * currentPage));
		}
		// back button
		if (currentPage > 0) {
			ItemStack back = new ItemStack(Material.ARROW);
			ISUtils.setName(back, ChatColor.GOLD + "Go to previous page");
			Clickable baCl = new Clickable(back) {

				@Override
				public void clicked(Player arg0) {
					if (currentPage > 0) {
						currentPage--;
					}
					showScreen();
				}
			};
			ci.setSlot(baCl, 45);
		}
		// next button
		if ((45 * (currentPage + 1)) <= clicks.size()) {
			ItemStack forward = new ItemStack(Material.ARROW);
			ISUtils.setName(forward, ChatColor.GOLD + "Go to next page");
			Clickable forCl = new Clickable(forward) {

				@Override
				public void clicked(Player arg0) {
					if ((45 * (currentPage + 1)) <= clicks.size()) {
						currentPage++;
					}
					showScreen();
				}
			};
			ci.setSlot(forCl, 53);
		}

		// exit button
		ItemStack backToOverview = new ItemStack(Material.WOOD_DOOR);
		ISUtils.setName(backToOverview, ChatColor.GOLD + "Close");
		ci.setSlot(new Clickable(backToOverview) {

			@Override
			public void clicked(Player arg0) {
				ClickableInventory.forceCloseInventory(arg0);
			}
		}, 49);

		// options at the top
		ci.setSlot(getTypeToggleClick(), 0);
		ci.setSlot(getInvitePlayerClickable(), 1);
		ci.setSlot(getAddBlackListClickable(), 2);
		ci.setSlot(getLeaveGroupClickable(), 3);
		ci.setSlot(getInfoStack(), 4);
		ci.setSlot(getDefaultGroupStack(), 5);
		ci.setSlot(getPasswordClickable(), 6);
		ci.setSlot(getPermOptionClickable(), 7);
		ci.setSlot(getAdminStuffClickable(), 8);
		ci.showInventory(p);
	}

	/**
	 * Creates a list of all Clickables representing members, invitees and
	 * blacklisted players, if they are supposed to be displayed. This is whats
	 * directly fed into the middle of the gui
	 * 
	 */
	private List<IClickable> constructClickables() {
		List<IClickable> clicks = new ArrayList<IClickable>();
		PlayerTypeHandler handler = g.getPlayerTypeHandler();
		for (PlayerType type : handler.getAllTypes()) {
			if (!shownTypes.contains(type)) {
				continue;
			}
			ItemStack example = MenuUtils.getHashedItem(type.getName().hashCode());
			boolean blackListed = handler.isBlackListedType(type);
			boolean hasPerm = gm.hasAccess(g, p.getUniqueId(), type.getRemovalPermissionType());
			for (final UUID uuid : g.getAllTrackedByType(type)) {
				String name = NameAPI.getCurrentName(uuid);
				ItemStack is = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
				SkullMeta meta = (SkullMeta) is.getItemMeta();
				meta.setOwner(NameAPI.getMojangName(uuid));
				is.setItemMeta(meta);
				ISUtils.setName(is, ChatColor.GOLD + name);
				if (g.isOwner(uuid)) {
					ISUtils.addLore(is, ChatColor.AQUA + "Rank: Primary owner");
				} else {
					if (blackListed) {
						ISUtils.addLore(is, ChatColor.BLUE + "Blacklisted as " + type.getName());
					} else {
						ISUtils.addLore(is, ChatColor.AQUA + "Rank: " + type.getName());
					}
				}
				Clickable c;
				if (hasPerm) {
					ISUtils.addLore(is, ChatColor.GREEN + "Click to modify the rank of " + name);
					c = new Clickable(is) {

						@Override
						public void clicked(Player arg0) {
							showDetail(uuid);
						}
					};
				} else {
					ISUtils.addLore(is, ChatColor.RED + "You don't have permission to modify the rank of " + name);
					c = new DecorationStack(is);
				}
				clicks.add(c);
			}
		}
		if (showInvites) {
			Map<UUID, PlayerType> invites = g.dumpInvites();
			for (Entry<UUID, PlayerType> entry : invites.entrySet()) {
				UUID player = entry.getKey();
				final PlayerType type = entry.getValue();
				if (!gm.hasAccess(g, p.getUniqueId(), type.getInvitePermissionType(), type.getListPermissionType())) {
					continue;
				}
				ItemStack is = MenuUtils.getHashedItem(type.getName().hashCode());
				ItemMeta im = is.getItemMeta();
				im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
				im.addEnchant(Enchantment.DURABILITY, 1, true);
				is.setItemMeta(im);
				final String playerName = NameAPI.getCurrentName(player);
				ISUtils.setName(is, ChatColor.GOLD + playerName);
				ISUtils.addLore(is, ChatColor.AQUA + "Invited as " + type.getName());
				boolean canRevoke = gm.hasAccess(g, p.getUniqueId(), type.getInvitePermissionType());
				Clickable c = null;
				if (canRevoke) {
					ISUtils.addLore(is, ChatColor.GREEN + "Click to revoke this invite");
					c = new Clickable(is) {

						@Override
						public void clicked(Player arg0) {
							UUID invitedUUID = NameAPI.getUUID(playerName);
							PlayerType pType = g.getInvite(invitedUUID);
							if (pType == null) {
								p.sendMessage(ChatColor.RED + "Failed to revoke invite for " + playerName
										+ ". This player isn't invited currently.");
								showScreen();
							}
							// make sure the player still has permission to do
							// this
							if (!gm.hasAccess(g, p.getUniqueId(), type.getInvitePermissionType())) {
								p.sendMessage(ChatColor.RED + "You don't have permission to revoke this invite");
							} else {
								NameLayerPlugin.getInstance().info(
										arg0.getName() + " revoked an invite for "
												+ NameAPI.getCurrentName(invitedUUID) + " for group " + g.getName()
												+ "via gui");
								g.removeInvite(invitedUUID, true);
								PlayerListener.removeNotification(invitedUUID, g);
								MercuryManager.remInvite(g.getGroupId(), invitedUUID);

								p.sendMessage(ChatColor.GREEN + playerName + "'s invitation has been revoked.");
							}
							showScreen();
						}
					};
				} else {
					ISUtils.addLore(is, ChatColor.RED + "You don't have permission to revoke this invite");
					c = new DecorationStack(is);
				}
				if (c != null) {
					clicks.add(c);
				}
			}
		}
		Collections.sort(clicks, new Comparator<IClickable>() {
			public int compare(IClickable c1, IClickable c2) {
				return c1.getItemStack().getItemMeta().getDisplayName().toLowerCase()
						.compareTo(c2.getItemStack().getItemMeta().getDisplayName().toLowerCase());
			}
		});
		return clicks;
	}

	/**
	 * Called when the icon representing a member in the middle of the gui is
	 * clicked, this opens up a detailed view where you can select what to do
	 * (promoting/removing)
	 * 
	 * @param uuid
	 */
	public void showDetail(final UUID uuid) {
		if (!validGroup() || !g.isTracked(uuid)) {
			showScreen();
			return;
		}
		ClickableInventory ci = new ClickableInventory(36, g.getName());
		String playerName = NameAPI.getCurrentName(uuid);
		PlayerTypeHandler handler = g.getPlayerTypeHandler();
		final PlayerType currentType = g.getPlayerType(uuid);
		boolean hasExitPermission = gm.hasAccess(g, p.getUniqueId(), currentType.getRemovalPermissionType());
		List<Clickable> clicks = new LinkedList<Clickable>();
		ItemStack removeStack = MenuUtils.getHashedItem(currentType.getName().hashCode());
		Clickable removeClick;
		if (hasExitPermission) {
			ISUtils.setName(removeStack, ChatColor.GREEN + "Remove " + playerName);
			removeClick = new Clickable(removeStack) {

				@Override
				public void clicked(Player arg0) {
					removeMember(uuid);
					showScreen();
				}
			};
		} else {
			ISUtils.setName(removeStack, ChatColor.RED + "You don't have permission to remove " + playerName);
			removeClick = new DecorationStack(removeStack);
		}
		clicks.add(removeClick);
		for (final PlayerType type : handler.getAllTypes()) {
			if (type == currentType) {
				continue;
			}
			ItemStack is = MenuUtils.getHashedItem(type.getName().hashCode());
			boolean canChange = hasExitPermission && gm.hasAccess(g, p.getUniqueId(), type.getInvitePermissionType());
			Clickable c;
			if (canChange) {
				ISUtils.addLore(is, ChatColor.GREEN + "Change rank to " + type.getName());
				c = new Clickable(is) {

					@Override
					public void clicked(Player arg0) {
						changePlayerRank(uuid, type);
						showScreen();
					}
				};
			} else {
				ISUtils.addLore(is, ChatColor.RED + "You don't have permission to change the rank", ChatColor.RED
						+ "of " + playerName + " to " + type.getName());
				c = new DecorationStack(is);
			}
			clicks.add(c);
		}
		int slot = 0;
		for (Clickable c : clicks) {
			ci.setSlot(c, slot);
			slot++;
		}
		ItemStack info = new ItemStack(Material.PAPER);
		ISUtils.setName(info, ChatColor.GOLD + playerName);
		ISUtils.addLore(info, ChatColor.GOLD + "Current rank: " + currentType.getName());
		ci.setSlot(new DecorationStack(info), 31);

		ItemStack backToOverview = new ItemStack(Material.WOOD_DOOR);
		ISUtils.setName(backToOverview, ChatColor.GOLD + "Back to overview");
		ci.setSlot(new Clickable(backToOverview) {

			@Override
			public void clicked(Player arg0) {
				showScreen();
			}
		}, 27);
		ci.showInventory(p);
	}

	private void removeMember(UUID toRemove) {
		if (!g.isMember(toRemove)) {
			p.sendMessage(ChatColor.RED + "This player is no longer tracked by the group and can't be removed");
			return;
		}
		PlayerType current = g.getPlayerType(toRemove);
		if (gm.hasAccess(g, p.getUniqueId(), current.getRemovalPermissionType())) {
			if (g.isOwner(toRemove)) {
				p.sendMessage(ChatColor.RED + "This player owns the group and can't be removed");
				return;
			}
			NameLayerPlugin.getInstance().info(
					p.getName() + " kicked " + NameAPI.getCurrentName(toRemove) + " from " + g.getName() + " via gui");
			g.removeFromTracking(toRemove);
			p.sendMessage(ChatColor.GREEN + NameAPI.getCurrentName(toRemove) + " has been removed from " + g.getName()
					+ " as " + current.getName());
		} else {
			p.sendMessage(ChatColor.RED + "You have lost permission to remove this player");
		}
	}

	private void changePlayerRank(UUID toChange, PlayerType newRank) {
		if (!g.isTracked(toChange)) {
			p.sendMessage(ChatColor.RED + "It seems like " + NameAPI.getCurrentName(toChange)
					+ " is no longer on the group?");
			return;
		}
		PlayerType currentRank = g.getPlayerType(toChange);
		if (gm.hasAccess(g, p.getUniqueId(), currentRank.getRemovalPermissionType())
				&& gm.hasAccess(g, p.getUniqueId(), newRank.getInvitePermissionType())) {
			if (g.isOwner(toChange)) {
				p.sendMessage(ChatColor.RED + "This player owns the group and can't be demoted");
				return;
			}
			PromotePlayerEvent event = new PromotePlayerEvent(toChange, g, currentRank, newRank);
			Bukkit.getPluginManager().callEvent(event);
			if (event.isCancelled()) {
				return;
			}
			Player promoted = Bukkit.getPlayer(NameAPI.getCurrentName(toChange));
			if (promoted != null) {
				promoted.sendMessage(ChatColor.GREEN + "Your rank in " + g.getName() + " was changed from "
						+ currentRank.getName() + " to " + newRank.getName() + " by " + p.getName());
			} else {
				MercuryManager.notifyPromote(g.getName(), toChange, p.getUniqueId(), currentRank.getName(),
						newRank.getName());
			}
			g.removeFromTracking(toChange);
			g.addToTracking(toChange, newRank);
		} else {
			p.sendMessage(ChatColor.RED + "You have lost permission to remove this player");
		}
	}

	private Clickable getAddBlackListClickable() {
		Clickable c;
		ItemStack is = new ItemStack(Material.LEASH);
		ISUtils.setName(is, ChatColor.GOLD + "Add player to blacklist");
		final List<PlayerType> blackListTypes = new LinkedList<PlayerType>();
		PlayerTypeHandler handler = g.getPlayerTypeHandler();
		for (PlayerType type : handler.getAllTypes()) {
			if (handler.isBlackListedType(type)) {
				blackListTypes.add(type);
			}
		}
		if (blackListTypes.size() == 0) {
			ISUtils.addLore(is, ChatColor.RED + "There are currently no blacklist types for this group!");
			c = new DecorationStack(is);
			return c;
		}
		if (blackListTypes.size() == 1) {
			final PlayerType type = blackListTypes.get(0);
			if (gm.hasAccess(g, p.getUniqueId(), type.getInvitePermissionType())) {
				ISUtils.addLore(is, ChatColor.GREEN + "Click to add a player as " + type.getName());
				c = new Clickable(is) {

					@Override
					public void clicked(Player arg0) {
						BlackListGUI blgui = new BlackListGUI(g, p, MainGroupGUI.this);
						blgui.selectName(type);

					}
				};
			} else {
				ISUtils.addLore(is, ChatColor.RED + "You don't have permission to do this");
				c = new DecorationStack(is);
			}
			return c;
		}

		c = new Clickable(is) {

			@Override
			public void clicked(final Player p) {
				BlackListGUI blgui = new BlackListGUI(g, p, MainGroupGUI.this);
				blgui.showScreen();
			}
		};

		return c;
	}

	private Clickable getPasswordClickable() {
		Clickable c;
		ItemStack is = new ItemStack(Material.SIGN);
		ISUtils.setName(is, ChatColor.GOLD + "Add or change password");
		if (gm.hasAccess(g, p.getUniqueId(), PermissionType.getPermission("PASSWORD"))) {
			String pass = g.getPassword();
			if (pass == null) {
				ISUtils.addLore(is, ChatColor.AQUA + "This group doesn't have a password currently");
			} else {
				ISUtils.addLore(is, ChatColor.AQUA + "The current password is: " + ChatColor.YELLOW + pass);
			}
			c = new Clickable(is) {

				@Override
				public void clicked(final Player p) {
					if (gm.hasAccess(g, p.getUniqueId(), PermissionType.getPermission("PASSWORD"))) {
						p.sendMessage(ChatColor.GOLD
								+ "Enter the new password for "
								+ g.getName()
								+ ". Enter \" delete\" to remove an existing password or \"cancel\" to exit this prompt");
						ClickableInventory.forceCloseInventory(p);
						new Dialog(p, NameLayerPlugin.getInstance()) {

							@Override
							public List<String> onTabComplete(String wordCompleted, String[] fullMessage) {
								return new LinkedList<String>();
							}

							@Override
							public void onReply(String[] message) {
								if (message.length == 0) {
									p.sendMessage(ChatColor.RED + "You entered nothing, no password was set");
									return;
								}
								if (message.length > 1) {
									p.sendMessage(ChatColor.RED + "Your password may not contain spaces");
									return;
								}
								String newPassword = message[0];
								if (newPassword.equals("cancel")) {
									p.sendMessage(ChatColor.GREEN + "Left password unchanged");
									return;
								}
								if (newPassword.equals("delete")) {
									g.setPassword(null);
									p.sendMessage(ChatColor.GREEN + "Removed the password from the group");
									NameLayerPlugin.getInstance().info(p.getName() + " removed password " + " for group "
											+ g.getName() + "via gui");
								} else {
									NameLayerPlugin.getInstance().info(p.getName() + " set password to " + newPassword
											+ " for group " + g.getName() + "via gui");
									g.setPassword(newPassword);
									p.sendMessage(ChatColor.GREEN + "Set new password: " + ChatColor.YELLOW
											+ newPassword);
								}
								showScreen();
							}
						};
					} else {
						p.sendMessage(ChatColor.RED + "You lost permission to do this");
						showScreen();
					}
				}
			};
		} else {
			ISUtils.addLore(is, ChatColor.RED + "You don't have permission to do this");
			c = new DecorationStack(is);
		}
		return c;
	}

	private Clickable getPermOptionClickable() {
		ItemStack permStack = new ItemStack(Material.FENCE_GATE);
		ISUtils.setName(permStack, ChatColor.GOLD + "View and manage group permissions");
		Clickable permClickable;
		if (gm.hasAccess(g, p.getUniqueId(), PermissionType.getPermission("LIST_PERMS"))) {
			permClickable = new Clickable(permStack) {
				@Override
				public void clicked(Player arg0) {
					PermissionManageGUI pmgui = new PermissionManageGUI(g, p, MainGroupGUI.this);
					pmgui.showScreen();
				}
			};
		} else {
			ISUtils.addLore(permStack, ChatColor.RED + "You don't have permission", ChatColor.RED + "to do this");
			permClickable = new DecorationStack(permStack);
		}
		return permClickable;
	}

	private Clickable getInvitePlayerClickable() {
		ItemStack inviteStack = new ItemStack(Material.COOKIE);
		ISUtils.setName(inviteStack, ChatColor.GOLD + "Invite new member");
		return new Clickable(inviteStack) {

			@Override
			public void clicked(Player arg0) {
				new InvitationGUI(g, p, MainGroupGUI.this);
			}
		};
	}

	private Clickable getDefaultGroupStack() {
		Clickable c;
		ItemStack is = new ItemStack(Material.BRICK);
		ISUtils.setName(is, ChatColor.GOLD + "Default group");
		final DefaultGroupHandler handler = NameLayerPlugin.getDefaultGroupHandler();
		final String defGroup = handler.getDefaultGroup(p);
		if (defGroup != null && defGroup.equals(g.getName())) {
			ISUtils.addLore(is, ChatColor.AQUA + "This group is your current default group");
			c = new DecorationStack(is);
		} else {
			ISUtils.addLore(is, ChatColor.AQUA + "Click to make this group your default group");
			if (defGroup != null) {
				ISUtils.addLore(is, ChatColor.BLUE + "Your current default group is : " + defGroup);
			}
			c = new Clickable(is) {

				@Override
				public void clicked(Player p) {
					NameLayerPlugin.getInstance().info(p.getName() + " set default group to " + g.getName() + "via gui");
					if (defGroup == null) {
						handler.setDefaultGroup(p, g);
						p.sendMessage(ChatColor.GREEN + "You have set your default group to " + g.getName());
					} else {
						handler.setDefaultGroup(p, g);
						p.sendMessage(ChatColor.GREEN + "You changed your default group from " + defGroup + " to "
								+ g.getName());
					}
					showScreen();
				}
			};
		}
		return c;
	}

	private Clickable getAdminStuffClickable() {
		ItemStack is = new ItemStack(Material.DIAMOND);
		ISUtils.setName(is, ChatColor.GOLD + "Owner functions");
		Clickable c = new Clickable(is) {

			@Override
			public void clicked(Player p) {
				AdminFunctionsGUI subGui = new AdminFunctionsGUI(p, g, MainGroupGUI.this);
				subGui.showScreen();
			}
		};
		return c;
	}

	/**
	 * Constructs the icon used in the gui for leaving a group
	 */
	private Clickable getLeaveGroupClickable() {
		Clickable c;
		ItemStack is = new ItemStack(Material.IRON_DOOR);
		ISUtils.setName(is, ChatColor.GOLD + "Leave group");
		if (g.isOwner(p.getUniqueId())) {
			ISUtils.addLore(is, ChatColor.RED + "You cant leave this group,", ChatColor.RED + "because you own it");
			c = new DecorationStack(is);
		} else {
			c = new Clickable(is) {

				@Override
				public void clicked(Player p) {
					ClickableInventory confirmInv = new ClickableInventory(27, g.getName());
					ItemStack info = new ItemStack(Material.PAPER);
					ISUtils.setName(info, ChatColor.GOLD + "Leave group");
					ISUtils.addLore(info, ChatColor.RED + "Are you sure that you want to", ChatColor.RED
							+ "leave this group? You can not undo this!");
					ItemStack yes = new ItemStack(Material.INK_SACK);
					yes.setDurability((short) 10); // green
					ISUtils.setName(yes, ChatColor.GOLD + "Yes, leave " + g.getName());
					ItemStack no = new ItemStack(Material.INK_SACK);
					no.setDurability((short) 1); // red
					ISUtils.setName(no, ChatColor.GOLD + "No, stay in " + g.getName());
					confirmInv.setSlot(new Clickable(yes) {

						@Override
						public void clicked(Player p) {
							if (!g.isMember(p.getUniqueId())) {
								p.sendMessage(ChatColor.RED + "You are not a member of this group.");
								showScreen();
								return;
							}
							if (g.isDisciplined()) {
								p.sendMessage(ChatColor.RED + "This group is disciplined.");
								showScreen();
								return;
							}
							NameLayerPlugin.getInstance().info(p.getName() + " left " + g.getName() + "via gui");
							g.removeFromTracking(p.getUniqueId());
							p.sendMessage(ChatColor.GREEN + "You have left " + g.getName());
						}
					}, 11);
					confirmInv.setSlot(new Clickable(no) {

						@Override
						public void clicked(Player p) {
							showScreen();
						}
					}, 15);
					confirmInv.setSlot(new DecorationStack(info), 4);
					confirmInv.showInventory(p);
				}
			};
		}
		return c;
	}

	private Clickable getInfoStack() {
		Clickable c;
		ItemStack is = new ItemStack(Material.PAPER);
		ISUtils.setName(is, ChatColor.GOLD + "Stats for " + g.getName());
		ISUtils.addLore(is,
				ChatColor.DARK_AQUA + "Your current rank: " + ChatColor.YELLOW
						+ g.getPlayerType(p.getUniqueId()).getName());
		boolean hasGroupStatsPerm = gm.hasAccess(g, p.getUniqueId(), PermissionType.getPermission("GROUPSTATS"));
		int blacklistCount = 0;
		int memberCount = 0;
		for (PlayerType type : g.getPlayerTypeHandler().getAllTypes()) {
			if (type == g.getPlayerTypeHandler().getDefaultNonMemberType()) {
				continue;
			}
			if (hasGroupStatsPerm || gm.hasAccess(g, p.getUniqueId(), type.getListPermissionType())) {
				int amount = g.getAllTrackedByType(type).size();
				ISUtils.addLore(is, ChatColor.AQUA + String.valueOf(amount) + " " + type.getName());
				if (g.getPlayerTypeHandler().isBlackListedType(type)) {
					blacklistCount += amount;
				} else {
					memberCount += amount;
				}
			}
		}
		if (hasGroupStatsPerm) {
			ISUtils.addLore(is, ChatColor.DARK_AQUA + String.valueOf(memberCount) + " total group members");
			ISUtils.addLore(is, ChatColor.BLUE + String.valueOf(blacklistCount) + " total blacklisted");
			ISUtils.addLore(is,
					ChatColor.DARK_AQUA + "Group owner: " + ChatColor.YELLOW + NameAPI.getCurrentName(g.getOwner()));
		}
		c = new DecorationStack(is);
		return c;
	}
	
	private Clickable getTypeToggleClick() {
		ItemStack is = new ItemStack(Material.SHIELD);
		ISUtils.setName(is, "Toggle showing player types");
		ISUtils.addLore(is, ChatColor.GOLD + "Click to toggle showing certain player types and filter shown players");
		final Clickable c = new Clickable(is) {
			
			@Override
			public void clicked(Player arg0) {
				showTypeToggleMenu();
			}
		};
		return c;
	}
	
	private void showTypeToggleMenu() {
		List <IClickable> clicks = new LinkedList<IClickable>();
		for(final PlayerType type : g.getPlayerTypeHandler().getAllTypes()) {
			if (type == g.getPlayerTypeHandler().getDefaultNonMemberType()) {
				continue;
			}
			Clickable typeClick;
			ItemStack is = new ItemStack(Material.INK_SACK);
			ISUtils.setName(is, type.getName());
			if (shownTypes.contains(type)) {
				is.setDurability((short) 10); //green
				ISUtils.addLore(is, ChatColor.GREEN + "Currently showing " + type.getName(), ChatColor.GOLD + "Click to hide");
				typeClick = new Clickable(is) {
					
					@Override
					public void clicked(Player arg0) {
						shownTypes.remove(type);
						showTypeToggleMenu();
					}
				};
			}
			else {
				is.setDurability((short) 1); //red
				ISUtils.addLore(is, ChatColor.RED + "Currently hiding " + type.getName() + "Click to show");
				if (!gm.hasAccess(g, p.getUniqueId(), type.getListPermissionType())) {
					ISUtils.addLore(is, ChatColor.RED + "You dont have permission to list members of " + type.getName());
					typeClick = new DecorationStack(is);
				}
				else {
					typeClick = new Clickable(is) {
						
						@Override
						public void clicked(Player arg0) {
							shownTypes.add(type);
							showTypeToggleMenu();
						}
					};
				}
			}
			clicks.add(typeClick);
		}
		MultiPageView view = new MultiPageView(p, clicks, g.getName(), true);
		//button to toggle all types
		ItemStack toggleAll = new ItemStack(Material.INK_SACK);
		ISUtils.setName(toggleAll, "Toggle all types");
		ISUtils.addLore(toggleAll, ChatColor.GOLD + "Hide all currently shown types and show all currently hidden types");
		view.setMenuSlot(new Clickable(toggleAll) {
			
			@Override
			public void clicked(Player arg0) {
				for(PlayerType type : g.getPlayerTypeHandler().getAllTypes()) {
					if (shownTypes.contains(type)) {
						shownTypes.remove(type);
					}
					else {
						if (gm.hasAccess(g, p.getUniqueId(), type.getListPermissionType())) {
							shownTypes.add(type);
						}
					}
				}	
				showTypeToggleMenu();
			}
		}, 0);
		//button to toggle all on
		ItemStack toggleOn = new ItemStack(Material.INK_SACK);
		toggleOn.setDurability((short) 10); //green
		ISUtils.setName(toggleOn, "Show all player types");
		ISUtils.addLore(toggleOn, ChatColor.GOLD + "Show all player types for which you have permission to list members");
		view.setMenuSlot(new Clickable(toggleOn) {
			
			@Override
			public void clicked(Player arg0) {
				for(PlayerType type : g.getPlayerTypeHandler().getAllTypes()) {
					if (!shownTypes.contains(type) && gm.hasAccess(g, p.getUniqueId(), type.getListPermissionType())) {
						shownTypes.add(type);
					}
				}
				showTypeToggleMenu();
			}
		}, 1);
		
		//button to toggle all off
		ItemStack toggleOff = new ItemStack(Material.INK_SACK);
		toggleOff.setDurability((short) 1);  //red
		ISUtils.setName(toggleOff, "Hide all player types");
		ISUtils.addLore(toggleOff, ChatColor.GOLD + "Hide all player types");
		view.setMenuSlot(new Clickable(toggleOff) {
			
			@Override
			public void clicked(Player arg0) {
				shownTypes.clear();
				showTypeToggleMenu();
			}
		}, 2);
		
		//back button
		ItemStack backToOverview = new ItemStack(Material.WOOD_DOOR);
		ISUtils.setName(backToOverview, ChatColor.GOLD + "Back to overview");
		ISUtils.setLore(backToOverview, ChatColor.GOLD + "Click to go back to the general group menu");
		view.setMenuSlot(new Clickable(backToOverview) {

			@Override
			public void clicked(Player arg0) {
				showScreen();
			}
		}, 3);
		ItemStack showInviteStack = MenuUtils.toggleButton(showInvites, ChatColor.GOLD + "Show invited players", true);
		view.setMenuSlot(new Clickable(showInviteStack) {
			
			@Override
			public void clicked(Player arg0) {
				showInvites = !showInvites;
				showTypeToggleMenu();
			}
		}, 4);
		view.showScreen();
	}
}
