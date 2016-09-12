package vg.civcraft.mc.namelayer.database;

import java.util.concurrent.Callable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;

import com.google.common.collect.Lists;

import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameLayerPlugin;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.listeners.PlayerListener;
import vg.civcraft.mc.namelayer.permission.PermissionType;
import vg.civcraft.mc.namelayer.permission.PlayerType;
import vg.civcraft.mc.namelayer.permission.PlayerTypeHandler;

import vg.civcraft.mc.civmodcore.dao.ManagedDatasource;
/**
 * First guinea pig of conversion to ManagedDatasource.
 *
 */
public class GroupManagerDao {
	private Logger logger;
	private ManagedDatasource db;
	protected NameLayerPlugin plugin = NameLayerPlugin.getInstance();
	
		
	private static final String createGroup = "call createGroup(?,?,?,?)";
	private static final String getGroup = "select f.group_name, f.founder, f.password, f.discipline_flags, fi.group_id " +
				"from faction f "
				+ "inner join faction_id fi on fi.group_name = f.group_name "
				+ "where f.group_name = ?";
	private static final String getGroupIDs = "SELECT f.group_id, count(DISTINCT fm.member_name) AS sz FROM faction_id f "
				+ "INNER JOIN faction_member fm ON f.group_id = fm.group_id WHERE f.group_name = ? GROUP BY f.group_id ORDER BY sz DESC";
	private static final String getGroupById = "select f.group_name, f.founder, f.password, f.discipline_flags, fi.group_id " +
				"from faction f "
				+ "inner join faction_id fi on fi.group_id = ? "
				+ "where f.group_name = fi.group_name";
	private static final String getAllGroupsNames = "select f.group_name from faction_id f "
				+ "inner join faction_member fm on f.group_id = fm.group_id "
				+ "where fm.member_name = ?";
	private static final String deleteGroup = "call deletegroupfromtable(?, ?)";

	private static final String addMember = "insert into faction_member(" +
				"group_id, member_name, role) select group_id, ?, ? from "
				+ "faction_id where group_name = ?";
	private static final String getMembers = "select fm.member_name from faction_member fm "
				+ "inner join faction_id id on id.group_name = ? "
				+ "where fm.group_id = id.group_id and fm.role = ?";
	private static final String removeMember = "delete fm.* from faction_member fm "
				+ "inner join faction_id fi on fi.group_id = fm.group_id "
				+ "where fm.member_name = ? and fi.group_name =?";
		
	private static final String removeAllMembers = "delete fm.* from faction_member fm "
				+ "inner join faction_id fi on fi.group_id = fm.group_id "
				+ "where fi.group_name =?";
		
		// So this will link all instances (name/id pairs) of the subgroup to all instances (name/id pairs) of the supergroup.
	private static final String addSubGroup = "INSERT INTO subgroup (group_id, sub_group_id) "
				+ "SELECT super.group_id, sub.group_id "
				+ "FROM faction_id super "
				+ "INNER JOIN faction_id sub "
				+ "ON sub.group_name = ? "
				+ "WHERE super.group_name = ?";
		
		// This undoes the above. It unlinks all instances (name/id pairs) of the subgroup from all instances (name/id pairs) of the supergroup.
	private static final String removeSubGroup ="DELETE FROM subgroup "
				+ "WHERE group_id IN (SELECT group_id FROM faction_id WHERE group_name = ?) "
				+ "AND sub_group_id IN (SELECT group_id FROM faction_id WHERE group_name = ?)";
		
		// This lists all unique subgroups (names) for all instances (name/id pairs) of the supergroup.
	private static final String getSubGroups = "SELECT DISTINCT sub.group_name FROM faction_id sub "
				+ "INNER JOIN faction_id super "
				+ "ON super.group_name = ? "
				+ "INNER JOIN subgroup other "
				+ "ON other.group_id = super.group_id "
				+ "WHERE sub.group_id = other.sub_group_id";
		
		// This lists all unique supergroups (names) which are parent(s) for all instances (name/id pairs) of the subgroup. 
		// I expect most implementations to ignore if this has multiple results; a "safe" implementation will check.
	private static final String getSuperGroup ="SELECT DISTINCT f.group_name FROM faction_id f "
				+ "INNER JOIN faction_id sf ON sf.group_name = ? "
				+ "INNER JOIN subgroup sg ON sg.group_id = sf.group_id "
				+ "WHERE f.group_id = sg.sub_group_id";
		
		// returns count of unique names, but not (name / id pairs) of all groups.
	private static final String countGroups = "select count(DISTINCT group_name) as count from faction";
		
		// returns count of unique names of groups owned by founder
	private static final String countGroupsFromUUID = "select count(DISTINCT group_name) as count from faction where founder = ?";
		
	private static final String mergeGroup = "call mergeintogroup(?,?)";
		
	private static final String updatePassword = "update faction set `password` = ? "
				+ "where group_name = ?";
		
	private static final String updateOwner = "update faction set founder = ? "
				+ "where group_name = ?";
		
	private static final String updateDisciplined = "update faction set discipline_flags = ? "
				+ "where group_name = ?";
		
	private static final String addAutoAcceptGroup = "insert into toggleAutoAccept(uuid)"
				+ "values(?)";
	private static final String getAutoAcceptGroup = "select uuid from toggleAutoAccept "
				+ "where uuid = ?";
	private static final String removeAutoAcceptGroup = "delete from toggleAutoAccept where uuid = ?";
		
	private static final String loadAllAutoAcceptGroup = "select uuid from toggleAutoAccept;";
		
	private static final String setDefaultGroup = "insert into default_group values(?, ?)";
		
	private static final String changeDefaultGroup = "update default_group set defaultgroup = ? where uuid = ?";
	
		
	private static final String getDefaultGroup = "select defaultgroup from default_group "
				+ "where uuid = ?";
	private static final String getAllDefaultGroups = "select uuid,defaultgroup from default_group";
		
	private static final String loadGroupsInvitations = "select uuid, groupName, role from group_invitation";
		
	private static final String addGroupInvitation = "insert into group_invitation(uuid, groupName, role) values(?, ?, ?) on duplicate key update role=values(role), date=now();";
		
	private static final String removeGroupInvitation = "delete from group_invitation where uuid = ? and groupName = ?";
		
	private static final String loadGroupInvitation = "select role from group_invitation where uuid = ? and groupName = ?";
		
	private static final String loadGroupInvitationsForGroup = "select uuid,role from group_invitation where groupName=?";
		
		// Gets all unique names (not instances) of groups having this member at that role.
	private static final String getGroupNameFromRole = "SELECT DISTINCT faction_id.group_name FROM faction_member "
								+ "inner join faction_id on faction_member.group_id = faction_id.group_id "
								+ "WHERE member_name = ? "
								+ "AND role = ?;";
		
		// Gets the "most recent" updated group from all groups that share the name.
	private static final String getTimestamp = "SELECT MAX(faction.last_timestamp) FROM faction "
								+ "WHERE group_name = ?;";
		
		// updates "most recent" of all groups with a given name.
	private static final String updateLastTimestamp = "UPDATE faction SET faction.last_timestamp = NOW() "
								+ "WHERE group_name = ?;";
		
		// Breaking the pattern. Here we directly access a role based on _group ID_ rather then group_name. TODO: evaluate safety.
	private static final String getPlayerType = "SELECT role FROM faction_member "
						+ "WHERE group_id = ? "
                        + "AND member_name = ?;";
	private static final String logNameChange = "insert into nameLayerNameChanges (uuid,oldName,newName) values(?,?,?);";
	private static final String checkForNameChange = "select * from nameLayerNameChanges where uuid=?;";
		
	private static final String addPermission = "insert into permissionByGroup(group_id,role,perm_id) select g.group_id, ?, ? from faction_id g where g.group_name = ?;";
	private static final String addPermissionById = "insert into permissionByGroup(group_id,role,perm_id) values(?,?,?);";
	private static final String getPermission = "select pg.role,pg.perm_id from permissionByGroup pg inner join faction_id fi on fi.group_name=? "
				+ "where pg.group_id = fi.group_id";
	private static final String removePermission = "delete from permissionByGroup where group_id IN (SELECT group_id FROM faction_id WHERE group_name = ?) and role=? and perm_id=?;";
	private static final String registerPermission = "insert into permissionIdMapping(perm_id,name) values(?,?);"; 
	private static final String getPermissionMapping = "select * from permissionIdMapping;";
		
