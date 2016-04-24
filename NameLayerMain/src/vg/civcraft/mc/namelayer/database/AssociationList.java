package vg.civcraft.mc.namelayer.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import vg.civcraft.mc.namelayer.NameLayerPlugin;


public class AssociationList {
	private Database db;

	public AssociationList(Database db){
		this.db = db;
		if (db.isConnected()){
			genTables();
			initializeProcedures();
			initializeStatements();
		}
	}

	public void genTables(){
		// creates the player table
		// Where uuid and host names will be stored
		db.execute("CREATE TABLE IF NOT EXISTS `Name_player` (" +
				"`uuid` varchar(40) NOT NULL," +
				"`player` varchar(40) NOT NULL,"
				+ "UNIQUE KEY `uuid_player_combo` (`uuid`, `player`));");
	}

	private String addPlayer;
	private String getUUIDfromPlayer;
	private String getPlayerfromUUID;
	private String changePlayerName;
	private String getAllPlayerInfo;

	public void initializeStatements(){
		addPlayer = "call addplayertotable(?, ?)"; // order player name, uuid
		getUUIDfromPlayer = "select uuid from Name_player " +
				"where player=?";
		getPlayerfromUUID = "select player from Name_player " +
				"where uuid=?";
		changePlayerName = "delete from Name_player " +
				"where uuid=?";
		getAllPlayerInfo = "select * from Name_player";
	}

	public void initializeProcedures(){
		db.execute("drop procedure if exists addplayertotable");
		db.execute("create definer=current_user procedure"
				+ "addplayertotable(in pl varchar(16), in uu varchar(36))"
				+ "sql security invoker"
				+ "begin"
				+ "  declare account varchar(16);"
				+ "  declare counter int;"
				+ "  declare safe boolean;"
				+ "  set account = pl;"
				+ "  if NOT EXISTS(select uuid from Name_player where uuid=uu) then"
				+ "    --this uuid is not in the table yet"
				+ "    if NOT EXISTS(select uuid from Name_player where player=pl) then"
				+ "      --no other player has this name yet, so we can safely insert it"
				+ "      insert into Name_player(player,uuid) values(pl,uu);"
				+ "    else"
				+ "      --name conflict resolution"
				+ "      set safe = false;"
				+ "      set counter = 1;"
				+ "      REPEAT"
				+ "        IF (LENGTH(pl) + LENGTH(cast(counter as char)) > 16) THEN"
				+ "          set account = CONCAT(SUBSTRING(pl, 0, 16 - length(cast(counter as char))), cast(counter as char));"
				+ "        ELSE"
				+ "          set account = CONCAT(pl, cast(counter as char));"
				+ "        END IF;"
				+ "        IF NOT EXISTS(SELECT uuid FROM Name_player where player=account) THEN"
				+ "          insert into Name_player(player, uuid) values(account, uu);"
				+ "          set safe = true;"
				+ "        ELSE"
				+ "          set counter = counter + 1;"
				+ "        END IF;"
				+ "      UNTIL safe END REPEAT;"
				+ "    end if;"
				+ "  end if;"
				+ "  --return new player name"
				+ "  select account;"
				+ "end");
	}

	// returns null if no uuid was found
	public UUID getUUID(String playername){
		NameLayerPlugin.reconnectAndReintializeStatements();
		PreparedStatement getUUIDfromPlayer = db.prepareStatement(this.getUUIDfromPlayer);
		try {
			getUUIDfromPlayer.setString(1, playername);
			ResultSet set = getUUIDfromPlayer.executeQuery();
			if (!set.next() || set.wasNull()) return null;
			String uuid = set.getString("uuid");
			return UUID.fromString(uuid);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	// returns null if no playername was found
	public String getCurrentName(UUID uuid){
		NameLayerPlugin.reconnectAndReintializeStatements();
		PreparedStatement getPlayerfromUUID = db.prepareStatement(this.getPlayerfromUUID);
		try {
			getPlayerfromUUID.setString(1, uuid.toString());
			ResultSet set = getPlayerfromUUID.executeQuery();
			if (!set.next()) return null;
			String playername = set.getString("player");
			return playername;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public void addPlayer(String playername, UUID uuid){
		NameLayerPlugin.reconnectAndReintializeStatements();
		PreparedStatement addPlayer = db.prepareStatement(this.addPlayer);
		try {
			addPlayer.setString(1, playername);
			addPlayer.setString(2, uuid.toString());
			ResultSet rs = addPlayer.executeQuery();
			String newname = rs.getString(0);
			
			if (!playername.equals(newname)) {
				NameLayerPlugin.log(Level.INFO, "Had to update the name " + playername + " to " + newname + ", because the name already existed"); 
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * This appears to be unsafe (doesn't check name uniqueness).
	 * 
	 * @param newName
	 * @param uuid
	 */
	public void changePlayer(String newName, UUID uuid) {
		NameLayerPlugin.reconnectAndReintializeStatements();
		PreparedStatement changePlayerName = db.prepareStatement(this.changePlayerName);
		try {
			changePlayerName.setString(1, uuid.toString());
			changePlayerName.execute();
			addPlayer(newName, uuid);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/**
	 * This method returns all player info in the table.  It is used mainly
	 * by NameAPI class to prepopulate the maps.
	 * As such Object[0] will return Map<String, UUID> while Object[1]
	 * will return Map<UUID, String>
	 */
	public PlayerMappingInfo getAllPlayerInfo(){
		NameLayerPlugin.reconnectAndReintializeStatements();
		PreparedStatement getAllPlayerInfo = db.prepareStatement(this.getAllPlayerInfo);
		Map<String, UUID> nameMapping = new HashMap<String, UUID>();
		Map<UUID, String> uuidMapping = new HashMap<UUID, String>();
		try {
			ResultSet set = getAllPlayerInfo.executeQuery();
			while (set.next()){
				UUID uuid = UUID.fromString(set.getString("uuid"));
				String playername = set.getString("player");
				nameMapping.put(playername, uuid);
				uuidMapping.put(uuid, playername);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new PlayerMappingInfo(nameMapping, uuidMapping);
	}

	public static class PlayerMappingInfo {
		public final Map<String, UUID> nameMapping;
		public final Map<UUID, String> uuidMapping;
		public PlayerMappingInfo(Map<String, UUID> nameMap, Map<UUID, String> uuidMap) {
			this.nameMapping = nameMap;
			this.uuidMapping = uuidMap;
		}
	}
}
