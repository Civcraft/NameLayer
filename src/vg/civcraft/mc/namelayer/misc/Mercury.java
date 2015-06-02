package vg.civcraft.mc.namelayer.misc;

import vg.civcraft.mc.mercury.MercuryAPI;
import vg.civcraft.mc.namelayer.NameLayerPlugin;

public class Mercury {

	/**
	 * Invalidates a group across all servers.
	 * @param message- The message to send.
	 */
	public static void invalidateGroup(String message){
		NameLayerPlugin.mercuryapi.sendMessage("all", "namelayer", message);
	}
}