	private static final String addBlacklistMember = "insert into blacklist(group_id, member_name) select group_id,? from faction_id where group_name=?;";
	private static final String removeBlackListMember = "delete from blacklist WHERE group_id IN (SELECT group_id FROM faction_id WHERE group_name = ?) and member_name=?;";
	private static final String getBlackListMembers = "select b.member_name from blacklist b inner join faction_id fi on fi.group_name=? where b.group_id=fi.group_id;";
		
	private static final String getAllGroupIds = "select group_id from faction_id";


	public GroupManagerDao(Logger logger, ManagedDatasource db){
		this.logger = logger;
		this.db = db;
	}
	
	/**
	 * Not going to lie, I can't make heads or tails out of half of this.
	 */
	public void registerMigrations() {
		db.registerMigration(1, false, 
				"alter table faction drop `version`;",
				"alter table faction add type int default 0;",
				"create table faction_id("
					+ "group_id int not null AUTO_INCREMENT,"
					+ "group_name varchar(255),"
					+ "primary key(group_id));",
				"create table if not exists permissions(" +
					"group_id varchar(255) not null," +
					"role varchar(40) not null," +
					"tier varchar(255) not null," +
					"unique key (group_id, role));",
				"delete from faction where `name` is null;",
				"delete from faction_member where faction_name is null;",
				"delete from moderator where faction_name is null;",
				"insert into faction_id (group_name) select `name` from faction;",
				"alter table faction add group_name varchar(255) default null;",
				"update faction g set g.group_name = g.name;",
				"alter table faction drop `name`;",
				"alter table faction add primary key group_primary_key (group_name);",
				"drop table personal_group;",
				"alter table faction_member change member_name member_name varchar(36);",
				"alter table faction_member add role varchar(10) not null default 'MEMBERS';",
				"alter table faction_member add group_id int not null;",
				"delete fm.* from faction_member fm where not exists " // deletes any non faction_id entries.
					+ "(select fi.group_id from faction_id fi "
					+ "where fi.group_name = fm.faction_name limit 1);",
				"update faction_member fm set fm.group_id = (select fi.group_id from faction_id fi "
					+ "where fi.group_name = fm.faction_name limit 1);",
				"alter table faction_member add unique key uq_meber_faction(member_name, group_id);",
				"alter table faction_member drop index uq_faction_member_1;",
				"alter table faction_member drop faction_name;",
				"insert ignore into faction_member (group_id, member_name, role)" +
					"select g.group_id, m.member_name, 'MODS' from moderator m "
					+ "inner join faction_id g on g.group_name = m.faction_name",
				"insert into faction_member (group_id, member_name, role)"
					+ "select fi.group_id, f.founder, 'OWNER' from faction f "
					+ "inner join faction_id fi on fi.group_name = f.group_name;",
				"drop table moderator;",
				"alter table faction change `type` group_type varchar(40) not null default 'PRIVATE';",
				"update faction set group_type = 'PRIVATE';",
				"alter table faction change founder founder varchar(36);",
				"insert into permissions (group_id, role, tier) "
					+ "select f.group_id, 'OWNER', "
					+ "'DOORS CHESTS BLOCKS OWNER ADMINS MODS MEMBERS PASSWORD SUBGROUP PERMS DELETE MERGE LIST_PERMS TRANSFER CROPS' "
					+ "from faction_id f;",
				"insert into permissions (group_id, role, tier) "
					+ "select f.group_id, 'ADMINS', "
					+ "'DOORS CHESTS BLOCKS MODS MEMBERS PASSWORD LIST_PERMS CROPS' "
					+ "from faction_id f;",	
				"insert into permissions (group_id, role, tier) "
					+ "select f.group_id, 'MODS', "
					+ "'DOORS CHESTS BLOCKS MEMBERS CROPS' "
					+ "from faction_id f;",
				"insert into permissions (group_id, role, tier) "
					+ "select f.group_id, 'MEMBERS', "
					+ "'DOORS CHESTS' "
					+ "from faction_id f;");
				
		db.registerMigration(2, false, 
				new Callable<Boolean>() {
					@Override
					public Boolean call() {
						// Procedures may not be initialized yet.
						Bukkit.getScheduler().scheduleSyncDelayedTask(NameLayerPlugin.getInstance(), new Runnable(){
							@Override
							public void run() {
								Group g = getGroup(NameLayerPlugin.getSpecialAdminGroup());
								if (g == null) {
									createGroup(NameLayerPlugin.getSpecialAdminGroup(), null, null);
								} else {
									removeAllMembers(g.getName());
								}
							}
						});
						return true;
					}
				},
				"create table if not exists faction_id("
					+ "group_id int not null AUTO_INCREMENT,"
					+ "group_name varchar(255),"
					+ "primary key(group_id));",
			/* In the faction table we use group names. This is important because when merging other groups
			 * it will create multiple same group_names within the faction_id table. The benefits are that when other
			 * tables come looking for a group they always find the right one due to their only being one group with a name.
			 */
				"create table if not exists faction(" +
					"group_name varchar(255)," +
					"founder varchar(36)," +
					"password varchar(255) default null," +
					"discipline_flags int(11) not null," +
					"group_type varchar(40) not null default 'PRIVATE'," +
					"primary key(group_name));",
				"create table if not exists faction_member(" +
					"group_id int not null," +
					"member_name varchar(36)," +
					"role varchar(10) not null default 'MEMBERS'," +
					"unique key (group_id, member_name));",
				"create table if not exists blacklist(" +
					"member_name varchar(36) not null," +
					"group_id varchar(255) not null);",
				"create table if not exists permissions(" +
					"group_id varchar(255) not null," +
					"role varchar(40) not null," +
					"tier varchar(255) not null," +
					"unique key (group_id, role));",
				"create table if not exists subgroup(" +
					"group_id varchar(255) not null," +
					"sub_group_id varchar(255) not null," +
					"unique key (group_id, sub_group_id));");
			
		db.registerMigration(3, false, 
				"create table if not exists toggleAutoAccept("
					+ "uuid varchar(36) not null,"
					+ "primary key key_uuid(uuid));");

		db.registerMigration(4, false, 
				"alter table faction_id add index `faction_id_index` (group_name);");

		db.registerMigration(5, false, 
				"alter table faction_member add index `faction_member_index` (group_id);");

		db.registerMigration(6, false,
				"create table if not exists default_group(" + 
					"uuid varchar(36) NOT NULL," +
					"defaultgroup varchar(255) NOT NULL,"+
					"primary key key_uuid(uuid))");

		db.registerMigration(7, false,
				"create table if not exists group_invitation(" + 
					"uuid varchar(36) NOT NULL," +
					"groupName varchar(255) NOT NULL,"+
					"role varchar(10) NOT NULL default 'MEMBERS'," +
					"date datetime NOT NULL default NOW()," +
					"constraint UQ_uuid_groupName unique(uuid, groupName))");

		db.registerMigration(8, false,
				"alter table faction add last_timestamp datetime NOT NULL default NOW();");

		db.registerMigration(9, false,
				"alter table blacklist modify column group_id int;",
				"alter table permissions modify column group_id int;",
				"alter table subgroup modify column group_id int, modify column sub_group_id int;");

		db.registerMigration(10, false,
				"create table if not exists nameLayerNameChanges(uuid varchar(36) not null, oldName varchar(32) not null, newName varchar(32) not null, primary key(uuid));");

		db.registerMigration(11, false,
				new Callable<Boolean>() {
					@Override
					public Boolean call() {
						try (Connection connection = db.getConnection();
								PreparedStatement permInit = connection.prepareStatement(addPermissionById);
								PreparedStatement permReg = connection.prepareStatement(registerPermission); ) {
							Map <String, Integer> permIds = new HashMap<String, Integer>();

							LinkedList<Object[]> unspool = new LinkedList<Object[]>();
							int maximumId = 0;
							try (Statement getOldPerms = connection.createStatement();
									ResultSet res = getOldPerms.executeQuery("select * from permissions");) {
								while(res.next()) {
									unspool.add(new Object[]{res.getInt(1), res.getString(2), res.getString(3)});
									if (res.getInt(1) > maximumId) maximumId = res.getInt(1);
								}
							} catch (SQLException e) {
								logger.log(Level.SEVERE, "Failed to get old permissions, things might get a little wonky now.", e);
							}
							
							int maxBatch = 100, count = 0, regadd = 0;
							
							for (Object[] spool : unspool) {
								int groupId = (int) spool[0];
								String role = (String) spool[1];
								String [] perms = ((String) spool[2]).split(" ");
								for(String p : perms) {
									if (!p.equals("")) {
										if(p.equals("BLOCKS")) {
											//this permission was renamed and now includes less functionality than previously
											p = "REINFORCE";
										}
										Integer id = permIds.get(p);
										if (id == null) {
											//unknown perm, so we register it
											id = ++maximumId; // prefix mutator!
											
											permReg.setInt(1, maximumId);
											permReg.setString(2, p);
											permReg.addBatch(); // defer insert.
											
											permIds.put(p, id);
											regadd ++;
										}
										permInit.setInt(1, groupId);
										permInit.setString(2, role);
										permInit.setInt(3, id);
										permInit.addBatch();
										count ++;
										
										if (count > maxBatch) {
											permInit.executeBatch();
											// TODO process warnings / errors
											count = 0;
										}
									}
								}
							}
							if (count > 0) {
								permInit.executeBatch();
								// TODO process warnings / errors
							}
							
							if (regadd > 0) {
								permReg.executeBatch();
								// TODO process warnings / errors
							}

						} catch (SQLException se) {
							logger.log(Level.SEVERE, "Something fatal occured while updating permissions", se);
							return false;
						}
						return true;
					}
				},
				"create table if not exists permissionByGroup(group_id int not null,role varchar(40) not null,perm_id int not null, primary key(group_id,role,perm_id));",
				"create table if not exists permissionIdMapping(perm_id int not null, name varchar(64) not null,primary key(perm_id));",
				"alter table faction drop column group_type");


		db.registerMigration(12, false, 
				"UPDATE faction SET group_name=REPLACE(group_name,'|','');");
		
		db.registerMigration(13, false,
				"drop procedure if exists deletegroupfromtable;",
				"create definer=current_user procedure deletegroupfromtable(" +
					"in groupName varchar(36),"
					+ "in specialAdminGroup varchar(36)" +
					") sql security invoker begin " +
					"delete fm.* from faction_member fm "
					+ "inner join faction_id fi on fm.group_id = fi.group_id "
					+ "where fi.group_name = groupName;" +
					"delete b.* from blacklist b "
					+ "inner join faction_id fi on b.group_id = fi.group_id "
					+ "where fi.group_name = groupName;" +
					"delete s.* from subgroup s "
					+ "inner join faction_id fi on s.group_id = fi.group_id "
					+ "where fi.group_name = groupName;" +
					"delete p.* from permissions p "
					+ "inner join faction_id fi on p.group_id = fi.group_id "
					+ "where fi.group_name = groupName;" +
					"update faction f set f.group_name = specialAdminGroup "
					+ "where f.group_name = specialAdminGroup;" +
					"update faction_id set group_name = specialAdminGroup where group_name = groupName;" +
					"delete from faction where group_name = groupName;" +
					"end;",
				"drop procedure if exists mergeintogroup;",
			// needs to be set with inner joins
				"create definer=current_user procedure mergeintogroup(" +
					"in groupName varchar(255), in tomerge varchar(255)) " +
					"sql security invoker begin " +
					"DECLARE destID, tmp int;" +
					"SELECT f.group_id, count(DISTINCT fm.member_name) AS sz INTO destID, tmp FROM faction_id f "
					+ "INNER JOIN faction_member fm ON f.group_id = fm.group_id WHERE f.group_name = groupName GROUP BY f.group_id ORDER BY sz DESC LIMIT 1;" +
					"update ignore faction_member fm " // move all members from group From to To
					+ "inner join faction_id fii on fii.group_name = tomerge "
					+ "set fm.group_id = destID "
					+ "where fm.group_id = fii.group_id;" +
					/*"UPDATE faction_member fm " // update roles in dest on overlaps to be from merged group
					+ "INNER JOIN (SELECT faction_member fq JOIN faction_id fii ON fii.group_name = tomerge) fmerg "
					+ "ON fm.member_name = fmerge.member_name"
					+ "SET fm.role = fmerg.role WHERE fm.group_id = destID;" +*/
					"DELETE fm.* from faction_member fm " // Remove those "overlap" members left behind by IGNORE
					+ "inner join faction_id fi on fi.group_name = tomerge "
					+ "where fm.group_id = fi.group_id;" +
					/*"DELETE FROM subgroup s " // If this was a subgroup for someone, unlink. subgroups to new group.
					+ "WHERE sub_group_id IN " // TODO: might be double effort?
					+ "(SELECT group_id from faction_id where group_name = tomerge);" +*/ // handled using unlink
					"UPDATE subgroup s " // If it was a subgroup's supergroup, redirect
					+ "SET s.group_id = destID "
					+ "WHERE s.group_id IN "
					+ "(SELECT group_id from faction_id where group_name = tomerge);" +
					"delete from faction where group_name = tomerge;" + // Remove "faction" record of From
					"update faction_id fi " // Point "faction_id" records to TO's Name instead
					+ "inner join faction_id fii on fii.group_name = tomerge "
					+ "set fi.group_name = groupName "
					+ "where fi.group_id = fii.group_id;" +
					"end;",
				"drop procedure if exists createGroup;",
				"create definer=current_user procedure createGroup(" + 
					"in group_name varchar(255), " +
					"in founder varchar(36), " +
					"in password varchar(255), " +
					"in discipline_flags int(11)) " +
					"sql security invoker " +
					"begin" +
					" if (select (count(*) = 0) from faction_id q where q.group_name = group_name) is true then" + 
					"  insert into faction_id(group_name) values (group_name); " +
					"  insert into faction(group_name, founder, password, discipline_flags) values (group_name, founder, password, discipline_flags);" + 
					"  insert into faction_member (member_name, role, group_id) select founder, 'OWNER', f.group_id from faction_id f where f.group_name = group_name; " +
					"  select f.group_id from faction_id f where f.group_name = group_name; " +
					" end if; " +
					"end;");
		
		/*if (ver == 13){
			long first_time = System.currentTimeMillis();
			logger.log(Level.INFO, "Database updating to version fourteen, reworking player types");
			//leftover from previous upgrade
			cleanExecute("drop table permissions");
			//player type group mapping table
			cleanExecute("create table if not exists groupPlayerTypes(type_id int not null autoincrement, "
					+ "group_id int not null foreign key references faction_id (group_id) on delete cascade,"
					+ "rank_id int not null, type_name varchar(40) not null, parent_rank_id int, primary key(type_id), "
					+ "unique key(group_id,type_id), index groupPlayerTypes_tupel (group_id, rank_id));");
			
			//init new default player types for all groups
			cleanExecute("insert into groupPlayerTypes (group_id,rank_id,type_name) select group_id, 0, 'OWNER' from faction_id;");
			cleanExecute("insert into groupPlayerTypes (group_id,rank_id,type_name,parent_rank_id) select group_id, 1, 'ADMINS',0 from faction_id;");
			cleanExecute("insert into groupPlayerTypes (group_id,rank_id,type_name,parent_rank_id) select group_id, 2, 'MODS',1 from faction_id;");
			cleanExecute("insert into groupPlayerTypes (group_id,rank_id,type_name,parent_rank_id) select group_id, 3, 'MEMBERS',2 from faction_id;");
			cleanExecute("insert into groupPlayerTypes (group_id,rank_id,type_name,parent_rank_id) select group_id, 4, 'DEFAULT',0 from faction_id;");
			cleanExecute("insert into groupPlayerTypes (group_id,rank_id,type_name,parent_rank_id) select group_id, 5, 'BLACKLISTED',4 from faction_id;");
			
			
			//update permission table
			cleanExecute("alter table permissionByGroup drop primary key;");
			cleanExecute("alter table permissionByGroup add type_id int;");
			cleanExecute("alter table permissionByGroup add temp_rank_id int default null;");
			cleanExecute("update permissionByGroup set temp_rank_id=0 where role='OWNER';");
			cleanExecute("update permissionByGroup set temp_rank_id=1 where role='ADMINS';");
			cleanExecute("update permissionByGroup set temp_rank_id=2 where role='MODS';");
			cleanExecute("update permissionByGroup set temp_rank_id=3 where role='MEMBERS';");
			cleanExecute("update permissionByGroup set temp_rank_id=4 where role='NOT_BLACKLISTED';");
			//maybe some broken entries exist, we make sure to clean those out
			cleanExecute("delete from faction_member WHERE temp_rank_id IS NULL;");
			cleanExecute("alter table permissionByGroup drop column role;");
			cleanExecute("update pbg set pbg.type_id=gpt.type_id from permissionByGroup as pbg inner join groupPlayerTypes as gpt "
					+ "on gpt.group_id=pbg.group_id and pbg.temp_rank_id = gpt.rank_id");
			cleanExecute("alter table permissionByGroup drop column temp_rank_id;");
			cleanExecute("alter table permissionByGroup drop column group_id;");
			cleanExecute("delete from permissionByGroup where type_id is null;");
			cleanExecute("alter table permissionByGroup alter column type_id int not null foreign key references groupPlayerTypes(type_id) on delete cascade;");
			cleanExecute("alter table permissionByGroup add constraint foreign key (perm_id) references permissionIdMapping(perm_id) on delete cascade;");
			cleanExecute("alter table permissionByGroup add constraint uniquePermissions unique key(type_id, perm_id");
			cleanExecute("create index permissionTypeIdIndex on permissionByGroup(type_id);");
			
			
			//update group member table
			cleanExecute("alter table faction_member add foreign key(group_id) references faction_id(group_id) on delete cascade");
			cleanExecute("alter table faction_member add type_id int;");
			cleanExecute("alter table faction_member add temp_rank_id int;");
			cleanExecute("update faction_member set temp_rank_id=0 where role='OWNER';");
			cleanExecute("update faction_member set temp_rank_id=1 where role='ADMINS';");
			cleanExecute("update faction_member set temp_rank_id=2 where role='MODS';");
			cleanExecute("update faction_member set temp_rank_id=3 where role='MEMBERS';");
			cleanExecute("update faction_member set temp_rank_id=4 where role='NOT_BLACKLISTED';");
			//maybe some broken entries exist, we make sure to clean those out
			cleanExecute("delete from faction_member WHERE temp_rank_id IS NULL;");
			cleanExecute("alter table faction_member drop column role;");
			cleanExecute("update fm set fm.type_id=gpt.type_id from faction_member as fm inner join groupPlayerTypes as gpt "
					+ "on gpt.group_id=fm.group_id and fm.temp_rank_id = gpt.rank_id");
			cleanExecute("alter table faction_member drop column temp_rank_id;");
			cleanExecute("alter table faction_member drop column group_id;");
			cleanExecute("delete from faction_member where type_id is null;");
			cleanExecute("alter table faction_member alter column type_id int not null foreign key references groupPlayerTypes(type_id) on delete cascade;");
			cleanExecute("alter table faction_member add constraint uniqueMembers unique key(type_id, member_name");
			//index both
			cleanExecute("create index memberTypeIdIndex on faction_member(type_id);");
			cleanExecute("create index groupMemberIndex on faction_member(member_name);");

			//invitation table uses names as identifier, so we fix faction_id before getting to invitations
			cleanExecute("create table mergedGroups (oldGroup int not null, newGroup int not null references faction_id (group_id) on delete cascade, primary key(mergedGroups)");
			cleanExecute("insert into mergedGroups (oldGroup, newGroup) select fi.group_id,fa.group_id from faction_id fi left join "
					+ "(select max(group_id) as newId from faction_id group by group_name;) ma"
					+ "on ma.newId = fi.group_id inner join (select max(group_id) as max, group_name as newId from faction_id group by group_name;) fa on fa.group_name=fi.group_name;");
			cleanExecute("delete from faction_id where group_id in (select fi.group_id from faction_id fi left join "
					+ "(select max(group_id) as newId from faction_id group by group_name;) ma on ma.newId = fi.group_id)");
			
			
			//update invitation table
			cleanExecute("alter table group_invitation drop primary key;");
			cleanExecute("alter table group_invitation add group_id int;");
			cleanExecute("update gi set gi.group_id = fi.group_id from group_invitation as gi inner join faction_id as fi on fi.group_name=gi.group_name");
			cleanExecute("delete from group_invitation where group_id is null");
			cleanExecute("alter table group_invitation drop column group_name;");
			cleanExecute("alter table group_invitation add type_id int");
			cleanExecute("alter table group_invitation add temp_rank_id int;");
			cleanExecute("update group_invitation set temp_rank_id=0 where role='OWNER';");
			cleanExecute("update group_invitation set temp_rank_id=1 where role='ADMINS';");
			cleanExecute("update group_invitation set temp_rank_id=2 where role='MODS';");
			cleanExecute("update group_invitation set temp_rank_id=3 where role='MEMBERS';");
			cleanExecute("update group_invitation set temp_rank_id=4 where role='NOT_BLACKLISTED';");
			cleanExecute("delete from group_invitation WHERE temp_rank_id IS NULL;");
			cleanExecute("alter table group_invitation drop column role;");
			cleanExecute("update gi set gi.type_id=gpt.type_id from group_invitation as gi inner join groupPlayerTypes gpt "
					+ "on gpt.group_id=fm.group_id and fm.temp_rank_id = gpt.rank_id");
			cleanExecute("alter table group_invitation drop column temp_rank_id;");
			cleanExecute("alter table group_invitation drop column group_id;");
			cleanExecute("delete from group_invitation where type_id is null;");
			cleanExecute("alter table group_invitation alter column type_id int not null foreign key references groupPlayerTypes(type_id) on delete cascade;");
			cleanExecute("alter table group_invitation add constraint uniqueInvitations unique key(type_id, uuid");
			//index both
			cleanExecute("create index inviteTypeIdIndex on group_invitation(type_id);");
			cleanExecute("create index inviteUUIDIndex on group_invitation(uuid);");
			
			
			
			ver = updateVersion(ver, plugin.getName());
			logger.log(Level.INFO, "Database update to Version fourteen took {0} seconds", (System.currentTimeMillis() - first_time) / 1000);
		} */
	}
	
