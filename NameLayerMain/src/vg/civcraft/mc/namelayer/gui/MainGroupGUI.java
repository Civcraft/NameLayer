package vg.civcraft.mc.namelayer.gui;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import vg.civcraft.mc.civmodcore.chatDialog.Dialog;
import vg.civcraft.mc.civmodcore.chatDialog.DialogManager;
import vg.civcraft.mc.civmodcore.inventorygui.Clickable;
import vg.civcraft.mc.civmodcore.inventorygui.ClickableInventory;
import vg.civcraft.mc.civmodcore.inventorygui.DecorationStack;
import vg.civcraft.mc.civmodcore.itemHandling.ISUtils;
import vg.civcraft.mc.mercury.MercuryAPI;
import vg.civcraft.mc.namelayer.NameAPI;
import vg.civcraft.mc.namelayer.NameLayerPlugin;
import vg.civcraft.mc.namelayer.events.PromotePlayerEvent;
import vg.civcraft.mc.namelayer.group.BlackList;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.listeners.PlayerListener;
import vg.civcraft.mc.namelayer.permission.PermissionType;
import vg.civcraft.mc.namelayer.permission.PlayerType;
import vg.civcraft.mc.namelayer.permission.PlayerTypeHandler;

public class MainGroupGUI extends AbstractGroupGUI {

	private boolean showInheritedMembers;
	private boolean showBlacklist;
	private boolean showInvites;

	private int currentPage;

	public MainGroupGUI(Player p, Group g) {
		super(g, p);
		showBlacklist = false;
		showInvites = false;
		showInheritedMembers = false;
		currentPage = 0;
		showScreen();
	}

