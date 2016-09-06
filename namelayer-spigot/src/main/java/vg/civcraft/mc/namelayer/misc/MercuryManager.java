package vg.civcraft.mc.namelayer.misc;

import java.util.UUID;

import vg.civcraft.mc.mercury.MercuryAPI;
import vg.civcraft.mc.namelayer.NameLayerPlugin;
import vg.civcraft.mc.namelayer.group.Group;

public class MercuryManager {
	private static boolean isEnabled(){
		return NameLayerPlugin.isMercuryEnabled();
	}
	
	public static void createGroup(Group group, int id){
		if (isEnabled() == false)
			return;
		StringBuilder msg = new StringBuilder();
		msg.append("create|");
		msg.append(group.getName()); msg.append("|");
		msg.append(group.getOwner()); msg.append("|");
		msg.append(group.isDisciplined()); msg.append("|");
		msg.append(group.getPassword()); msg.append("|");
		msg.append(String.valueOf(id));
		MercuryAPI.sendGlobalMessage(msg.toString(), "namelayer");
	}
	
	public static void deleteGroup(String group){
		if (isEnabled() == false)
			return;
		StringBuilder msg = new StringBuilder();
		msg.append("delete|");
		msg.append(group);
		MercuryAPI.sendGlobalMessage(msg.toString(), "namelayer");
	}
	
	public static void transferGroup(Group g, UUID uuid) {
		if (isEnabled() == false)
			return;
		StringBuilder msg = new StringBuilder();
		msg.append("transfer|");
		msg.append(g.getName()); msg.append("|");
		msg.append(uuid.toString());
		MercuryAPI.sendGlobalMessage(msg.toString(), "namelayer");
	}

	public static void mergeGroup(String groupFrom, String groupTo) {
		if (isEnabled() == false)
			return;
		StringBuilder msg = new StringBuilder();
		msg.append("merge|");
		msg.append(groupFrom); msg.append("|");
		msg.append(groupTo);
		MercuryAPI.sendGlobalMessage(msg.toString(), "namelayer");
	}

	public static void doneMergeGroup(String groupFrom, String groupTo) {
		if (isEnabled() == false)
			return;
		StringBuilder msg = new StringBuilder();
		msg.append("donemerge|");
		msg.append(groupFrom); msg.append("|");
		msg.append(groupTo);
		MercuryAPI.sendGlobalMessage(msg.toString(), "namelayer");
	}

	public static void addPerm(String group, int ptype, String permtype) {
		if (isEnabled() == false)
			return;
		StringBuilder msg = new StringBuilder();
		msg.append("permadd|");
		msg.append(group); msg.append("|");
		msg.append(ptype); msg.append("|");
		msg.append(permtype);
		MercuryAPI.sendGlobalMessage(msg.toString(), "namelayer");
	}

	public static void remPerm(String group, int ptype, String permtype) {
		if (isEnabled() == false)
			return;
		StringBuilder msg = new StringBuilder();
		msg.append("permrem|");
		msg.append(group); msg.append("|");
		msg.append(ptype); msg.append("|");
		msg.append(permtype);
		MercuryAPI.sendGlobalMessage(msg.toString(), "namelayer");
	}

	public static void addMember(String group, String uuid, String type) {
		if (isEnabled() == false)
			return;
		StringBuilder msg = new StringBuilder();
		msg.append("addMember|");
		msg.append(group); msg.append("|");
		msg.append(uuid); msg.append("|");
		msg.append(type);
		MercuryAPI.sendGlobalMessage(msg.toString(), "namelayer");
	}

	public static void remMember(String group, String uuid) {
		if (isEnabled() == false)
			return;
		StringBuilder msg = new StringBuilder();
		msg.append("removeMember|");
		msg.append(group); msg.append("|");
		msg.append(uuid);
		MercuryAPI.sendGlobalMessage(msg.toString(), "namelayer");
	}

	public static void setPassword(String group, String password) {
		if (isEnabled() == false)
			return;
		StringBuilder msg = new StringBuilder();
		msg.append("setPass|");
		msg.append(group); msg.append("|");
		msg.append(password);
		MercuryAPI.sendGlobalMessage(msg.toString(), "namelayer");
	}

	public static void setFounder(String group, String uuid) {
		if (isEnabled() == false)
			return;
		StringBuilder msg = new StringBuilder();
		msg.append("setOwner|");
		msg.append(group); msg.append("|");
		msg.append(uuid);
		MercuryAPI.sendGlobalMessage(msg.toString(), "namelayer");
	}