	public int createGroup(String group, UUID owner, String password){
		int ret = -1;
		try (Connection connection = db.getConnection();
				PreparedStatement createGroup = connection.prepareStatement(GroupManagerDao.createGroup)){
			String own = null;
			if (owner != null) own = owner.toString();
			createGroup.setString(1, group);
			createGroup.setString(2, own);
			createGroup.setString(3, password);
			createGroup.setInt(4, 0);
			try (ResultSet set = createGroup.executeQuery();)  {
				ret = set.next() ? set.getInt("f.group_id") : -1;
				logger.log(Level.INFO, "Created group {0} w/ id {1} for {2}", new Object[] {group, ret, own});
			} catch (SQLException e) {
				logger.log(Level.WARNING, "Problem creating group " + group, e);
				ret = -1;
			}
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem setting up query to create group " + group, e);
			ret = -1;
		}
		
		return ret;
	}
	
	public Group getGroup(String groupName){
		try (Connection connection = db.getConnection();
				PreparedStatement getGroup = connection.prepareStatement(GroupManagerDao.getGroup)){
			
			getGroup.setString(1, groupName);
			try (ResultSet set = getGroup.executeQuery()){
				if (!set.next()) {
					return null;
				}
				
				String name = set.getString(1);
				String uuid = set.getString(2);
				UUID owner = (uuid != null) ? UUID.fromString(uuid) : null;
				boolean discipline = set.getInt(4) != 0;
				String password = set.getString(3);
				int id = set.getInt(5);
				
				Group g = new Group(name, owner, discipline, password, id);
				
				// other group IDs cached via the constructor.
				return g;
			} catch (SQLException e) {
				logger.log(Level.WARNING, "Problem getting group " + groupName, e);
			}
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem preparing query to get group " + groupName, e);
		}
		return null;
	}
	
