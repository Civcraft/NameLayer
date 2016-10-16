package vg.civcraft.mc.namelayer;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.scheduler.BukkitRunnable;

import vg.civcraft.mc.civmodcore.annotations.CivConfig;
import vg.civcraft.mc.civmodcore.annotations.CivConfigType;
import vg.civcraft.mc.namelayer.database.AssociationList;
import vg.civcraft.mc.namelayer.misc.NameFetcher;

public class NameAPI {
	private static GroupManager groupManager;
	private static AssociationList associations;
	
	private static Map<UUID, String> uuidsToName = new HashMap<UUID, String>();
	private static Map<String, UUID> nameToUUIDS = new HashMap<String, UUID>();
	private static Map<UUID, String> mojangNameByUUID = new HashMap<UUID, String>();
	
	public NameAPI(GroupManager man, AssociationList ass){
		groupManager = man;
		associations =  ass;
		loadAllPlayerInfo();
	}
	
	@CivConfig(name = "persistance.forceloadnamecaching", def = "false", type = CivConfigType.Bool)
	public void loadAllPlayerInfo(){
		uuidsToName.clear();
		nameToUUIDS.clear();
		mojangNameByUUID.clear();
		
		boolean load = NameLayerPlugin.getInstance().GetConfig().get("persistance.forceloadnamecaching").getBool();
		if (!load)
			return;
		final AssociationList.PlayerMappingInfo pmi = associations.getAllPlayerInfo();
		nameToUUIDS = pmi.nameMapping;
		uuidsToName = pmi.uuidMapping;
		mojangNameByUUID = pmi.mojangName;
		System.out.println("init call");
		new BukkitRunnable() {
			
			@Override
			public void run() {
				Logger logger = NameLayerPlugin.getInstance().getLogger();
				System.out.println("Im alive");
				logger.info("Loading mojang names for " + pmi.unknownNames.size() + " players");
				for(UUID uuid : pmi.unknownNames) {
					List <UUID> toResolve = new LinkedList<UUID>();
					toResolve.add(uuid);
					NameFetcher fetcher = new NameFetcher(toResolve);
					try {
						Map <UUID, String> names = fetcher.call();
						String mojangName = names.get(uuid);
						if (mojangName != null) {
							mojangNameByUUID.put(uuid, mojangName);
							associations.updateMojangName(mojangName, uuid);
							logger.log(Level.INFO, "Updating mojang name for " + uuidsToName.get(uuid) + " to " + mojangName);
						}
					} catch (Exception e) {
						logger.log(Level.WARNING, "Failed to resolve mojang name for " + uuid.toString(), e);
					}
				}
			}
		}.runTaskAsynchronously(NameLayerPlugin.getInstance());
	}
	
	public static void resetCache(UUID uuid) {
		String name = getCurrentName(uuid);
		uuidsToName.remove(uuid);
		nameToUUIDS.remove(name);
	}
	/**
	 * Returns the UUID of the player on the given server.
	 * @param playerName The playername.
	 * @return Returns the UUID of the player.
	 */
	public static UUID getUUID(String playerName) {
		UUID uuid = nameToUUIDS.get(playerName);
		if (uuid == null){
			uuid = associations.getUUID(playerName);
			if (uuid != null) {
				nameToUUIDS.put(playerName, uuid);
			}
		}
		return uuid;
	}
	/**
	 * Gets the playername from a given server from their uuid.
	 * @param uuid.
	 * @return Returns the PlayerName from the UUID.
	 */
	public static String getCurrentName(UUID uuid) {
		String name = uuidsToName.get(uuid);
		if (name == null){
			name = associations.getCurrentName(uuid);
			if (name != null) {
				uuidsToName.put(uuid, name);
			}
		}
		return name;
	}
	
	/**
	 * Gets the name the given uuid has in mojangs servers. For a given uuid no name may be known, because it wasnt cached yet, in this case the name used
	 * on this server is returned as fallback. If the given uuid is completly unknown, meaning no name for this server exists, null will be returned
	 * @param uuid UUID to get name for
	 * @return Mojang name of the player if available
	 */
	public static String getMojangName(UUID uuid) {
		String name = mojangNameByUUID.get(uuid);
		if (name == null) {
			name = getCurrentName(uuid);
		}
		return name;
	}
	
	/**
	 * @return Returns an instance of the GroupManager.
	 */
	public static GroupManager getGroupManager(){
		return groupManager;
	}
	/**
	 * @return Returns an instance of the AssociationList.
	 */
	public static AssociationList getAssociationList(){
		return associations;
	}
}
