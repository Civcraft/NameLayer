package vg.civcraft.mc.namelayer.gui;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import vg.civcraft.mc.civmodcore.chatDialog.Dialog;
import vg.civcraft.mc.civmodcore.inventorygui.Clickable;
import vg.civcraft.mc.civmodcore.inventorygui.ClickableInventory;
import vg.civcraft.mc.civmodcore.inventorygui.DecorationStack;
import vg.civcraft.mc.civmodcore.itemHandling.ISUtils;
import vg.civcraft.mc.mercury.MercuryAPI;
import vg.civcraft.mc.namelayer.NameAPI;
import vg.civcraft.mc.namelayer.NameLayerPlugin;
import vg.civcraft.mc.namelayer.command.commands.InvitePlayer;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.permission.PlayerType;
import vg.civcraft.mc.namelayer.permission.PlayerTypeHandler;

public class InvitationGUI extends AbstractGroupGUI {

	private MainGroupGUI parent;

	public InvitationGUI(Group g, Player p, MainGroupGUI parent) {
		super(g, p);
		this.parent = parent;
		showScreen();
	}

	private void showScreen() {
		ClickableInventory ci = new ClickableInventory(27, g.getName());

		// back button
		ItemStack backToOverview = new ItemStack(Material.WOOD_DOOR);
		ISUtils.setName(backToOverview, ChatColor.GOLD + "Back to overview");
		ci.setSlot(new Clickable(backToOverview) {

			@Override
			public void clicked(Player arg0) {
				parent.showScreen();
			}
		}, 0);

		ItemStack explain = new ItemStack(Material.PAPER);
		ISUtils.setName(explain, ChatColor.GOLD + "Select an option");
		ISUtils.addLore(explain, ChatColor.AQUA + "Please select the rank ",
				ChatColor.AQUA + "you want the invited player to have");
		ci.setSlot(new DecorationStack(explain), 4);
		List<PlayerType> playerTypes = g.getPlayerTypeHandler().getAllTypes();
		for (int i = 0; i < playerTypes.size(); i++) {
			PlayerType pType = playerTypes.get(i);
			if (pType.equals(g.getPlayerTypeHandler().getBlacklistedType())
					|| pType.equals(g.getPlayerTypeHandler()
							.getDefaultNonMemberType())) {
				continue;
			}
			ci.setSlot(produceOptionStack(pType), 9 + i);
		}
		ci.showInventory(p);
	}

	private Clickable produceOptionStack(final PlayerType pType) {
		ItemStack is = MenuUtils.getPlayerTypeStack(pType.getId());
		ItemMeta im = is.getItemMeta();
		im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		is.setItemMeta(im);
		ISUtils.setName(is, ChatColor.GOLD + "Invite as " + pType.getName());
		final PlayerTypeHandler handler = g.getPlayerTypeHandler();
		Clickable c;
		if (p.hasPermission("namelayer.admin")
				|| (g.isMember(p.getUniqueId()) && handler.isRelated(pType,
						g.getPlayerType(p.getUniqueId())))) {
			c = new Clickable(is) {

				@Override
				public void clicked(Player arg0) {
					p.sendMessage(ChatColor.GOLD
							+ "Enter the name of the player to invite or \"cancel\" to exit this prompt. You may also enter the names"
							+ "of multiple players, separated with spaces to invite all of them");
					Dialog enterName = new Dialog(arg0,
							NameLayerPlugin.getInstance()) {
						public void onReply(String[] message) {
							if (p.hasPermission("namelayer.admin")
									|| (g.isMember(p.getUniqueId()) && handler
											.isRelated(pType, g.getPlayerType(p
													.getUniqueId())))) {
								for (String s : message) {
									if (s.equalsIgnoreCase("cancel")) {
										parent.showScreen();
										return;
									}
									UUID inviteUUID = NameAPI.getUUID(s);
									if (inviteUUID == null) {
										p.sendMessage(ChatColor.RED
												+ "The player " + s
												+ " doesn't exist");
										continue;
									}
									if (g.isMember(inviteUUID)) { // So a player
																	// can't
																	// demote
																	// someone
																	// who is
																	// above
																	// them.
										p.sendMessage(ChatColor.RED
												+ NameAPI
														.getCurrentName(inviteUUID)
												+ " is already a member of "
												+ g.getName());
										continue;
									}
									if (NameLayerPlugin.getBlackList()
											.isBlacklisted(g, inviteUUID)) {
										p.sendMessage(ChatColor.RED
												+ NameAPI
														.getCurrentName(inviteUUID)
												+ " is currently blacklisted, you have to unblacklist him before inviting him to the group");
										continue;
									}
									NameLayerPlugin
											.log(Level.INFO,
													p.getName()
															+ " invited "
															+ NameAPI
																	.getCurrentName(inviteUUID)
															+ " to group "
															+ g.getName()
															+ "via gui");
									InvitePlayer.sendInvitation(g, pType,
											inviteUUID, p.getUniqueId(), true);

									if (NameLayerPlugin.isMercuryEnabled()) {
										MercuryAPI.sendGlobalMessage(
												"addInvitation "
														+ g.getGroupId() + " "
														+ pType.toString()
														+ " " + inviteUUID,
												"namelayer");
									}
									p.sendMessage(ChatColor.GREEN
											+ "Invited "
											+ NameAPI
													.getCurrentName(inviteUUID)
											+ " as "
											+ pType.getName());
								}
							} else {
								p.sendMessage(ChatColor.RED
										+ "You lost permission to invite a player to this rank");
							}
							parent.showScreen();
						}

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
							names.add("cancel");
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
					};
				}
			};
		} else {
			ISUtils.addLore(is,
					ChatColor.RED + "You don't have permission to invite as "
							+ pType.getName());
			c = new DecorationStack(is);
		}
		return c;
	}
}