	public Group getGroup(int groupId){
		try (Connection connection = db.getConnection();
				PreparedStatement getGroupById = connection.prepareStatement(GroupManagerDao.getGroupById)){
			getGroupById.setInt(1, groupId);
			try (ResultSet set = getGroupById.executeQuery();) {
				if (!set.next()) return null;

				String name = set.getString(1);
				String uuid = set.getString(2);
				UUID owner = null;
				if (uuid != null)
					owner = UUID.fromString(uuid);
				boolean dis = set.getInt(4) != 0;
				String password = set.getString(3);
				int id = set.getInt(5);
				
				Group g = new Group(name, owner, dis, password, id);
				return g;
			} catch (SQLException e) {
				logger.log(Level.WARNING, "Problem getting group " + groupId, e);
			}
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem preparing query to get group " + groupId, e);
		}
		return null;
	}
	
	public List<String> getGroupNames(UUID uuid){
		List<String> groups = new ArrayList<String>();
		try (Connection connection = db.getConnection();
				PreparedStatement getAllGroupsNames = connection.prepareStatement(GroupManagerDao.getAllGroupsNames)){
			getAllGroupsNames.setString(1, uuid.toString());
			try (ResultSet set = getAllGroupsNames.executeQuery();) {
				while(set.next()) {
					groups.add(set.getString(1));
				}
			} catch (SQLException e) {
				logger.log(Level.WARNING, "Problem getting player's groups " + uuid, e);
			}
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem preparing to get player's groups " + uuid, e);
		}
		return groups;
	}
	
