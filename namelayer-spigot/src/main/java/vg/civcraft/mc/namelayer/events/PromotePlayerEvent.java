package vg.civcraft.mc.namelayer.events;

import java.util.UUID;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import vg.civcraft.mc.namelayer.NameLayerPlugin;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.permission.PlayerType;

public class PromotePlayerEvent extends Event implements Cancellable{
	private static final HandlerList handlers = new HandlerList();
	
	private boolean finished;
	private boolean cancelled;
	private UUID p;
	private Group g;
	private PlayerType c;
	private PlayerType f;

	public PromotePlayerEvent(UUID p, Group g, PlayerType currentType, PlayerType futureType){
		this.p = p;
		this.g = g;
		this.c = currentType;
		this.f = futureType;
		NameLayerPlugin.getInstance().debug("Promote Player Event Occured");
	}
	
	public UUID getPlayer(){
		return p;
	}
	
	public Group getGroup(){
		return g;
	}
	
	public PlayerType getCurrentPlayerType(){
		return c;
	}
	
	public PlayerType getFuturePlayerType(){
		return f;
	}
	
	public boolean isCancelled() {
		return cancelled;
	}

	public void setCancelled(boolean setvalue) {
		cancelled = setvalue;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
	
	public static HandlerList getHandlerList() {
	    return handlers;
	}
	
	public void setHasFinished(boolean value){
		finished = value;
	}
	
	public boolean hasFinished(){
		return finished;
	}

}
