package vg.civcraft.mc.namelayer.command.commands;

import java.util.List;
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
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameAPI;
import vg.civcraft.mc.namelayer.NameLayerPlugin;
import vg.civcraft.mc.namelayer.command.NameLayerTabCompleter;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.listeners.PlayerListener;
import vg.civcraft.mc.namelayer.misc.MercuryManager;
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
		PlayerTypeHandler handler = group.getPlayerTypeHandler();
		final PlayerType pType = targetType != null ? handler.getType(targetType) : handler.getDefaultInvitationType();
		if (pType == null) {
			s.sendMessage(ChatColor.RED + "The player type you entered was not valid and no default invitation type was set");
			return true;
		}
		if (!isAdmin) {
			// Perform access check
			final UUID executor = p.getUniqueId();
			PlayerType executorType = group.getPlayerType(executor);
			PermissionType permNeeded = pType.getInvitePermissionType();
			if (!executorType.hasPermission(permNeeded)) {
				s.sendMessage(ChatColor.RED + "You do not have permissions to invite to this player type");
				return true;
			}
		}
		if (group.isTracked(targetAccount)) {
			s.sendMessage(ChatColor.RED + "Player is already tracked."
					+ "Use /nlpp to change their PlayerType.");
			return true;
		}
		if (!isAdmin) {
			sendInvitation(group, pType, targetAccount, p.getUniqueId(), true);
			MercuryManager.addInvite(group.getGroupId(), pType.toString(), targetAccount, p.getUniqueId().toString());
		}
		else {
			sendInvitation(group, pType, targetAccount, null, true);
			MercuryManager.addInvite(group.getGroupId(), pType.toString(), targetAccount, null);
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
			if (args.length == 0) {
				return NameLayerTabCompleter.completeGroupWithPermission(null, null, (Player) sender);
			}
			else {
				return NameLayerTabCompleter.completeGroupWithPermission(args[0], null, (Player)sender);
			}

		} else if (args.length == 2) {
			return NameLayerTabCompleter.completeOnlinePlayer(args[1]);
		}
		else if (args.length == 3)
			return NameLayerTabCompleter.completePlayerType(args [2], GroupManager.getGroup(args [0]));

		else return null;
	}
}