	public List<String> getGroupNames(UUID uuid, String role){
		List<String> groups = new ArrayList<String>();
		try (Connection connection = db.getConnection();
				PreparedStatement getGroupNameFromRole = connection.prepareStatement(GroupManagerDao.getGroupNameFromRole)){
			getGroupNameFromRole.setString(1, uuid.toString());
			getGroupNameFromRole.setString(2, role);
			try (ResultSet set = getGroupNameFromRole.executeQuery();) {
				while(set.next()) {
					groups.add(set.getString(1));
				}
			} catch (SQLException e) {
				logger.log(Level.WARNING, "Problem getting player " + uuid + " groups by role " + role, e);
			}
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem preparing to get player " + uuid + " groups by role " + role, e);
		}
		return groups;
	}
	
	public Timestamp getTimestamp(String group){
		Timestamp timestamp = null;
		try (Connection connection = db.getConnection();
				PreparedStatement getTimestamp = connection.prepareStatement(GroupManagerDao.getTimestamp)){
			getTimestamp.setString(1, group);
			try (ResultSet set = getTimestamp.executeQuery();) {
				if(set.next()) {
					timestamp = set.getTimestamp(1);
				}
			} catch (SQLException e) {
				logger.log(Level.WARNING, "Problem getting group timestamp " + group, e);
			} 
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem preparing to get group timestamp " + group, e);
		}
		
		return timestamp;
	}
	
