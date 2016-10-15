package vg.civcraft.mc.namelayer.database;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;

import vg.civcraft.mc.civmodcore.dao.ManagedDatasource;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameLayerPlugin;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.listeners.PlayerListener;
import vg.civcraft.mc.namelayer.permission.PermissionType;
import vg.civcraft.mc.namelayer.permission.PlayerType;
import vg.civcraft.mc.namelayer.permission.PlayerTypeHandler;

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
	private static final String getAllGroupsForPlayer = "select f.group_name from faction_id f "
				+ "inner join faction_member fm on f.group_id = fm.group_id "
				+ "where fm.member_name = ?";
	private static final String deleteGroup = "delete from faction_id where group_id = ?;";

	private static final String getAllMembers = "select member_name, rank_id from faction_member where faction_id = ?;";
	private static final String addMember = "insert into faction_member(group_id, rank_id, member_name) values(?,?,?)";

	private static final String removeMember = "delete from faction_member where member_name = ? and group_id = ?";
		
	private static final String removeAllMembers = "delete fm.* from faction_member fm "
				+ "inner join faction_id fi on fi.group_id = fm.group_id "
				+ "where fi.group_name =?";
		
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
		
	private static final String loadGroupsInvitations = "select uuid, group_id, rank_id from group_invitation";
		
	private static final String addGroupInvitation = "insert into group_invitation(uuid, group_id, rank_id) values(?, ?, ?) on duplicate key update rank_id=values(rank_id), date=now();";
		
	private static final String removeGroupInvitation = "delete from group_invitation where uuid = ? and group_id = ?";
		
	private static final String loadGroupInvitation = "select rank_id from group_invitation where uuid = ? and group_id = ?";
		
	private static final String loadGroupInvitationsForGroup = "select uuid, rank_id from group_invitation where group_id = ?";
		
	// Gets the "most recent" updated group from all groups that share the name.
	private static final String getTimestamp = "SELECT MAX(faction.last_timestamp) FROM faction "
								+ "WHERE group_name = ?;";
		
	// updates "most recent" of all groups with a given name.
	private static final String updateLastTimestamp = "UPDATE faction SET faction.last_timestamp = NOW() "
								+ "WHERE group_name = ?;";
		
	private static final String addPermission = "insert into permissionByGroup(group_id,rank_id,perm_id) values(?, ?, ?);";
	private static final String getPermission = "select rank_id, perm_id from permissionByGroup where group_id = ?;";
	private static final String removePermission = "delete from permissionByGroup where group_id = ? and rank_id = ? and perm_id = ?;";
	private static final String registerPermission = "insert into permissionIdMapping(perm_id, name) values(?, ?);"; 
	private static final String getPermissionMapping = "select * from permissionIdMapping;";
	
	private static final String addPermissionLegacy = "insert into permissionByGroup(group_id,role,perm_id) select g.group_id, ?, ? from faction_id g where g.group_name = ?;";
		
	private static final String getAllGroupIds = "select group_id from faction_id";
	private static final String getAllGroupNames = "select distinct(group_name) from faction_id;";
	
	private static final String addPlayerType = "insert into groupPlayerTypes (group_id, rank_id, type_name, parent_rank_id) values(?,?,?,?);";
	private static final String deletePlayerType = "delete from groupPlayerTypes where group_id = ? and rank_id = ?;";
	private static final String renamePlayerType = "update groupPlayerTypes set type_name = ? where group_id = ? and rank_id = ?;";
	private static final String getAllPlayerTypesForGroup = "select rank_id, type_name, parent_rank_id from groupPlayerTypes where group_id = ?;";
	
	private static final String addMergeMapping = "insert into mergedGroups (oldGroup,newGroup) values(?,?);";
	private static final String updateMergeMapping = "update mergedGroups set newGroup = ? where newGroup = ?;";
			 

	public GroupManagerDao(Logger logger, ManagedDatasource db){
		this.logger = logger;
		this.db = db;
	}
	
	/**
	 * Not going to lie, I can't make heads or tails out of half of this.
	 */
	public void registerMigrations() {
		db.registerMigration(2, true, 
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
				
		db.registerMigration(1, false, 
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
								PreparedStatement permInit = connection.prepareStatement(addPermissionLegacy);
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
			db.registerMigration(14, false,
					"create table if not exists mergedGroups (oldGroup int not null, newGroup int not null, primary key(oldGroup));");
			db.registerMigration(15, false, 
					new Callable<Boolean>() {

						@Override
						public Boolean call() {
							//Previously if a group was merged, it had multiple ids assigned to it and the "real id" was simply the first
							//one returned by the database. We fix this here, but one last time have to extract the first one returned by
							//ResultSet to turn it into the real id, which is not doable completly in sql, so we have to do this slow 
							//work around
							List <String> groupNames = new LinkedList<String>();
							//first we get a list of all group names
							try (Connection connection = db.getConnection();
									PreparedStatement getGroupNames = connection.prepareStatement(GroupManagerDao.getAllGroupNames);
											ResultSet allNameSet = getGroupNames.executeQuery()) {
								while (allNameSet.next()) {
									String groupName = allNameSet.getString(1);
									groupNames.add(groupName);
								}
								
							} catch (SQLException e) {
								logger.log(Level.SEVERE, "Error while collecting group names to update merge mapping", e);
								return false;
							}
							//now we iterate over each group name and first get all ids, which map to this group name
							for(String groupName : groupNames) {
								List <Integer> retrievedIds = new LinkedList<Integer>();
								try (Connection connection = db.getConnection();
										PreparedStatement getIdsForName = connection.prepareStatement(GroupManagerDao.getGroup);) {
									getIdsForName.setString(1, groupName);
									try(ResultSet specificIdSet = getIdsForName.executeQuery()) {
										while (specificIdSet.next()) {
											retrievedIds.add(specificIdSet.getInt(5));			
										}
									}
								} catch (SQLException e) {
									logger.log(Level.SEVERE, "Error collecting merged id collections", e);
									return false;
								}
								if (retrievedIds.size() <= 1) {
									//nothing to do, group was never merged
									continue;
								}
								//this is the first id returned and the main id of the group. All other ones are pointing to the same group, but should
								//not have their own entry in any table other than the merge mapping, which we create here
								int realId = retrievedIds.get(0);
								for(int i = 1 ; i < retrievedIds.size(); i++) {
									try (Connection connection = db.getConnection();
											PreparedStatement addMerge = connection.prepareStatement(GroupManagerDao.addMergeMapping)) {
										addMerge.setInt(1, retrievedIds.get(i));
										addMerge.setInt(2, realId);
										addMerge.execute();
									} catch (SQLException e) {
										logger.log(Level.SEVERE, "Error while inserting updated merge mapping", e);
										return false;
									}
								}
							}
							return true;
						}
				
				}
			);
			
			db.registerMigration(16, false, 
				//now we just clean up all duplicates entries for which we have replacements in mergeGroups
				"delete from faction_id where group_id in (select oldGroup from mergedGroups);",
				//now we can also add a foreign key relation ship between faction and faction_id, because we can guarantee that there is only one entry per group
				//in faction_id
				"delete from faction where group_name not in (select group_name from faction_id);",
				"alter table faction add constraint foreign key (group_name) references faction_id(group_name) on delete cascade;");
			
			db.registerMigration(17, false, 
					//leftover cleanup from migration 13
					"drop table if exists permissions;",
					//we no longer use this table, so might as well get rid of it
					"drop table if exists nameLayerNameChanges;",
					//make new table to hold player types
					"create table if not exists groupPlayerTypes(group_id int not null,"
							+ "rank_id int not null, type_name varchar(40) not null, parent_rank_id int, constraint unique (group_id, rank_id), constraint unique (group_id, type_name), "
							+ "primary key(group_id,rank_id), foreign key(group_id) references faction_id (group_id) on delete cascade);",
					//convert old player types over to new format
					"insert into groupPlayerTypes (group_id,rank_id,type_name) select group_id, 0, 'OWNER' from faction_id;",
					"insert into groupPlayerTypes (group_id,rank_id,type_name,parent_rank_id) select group_id, 1, 'ADMINS',0 from faction_id;",
					"insert into groupPlayerTypes (group_id,rank_id,type_name,parent_rank_id) select group_id, 2, 'MODS',1 from faction_id;",
					"insert into groupPlayerTypes (group_id,rank_id,type_name,parent_rank_id) select group_id, 3, 'MEMBERS',2 from faction_id;",
					"insert into groupPlayerTypes (group_id,rank_id,type_name,parent_rank_id) select group_id, 4, 'DEFAULT',0 from faction_id;",
					"insert into groupPlayerTypes (group_id,rank_id,type_name,parent_rank_id) select group_id, 5, 'BLACKLISTED',4 from faction_id;",
					//previously permissions were assigned to player ranks by the static name of that type, so we need to change that into an id. First of all we
					//need to get rid of the old primary key, which was a (group_id,role,perm_id) combo
					"alter table permissionByGroup drop primary key;",
					//add the new column we need to track specific player types for a group
					"alter table permissionByGroup add rank_id int default null;",
					//convert old permissions over
					"update permissionByGroup set rank_id=0 where role='OWNER';",
					"update permissionByGroup set rank_id=1 where role='ADMINS';",
					"update permissionByGroup set rank_id=2 where role='MODS';",
					"update permissionByGroup set rank_id=3 where role='MEMBERS';",
					"update permissionByGroup set rank_id=4 where role='NOT_BLACKLISTED';",
					//maybe some broken entries exist, we make sure to clean those out
					"delete from permissionByGroup where rank_id IS NULL;",
					//also delete entries which dont refer to an existing group
					"delete from permissionByGroup where group_id not in (select group_id from faction_id);",
					//now we no longer need the varchar role column
					"alter table permissionByGroup drop column role;",
					//this should have been done in the previous upgrade, it ensures a proper cleanup if someone messes with the master perm table
					"alter table permissionByGroup add constraint foreign key (perm_id) references permissionIdMapping(perm_id) on delete cascade;",
					//ensure perms clean themselves up if the group is deleted
					"alter table permissionByGroup add constraint foreign key (group_id, rank_id) references groupPlayerTypes(group_id, rank_id) on delete cascade;",
					//ensure perms cant be inserted multiple times
					"alter table permissionByGroup add constraint unique (group_id, rank_id, perm_id)",
					
					//interaction of ranks works differently now, so we need to adjust permissions for that
					
					//at this point the new invite/remove permissions were already created and registered, we can just use them
					"insert into permissionByGroup (group_id,perm_id,rank_id) select pbg.group_id, pimn.perm_id, pbg.rank_id from permissionByGroup pbg inner join permissionIdMapping pim " +
						"on pim.perm_id = pbg.perm_id cross join permissionidmapping pimn where pim.name = 'MEMBERS' and pimn.name = 'invitePlayer#3';",
					"insert into permissionByGroup (group_id,perm_id,rank_id) select pbg.group_id, pimn.perm_id, pbg.rank_id from permissionByGroup pbg inner join permissionIdMapping pim " +
							"on pim.perm_id = pbg.perm_id cross join permissionidmapping pimn where pim.name = 'MEMBERS' and pimn.name = 'removePlayer#3';",
					"insert into permissionByGroup (group_id,perm_id,rank_id) select pbg.group_id, pimn.perm_id, pbg.rank_id from permissionByGroup pbg inner join permissionIdMapping pim " +
							"on pim.perm_id = pbg.perm_id cross join permissionidmapping pimn where pim.name = 'MEMBERS' and pimn.name = 'listPlayer#3';",
					"insert into permissionByGroup (group_id,perm_id,rank_id) select pbg.group_id, pimn.perm_id, pbg.rank_id from permissionByGroup pbg inner join permissionIdMapping pim " +
							"on pim.perm_id = pbg.perm_id cross join permissionidmapping pimn where pim.name = 'MODS' and pimn.name = 'invitePlayer#2';",
					"insert into permissionByGroup (group_id,perm_id,rank_id) select pbg.group_id, pimn.perm_id, pbg.rank_id from permissionByGroup pbg inner join permissionIdMapping pim " +
							"on pim.perm_id = pbg.perm_id cross join permissionidmapping pimn where pim.name = 'MODS' and pimn.name = 'removePlayer#2';",
					"insert into permissionByGroup (group_id,perm_id,rank_id) select pbg.group_id, pimn.perm_id, pbg.rank_id from permissionByGroup pbg inner join permissionIdMapping pim " +
							"on pim.perm_id = pbg.perm_id cross join permissionidmapping pimn where pim.name = 'MODS' and pimn.name = 'listPlayer#2';",
					"insert into permissionByGroup (group_id,perm_id,rank_id) select pbg.group_id, pimn.perm_id, pbg.rank_id from permissionByGroup pbg inner join permissionIdMapping pim " +
							"on pim.perm_id = pbg.perm_id cross join permissionidmapping pimn where pim.name = 'ADMINS' and pimn.name = 'invitePlayer#1';",
					"insert into permissionByGroup (group_id,perm_id,rank_id) select pbg.group_id, pimn.perm_id, pbg.rank_id from permissionByGroup pbg inner join permissionIdMapping pim " +
							"on pim.perm_id = pbg.perm_id cross join permissionidmapping pimn where pim.name = 'ADMINS' and pimn.name = 'removePlayer#1';",
					"insert into permissionByGroup (group_id,perm_id,rank_id) select pbg.group_id, pimn.perm_id, pbg.rank_id from permissionByGroup pbg inner join permissionIdMapping pim " +
							"on pim.perm_id = pbg.perm_id cross join permissionidmapping pimn where pim.name = 'ADMINS' and pimn.name = 'listPlayer#1';",
					"insert into permissionByGroup (group_id,perm_id,rank_id) select pbg.group_id, pimn.perm_id, pbg.rank_id from permissionByGroup pbg inner join permissionIdMapping pim " +
							"on pim.perm_id = pbg.perm_id cross join permissionidmapping pimn where pim.name = 'OWNER' and pimn.name = 'invitePlayer#0';",
					"insert into permissionByGroup (group_id,perm_id,rank_id) select pbg.group_id, pimn.perm_id, pbg.rank_id from permissionByGroup pbg inner join permissionIdMapping pim " +
							"on pim.perm_id = pbg.perm_id cross join permissionidmapping pimn where pim.name = 'OWNER' and pimn.name = 'removePlayer#0';",
					"insert into permissionByGroup (group_id,perm_id,rank_id) select pbg.group_id, pimn.perm_id, pbg.rank_id from permissionByGroup pbg inner join permissionIdMapping pim " +
							"on pim.perm_id = pbg.perm_id cross join permissionidmapping pimn where pim.name = 'OWNER' and pimn.name = 'listPlayer#0';",
					"insert into permissionByGroup (group_id,perm_id,rank_id) select pbg.group_id, pimn.perm_id, pbg.rank_id from permissionByGroup pbg inner join permissionIdMapping pim " +
							"on pim.perm_id = pbg.perm_id cross join permissionidmapping pimn where pim.name = 'BLACKLIST' and pimn.name = 'invitePlayer#5';",
					"insert into permissionByGroup (group_id,perm_id,rank_id) select pbg.group_id, pimn.perm_id, pbg.rank_id from permissionByGroup pbg inner join permissionIdMapping pim " +
							"on pim.perm_id = pbg.perm_id cross join permissionidmapping pimn where pim.name = 'BLACKLIST' and pimn.name = 'removePlayer#5';",
					"insert into permissionByGroup (group_id,perm_id,rank_id) select pbg.group_id, pimn.perm_id, pbg.rank_id from permissionByGroup pbg inner join permissionIdMapping pim " +
							"on pim.perm_id = pbg.perm_id cross join permissionidmapping pimn where pim.name = 'BLACKLIST' and pimn.name = 'listPlayer#5';",
							
					//we are done converting permissions over, so we can now delete the old ones, foreign keys will leftover perms for groups
					"delete from permissionIdMapping where name = 'MEMBERS';",
					"delete from permissionIdMapping where name = 'MODS';",
					"delete from permissionIdMapping where name = 'ADMINS';",
					"delete from permissionIdMapping where name = 'OWNER';",
					"delete from permissionIdMapping where name = 'BLACKLIST';",
					
					//now we basically repeat the same for the group members table (faction_member) and invitation table (group_invitation)
					//lets start with group members
					"alter table faction_member add rank_id int default null;",
					"update faction_member set rank_id=0 where role='OWNER';",
					"update faction_member set rank_id=1 where role='ADMINS';",
					"update faction_member set rank_id=2 where role='MODS';",
					"update faction_member set rank_id=3 where role='MEMBERS';",
					"update faction_member set rank_id=4 where role='NOT_BLACKLISTED';",
					"delete from faction_member where rank_id is null;",
					"alter table faction_member drop column role;",
					"delete from faction_member where group_id not in (select group_id from faction_id);",
					"alter table faction_member add constraint foreign key (group_id, rank_id) references groupPlayerTypes(group_id, rank_id) on delete cascade;",
					"delete from faction_member where member_name is null",
					
					//remove old restrictions
					"alter table group_invitation drop index `UQ_uuid_groupName`;",
					
					//first of all use group id instead of name
					"alter table group_invitation add group_id int;",
					"update group_invitation as gi inner join faction_id as fi on fi.group_name=gi.groupName set gi.group_id = fi.group_id;",
					"delete from group_invitation where group_id is null;",
					"alter table group_invitation drop column groupName;",
					
					//now apply our player type changes
					"alter table group_invitation add rank_id int;",
					"update group_invitation set rank_id=0 where role='OWNER';",
					"update group_invitation set rank_id=1 where role='ADMINS';",
					"update group_invitation set rank_id=2 where role='MODS';",
					"update group_invitation set rank_id=3 where role='MEMBERS';",
					"delete from group_invitation where rank_id is null;",
					"alter table group_invitation drop column role;",
					"alter table group_invitation add constraint foreign key (group_id, rank_id) references groupPlayerTypes(group_id, rank_id) on delete cascade;",
					"alter table group_invitation add constraint unique (group_id, uuid);",
					
					//finally easy lookup by group id is nice
					"create index inviteTypeIdIndex on group_invitation(group_id);",
					//redo group deletion
					"drop procedure if exists deletegroupfromtable;",
					"create definer=current_user procedure deletegroupfromtable(" +
						"in groupId int,"
						+ "in specialAdminGroup varchar(36)" +
						") sql security invoker begin " +
							"declare specialId INT;" +
							"select group_id into specialId from faction_id where group_name = specialAdminGroup;" +
							"call mergeintogroup(specialId, groupId);" + 
						"end;",
					//need to update the merging procedure
					"drop procedure if exists mergeintogroup;",
					"create definer=current_user procedure mergeintogroup(" +
						"in remainingId int, in tomergeId int) " +
						"sql security invoker begin " +
							//update preexisting merge mapping to the group which we now remove
							"update mergedGroups set newGroup = remainingId where newGroup = tomergeId;" + 
							"insert into mergedGroups (oldGroup,newGroup) values(tomergeId, remainingId);" + 
							"delete from faction_id where group_id = tomergeId;" +
						"end;",
						//foreign keys will do everything else
					//need to update group creation as well
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
							"  insert into faction_member (member_name, rank_id, group_id) select founder, 0, f.group_id from faction_id f where f.group_name = group_name; " +
							"  insert into groupPlayerTypes (group_id, rank_id, type_name, parent_rank_id) select f.group_id, 0, 'OWNER', null from faction_id f where f.group_name = group_name;" + 
							"  insert into groupPlayerTypes (group_id, rank_id, type_name, parent_rank_id) select f.group_id, 1, 'ADMINS', 0 from faction_id f where f.group_name = group_name;" +
							"  insert into groupPlayerTypes (group_id, rank_id, type_name, parent_rank_id) select f.group_id, 2, 'MODS', null from faction_id f where f.group_name = group_name;" +
							"  insert into groupPlayerTypes (group_id, rank_id, type_name, parent_rank_id) select f.group_id, 3, 'MEMBERS', null from faction_id f where f.group_name = group_name;" +
							"  insert into groupPlayerTypes (group_id, rank_id, type_name, parent_rank_id) select f.group_id, 4, 'DEFAULT', 0 from faction_id f where f.group_name = group_name;" +
							"  insert into groupPlayerTypes (group_id, rank_id, type_name, parent_rank_id) select f.group_id, 5, 'BLACKLISTED', 4 from faction_id f where f.group_name = group_name;" +
						" end if; " +
						"end;");
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
		String name = null;
		UUID owner = null;
		boolean discipline = false;
		String password = null;
		int id = -1;
		
		try (Connection connection = db.getConnection();
				PreparedStatement getGroup = connection.prepareStatement(GroupManagerDao.getGroup)){
			
			getGroup.setString(1, groupName);
			try (ResultSet set = getGroup.executeQuery()){
				if (!set.next()) {
					return null;
				}
				
				name = set.getString(1);
				String uuid = set.getString(2);
				owner = (uuid != null) ? UUID.fromString(uuid) : null;
				discipline = set.getInt(4) != 0;
				password = set.getString(3);
				id = set.getInt(5);
			} catch (SQLException e) {
				logger.log(Level.WARNING, "Problem getting group " + groupName, e);
				return null;
			}
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem preparing query to get group " + groupName, e);
			return null;
		}
		Group g = null;
		try {
			g = new Group(name, owner, discipline, password, id);
		} catch (Exception e) {
			logger.log(Level.WARNING, "Problem retrieving group " + groupName, e);
			g = null;
		}
		if (!loadGroupMetaData(g)) {
			return null;
		}
		return g;
	}
	
	public Group getGroup(int groupId){
		String name = null;
		UUID owner = null;
		boolean dis = false;
		String password = null;
		int id = -1;
		
		try (Connection connection = db.getConnection();
				PreparedStatement getGroupById = connection.prepareStatement(GroupManagerDao.getGroupById)){
			getGroupById.setInt(1, groupId);
			try (ResultSet set = getGroupById.executeQuery();) {
				if (!set.next()) {
					return null;
				}

				name = set.getString(1);
				String uuid = set.getString(2);
				owner = null;
				if (uuid != null)
					owner = UUID.fromString(uuid);
				dis = set.getInt(4) != 0;
				password = set.getString(3);
				id = set.getInt(5);
			} catch (SQLException e) {
				logger.log(Level.WARNING, "Problem getting group " + groupId, e);
				return null;
			}
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem preparing query to get group " + groupId, e);
			return null;
		}
		Group g = null;
		try {
			g = new Group(name, owner, dis, password, id);
		} catch (Exception e) {
			logger.log(Level.WARNING, "Problem retrieving group " + groupId, e);
			g = null;
		}
		if (!loadGroupMetaData(g)) {
			return null;
		}
		return g;
	}
	
	/**
	 * Loads player types, members and invitations
	 */
	private boolean loadGroupMetaData(Group g) {
		if (g == null) {
			return false;
		}
		PlayerTypeHandler typeHandler = getPlayerTypes(g);
		if (typeHandler == null) {
			return false;
		}
		g.setPlayerTypeHandler(typeHandler);
		try (Connection connection = db.getConnection();
				PreparedStatement getAllMembers = connection.prepareStatement(GroupManagerDao.getAllMembers)) {
			getAllMembers.setInt(1, g.getGroupId());
			try (ResultSet rs = getAllMembers.executeQuery()) {
				while (rs.next()) {
					g.addToTracking(UUID.fromString(rs.getString(1)), typeHandler.getType(rs.getInt(2)), false);
				}
			}
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem preparing to get all members for group " + g.getName(), e);
		}
		for(Entry <UUID, PlayerType> invEntry : getInvitesForGroup(g).entrySet()) {
			g.addInvite(invEntry.getKey(), invEntry.getValue(), false);
		}
		for(Entry <PlayerType, List <PermissionType>> entry : getPermissions(g).entrySet()) {
			PlayerType type = entry.getKey();
			for(PermissionType perm : entry.getValue()) {
				type.addPermission(perm, false);
			}
		}
		return true;
	}
	
	public List<String> getGroupNames(UUID uuid){
		List<String> groups = new ArrayList<String>();
		try (Connection connection = db.getConnection();
				PreparedStatement getAllGroupsNames = connection.prepareStatement(GroupManagerDao.getAllGroupsForPlayer)){
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
	
	public void deleteGroupAsync(final Group group){
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable(){

			@Override
			public void run() {
				deleteGroup(group);
			}
			
		});
	}
	
	public void deleteGroup(Group group){
		try (Connection connection = db.getConnection();
				PreparedStatement deleteGroup = connection.prepareStatement(GroupManagerDao.deleteGroup)){
			deleteGroup.setInt(1, group.getGroupId());
			deleteGroup.setString(2, NameLayerPlugin.getSpecialAdminGroup());
			deleteGroup.executeUpdate();
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem deleting group " + group.getName(), e);
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
		try (Connection connection = db.getConnection();
				PreparedStatement addMember = connection.prepareStatement(GroupManagerDao.addMember)){
			addMember.setInt(1, group.getGroupId());
			addMember.setInt(2, role.getId());
			addMember.setString(3, member.toString());
			addMember.executeUpdate();
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem adding " + member + " as " + role.toString() 
					+ " to group " + group.getName(), e);
		}			
	}

	public void removeMemberAsync(final UUID member, final Group group){
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable(){

			@Override
			public void run() {
				removeMember(member,group);
			}
			
		});
	}
	
	public void removeMember(UUID member, Group group){
		try (Connection connection = db.getConnection();
				PreparedStatement removeMember = connection.prepareStatement(GroupManagerDao.removeMember)){
			removeMember.setString(1, member.toString());
			removeMember.setInt(2, group.getGroupId());
			removeMember.executeUpdate();
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem removing " + member + " from group " + group, e);
		}
	}

	public void removeAllMembersAsync(final String group){
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable(){

			@Override
			public void run() {
				removeAllMembers(group);
			}
			
		});
	}
	
	public void removeAllMembers(String group){
		try (Connection connection = db.getConnection();
				PreparedStatement removeAllMembers = connection.prepareStatement(GroupManagerDao.removeAllMembers)){
			removeAllMembers.setString(1, group);
			removeAllMembers.executeUpdate();
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem removing all members from group " + group, e);
		}
	}
	
	public void addAllPermissionsAsync(final int groupId, final Map <PlayerType, List <PermissionType>> perms) {
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable(){

			@Override
			public void run() {
				addAllPermissions(groupId , perms);
			}
			
		});
	}
	
	public void addAllPermissions(int groupId, Map <PlayerType, List <PermissionType>> perms) {
		try (Connection connection = db.getConnection();
				PreparedStatement addPermissionById = connection.prepareStatement(GroupManagerDao.addPermission)){
			for (Entry <PlayerType, List <PermissionType>> entry: perms.entrySet()){
				int typeId = entry.getKey().getId();
				for(PermissionType perm : entry.getValue()) {
					addPermissionById.setInt(1,  groupId);
					addPermissionById.setInt(2, typeId);
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
		int groupId = g.getGroupId();
		try (Connection connection = db.getConnection();
				PreparedStatement removePermissionById = connection.prepareStatement(GroupManagerDao.removePermission)){
			for (Entry <PlayerType, List <PermissionType>> entry: perms.entrySet()){
				int typeId = entry.getKey().getId();
				for(PermissionType perm : entry.getValue()) {
					removePermissionById.setInt(1,  groupId);
					removePermissionById.setInt(2, typeId);
					removePermissionById.setInt(3, perm.getId());
					removePermissionById.addBatch();
				}
			}
			
			int[] res = removePermissionById.executeBatch();
			if (res == null) {
				logger.log(Level.WARNING, "Failed to remove all permissions from group {0}", groupId);
			} else {
				int cnt = 0;
				for (int r : res) cnt += r;
				logger.log(Level.INFO, "Removed {0} of {1} permissions from group {2}",
						new Object[] {cnt, res.length, groupId});
			}
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem removing all permissions from group " + groupId, e);
		}
	}

	public void addPermission(Group group, PlayerType type, PermissionType perm) {
		try (Connection connection = db.getConnection();
			PreparedStatement addPermission = connection.prepareStatement(GroupManagerDao.addPermission)){
			addPermission.setInt(1, group.getGroupId());
			addPermission.setInt(2, type.getId());
			addPermission.setInt(3, perm.getId());
			addPermission.execute();
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem adding " + type + " with " + perm
					+ " to group " + group.getName(), e);
		}
	}
	
	public Map<PlayerType, List<PermissionType>> getPermissions(Group group){
		Map<PlayerType, List<PermissionType>> perms = new HashMap<PlayerType, List<PermissionType>>();
		try (Connection connection = db.getConnection();
				PreparedStatement getPermission = connection.prepareStatement(GroupManagerDao.getPermission)){
			getPermission.setInt(1, group.getGroupId());
			PlayerTypeHandler handler = group.getPlayerTypeHandler();
			try (ResultSet set = getPermission.executeQuery();) {
				while(set.next()){
					PlayerType type = handler.getType(set.getInt(1));
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
		} 
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
		try (Connection connection = db.getConnection();
				PreparedStatement removePermission = connection.prepareStatement(GroupManagerDao.removePermission)){
			removePermission.setInt(1, group.getGroupId());
			removePermission.setInt(2, pType.getId());
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
	
	public void registerPlayerType(Group g, PlayerType type) {
		try (Connection connection = db.getConnection();
				PreparedStatement addType = connection.prepareStatement(GroupManagerDao.addPlayerType);) {
			addType.setInt(1, g.getGroupId());
			addType.setInt(2, type.getId());
			addType.setString(3, type.getName());
			if (type.getParent() == null) {
				addType.setNull(4, Types.INTEGER);
			}
			else {
				addType.setInt(4, type.getParent().getId());
			}
			addType.execute();
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem adding player type " + type.getName() + " for " + g.getName(), e);
		}
	}
	
	public void removePlayerType(Group g, PlayerType type) {
		try (Connection connection = db.getConnection();
				PreparedStatement removeType = connection.prepareStatement(GroupManagerDao.deletePlayerType);) {
			removeType.setInt(1, g.getGroupId());
			removeType.setInt(2, type.getId());
			removeType.execute();
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem removing player type " + type.getName() + " from " + g.getName(), e);
		}
	}
	
	public void updatePlayerTypeName(Group g, PlayerType type) {
		try (Connection connection = db.getConnection();
				PreparedStatement renameType = connection.prepareStatement(GroupManagerDao.renamePlayerType);) {
			renameType.setString(1, type.getName());
			renameType.setInt(2, g.getGroupId());
			renameType.setInt(3, type.getId());
			renameType.execute();
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem updating player type name " + type.getName() + " from " + g.getName(), e);
		}
	}
	
	/**
	 * Loads all player types for the given group from the database and initializes them with empty permissions
	 * @param g Group to load types for
	 * @return Complete playertypehandler with all playertypes, but no permissions if everything worked or null if something went wrong
	 */
	public PlayerTypeHandler getPlayerTypes(Group g) {
		Map <Integer, PlayerType> retrievedTypes = new TreeMap<Integer, PlayerType>();
		Map <Integer, List <PlayerType>> pendingParents = new TreeMap<Integer, List <PlayerType>>();
		try (Connection connection = db.getConnection();
				PreparedStatement renameType = connection.prepareStatement(GroupManagerDao.getAllPlayerTypesForGroup);) {
			renameType.setInt(1, g.getGroupId());
			try (ResultSet rs = renameType.executeQuery()) {
				while (rs.next()) {
					int id = rs.getInt(1);
					String name = rs.getString(2);
					int parent = rs.getInt(3);
					PlayerType type = new PlayerType(name, id, null, new LinkedList<PermissionType>(), g);
					retrievedTypes.put(id, type);
					List <PlayerType> brothers = pendingParents.get(parent);
					if (brothers == null) {
						brothers = new LinkedList<PlayerType>();
						pendingParents.put(parent, brothers);
					}
					brothers.add(type);
				}
			}
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem loading player types for " + g.getName(), e);
			return null;
		}
		PlayerType root = retrievedTypes.get(retrievedTypes.get(PlayerTypeHandler.OWNER_ID));
		if (root == null) {
			logger.log(Level.WARNING, "Could not load root node for group " + g.getName() + "; Failed to load group");
			return null;
		}
		PlayerTypeHandler handler = new PlayerTypeHandler(retrievedTypes.get(PlayerTypeHandler.OWNER_ID), g);
		Queue <PlayerType> toHandle = new LinkedList<PlayerType>();
		toHandle.add(root);
		while (!toHandle.isEmpty()) {
			PlayerType parent = toHandle.poll();
			List <PlayerType> children = pendingParents.get(parent.getId());
			if (children == null) {
				continue;
			}
			for(PlayerType child : children) {
				//parent is intentionally non modifiable, so we create a new instance
				PlayerType type = new PlayerType(child.getName(), child.getId(), parent, g);
				parent.addChild(type);
				handler.loadPlayerType(type);
				toHandle.add(type);
				retrievedTypes.remove(type.getId());
			}
		}
		if (!retrievedTypes.isEmpty()) {
			//a type exists for this group, which is not part of the tree rooted at perm 0
			logger.log(Level.WARNING, "A total of " + retrievedTypes.values().size() + " player types could not be loaded for group " + g.getName() 
					+ ", because they arent part of the normal tree structure");
		}		
		return handler;
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
	
	public void mergeGroupAsync(final Group groupThatStays,final Group groupToMerge){
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable(){

			@Override
			public void run() {
				mergeGroup(groupThatStays,groupToMerge);
			}
			
		});
	}
	
	public void mergeGroup(Group groupThatStays, Group groupToMerge){
		try (Connection connection = db.getConnection();
				PreparedStatement mergeGroup = connection.prepareStatement(GroupManagerDao.mergeGroup);){
			mergeGroup.setInt(1, groupThatStays.getGroupId());
			mergeGroup.setInt(2, groupToMerge.getGroupId());
			mergeGroup.execute();
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem merging group " + groupToMerge.getName() + " into " + groupThatStays.getName(), e);
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
			addGroupInvitation.setInt(2, group.getGroupId());
			addGroupInvitation.setInt(3, role.getId());
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
			removeGroupInvitation.setInt(2, group.getGroupId());
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
			loadGroupInvitation.setInt(2, group.getGroupId());
			try (ResultSet set = loadGroupInvitation.executeQuery();) {
				while(set.next()){
					int role = set.getInt(1);
					PlayerType type = group.getPlayerTypeHandler().getType(role);
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
		PlayerTypeHandler handler = group.getPlayerTypeHandler();
		try (Connection connection = db.getConnection();
				PreparedStatement loadGroupInvitationsForGroup = connection.prepareStatement(GroupManagerDao.loadGroupInvitationsForGroup);){
			loadGroupInvitationsForGroup.setInt(1, group.getGroupId());
			try (ResultSet set = loadGroupInvitationsForGroup.executeQuery();) {
				while(set.next()) {
					String uuid = set.getString(1);
					int rankId = set.getInt(2);
					UUID playerUUID = UUID.fromString(uuid);
					PlayerType pType = handler.getType(rankId);
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
				int groupId = set.getInt("group_id");
				int rankId = set.getInt("rank_id");
				UUID playerUUID = UUID.fromString(uuid);
				Group g = GroupManager.getGroup(groupId);
				PlayerType type = null;
				if(g != null){
					type = g.getPlayerTypeHandler().getType(rankId);
				}
				if(type != null){
					g.addInvite(playerUUID, type, false);
					PlayerListener.addNotification(playerUUID, g);
				}
			}
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Problem loading all group invitations", e);
		}
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
	
	public GroupManager loadAllGroups() {
		Map <String, Integer> idsByName = new HashMap<String, Integer>();
		try (Connection connection = db.getConnection();
				PreparedStatement getIds = connection.prepareStatement("select * from faction_id;");
				ResultSet ids = getIds.executeQuery()) {
			while (ids.next()) {
				idsByName.put(ids.getString("group_name"), ids.getInt("group_id"));
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Map <Integer, Group> groupById = new ConcurrentHashMap<Integer, Group>();
		Map <String, Group> groupByName = new ConcurrentHashMap<String, Group>();
		try (Connection connection = db.getConnection();
				PreparedStatement getGroups = connection.prepareStatement("select group_name, founder, password, discipline_flags from faction;");
				ResultSet groups = getGroups.executeQuery()) {
			while (groups.next()) {
				String name = groups.getString(1);
				Integer id = idsByName.get(name);
				if (id == null) {
					logger.log(Level.WARNING, "No valid id found for group " + name);
					continue;
				}
				String uuidString = groups.getString(2);
				UUID founder = null;
				if (uuidString != null) {
					founder = UUID.fromString(uuidString);
				}
				Group g = new Group(name , founder, groups.getBoolean(4), groups.getString(3), id);
				groupById.put(id, g);
				groupByName.put(g.getName().toLowerCase(), g);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Map <Integer, Map <Integer,PlayerType>> retrievedTypes = new TreeMap<Integer, Map<Integer,PlayerType>>();
		Map <Integer, Map <Integer, List <PlayerType>>> pendingParents = new TreeMap<Integer, Map<Integer,List<PlayerType>>>();
		try (Connection connection = db.getConnection();
				PreparedStatement getTypes = connection.prepareStatement("select * from groupPlayerTypes;");
				ResultSet types = getTypes.executeQuery()) {
			while (types.next()) {
				String name = types.getString("type_name");
				int groupId = types.getInt("group_id");
				int rankId = types.getInt("rank_id");
				int parentId = types.getInt("parent_rank_id");
				PlayerType type = new PlayerType(name, rankId, null, new LinkedList<PermissionType>(), null);
				Map <Integer,PlayerType> typeMap = retrievedTypes.get(groupId);
				if (typeMap == null) {
					typeMap = new TreeMap<Integer, PlayerType>();
					retrievedTypes.put(groupId, typeMap);
				}
				typeMap.put(rankId, type);
				Map <Integer, List <PlayerType>> parentMap = pendingParents.get(groupId);
				if (parentMap == null) {
					parentMap = new TreeMap<Integer, List<PlayerType>>();
					pendingParents.put(groupId, parentMap);
				}
				List <PlayerType> brothers = parentMap.get(parentId);
				if (brothers == null) {
					brothers = new LinkedList<PlayerType>();
					parentMap.put(parentId, brothers);
				}
				brothers.add(type);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for(Entry <Integer, Map <Integer,PlayerType>> entry : retrievedTypes.entrySet()) {
			Map <Integer,PlayerType> loadedTypes = entry.getValue();
			Map <Integer, List <PlayerType>> loadedParents = pendingParents.get(entry.getKey());
			Group g = groupById.get(entry.getKey());
			if (g == null) {
				logger.log(Level.WARNING, "Found player types, but no group for id " + entry.getKey());
				continue;
			}
			PlayerType root = loadedTypes.get(PlayerTypeHandler.OWNER_ID);
			PlayerTypeHandler handler = new PlayerTypeHandler(root, g);
			Queue <PlayerType> toHandle = new LinkedList<PlayerType>();
			toHandle.add(root);
			while (!toHandle.isEmpty()) { 
				PlayerType parent = toHandle.poll();
				List <PlayerType> children = loadedParents.get(parent.getId());
				loadedParents.remove(parent.getId());
				if (children == null) {
					continue;
				}
				for(PlayerType child : children) {
					//parent is intentionally non modifiable, so we create a new instance
					PlayerType type = new PlayerType(child.getName(), child.getId(), parent, g);
					parent.addChild(type);
					handler.loadPlayerType(type);
					toHandle.add(type);
					loadedTypes.remove(type.getId());
				}
			}
			if (!loadedTypes.isEmpty()) {
				//a type exists for this group, which is not part of the tree rooted at perm 0
				logger.log(Level.WARNING, "A total of " + loadedTypes.values().size() + " player types could not be loaded for group " + g.getName() 
						+ ", because they arent part of the normal tree structure");
			}		
			g.setPlayerTypeHandler(handler);
		}
		try (Connection connection = db.getConnection();
				PreparedStatement getTypes = connection.prepareStatement("select * from faction_member;");
				ResultSet types = getTypes.executeQuery()) {
			while (types.next()) {
				int groupId = types.getInt("group_id");
				String memberUUIDString = types.getString("member_name");
				UUID member = null;
				if (memberUUIDString != null) {
					member = UUID.fromString(memberUUIDString);
				}
				else {
					logger.log(Level.WARNING, "Found invalid uuid " + memberUUIDString + " for group " + groupId);
					continue;
				}
				int rankId = types.getInt("rank_id");
				Group g = groupById.get(groupId);
				if (g == null) {
					logger.log(Level.WARNING, "Couldnt not load group " + groupId + " from cache to add member");
					continue;
				}
				g.addToTracking(member, g.getPlayerTypeHandler().getType(rankId), false);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try (Connection connection = db.getConnection();
				PreparedStatement getTypes = connection.prepareStatement("select * from permissionByGroup;");
				ResultSet types = getTypes.executeQuery()) {
			while (types.next()) {
				int groupId = types.getInt("group_id");
				int rankId = types.getInt("rank_id");
				int permId = types.getInt("perm_id");
				PermissionType perm = PermissionType.getPermission(permId);
				Group g = groupById.get(groupId);
				if (g == null) {
					logger.log(Level.WARNING, "Couldnt not load group " + groupId + " from cache to add permission");
					continue;
				}
				if (perm != null) {
					g.getPlayerTypeHandler().getType(rankId).addPermission(perm, false);
				}
				else {
					logger.log(Level.WARNING, "Could not load permission with id " + permId);
				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try (Connection connection = db.getConnection();
				PreparedStatement getTypes = connection.prepareStatement("select * from mergedGroups;");
				ResultSet types = getTypes.executeQuery()) {
			while (types.next()) {
				int oldId = types.getInt("oldGroup");
				int newId = types.getInt("newGroup");
				Group g = groupById.get(newId);
				if (g == null) {
					logger.log(Level.WARNING, "Inconsistent mapping from " + oldId + " to " + newId  + " found");
				}
				else {
					groupById.put(oldId, g);
				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		logger.log(Level.INFO, "Loaded a total of " + groupByName.size() + " groups with " + groupById.size() + " ids from the database");
		return new GroupManager(groupById, groupByName);
	}
}
