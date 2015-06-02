package vg.civcraft.mc.namelayer.listeners;

import java.util.ArrayList;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import vg.civcraft.mc.mercury.MercuryPlugin;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.GroupManager.PlayerType;
import vg.civcraft.mc.namelayer.NameAPI;
import vg.civcraft.mc.namelayer.NameLayerPlugin;
import vg.civcraft.mc.namelayer.events.GroupDeleteEvent;
import vg.civcraft.mc.namelayer.events.GroupMergeEvent;
import vg.civcraft.mc.namelayer.events.GroupTransferEvent;
import vg.civcraft.mc.namelayer.events.PromotePlayerEvent;
import vg.civcraft.mc.namelayer.group.Group;

public class MercuryMessageListener extends BukkitRunnable {
	
	private GroupManager gm = NameAPI.getGroupManager();
	@EventHandler(priority = EventPriority.HIGHEST)
	private void onMercuryMessage(String message){
		String[] splitmessage = message.split(" ");
		String reason = splitmessage[0];
		String group = splitmessage[1];
		Group g = gm.getGroup(group);
		if (reason.equals("recache")){
			gm.invalidateCache(g.getName());
		}
		else if (reason.equals("delete")){
			GroupDeleteEvent e = new GroupDeleteEvent(g, true);
			Bukkit.getPluginManager().callEvent(e);
			gm.invalidateCache(g.getName());
		}
		else if (reason.equals("merge")){
			GroupMergeEvent e = new GroupMergeEvent(g, gm.getGroup(splitmessage[2]), true);
			Bukkit.getPluginManager().callEvent(e);
			gm.invalidateCache(g.getName());
			gm.invalidateCache(splitmessage[2]);
		}
		else if (reason.equals("transfer")){
			GroupTransferEvent e = new GroupTransferEvent(g, UUID.fromString(splitmessage[2]));
			Bukkit.getPluginManager().callEvent(e);
			gm.invalidateCache(g.getName());
		}
	}
	@Override
	public void run() {
		if (NameLayerPlugin.mercuryapi.hasMessages("namelayer")){
			ArrayList<String> msgs = MercuryPlugin.api.getMessages("namelayer");
			for (String msg : msgs){
				this.onMercuryMessage(msg);
				
			}
		}
	}

}