	/**
	 * Shows the main gui overview for a specific group based on the properties
	 * of this class
	 */
	public void showScreen() {
		ClickableInventory.forceCloseInventory(p);
		if (!validGroup()) {
			return;
		}
		ClickableInventory ci = new ClickableInventory(54, g.getName());
		final List<Clickable> clicks = constructClickables();
		if (clicks.size() < 45 * currentPage) {
			// would show an empty page, so go to previous
			currentPage--;
			showScreen();
		}
		// fill gui
		for (int i = 36 * currentPage; i < 36 * (currentPage + 1)
				&& i < clicks.size(); i++) {
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

		// options

		ci.setSlot(createInheritedMemberToggle(), 46);
		ci.setSlot(createInviteToggle(), 47);

		// exit button
		ItemStack backToOverview = new ItemStack(Material.WOOD_DOOR);
		ISUtils.setName(backToOverview, ChatColor.GOLD + "Close");
		ci.setSlot(new Clickable(backToOverview) {

			@Override
			public void clicked(Player arg0) {
				// just let it close, dont do anything
			}
		}, 49);

		// options at the top
		ci.setSlot(getInvitePlayerClickable(), 0);
		ci.setSlot(createBlacklistToggle(), 1);
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
	private List<Clickable> constructClickables() {
		List<Clickable> clicks = new ArrayList<Clickable>();
		if (showInheritedMembers) {
			if (g.hasSuperGroup()) {
				clicks.addAll(getRecursiveInheritedMembers(g.getSuperGroup()));
			}
		}
		if (showBlacklist) {
			final BlackList black = NameLayerPlugin.getBlackList();
			for (final UUID uuid : black.getBlacklist(g)) {
				ItemStack is = new ItemStack(Material.LEATHER_CHESTPLATE);
				LeatherArmorMeta meta = (LeatherArmorMeta) is.getItemMeta();
				meta.setColor(Color.BLACK);
				meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
				is.setItemMeta(meta);
				ISUtils.setName(is, NameAPI.getCurrentName(uuid));
				Clickable c;
				if (gm.hasAccess(g, p.getUniqueId(),
						PermissionType.getPermission("BLACKLIST"))) {
					ISUtils.addLore(is, ChatColor.GREEN + "Click to remove "
							+ NameAPI.getCurrentName(uuid), ChatColor.GREEN
							+ "from the blacklist");
					c = new Clickable(is) {

						@Override
						public void clicked(Player arg0) {
							if (gm.hasAccess(g, p.getUniqueId(),
									PermissionType.getPermission("BLACKLIST"))) {
								NameLayerPlugin.log(
										Level.INFO,
										arg0.getName() + " removed "
												+ NameAPI.getCurrentName(uuid)
												+ " from the blacklist of "
												+ g.getName() + "via gui");
								black.removeBlacklistMember(g, uuid, true);
								checkRecacheGroup();
								p.sendMessage(ChatColor.GREEN + "You removed "
										+ NameAPI.getCurrentName(uuid)
										+ " from the blacklist");
							} else {
								p.sendMessage(ChatColor.RED
										+ "You lost permission to remove this player from the blacklist");
							}
							showScreen();
						}
					};
				} else {
					ISUtils.addLore(is, ChatColor.RED
							+ "You dont have permission to remove",
							ChatColor.RED + NameAPI.getCurrentName(uuid)
									+ "from the blacklist");
					c = new DecorationStack(is);
				}
				clicks.add(c);
			}

		}
		if (showInvites) {
			Map<UUID, PlayerType> invites = NameLayerPlugin
					.getGroupManagerDao().getInvitesForGroup(g);
			for (Entry<UUID, PlayerType> entry : invites.entrySet()) {
				ItemStack is = new ItemStack(Material.CHAINMAIL_CHESTPLATE);
				ItemMeta im = is.getItemMeta();
				im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
				is.setItemMeta(im);
				final String playerName = NameAPI
						.getCurrentName(entry.getKey());
				ISUtils.setName(is, ChatColor.GOLD + playerName);
				PlayerType pType = entry.getValue();
				if (!p.hasPermission("namelayer.admin")
						&& !(g.isMember(p.getUniqueId()) && g
								.getPlayerTypeHandler().isRelated(pType,
										g.getPlayerType(p.getUniqueId())))) {
					// not enough permission to see it
					continue;
				}
				ISUtils.addLore(is,
						ChatColor.AQUA + "Invited as " + pType.getName());
				Clickable c = null;
				ISUtils.addLore(is, ChatColor.GREEN
						+ "Click to revoke this invite");
				c = new Clickable(is) {

					@Override
					public void clicked(Player arg0) {
						UUID invitedUUID = NameAPI.getUUID(playerName);
						PlayerType pType = g.getInvite(invitedUUID);
						if (pType == null) {
							p.sendMessage(ChatColor.RED
									+ "Failed to revoke invite for "
									+ playerName
									+ ". This player isn't invited currently.");
							showScreen();
						}
						// make sure the player still has permission to do
						// this
						if (!p.hasPermission("namelayer.admin")
								&& !(g.isMember(p.getUniqueId()) && g
										.getPlayerTypeHandler()
										.isRelated(
												pType,
												g.getPlayerType(p.getUniqueId())))) {
							p.sendMessage(ChatColor.RED
									+ "You don't have permission to revoke this invite");
						} else {
							NameLayerPlugin
									.log(Level.INFO,
											arg0.getName()
													+ " revoked an invite for "
													+ NameAPI
															.getCurrentName(invitedUUID)
													+ " for group "
													+ g.getName() + "via gui");
							g.removeInvite(invitedUUID, true);
							PlayerListener.removeNotification(invitedUUID, g);

							if (NameLayerPlugin.isMercuryEnabled()) {
								MercuryAPI.sendGlobalMessage(
										"removeInvitation " + g.getGroupId()
												+ " " + invitedUUID,
										"namelayer");
							}
							p.sendMessage(ChatColor.GREEN + playerName
									+ "'s invitation has been revoked.");
						}
						showScreen();
					}
				};
				if (c != null) {
					clicks.add(c);
				}
			}
		}
		for (UUID uuid : g.getAllMembers()) {
			Clickable c = null;
			PlayerType pType = g.getPlayerType(uuid);
			c = constructMemberClickable(uuid, pType);
			if (c != null) {
				clicks.add(c);
			}
		}

		return clicks;
	}

	/**
	 * Convenience method used when constructing clickables in the middle of the
	 * gui, which represent members
	 */
	private Clickable constructMemberClickable(final UUID toDisplay,
			PlayerType rank) {
		Clickable c;
		ItemStack is = MenuUtils.getPlayerTypeStack(rank.getId());
		ItemMeta im = is.getItemMeta();
		im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		is.setItemMeta(im);
		ISUtils.setName(is, ChatColor.GOLD + NameAPI.getCurrentName(toDisplay));
		if (g.isOwner(toDisplay)) { // special case for primary owner
			ISUtils.addLore(is, ChatColor.AQUA + "Rank: Primary Owner");
			ISUtils.addLore(is, ChatColor.RED + "You don't have permission",
					ChatColor.RED + "to modify the rank of this player");
			c = new DecorationStack(is);

		} else {
			ISUtils.addLore(is,
					ChatColor.AQUA + "Rank: "
							+ g.getPlayerType(toDisplay).getName());
			if (g.getPlayerTypeHandler().isRelated(rank,
					g.getPlayerType(p.getUniqueId()))
					|| p.hasPermission("namelayer.admin")) {
				ISUtils.addLore(is, ChatColor.GREEN
						+ "Click to modify this player's", ChatColor.GREEN
						+ "rank or to remove them");
				c = new Clickable(is) {

					@Override
					public void clicked(Player arg0) {
						showDetail(toDisplay);
					}
				};
			} else {
				ISUtils.addLore(is,
						ChatColor.RED + "You don't have permission",
						ChatColor.RED + "to modify the rank of this player");
				c = new DecorationStack(is);
			}
		}
		return c;
	}

	/**
	 * Called when the icon representing a member in the middle of the gui is
	 * clicked, this opens up a detailed view where you can select what to do
	 * (promoting/removing)
	 * 
	 * @param uuid
	 */
	public void showDetail(final UUID uuid) {
		if (!validGroup()) {
			showScreen();
			return;
		}
		ClickableInventory.forceCloseInventory(p);
		ClickableInventory ci = new ClickableInventory(36, g.getName());
		String playerName = NameAPI.getCurrentName(uuid);

		ItemStack info = new ItemStack(Material.PAPER);
		ISUtils.setName(info, ChatColor.GOLD + playerName);
		ISUtils.addLore(info, ChatColor.GOLD + "Current rank: "
				+ g.getPlayerType(uuid).getName());
		ci.setSlot(new DecorationStack(info), 4);

		List<PlayerType> playerTypes = g.getPlayerTypeHandler().getAllTypes();
		for (int i = 0; i < playerTypes.size(); i++) {
			PlayerType pType = playerTypes.get(i);
			if (pType.equals(g.getPlayerTypeHandler().getBlacklistedType())
					|| pType.equals(g.getPlayerTypeHandler()
							.getDefaultNonMemberType())) {
				continue;
			}
			ci.setSlot(setupDetailSlot(uuid, pType), 9 + i);
		}

		ItemStack backToOverview = new ItemStack(Material.WOOD_DOOR);
		ISUtils.setName(backToOverview, ChatColor.GOLD + "Back to overview");
		ci.setSlot(new Clickable(backToOverview) {

			@Override
			public void clicked(Player arg0) {
				showScreen();
			}
		}, 0);
		ci.showInventory(p);
	}

	/**
	 * Used by the gui that allows selecting an action for a specific member to
	 * easily construct the clickables needed
	 */
	private Clickable setupDetailSlot(final UUID toChange,
			final PlayerType pType) {
		final PlayerType rank = g.getPlayerType(toChange);
		ItemStack mod = MenuUtils.getPlayerTypeStack(pType.getId());
		ItemMeta im = mod.getItemMeta();
		im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		mod.setItemMeta(im);
		Clickable modClick;
		if (rank == pType) {
			ISUtils.setName(mod, ChatColor.GOLD + "Remove this player");
			if (!(g.getPlayerTypeHandler().isRelated(pType,
					g.getPlayerType(p.getUniqueId())) || p
					.hasPermission("namelayer.admin"))) {
				ISUtils.addLore(mod, ChatColor.RED
						+ "You dont have permission to do this");
				modClick = new DecorationStack(mod);
			} else {
				modClick = new Clickable(mod) {

					@Override
					public void clicked(Player arg0) {
						if (g.getPlayerTypeHandler().isRelated(pType,
								g.getPlayerType(p.getUniqueId()))
								|| p.hasPermission("namelayer.admin")) {
							removeMember(toChange);
							showScreen();
						}
					}
				};
			}
		} else {
			ISUtils.setName(mod, ChatColor.GOLD + "Promote this player to "
					+ pType.getName());
			if (!(g.getPlayerTypeHandler().isRelated(pType,
					g.getPlayerType(p.getUniqueId())) || p
					.hasPermission("namelayer.admin"))) {
				ISUtils.addLore(mod, ChatColor.RED
						+ "You dont have permission to do this");
				modClick = new DecorationStack(mod);
			} else {
				modClick = new Clickable(mod) {

					@Override
					public void clicked(Player arg0) {
						changePlayerRank(toChange, pType);
						showDetail(toChange);
					}
				};
			}
		}
		return modClick;
	}

	private void removeMember(UUID toRemove) {
		if (!(g.getPlayerTypeHandler().isRelated(g.getPlayerType(toRemove),
				g.getPlayerType(p.getUniqueId())) || p
				.hasPermission("namelayer.admin"))) {
			if (!g.isMember(toRemove)) {
				p.sendMessage(ChatColor.RED
						+ "This player is no longer on the group and can't be removed");
				return;
			}
			if (g.isOwner(toRemove)) {
				p.sendMessage(ChatColor.RED
						+ "This player owns the group and can't be removed");
			}
			NameLayerPlugin.log(Level.INFO,
					p.getName() + " kicked " + NameAPI.getCurrentName(toRemove)
							+ " from " + g.getName() + "via gui");
			g.removeMember(toRemove);
			checkRecacheGroup();
			p.sendMessage(ChatColor.GREEN + NameAPI.getCurrentName(toRemove)
					+ " has been removed from the group");
		} else {
			p.sendMessage(ChatColor.RED
					+ "You have lost permission to remove this player");
		}
	}

	private void changePlayerRank(UUID toChange, PlayerType newRank) {
		PlayerType executorRank = g.getPlayerType(p.getUniqueId());
		PlayerType oldRank = g.getPlayerType(toChange);
		PlayerTypeHandler handler = g.getPlayerTypeHandler();
		if ((handler.isRelated(newRank, executorRank) && handler.isRelated(
				oldRank, executorRank)) || p.hasPermission("namelayer.admin")) {
			if (!g.isMember(toChange)) {
				p.sendMessage(ChatColor.RED
						+ "This player is no longer on the group and can't be "
						+ "demoted");
				return;
			}
			if (g.isOwner(toChange)) {
				p.sendMessage(ChatColor.RED
						+ "This player owns the group and can't be demoted");
			}
			OfflinePlayer prom = Bukkit.getOfflinePlayer(toChange);
			NameLayerPlugin.log(
					Level.INFO,
					p.getName() + " changed player rank for "
							+ NameAPI.getCurrentName(toChange) + " from "
							+ oldRank.getName() + " to " + newRank.getName()
							+ " for group " + g.getName() + "via gui");
			if (prom.isOnline()) {
				Player oProm = (Player) prom;
				PromotePlayerEvent event = new PromotePlayerEvent(oProm, g,
						oldRank, newRank);
				Bukkit.getPluginManager().callEvent(event);
				if (event.isCancelled()) {
					p.sendMessage(ChatColor.RED
							+ "Could not change player rank, you should complain about this");
					return;
				}
				g.removeMember(toChange);
				g.addMember(toChange, newRank);
				oProm.sendMessage(ChatColor.GREEN
						+ "You have been promoted to " + newRank.getName()
						+ " in " + g.getName());
			} else {
				// player is offline change their perms
				g.removeMember(toChange);
				g.addMember(toChange, newRank);
			}
			checkRecacheGroup();
			p.sendMessage(ChatColor.GREEN + NameAPI.getCurrentName(toChange)
					+ " has been " + "promoted to " + newRank.getName());
		} else {
			p.sendMessage(ChatColor.RED
					+ "You have lost permission to remove this player");
		}
	}

	private Clickable createBlacklistToggle() {
		ItemStack is = MenuUtils.toggleButton(
				showBlacklist,
				ChatColor.GOLD + "Show blacklisted players",
				gm.hasAccess(g, p.getUniqueId(),
						PermissionType.getPermission("GROUPSTATS")));
		Clickable c;
		if (gm.hasAccess(g, p.getUniqueId(),
				PermissionType.getPermission("GROUPSTATS"))) {
			c = new Clickable(is) {

				@Override
				public void clicked(Player arg0) {
					showBlacklist = !showBlacklist;
					showScreen();
				}
			};
		} else {
			c = new DecorationStack(is);
		}
		return c;
	}

	private Clickable createInheritedMemberToggle() {
		boolean canToggle = gm.hasAccess(g, p.getUniqueId(),
				PermissionType.getPermission("GROUPSTATS"));
		ItemStack is = MenuUtils.toggleButton(showInheritedMembers,
				ChatColor.GOLD + "Show inherited members", canToggle);
		Clickable c;
		if (canToggle) {
			c = new Clickable(is) {

				@Override
				public void clicked(Player p) {
					showInheritedMembers = !showInheritedMembers;
					showScreen();

				}
			};
		} else {
			c = new DecorationStack(is);
		}
		return c;
	}

	private Clickable createInviteToggle() {
		ItemStack is = MenuUtils.toggleButton(showInvites, ChatColor.GOLD
				+ "Show invited players", true);
		return new Clickable(is) {

			@Override
			public void clicked(Player arg0) {
				showInvites = !showInvites;
				showScreen();
			}
		};
	}

	private Clickable getAddBlackListClickable() {
		Clickable c;
		ItemStack is = new ItemStack(Material.LEASH);
		ISUtils.setName(is, ChatColor.GOLD + "Add player to blacklist");
		if (gm.hasAccess(g, p.getUniqueId(),
				PermissionType.getPermission("BLACKLIST"))) {
			c = new Clickable(is) {

				@Override
				public void clicked(final Player p) {
					p.sendMessage(ChatColor.GOLD
							+ "Enter the name of the player to blacklist or \"cancel\" to exit this prompt");
					Dialog dia = new Dialog(p, NameLayerPlugin.getInstance()) {

						@Override
						public List<String> onTabComplete(String word,
								String[] msg) {
							List<String> names;
							if (NameLayerPlugin.isMercuryEnabled()) {
								names = new LinkedList<String>(
										MercuryAPI.getAllPlayers());
							} else {
								names = new LinkedList<String>();
								for (Player p : Bukkit.getOnlinePlayers()) {
									names.add(p.getName());
								}
							}
							if (word.equals("")) {
								return names;
							}
							List<String> result = new LinkedList<String>();
							String comp = word.toLowerCase();
							for (String s : names) {
								if (s.toLowerCase().startsWith(comp)) {
									result.add(s);
								}
							}
							return result;
						}

						@Override
						public void onReply(String[] message) {
							if (message[0].equalsIgnoreCase("cancel")) {
								showScreen();
								return;
							}
							if (gm.hasAccess(g, p.getUniqueId(),
									PermissionType.getPermission("BLACKLIST"))) {
								boolean didSomething = false;
								for (String playerName : message) {
									UUID blackUUID = NameAPI
											.getUUID(playerName);
									if (blackUUID == null) {
										p.sendMessage(ChatColor.RED
												+ playerName + " doesn't exist");
										continue;
									}
									if (g.isMember(blackUUID)) {
										p.sendMessage(ChatColor.RED
												+ NameAPI
														.getCurrentName(blackUUID)
												+ " is currently a member of this group and can't be blacklisted");
										continue;
									}
									BlackList bl = NameLayerPlugin
											.getBlackList();
									if (bl.isBlacklisted(g, blackUUID)) {
										p.sendMessage(ChatColor.RED
												+ NameAPI
														.getCurrentName(blackUUID)
												+ " is already blacklisted");
										continue;
									}
									didSomething = true;
									NameLayerPlugin
											.log(Level.INFO,
													p.getName()
															+ " blacklisted "
															+ NameAPI
																	.getCurrentName(blackUUID)
															+ " for group "
															+ g.getName()
															+ "via gui");
									bl.addBlacklistMember(g, blackUUID, true);
									p.sendMessage(ChatColor.GREEN
											+ NameAPI.getCurrentName(blackUUID)
											+ " was successfully blacklisted");
								}
								if (didSomething) {
									checkRecacheGroup();
								}
							} else {
								p.sendMessage(ChatColor.RED
										+ "You lost permission to do this");
							}
							showScreen();
						}
					};

				}
			};
		} else {
			ISUtils.addLore(is, ChatColor.RED
					+ "You don't have permission to do this");
			c = new DecorationStack(is);
		}
		return c;
	}

	private Clickable getPasswordClickable() {
		Clickable c;
		ItemStack is = new ItemStack(Material.SIGN);
		ISUtils.setName(is, ChatColor.GOLD + "Add or change password");
		if (gm.hasAccess(g, p.getUniqueId(),
				PermissionType.getPermission("PASSWORD"))) {
			String pass = g.getPassword();
			if (pass == null) {
				ISUtils.addLore(is, ChatColor.AQUA
						+ "This group doesn't have a password currently");
			} else {
				ISUtils.addLore(is, ChatColor.AQUA
						+ "The current password is: " + ChatColor.YELLOW + pass);
			}
			c = new Clickable(is) {

				@Override
				public void clicked(final Player p) {
					if (gm.hasAccess(g, p.getUniqueId(),
							PermissionType.getPermission("PASSWORD"))) {
						p.sendMessage(ChatColor.GOLD
								+ "Enter the new password for "
								+ g.getName()
								+ ". Enter \" delete\" to remove an existing password or \"cancel\" to exit this prompt");
						new Dialog(p, NameLayerPlugin.getInstance()) {

							@Override
							public List<String> onTabComplete(
									String wordCompleted, String[] fullMessage) {
								return new LinkedList<String>();
							}

							@Override
							public void onReply(String[] message) {
								if (message.length == 0) {
									p.sendMessage(ChatColor.RED
											+ "You entered nothing, no password was set");
									return;
								}
								if (message.length > 1) {
									p.sendMessage(ChatColor.RED
											+ "Your password may not contain spaces");
									return;
								}
								String newPassword = message[0];
								if (newPassword.equals("cancel")) {
									p.sendMessage(ChatColor.GREEN
											+ "Left password unchanged");
									return;
								}
								if (newPassword.equals("delete")) {
									g.setPassword(null);
									p.sendMessage(ChatColor.GREEN
											+ "Removed the password from the group");
									NameLayerPlugin.log(Level.INFO, p.getName()
											+ " removed password "
											+ " for group " + g.getName()
											+ "via gui");
								} else {
									NameLayerPlugin.log(Level.INFO, p.getName()
											+ " set password to " + newPassword
											+ " for group " + g.getName()
											+ "via gui");
									g.setPassword(newPassword);
									p.sendMessage(ChatColor.GREEN
											+ "Set new password: "
											+ ChatColor.YELLOW + newPassword);
								}
								checkRecacheGroup();
								showScreen();
							}
						};
					} else {
						p.sendMessage(ChatColor.RED
								+ "You lost permission to do this");
						showScreen();
					}
				}
			};
		} else {
			ISUtils.addLore(is, ChatColor.RED
					+ "You don't have permission to do this");
			c = new DecorationStack(is);
		}
		return c;
	}

	private Clickable getPermOptionClickable() {
		ItemStack permStack = new ItemStack(Material.FENCE_GATE);
		ISUtils.setName(permStack, ChatColor.GOLD
				+ "View and manage group permissions");
		Clickable permClickable;
		if (gm.hasAccess(g, p.getUniqueId(),
				PermissionType.getPermission("LIST_PERMS"))) {
			permClickable = new Clickable(permStack) {
				@Override
				public void clicked(Player arg0) {
					PermissionManageGUI pmgui = new PermissionManageGUI(g, p,
							MainGroupGUI.this);
					pmgui.showScreen();
				}
			};
		} else {
			ISUtils.addLore(permStack, ChatColor.RED
					+ "You don't have permission", ChatColor.RED + "to do this");
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
		final String defGroup = gm.getDefaultGroup(p.getUniqueId());
		if (defGroup != null && defGroup.equals(g.getName())) {
			ISUtils.addLore(is, ChatColor.AQUA
					+ "This group is your current default group");
			c = new DecorationStack(is);
		} else {
			ISUtils.addLore(is, ChatColor.AQUA
					+ "Click to make this group your default group");
			if (defGroup != null) {
				ISUtils.addLore(is, ChatColor.BLUE
						+ "Your current default group is : " + defGroup);
			}
			c = new Clickable(is) {

				@Override
				public void clicked(Player p) {
					NameLayerPlugin.log(Level.INFO, p.getName()
							+ " set default group to " + g.getName()
							+ "via gui");
					if (defGroup == null) {
						g.setDefaultGroup(p.getUniqueId());
						p.sendMessage(ChatColor.GREEN
								+ "You have set your default group to "
								+ g.getName());
					} else {
						g.changeDefaultGroup(p.getUniqueId());
						p.sendMessage(ChatColor.GREEN
								+ "You changed your default group from "
								+ defGroup + " to " + g.getName());
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
				AdminFunctionsGUI subGui = new AdminFunctionsGUI(p, g,
						MainGroupGUI.this);
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
			ISUtils.addLore(is, ChatColor.RED + "You cant leave this group,",
					ChatColor.RED + "because you own it");
			c = new DecorationStack(is);
		} else {
			c = new Clickable(is) {

				@Override
				public void clicked(Player p) {
					ClickableInventory confirmInv = new ClickableInventory(27,
							g.getName());
					ItemStack info = new ItemStack(Material.PAPER);
					ISUtils.setName(info, ChatColor.GOLD + "Leave group");
					ISUtils.addLore(info, ChatColor.RED
							+ "Are you sure that you want to", ChatColor.RED
							+ "leave this group? You can not undo this!");
					ItemStack yes = new ItemStack(Material.INK_SACK);
					yes.setDurability((short) 10); // green
					ISUtils.setName(yes,
							ChatColor.GOLD + "Yes, leave " + g.getName());
					ItemStack no = new ItemStack(Material.INK_SACK);
					no.setDurability((short) 1); // red
					ISUtils.setName(no,
							ChatColor.GOLD + "No, stay in " + g.getName());
					confirmInv.setSlot(new Clickable(yes) {

						@Override
						public void clicked(Player p) {
							if (!g.isMember(p.getUniqueId())) {
								p.sendMessage(ChatColor.RED
										+ "You are not a member of this group.");
								showScreen();
								return;
							}
							if (g.isDisciplined()) {
								p.sendMessage(ChatColor.RED
										+ "This group is disciplined.");
								showScreen();
								return;
							}
							NameLayerPlugin.log(Level.INFO, p.getName()
									+ " left " + g.getName() + "via gui");
							g.removeMember(p.getUniqueId());
							p.sendMessage(ChatColor.GREEN + "You have left "
									+ g.getName());
							checkRecacheGroup();
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
		ISUtils.addLore(is, ChatColor.DARK_AQUA + "Your current rank: "
				+ ChatColor.YELLOW + g.getPlayerType(p.getUniqueId()).getName());
		boolean hasGroupStatsPerm = gm.hasAccess(g, p.getUniqueId(),
				PermissionType.getPermission("GROUPSTATS"));
		PlayerType executorRank = g.getPlayerType(p.getUniqueId());
		PlayerTypeHandler handler = g.getPlayerTypeHandler();
		List<PlayerType> types = handler.getAllTypes();
		for (PlayerType type : types) {
			if (type.equals(handler.getBlacklistedType())
					|| type.equals(handler.getDefaultNonMemberType())) {
				continue;
			}
			if (hasGroupStatsPerm || handler.isRelated(type, executorRank)) {
				ISUtils.addLore(is,
						ChatColor.AQUA + String.valueOf(g.getAllMembers(type))
								+ " " + type.getName());
			}
		}
		if (hasGroupStatsPerm) {
			ISUtils.addLore(
					is,
					ChatColor.DARK_AQUA
							+ String.valueOf(g.getAllMembers().size())
							+ " total group members");
			ISUtils.addLore(is, ChatColor.DARK_AQUA + "Group owner: "
					+ ChatColor.YELLOW + NameAPI.getCurrentName(g.getOwner()));
		}
		c = new DecorationStack(is);
		return c;
	}

	private List<Clickable> getRecursiveInheritedMembers(Group g) {
		List<Clickable> clicks = new LinkedList<Clickable>();
		if (g.hasSuperGroup()) {
			clicks.addAll(getRecursiveInheritedMembers(g.getSuperGroup()));
		}
		for (UUID uuid : g.getAllMembers()) {
			ItemStack is = MenuUtils.getPlayerTypeStack(g.getGroupId());
			ISUtils.setName(is, NameAPI.getCurrentName(uuid));
			ISUtils.addLore(is, ChatColor.AQUA + "Inherited "
					+ g.getPlayerType(uuid).getName() + " from " + g.getName());
			clicks.add(new DecorationStack(is));
		}
		return clicks;
	}
}