	public void updateTimestampAsync(final String group){
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable(){

			@Override
			public void run() {
				updateTimestamp(group);
			}
			
		});
	}
	
	public void updateTimestamp(String group){
		try (Connection connection = db.getConnection();
				PreparedStatement updateLastTimestamp = connection.prepareStatement(GroupManagerDao.updateLastTimestamp)){
			updateLastTimestamp.setString(1, group);
			updateLastTimestamp.executeUpdate();
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem updating timestamp for group " + group, e);
		}
	}
	
	public void deleteGroupAsync(final String groupName){
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable(){

			@Override
			public void run() {
				deleteGroup(groupName);
			}
			
		});
	}
	
	public void deleteGroup(String groupName){
		try (Connection connection = db.getConnection();
				PreparedStatement deleteGroup = connection.prepareStatement(GroupManagerDao.deleteGroup)){
			deleteGroup.setString(1, groupName);
			deleteGroup.setString(2, NameLayerPlugin.getSpecialAdminGroup());
			deleteGroup.executeUpdate();
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem deleting group " + groupName, e);
		}
	}
	
	public void addMemberAsync(final UUID member, final Group group, final PlayerType role){
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable(){

			@Override
			public void run() {
				addMember(member,group,role);
			}
			
		});
	}
	
	public void addMember(UUID member, Group group, PlayerType role){
		//TODO Redo addMember
		try (Connection connection = db.getConnection();
				PreparedStatement addMember = connection.prepareStatement(GroupManagerDao.addMember)){
			//addMember.setString(1, member.toString());
			//addMember.setString(2, role.name());
			//addMember.setString(3, faction);
			//addMember.executeUpdate();
		
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem adding " + member + " as " + role.toString() 
					+ " to group " + group.getName(), e);
		}			
	}
	
	/*
		TODO: Refactor based on Group as param and appropriate integration with new type system.
	public List<UUID> getAllMembers(String groupName, PlayerType role){
		List<UUID> members = new ArrayList<UUID>();
		try (Connection connection = db.getConnection();
				PreparedStatement getMembers = connection.prepareStatement(GroupManagerDao.getMembers)){
			getMembers.setString(1, groupName);
			getMembers.setString(2, role.name());
			try (ResultSet set = getMembers.executeQuery();) {
				while(set.next()){
					String uuid = set.getString(1);
					if (uuid == null) {
						continue;
					}
					members.add(UUID.fromString(uuid));
				}
			} catch (SQLException e) {
				logger.log(Level.WARNING, "Problem getting all " + role.toString() + " for group " + groupName, e);
			}
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem preparing to get all " + role.toString() + " for group " + groupName, e);
		}
		return members;
	}*/
	
	public void removeMemberAsync(final UUID member, final Group group){
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable(){

			@Override
			public void run() {
				removeMember(member,group);
			}
			
		});
	}
	
	public void removeMember(UUID member, Group group){
		//TODO Redo remove member
		try (Connection connection = db.getConnection();
				PreparedStatement removeMember = connection.prepareStatement(GroupManagerDao.removeMember)){
			removeMember.setString(1, member.toString());
			//removeMember.setString(2, group);
			removeMember.executeUpdate();
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem removing " + member + " from group " + group, e);
		}
	}

	public void removeAllMembersAsync(final Group group){
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable(){

			@Override
			public void run() {
				removeAllMembers(group);
			}
			
		});
	}
	
	public void removeAllMembers(Group group){
		//TODO review this
		try (Connection connection = db.getConnection();
				PreparedStatement removeAllMembers = connection.prepareStatement(GroupManagerDao.removeAllMembers)){
			removeAllMembers.setString(1, group.getName());
			removeAllMembers.executeUpdate();
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem removing all members from group " + group, e);
		}
	}
	
	public void addAllPermissions(int groupId, Map <PlayerType, List <PermissionType>> perms) {
		//TODO Redo this
		try (Connection connection = db.getConnection();
				PreparedStatement addPermissionById = connection.prepareStatement(GroupManagerDao.addPermissionById)){
			for (Entry <PlayerType, List <PermissionType>> entry: perms.entrySet()){
				String role = entry.getKey().getName();
				for(PermissionType perm : entry.getValue()) {
					addPermissionById.setInt(1,  groupId);
					addPermissionById.setString(2, role);
					addPermissionById.setInt(3, perm.getId());
					addPermissionById.addBatch();
				}
			}
			
			int[] res = addPermissionById.executeBatch();
			if (res == null) {
				logger.log(Level.WARNING, "Failed to add all permissions to group {0}", groupId);
			} else {
				int cnt = 0;
				for (int r : res) cnt += r;
				logger.log(Level.INFO, "Added {0} of {1} permissions to group {2}",
						new Object[] {cnt, res.length, groupId});
			}
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem adding all permissions to group " + groupId, e);
		}
	}
	
	public void addPermissionAsync(final Group g, final PlayerType role, final PermissionType perms){
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable(){

			@Override
			public void run() {
				addPermission(g ,role,perms);
			}
			
		});
	}
	
	public void removeAllPermissions(Group g, Map <PlayerType, List <PermissionType>> perms) {
		//TODO remove all of those, definitely batch
	}

	public void addPermission(Group group, PlayerType type, PermissionType perm) {
		//TODO Redo this
		try (Connection connection = db.getConnection();
				PreparedStatement addPermission = connection.prepareStatement(GroupManagerDao.addPermission)){
			for(PermissionType perm : perms) {
				addPermission.setString(1, perm.getName());
				addPermission.setInt(2, perm.getId());
				addPermission.setString(3, group.getName());
				addPermission.addBatch();
			int[] res = addPermission.executeBatch();
			if (res == null) {
				logger.log(Level.WARNING, "Failed to add all permissions to group {0}, role {1}",
						new Object[] {group.getName(), perm} );
			} else {
				int cnt = 0;
				for (int r : res) cnt += r;
				logger.log(Level.INFO, "Added {0} of {1} permissions to group {2}, role {3}",
						new Object[] {cnt, res.length, group.getName(), type});
			}
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem adding " + type + " with " + perm
					+ " to group " + group.getName(), e);
		}
	}
	
	public Map<PlayerType, List<PermissionType>> getPermissions(Group group){
		//TODO Redo this, assume playertypehandler is initialized here
		Map<PlayerType, List<PermissionType>> perms = new HashMap<PlayerType, List<PermissionType>>();
		/*try (Connection connection = db.getConnection();
				PreparedStatement getPermission = connection.prepareStatement(GroupManagerDao.getPermission)){
			getPermission.setString(1, group);
			try (ResultSet set = getPermission.executeQuery();) {
				while(set.next()){
					PlayerType type = PlayerType.getPlayerType(set.getString(1));
					List<PermissionType> listPerm = perms.get(type);
					if (listPerm == null) {
						listPerm = new ArrayList<PermissionType>();
						perms.put(type, listPerm);
					}
					int id = set.getInt(2);
					PermissionType perm = PermissionType.getPermission(id);
					if (perm != null && !listPerm.contains(perm)) {
						listPerm.add(perm);
					}
				}
			} catch (SQLException e) {
				logger.log(Level.WARNING, "Problem getting permissions for group " + group, e);
			}
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem preparing statement to get permissions for group " + group, e);
		} */
		return perms;
	}
	
	public void removePermissionAsync(final Group group, final PlayerType ptype, final PermissionType perm){
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable(){

			@Override
			public void run() {
				removePermission(group,ptype,perm);
			}
			
		});
	}
	
	public void removePermission(Group group, PlayerType pType, PermissionType perm){
		//TODO Redo this
		try (Connection connection = db.getConnection();
				PreparedStatement removePermission = connection.prepareStatement(GroupManagerDao.removePermission)){
			removePermission.setString(1, group.getName());
			removePermission.setString(2, pType.getName());
			removePermission.setInt(3, perm.getId());
			removePermission.executeUpdate();
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem removing permissions for group " + group
					+ " on playertype " + pType.getName(), e);
		}
	}
	
	public void registerPermissionAsync(final PermissionType perm){
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable(){

			@Override
			public void run() {
				registerPermission(perm);
			}
			
		});
	}
	
	public void registerPermission(PermissionType perm) {
		try (Connection connection = db.getConnection();
				PreparedStatement registerPermission = connection.prepareStatement(GroupManagerDao.registerPermission)){
			registerPermission.setInt(1, perm.getId());
			registerPermission.setString(2, perm.getName());
			registerPermission.executeUpdate();
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem register permission " + perm.getName(), e);
		}
	}
	
	public Map<Integer, String> getPermissionMapping() {
		Map <Integer,String> perms = new TreeMap<Integer, String>();
		try (Connection connection = db.getConnection();
				Statement getPermissionMapping = connection.createStatement()) {
			try (ResultSet res = getPermissionMapping.executeQuery(GroupManagerDao.getPermissionMapping)) {
				while (res.next()) {
					perms.put(res.getInt(1), res.getString(2));
				}
			} catch (SQLException e) {
				logger.log(Level.WARNING, "Problem getting permissions from db", e);
			}
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem forming statement to get permissions from db", e);
		}
		return perms;
	}
	
	public void addNewDefaultPermissionAsync(final List <PlayerType> ptypes, final PermissionType perm){
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable(){

			@Override
			public void run() {
				addNewDefaultPermission(ptypes,perm);
			}
			
		});
	}
	
	public void registerPlayerType(Group g, PlayerType type) {
		//insert new player type together with its parent, id, group id and name
	}
	
	public void removePlayerType(Group g, PlayerType type) {
		//just completly remove from the db
		//this should also remove all permissions associated with this type
	}
	
	public void updatePlayerTypeName(Group g, PlayerType type) {
		//this will be called after the name of the type is already updated
		//simply write new name to db, based on group and type id
	}
	
	public PlayerTypeHandler getPlayerTypes(Group g) {
		//constructs a new player type handler based on information retrieved from db
		//this includes loading all permissions
		//possibly loading all groups with all perms on startup might be better, needs to be investigated
		return null;
	}
	
	public void batchSavePlayerTypeHandler(PlayerTypeHandler handler) {
		//TODO
		//called after initially creating a group to save all player types created and all of their permissions	
		
	}
	
	public void addNewDefaultPermission(List <PlayerType> playerTypes, PermissionType perm) {
		//TODO Maybe redo this, not sure if needed
		try (Connection connection = db.getConnection();) {
			List <Integer> groups = new LinkedList<Integer>();
			try (Statement getAllGroupIds = connection.createStatement();
					ResultSet set = getAllGroupIds.executeQuery(GroupManagerDao.getAllGroupIds);) {
				// unpack ids;
				while(set.next()) {
					groups.add(set.getInt(1));
				}
				// unpack and close, don't keep this query open!
			} catch (SQLException e) {
				logger.log(Level.WARNING, "Error retrieving all group Ids to initiate default perms for permission " + perm + 
						" for player types " + playerTypes, e);
			}
			
			int batchsize = 0, maxbatch = 100;
			try (PreparedStatement addPermissionById = connection.prepareStatement(GroupManagerDao.addPermissionById);) {
				for (int groupId : groups) {
					for(PlayerType pType: playerTypes) {
						addPermissionById.setInt(1, groupId);
						addPermissionById.setString(2, pType.getName());
						addPermissionById.setInt(3, perm.getId());
						addPermissionById.addBatch();
						batchsize ++;
					}
					// inline batch commit at cutoff level (100 default).
					if (batchsize >= maxbatch) {
						int[] res = addPermissionById.executeBatch();
						if (res == null) {
							logger.log(Level.WARNING, "Problem inserting new default permission into all groups");
						} else {
							int rc = 0;
							for (int r : res) rc+= r;
							if (rc != res.length) {
								logger.log(Level.WARNING, "Problem inserting new default permission into all groups, count mismatch");
							}
						}
						batchsize = 0; // reset.
					}
				}

				// final cleanup.
				if (batchsize > 0) {
					int[] res = addPermissionById.executeBatch();
					if (res == null) {
						logger.log(Level.WARNING, "Problem inserting new default permission into all groups");
					} else {
						int rc = 0;
						for (int r : res) rc+= r;
						if (rc != res.length) {
							logger.log(Level.WARNING, "Problem inserting new default permission into all groups, count mismatch");
						}
					}
					batchsize = 0; // reset.
				}
			} catch (SQLException e) {
				logger.log(Level.WARNING, "Error initiating default perms for permission " + perm + " for player types " + playerTypes, e);
			}
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Error initiating connection to set default perms for permission " + perm + " for player types " + playerTypes, e);
		}
	}
	
	public int countGroups(){
		int ret = 0;
		try (Connection connection = db.getConnection();
				Statement countGroups = connection.createStatement();
				ResultSet set = countGroups.executeQuery(GroupManagerDao.countGroups);){
			ret = set.next() ? set.getInt("count") : 0;
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem counting groups", e);
		}
		return ret;
	}
	
	public int countGroups(UUID uuid){
		int ret = 0;
		try (Connection connection = db.getConnection();
				PreparedStatement countGroupsFromUUID = connection.prepareStatement(GroupManagerDao.countGroupsFromUUID);){
			countGroupsFromUUID.setString(1, uuid.toString());
			try (ResultSet set = countGroupsFromUUID.executeQuery();) {
				ret = set.next() ? set.getInt("count") : 0;
			} catch (SQLException e) {
				logger.log(Level.WARNING, "Problem counting groups for " + uuid, e);
			} 
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem setting up statement to count groups for " + uuid, e);
		}
		return ret;
		
	}
	
	public void mergeGroupAsync(final String groupname, final String tomerge){
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable(){

			@Override
			public void run() {
				mergeGroup(groupname,tomerge);
			}
			
		});
	}
	
	public void mergeGroup(String groupName, String toMerge){
		try (Connection connection = db.getConnection();
				PreparedStatement mergeGroup = connection.prepareStatement(GroupManagerDao.mergeGroup);){
			mergeGroup.setString(1, groupName);
			mergeGroup.setString(2, toMerge);
			mergeGroup.execute();
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem merging group " + toMerge + " into " + groupName, e);
		}
	}
	
	public void updatePasswordAsync(final String groupname, final String password){
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable(){

			@Override
			public void run() {
				updatePassword(groupname,password);
			}
			
		});
	}
	
	public void updatePassword(String groupName, String password){
		try (Connection connection = db.getConnection();
				PreparedStatement updatePassword = connection.prepareStatement(GroupManagerDao.updatePassword);){
			updatePassword.setString(1, password);
			updatePassword.setString(2, groupName);
			updatePassword.executeUpdate();
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem updating password for group " + groupName, e);
		}
	}
	
	/**
	 * Loads the uuid of all players who have autoaccept for group invites turned on
	 * 
	 * @return All Players who have auto accept turned on
	 */
	public Set <UUID> loadAllAutoAccept() {
		Set <UUID> accepts = new HashSet<UUID>();
		try (Connection connection = db.getConnection();
				PreparedStatement addAutoAcceptGroup = connection.prepareStatement(GroupManagerDao.loadAllAutoAcceptGroup);
				ResultSet rs = addAutoAcceptGroup.executeQuery();){
			while (rs.next()) {
				accepts.add(UUID.fromString(rs.getString(1)));
			}
			
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem loading all autoaccepts", e);
		}
		return accepts;
	}
	
	/**
	 * Adds the uuid to the db if they should auto accept groups when invited.
	 * @param uuid
	 */
	public void autoAcceptGroupsAsync(final UUID uuid){
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable(){

			@Override
			public void run() {
				autoAcceptGroups(uuid);
			}
		});
	}
	
	public void autoAcceptGroups(final UUID uuid){
		try (Connection connection = db.getConnection();
				PreparedStatement addAutoAcceptGroup = connection.prepareStatement(GroupManagerDao.addAutoAcceptGroup);){
			addAutoAcceptGroup.setString(1, uuid.toString());
			addAutoAcceptGroup.executeUpdate();
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem setting autoaccept for " + uuid, e);
		}
	}
	
	/**
	 * @param uuid- The UUID of the player.
	 * @return Returns true if they should auto accept.
	 */
	@Deprecated
	public boolean shouldAutoAcceptGroups(UUID uuid){
		try (Connection connection = db.getConnection();
				PreparedStatement getAutoAcceptGroup = connection.prepareStatement(GroupManagerDao.getAutoAcceptGroup);){
			getAutoAcceptGroup.setString(1, uuid.toString());
			try (ResultSet set = getAutoAcceptGroup.executeQuery();) {
				return set.next();
			} catch (SQLException e) {
				logger.log(Level.WARNING, "Problem getting autoaccept for " + uuid, e);
			}
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem setting up query to get autoaccept for " + uuid, e);
		}
		return false;
	}
	
	public void removeAutoAcceptGroupAsync(final UUID uuid){
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable(){

			@Override
			public void run() {
				removeAutoAcceptGroup(uuid);
			}
		});
	}
	
	public void removeAutoAcceptGroup(final UUID uuid){
		try (Connection connection = db.getConnection();
				PreparedStatement removeAutoAcceptGroup = connection.prepareStatement(GroupManagerDao.removeAutoAcceptGroup);){
			removeAutoAcceptGroup.setString(1, uuid.toString());
			removeAutoAcceptGroup.executeUpdate();
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem removing autoaccept for " + uuid, e);
		}
	}
	
	public void setDefaultGroupAsync(final UUID uuid, final String groupname){
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable(){

			@Override
			public void run() {
				setDefaultGroup(uuid,groupname);
			}
			
		});
	}
	
	public void setDefaultGroup(UUID uuid, String groupName){
		try (Connection connection = db.getConnection();
				PreparedStatement setDefaultGroup = connection.prepareStatement(GroupManagerDao.setDefaultGroup);){
			setDefaultGroup.setString(1, uuid.toString());
			setDefaultGroup.setString(2, groupName );
			setDefaultGroup.executeUpdate();
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem setting user " + uuid + " default group to " + groupName, e);
		}
	}
	
	public void changeDefaultGroupAsync(final UUID uuid, final String groupname){
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable(){

			@Override
			public void run() {
				changeDefaultGroup(uuid,groupname);
			}
			
		});
	}
	
	public void changeDefaultGroup(UUID uuid, String groupName){
		try (Connection connection = db.getConnection();
				PreparedStatement changeDefaultGroup = connection.prepareStatement(GroupManagerDao.changeDefaultGroup);){
			changeDefaultGroup.setString(1, groupName);
			changeDefaultGroup.setString(2, uuid.toString());
			changeDefaultGroup.executeUpdate();
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem changing user " + uuid + " default group to " + groupName, e);
		}
	}

	public String getDefaultGroup(UUID uuid) {
		String group = null;
		try (Connection connection = db.getConnection();
				PreparedStatement getDefaultGroup = connection.prepareStatement(GroupManagerDao.getDefaultGroup);){
			getDefaultGroup.setString(1, uuid.toString());
			try (ResultSet set = getDefaultGroup.executeQuery();) {
				group = set.getString(1);
			} catch (SQLException e) {
				logger.log(Level.WARNING, "Problem getting default group for " + uuid, e);
			}
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem setting up query to get default group for " + uuid, e);
		}
		return group;
	}
	
	public Map <UUID, String> getAllDefaultGroups() {
		Map <UUID, String> groups = null;
		try (Connection connection = db.getConnection();
				Statement getAllDefaultGroups = connection.createStatement();
				ResultSet set = getAllDefaultGroups.executeQuery(GroupManagerDao.getAllDefaultGroups);){
			groups = new TreeMap<UUID, String>();
			while(set.next()) {
				UUID uuid = UUID.fromString(set.getString(1));
				String group = set.getString(2);
				groups.put(uuid, group);
			}
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem getting all default groups " , e);
		}
		return groups;
	}
	
	/**
	 * Use this method to override the current founder of a group.
	 * @param uuid This is the uuid of the player.
	 * @param group This is the group that we are changing the founder of.
	 */
	public void setFounderAsync(final UUID uuid, final Group group){
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable(){

			@Override
			public void run() {
				setFounder(uuid,group);
			}
			
		});
	}
	
	public void setFounder(UUID uuid, Group group) {
		try (Connection connection = db.getConnection();
				PreparedStatement updateOwner = connection.prepareStatement(GroupManagerDao.updateOwner);){
			updateOwner.setString(1, uuid.toString());
			updateOwner.setString(2, group.getName());
			updateOwner.executeUpdate();
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem setting founder of group " + group.getName() + " to " + uuid, e);
		}
	}
	
	public void setDisciplinedAsync(final Group group, final boolean disciplined){
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable(){

			@Override
			public void run() {
				setDisciplined(group,disciplined);
			}
			
		});
	}
	
	public void setDisciplined(Group group, boolean disciplined) {
		try (Connection connection = db.getConnection();
				PreparedStatement updateDisciplined = connection.prepareStatement(GroupManagerDao.updateDisciplined);){
			updateDisciplined.setInt(1, disciplined ? 1 : 0);
			updateDisciplined.setString(2, group.getName());
			updateDisciplined.executeUpdate();
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem setting disciplined of group " + group.getName() 
					+ " to " + disciplined, e);
		}
	}

	
	public void addGroupInvitationAsync(final UUID uuid, final Group group, final PlayerType role){
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable(){

			@Override
			public void run() {
				addGroupInvitation(uuid,group,role);
			}
			
		});
	}
	
	public void addGroupInvitation(UUID uuid, Group group, PlayerType role){
		//TODO possibly redo this
		try (Connection connection = db.getConnection();
				PreparedStatement addGroupInvitation = connection.prepareStatement(GroupManagerDao.addGroupInvitation);){
			addGroupInvitation.setString(1, uuid.toString());
			addGroupInvitation.setString(2, group.getName());
			addGroupInvitation.setString(3, role.getName());
			addGroupInvitation.executeUpdate();
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem adding group " + group.getName() + " invite for "
					+ uuid + " with role " + role, e);
		}
	}
	
	public void removeGroupInvitationAsync(final UUID uuid, final Group group){
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable(){

			@Override
			public void run() {
				removeGroupInvitation(uuid,group);
			}
			
		});
	}
	
	public void removeGroupInvitation(UUID uuid, Group group){
		try (Connection connection = db.getConnection();
				PreparedStatement removeGroupInvitation = connection.prepareStatement(GroupManagerDao.removeGroupInvitation);){
			removeGroupInvitation.setString(1, uuid.toString());
			removeGroupInvitation.setString(2, group.getName());
			removeGroupInvitation.executeUpdate();
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem removing group " + group.getName() + " invite for "
					+ uuid, e);
		}
	}
	
	
	/**
	 * Use this method to load a specific invitation to a group without the notification. 
	 * @param playerUUID The uuid of the invited player.
	 * @param group The group the player was invited to. 
	 */
	public void loadGroupInvitationAsync(final UUID playerUUID, final Group group){
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable(){

			@Override
			public void run() {
				loadGroupInvitation(playerUUID, group);
			}
			
		});
	}
	
	public void loadGroupInvitation(UUID playerUUID, Group group){
		if(group == null) return;
		
		try (Connection connection = db.getConnection();
				PreparedStatement loadGroupInvitation = connection.prepareStatement(GroupManagerDao.loadGroupInvitation);){
			loadGroupInvitation.setString(1, playerUUID.toString());
			loadGroupInvitation.setString(2, group.getName());
			try (ResultSet set = loadGroupInvitation.executeQuery();) {
				while(set.next()){
					String role = set.getString("role");
					PlayerType type = null;
					if(role != null){
						type = group.getPlayerTypeHandler().getType(role);
					}
					group.addInvite(playerUUID, type, false);
				}
			} catch(SQLException e) {
				logger.log(Level.WARNING, "Problem loading group " + group.getName() + " invites for " + playerUUID, e);
			}
		} catch(SQLException e) {
			logger.log(Level.WARNING, "Problem preparing query to load group " + group.getName() + 
				" invites for " + playerUUID, e);
		}
	}
	
	public Map<UUID, PlayerType> getInvitesForGroup(Group group) {
		Map <UUID, PlayerType> invs = new TreeMap<UUID, PlayerType>();
		if (group == null) {
			return invs;
		}
		try (Connection connection = db.getConnection();
				PreparedStatement loadGroupInvitationsForGroup = connection.prepareStatement(GroupManagerDao.loadGroupInvitationsForGroup);){
			loadGroupInvitationsForGroup.setString(1, group.getName());
			try (ResultSet set = loadGroupInvitationsForGroup.executeQuery();) {
				while(set.next()) {
					String uuid = set.getString(1);
					String role = set.getString(2);
					UUID playerUUID = null;
					if (uuid != null){
						playerUUID = UUID.fromString(uuid);
					}
					PlayerType pType = null;
					if(role != null){
						pType = group.getPlayerTypeHandler().getType(role);
					}
					if (uuid != null && pType != null) {
						invs.put(playerUUID, pType);
					}
				}
			} catch (SQLException e) {
				logger.log(Level.WARNING, "Problem loading group invitations for group " + group.getName(), e);
			}
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem preparing statement to load group invitations for group " + group.getName(), e);
		}
		return invs;
	}
	
	/**
	 * Use this method to load all invitations to all groups.
	 */
	public void loadGroupsInvitations(){
		try (Connection connection = db.getConnection();
				PreparedStatement loadGroupsInvitations = connection.prepareStatement(GroupManagerDao.loadGroupsInvitations);
				ResultSet set = loadGroupsInvitations.executeQuery();) {
			while(set.next()){
				String uuid = set.getString("uuid");
				String group = set.getString("groupName");
				String role = set.getString("role");
				UUID playerUUID = null;
				if (uuid != null){
					playerUUID = UUID.fromString(uuid);
				}
				Group g = null;
				if(group != null){
					g = GroupManager.getGroup(group);
				}
				PlayerType type = null;
				if(role != null){
					type = g.getPlayerTypeHandler().getType(role);
				}
				
				if(g != null){
					g.addInvite(playerUUID, type, false);
					PlayerListener.addNotification(playerUUID, g);
				}
			}
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem loading all group invitations", e);
		}
	}
	
	public void logNameChangeAsync(final UUID uuid, final String oldName, final String newName){
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable(){

			@Override
			public void run() {
				logNameChange(uuid,oldName,newName);
			}
			
		});
	}
	
	public void logNameChange(UUID uuid, String oldName, String newName) {
		try (Connection connection = db.getConnection();
				PreparedStatement logNameChange = connection.prepareStatement(GroupManagerDao.logNameChange);){
			logNameChange.setString(1, uuid.toString());
			logNameChange.setString(2, oldName);
			logNameChange.setString(3, newName);
			logNameChange.executeUpdate();
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Failed to log a name change for {0} from {1} -> {2}", new Object[]{uuid, oldName, newName});
			logger.log(Level.WARNING, "Exception during change.", e);
		}
	}
	
	public boolean hasChangedNameBefore(UUID uuid) {
		boolean ret = false;
		try (Connection connection = db.getConnection();
				PreparedStatement checkForNameChange = connection.prepareStatement(GroupManagerDao.checkForNameChange);){
			checkForNameChange.setString(1, uuid.toString());
			try (ResultSet set = checkForNameChange.executeQuery();) { 
				ret = set.next();
			} catch (SQLException e) {
				logger.log(Level.WARNING, "Failed to check if " + uuid + " has previously changed names", e);
			} 
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Failed to check if {0} has previously changed names", uuid);
			logger.log(Level.WARNING, "Exception during check.", e);
		}
		return ret;
	}

	/**
	 * Gets all the IDs for this group name, sorted by "size" in membercount.
	 * Ideally only one groupname/id has members and the rest are shadows, but in any case
	 * we arbitrarily define primacy as the one with the most members for ease of accounting
	 * and backwards compatibility.
	 *  
	 * @param groupName
	 * @return
	 */
	public List<Integer> getAllIDs(String groupName) {
		if (groupName == null) {
			return null;
		}
		try (Connection connection = db.getConnection();
				PreparedStatement getGroupIDs = connection.prepareStatement(GroupManagerDao.getGroupIDs);){
			getGroupIDs.setString(1, groupName);
			try (ResultSet set = getGroupIDs.executeQuery();) {
				LinkedList<Integer> ids = new LinkedList<Integer>();
			
				while (set.next()) {
					ids.add(set.getInt(1));
				}
				
				return ids;
			} catch (SQLException se) {
				logger.log(Level.WARNING, "Unable to fully load group ID set", se);
			}
		} catch (SQLException se) {
			logger.log(Level.WARNING, "Unable to prepare query to fully load group ID set", se);
		}
		return null;
	}
	
	
}
