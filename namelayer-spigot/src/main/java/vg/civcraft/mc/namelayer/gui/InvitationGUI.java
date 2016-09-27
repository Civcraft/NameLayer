package vg.civcraft.mc.namelayer.gui;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import vg.civcraft.mc.civmodcore.chatDialog.Dialog;
import vg.civcraft.mc.civmodcore.inventorygui.Clickable;
import vg.civcraft.mc.civmodcore.inventorygui.ClickableInventory;
import vg.civcraft.mc.civmodcore.inventorygui.DecorationStack;
import vg.civcraft.mc.civmodcore.itemHandling.ISUtils;
import vg.civcraft.mc.mercury.MercuryAPI;
import vg.civcraft.mc.namelayer.NameAPI;
import vg.civcraft.mc.namelayer.NameLayerPlugin;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.permission.PlayerType;
import vg.civcraft.mc.namelayer.permission.PlayerTypeHandler;

public class InvitationGUI extends AbstractGroupGUI {

	private MainGroupGUI parent;

	public InvitationGUI(Group g, Player p, MainGroupGUI parent) {
		super(g, p);
		this.parent = parent;
	}

	public void showScreen() {
		PlayerTypeHandler handler = g.getPlayerTypeHandler();
		ClickableInventory ci = new ClickableInventory(36, g.getName());
		int slot = 0;
		for (final PlayerType type : handler.getAllTypes()) {
			if (!handler.isMemberType(type)) {
				continue;
			}
			ItemStack is = MenuUtils.getPlayerTypeStack(type);
			ISUtils.setName(is, ChatColor.GOLD + type.getName());
			Clickable c;
			if (gm.hasAccess(g, p.getUniqueId(), type.getInvitePermissionType())) {
				ISUtils.addLore(is, ChatColor.GREEN + "Click to invite someone as " + type.getName());
				c = new Clickable(is) {

					@Override
					public void clicked(Player arg0) {
						selectName(type);
					}
				};
			} else {
				ISUtils.addLore(is, ChatColor.GREEN + "You dont have permission to invite to this rank");
				c = new DecorationStack(is);
			}
			ci.setSlot(c, slot++);
		}
		ItemStack backToOverview = new ItemStack(Material.WOOD_DOOR);
		ISUtils.setName(backToOverview, ChatColor.GOLD + "Go back");
		ci.setSlot(new Clickable(backToOverview) {

			@Override
			public void clicked(Player arg0) {
				parent.showScreen();
			}
		}, 31);
		ci.showInventory(p);
	}

	public void selectName(final PlayerType type) {
		p.sendMessage(ChatColor.GOLD
				+ "Enter the name of the player to invite or \"cancel\" to exit this prompt. You may also enter multiple names separated by a space to invite all of them");
		ClickableInventory.forceCloseInventory(p);
		new Dialog(p, NameLayerPlugin.getInstance()) {

			@Override
			public List<String> onTabComplete(String word, String[] msg) {
				List<String> names;
				if (NameLayerPlugin.isMercuryEnabled()) {
					names = new LinkedList<String>(MercuryAPI.getAllPlayers());
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
				if (gm.hasAccess(g, p.getUniqueId(), type.getInvitePermissionType())) {
					for (String playerName : message) {
						UUID blackUUID = NameAPI.getUUID(playerName);
						if (blackUUID == null) {
							p.sendMessage(ChatColor.RED + playerName + " doesn't exist");
							continue;
						}
						if (g.isTracked(blackUUID)) {
							p.sendMessage(ChatColor.RED
									+ NameAPI.getCurrentName(blackUUID)
									+ " is already either a member or blacklisted for this group and can't be invited");
							continue;
						}
						NameLayerPlugin.getInstance().info(
								p.getName() + " invited " + NameAPI.getCurrentName(blackUUID) + " for group "
										+ g.getName() + " via gui");
						g.addToTracking(blackUUID, type);
						p.sendMessage(ChatColor.GREEN + NameAPI.getCurrentName(blackUUID)
								+ " was successfully invited as " + type.getName());
					}
				} else {
					p.sendMessage(ChatColor.RED + "You lost permission to do this");
				}
				showScreen();
			}
		};
	}
}
