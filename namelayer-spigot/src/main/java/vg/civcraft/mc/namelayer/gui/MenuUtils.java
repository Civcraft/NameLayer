package vg.civcraft.mc.namelayer.gui;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import vg.civcraft.mc.civmodcore.itemHandling.ISUtils;
import vg.civcraft.mc.namelayer.permission.PlayerType;

public class MenuUtils {
	public static ItemStack toggleButton(boolean initState, String name, boolean canModify) {
		ItemStack is = new ItemStack(Material.INK_SACK);
		if (initState) {
			is.setDurability((short) 10); //dye green
			if (canModify) {
			ISUtils.addLore(is, ChatColor.GOLD + "Currently turned on", ChatColor.AQUA + "Click to turn off");
			}
		}
		else {
			is.setDurability((short) 1); //dye red
			if  (canModify) {
			ISUtils.addLore(is, ChatColor.GOLD + "Currently turned off", ChatColor.AQUA + "Click to turn on");
			}
		}
		if (!canModify) {
			ISUtils.addLore(is, ChatColor.RED + "You don't have permission to", ChatColor.RED + "modify this setting");
		}
		ISUtils.setName(is, name);
		return is;
	}
	
	public static ItemStack getPlayerSkull(UUID uuid) {
		return null; // TODO?
	}
	
	public static ItemStack getPlayerTypeStack(PlayerType type) {
		int id = type.getName().hashCode() % 112;
		if (id < 16) {
			ItemStack is = new ItemStack(Material.STAINED_CLAY);
			is.setDurability((short) id);
			return is;
		}
		if (id < 32) {
			ItemStack is = new ItemStack(Material.STAINED_GLASS);
			is.setDurability((short) (id - 16));
			return is;
		}
		if (id < 48) {
			ItemStack is = new ItemStack(Material.WOOL);
			is.setDurability((short) (id - 32));
			return is;
		}
		if (id < 64) {
			ItemStack is = new ItemStack(Material.STAINED_GLASS_PANE);
			is.setDurability((short) (id - 48));
			return is;
		}
		if (id < 80) {
			ItemStack is = new ItemStack(Material.CARPET);
			is.setDurability((short) (id - 64));
			return is;
		}
		if (id < 96) {
			ItemStack is = new ItemStack(Material.BANNER);
			is.setDurability((short) (id - 80));
			return is;
		}
		if (id < 112) {
			ItemStack is = new ItemStack(Material.INK_SACK);
			is.setDurability((short) (id - 96));
			return is;
		}
		return new ItemStack(Material.OBSIDIAN);
	}
}