	public static void setDisciplined(String group, boolean isDisciplined) {
		if (isEnabled() == false)
			return;
		StringBuilder msg = new StringBuilder();
		msg.append("disciplined|");
		msg.append(group); msg.append("|");
		msg.append(isDisciplined);
		MercuryAPI.sendGlobalMessage(msg.toString(), "namelayer");
	}

	public static void remInvite(int groupId, UUID uuid) {
		if (isEnabled() == false)
			return;
		StringBuilder msg = new StringBuilder();
		msg.append("removeInvitation|");
		msg.append(groupId); msg.append("|");
		msg.append(uuid);
		MercuryAPI.sendGlobalMessage(msg.toString(), "namelayer");
	}

	public static void addInvite(int groupId, String ptype, UUID targetAccount, String uuid) {
		if (isEnabled() == false)
			return;
		StringBuilder msg = new StringBuilder();
		msg.append("addInvitation|");
		msg.append(groupId); msg.append("|");
		msg.append(ptype); msg.append("|");
		msg.append(targetAccount.toString()); 
		if (uuid != null) {
			msg.append("|");
			msg.append(uuid);
		}
		MercuryAPI.sendGlobalMessage(msg.toString(), "namelayer");
	}

	public static void defaultGroup(String group, String uuid) {
		// Mercury.message("defaultGroup " + g.getName() + " " + uuid.toString());
		if (isEnabled() == false)
			return;
		StringBuilder msg = new StringBuilder();
		msg.append("defaultGroup|");
		msg.append(group); msg.append("|");
		msg.append(uuid);
		MercuryAPI.sendGlobalMessage(msg.toString(), "namelayer");		
	}
	
	public static void removeAutoAccept(UUID player) {
		if (!isEnabled()) {
			return;
		}
		StringBuilder msg = new StringBuilder();
		msg.append("removeAutoAccept|");
		msg.append(player.toString());
		MercuryAPI.sendGlobalMessage(msg.toString(), "namelayer");		
	}
	
	public static void addAutoAccept(UUID player) {
		if (!isEnabled()) {
			return;
		}
		StringBuilder msg = new StringBuilder();
		msg.append("addAutoAccept|");
		msg.append(player.toString());
		MercuryAPI.sendGlobalMessage(msg.toString(), "namelayer");		
	}

	public static void forceRecache(String group) {
		if (isEnabled() == false)
			return;
		StringBuilder msg = new StringBuilder();
		msg.append("recache|");
		msg.append(group);
		MercuryAPI.sendGlobalMessage(msg.toString(), "namelayer");
	}
	
	public static void addPlayerType(String group, String typeName, int typeId, int parentId) {
		if (isEnabled() == false)
			return;
		StringBuilder msg = new StringBuilder();
		msg.append("addPlayerType|");
		msg.append(group);
		msg.append("|");
		msg.append(typeName);
		msg.append("|");
		msg.append(typeId);
		msg.append("|");
		msg.append(parentId);
		MercuryAPI.sendGlobalMessage(msg.toString(), "namelayer");
	}
	
	public static void removePlayerType(String group, int id) {
		if (isEnabled() == false)
			return;
		StringBuilder msg = new StringBuilder();
		msg.append("remPlayerType|");
		msg.append(group);
		msg.append("|");
		msg.append(id);
		MercuryAPI.sendGlobalMessage(msg.toString(), "namelayer");
	}
	
	public static void renamePlayerType(String group, int id, String newName) {
		if (isEnabled() == false)
			return;
		StringBuilder msg = new StringBuilder();
		msg.append("renamePlayerType|");
		msg.append(group);
		msg.append("|");
		msg.append(id);
		msg.append("|");
		msg.append(newName);
		MercuryAPI.sendGlobalMessage(msg.toString(), "namelayer");
	}
	
	public static void notifyPromote(String group, UUID player, UUID executor, String oldRank, String newRank) {
		if (isEnabled() == false)
			return;
		StringBuilder msg = new StringBuilder();
		msg.append("promotePlayer|");
		msg.append(group);
		msg.append("|");
		msg.append(player.toString());
		msg.append("|");
		msg.append(executor.toString());
		msg.append("|");
		msg.append(oldRank);
		msg.append("|");
		msg.append(newRank);
		MercuryAPI.sendGlobalMessage(msg.toString(), "namelayer");
	}
}
