package vg.civcraft.mc.namelayer.command.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import vg.civcraft.mc.civmodcore.command.PlayerCommand;
import vg.civcraft.mc.mercury.MercuryAPI;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameAPI;
import vg.civcraft.mc.namelayer.NameLayerPlugin;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.listeners.PlayerListener;
import vg.civcraft.mc.namelayer.misc.Mercury;
import vg.civcraft.mc.namelayer.permission.PermissionType;
import vg.civcraft.mc.namelayer.permission.PlayerType;
import vg.civcraft.mc.namelayer.permission.PlayerTypeHandler;

public class InvitePlayer extends PlayerCommand {

	public InvitePlayer(String name) {
		super(name);
		setIdentifier("nlip");
		setDescription("Invite a player to a group.");
		setUsage("/nlip <group> <player> [playerType]");
		setArguments(2,3);
	}

	@Override
	public boolean execute(CommandSender s, String[] args) {
		final String targetGroup = args[0];
		final String targetPlayer = args[1];
		final String targetType = args.length >= 3 ? args[2] : null;
		final boolean isPlayer = s instanceof Player;
		final Player p = isPlayer ? (Player)s : null;
		final boolean isAdmin = !isPlayer || p.hasPermission("namelayer.admin");
		final Group group = GroupManager.getGroup(targetGroup);
		if (group == null) {
			p.sendMessage(ChatColor.RED + "This group doesn't exist");
			return true;
		}
		if (!isAdmin && group.isDisciplined()) {
			s.sendMessage(ChatColor.RED + "This group is disiplined.");
			return true;
		}
		final UUID targetAccount = NameAPI.getUUID(targetPlayer);
		if (targetAccount == null) {
			s.sendMessage(ChatColor.RED + "The player has never played before.");
			return true;
		}
		if (group.isTracked(targetAccount)) { // So a player can't demote someone who is above them.
			s.sendMessage(ChatColor.RED + "Player is already tracked."
					+ "Use /nlpp to change their PlayerType.");
			return true;
		}
		PlayerTypeHandler handler = group.getPlayerTypeHandler();
		final PlayerType pType = targetType != null ? handler.getType(targetType) : handler.getDefaultInvitationType();
		if (pType == null) {
			s.sendMessage(ChatColor.RED + "The player type you entered was not valid and no default invitation type was set");
			return true;
		}
		if (!isAdmin) {
			// Perform access check
			final UUID executor = p.getUniqueId();
			if (!NameAPI.getGroupManager().canModifyRank(executor, group, pType)) {
				s.sendMessage(ChatColor.RED + "You do not have permissions to invite to this player type");
				return true;
			}
			sendInvitation(group, pType, targetAccount, p.getUniqueId(), true);
			Mercury.addInvite(group.getGroupId(), pType.toString(), targetAccount, p.getUniqueId().toString());
		} else {
			sendInvitation(group, pType, targetAccount, null, true);
			Mercury.addInvite(group.getGroupId(), pType.toString(), targetAccount, null);
		}

		s.sendMessage(ChatColor.GREEN + "The invitation has been sent." + "\n Use /nlri to Revoke an invite.");
		return true;
	}

	public static void sendInvitation(Group group, PlayerType pType, UUID invitedPlayer, UUID inviter, boolean saveToDB){
		Player invitee = Bukkit.getPlayer(invitedPlayer);
		boolean shouldAutoAccept = NameLayerPlugin.getAutoAcceptHandler().getAutoAccept(invitedPlayer);
		if (invitee != null) {
			// invitee is online
			if (shouldAutoAccept) {
				// player auto accepts invite
				if (saveToDB) {
					group.addToTracking(invitedPlayer, pType);
				}
				else {
					group.addToTracking(invitedPlayer, pType, false);
				}
				invitee.sendMessage(
						ChatColor.GREEN + " You have auto-accepted invite to the group: " + group.getName());
			} else {
				group.addInvite(invitedPlayer, pType, saveToDB);
				PlayerListener.addNotification(invitedPlayer, group);
				String msg;
				if(inviter != null){
					String inviterName = NameAPI.getCurrentName(inviter);
					msg = "You have been invited to the group " + group.getName()
							+ " by " + inviterName + ".\n";
				} else {
					msg = "You have been invited to the group " + group.getName()+ ".\n";
				}
				TextComponent message = new TextComponent(msg + "Click this message to accept. If you wish to toggle invites "
						+ "so they always are accepted please run /nltaai");
				message.setColor(net.md_5.bungee.api.ChatColor.GREEN);
				message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/nlag " + group.getName()));
				message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("  ---  Click to accept").create()));
				invitee.spigot().sendMessage(message);
			}
		} else {
			// invitee is offline or on a different shard
			if (shouldAutoAccept) {
				if (saveToDB) {
					group.addToTracking(invitedPlayer, pType);
				}
				else {
					group.addToTracking(invitedPlayer, pType, false);
				}
			} else {
				// Player did not auto accept
				group.addInvite(invitedPlayer, pType, saveToDB);
			}
			PlayerListener.addNotification(invitedPlayer, group);
		}
	}
	
	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		if (!(sender instanceof Player)){
			sender.sendMessage(ChatColor.RED + "I'm sorry baby, please run this as a player :)");
			return null;
		}
		if (args.length < 2) {
			if (args.length == 0)
				return GroupTabCompleter.complete(null, null, (Player) sender);
			else
				return GroupTabCompleter.complete(args[0], null, (Player)sender);

		} else if (args.length == 2) {
			List<String> namesToReturn = new ArrayList<String>();
			if (NameLayerPlugin.isMercuryEnabled()) {
				Set<String> players = MercuryAPI.getAllPlayers();
				for (String x: players) {
					if (x.toLowerCase().startsWith(args[1].toLowerCase()))
						namesToReturn.add(x);
				}
			}
			else {
				for (Player p: Bukkit.getOnlinePlayers()) {
					if (p.getName().toLowerCase().startsWith(args[0].toLowerCase()))
						namesToReturn.add(p.getName());
				}
			}
			return namesToReturn;
		}
		else if (args.length == 3)
			return MemberTypeCompleter.complete(args[2]);

		else return null;
	}
}
