package vg.civcraft.mc.namelayer.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import vg.civcraft.mc.namelayer.NameLayerPlugin;


public class AssociationList {
	private Database db;
	
	private final int MAXIMUM_PLAYERNAME_LENGTH = 16;

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
		db.execute("create definer=current_user procedure addplayertotable("
				+ "in pl varchar(40), in uu varchar(40)) sql security invoker begin "
				+ ""
				+ "declare account varchar(40);"
				+ "declare counter int(10);"
				+ ""
				+ "set counter=0;"
				+ "set counter=(select count(*) from Name_player p where p.uuid=uu);" //count of all entries with the players uuid
				+ ""
				+ "if (counter == 0) then" //this uuid is not in the table yet
				+ "		set counter =(select count(uuid) from Name_player p where p.player=pl);" //count of all entries with the players name
				+ "		if (counter == 0) then" //no other player has this name yet, so we can safely insert it
				+ "			insert into Name_player(player,uuid) values(pl,uu);"
				+ "		end if;"
				+ "end if;");
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
		String initalName = playername;
		boolean insertedName = false;
		for(int i = 0; !insertedName; i++) {
			while(i != 0 && (playername.length() + String.valueOf(i).length()) > MAXIMUM_PLAYERNAME_LENGTH) { //ensure proper length
				playername = playername.substring(0, playername.length() - 1);
			}
			String toInsert;
			if (i == 0) { //playername is most likely not taken, so we first attempt to insert his normal name
				toInsert = playername;
			}
			else { //original name was taken, concat number
				toInsert = playername + String.valueOf(i); 
			}
			try {
				addPlayer.setString(1, toInsert);
				addPlayer.setString(2, uuid.toString());
				addPlayer.execute();
			} catch (SQLException e) {
				e.printStackTrace();
				break;
			}
			//check whether name was successfully inserted
			if (getCurrentName(uuid) != null) {
				insertedName = true;
				if (i != 0) {
					NameLayerPlugin.log(Level.INFO, "Had to update the name " + initalName + " to " + toInsert + ", because his name already existed"); 
				}
			}
		}
	}

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
