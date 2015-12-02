package vg.civcraft.mc.namelayer.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import vg.civcraft.mc.namelayer.GroupManager.PlayerType;
import vg.civcraft.mc.namelayer.group.Group;

public class GroupInviteEvent extends Event implements Cancellable {
	private static final HandlerList handlers = new HandlerList();

	private Group group;
	private Player player;
	private PlayerType perm;
	private Player inviter;
	private boolean cancelled = false;
	
	public GroupInviteEvent(Group group, Player player, PlayerType perm, Player inviter) {
		this.player = player;
		this.group = group;
		this.perm = perm;
		this.inviter = inviter;
	}
	
	public Player getPlayer() {
		return player;
	}
	
	public Group getGroup() {
		return group;
	}
	
	public PlayerType getPlayerType() {
		return perm;
	}
	
	public Player getInviter() {
		return inviter;
	}
	
	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancel) {
		cancelled = !cancelled;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

}
