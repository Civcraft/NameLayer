package vg.civcraft.mc.namelayer.command;

import java.util.HashMap;
import java.util.Map;

import vg.civcraft.mc.civmodcore.command.Command;
import vg.civcraft.mc.namelayer.command.commands.AcceptInvite;
import vg.civcraft.mc.namelayer.command.commands.ChangePlayerName;
import vg.civcraft.mc.namelayer.command.commands.CreateGroup;
import vg.civcraft.mc.namelayer.command.commands.CreatePlayerType;
import vg.civcraft.mc.namelayer.command.commands.DeleteGroup;
import vg.civcraft.mc.namelayer.command.commands.DeletePlayerType;
import vg.civcraft.mc.namelayer.command.commands.DisciplineGroup;
import vg.civcraft.mc.namelayer.command.commands.GroupStats;
import vg.civcraft.mc.namelayer.command.commands.InvitePlayer;
import vg.civcraft.mc.namelayer.command.commands.JoinGroup;
import vg.civcraft.mc.namelayer.command.commands.LeaveGroup;
import vg.civcraft.mc.namelayer.command.commands.NameLayerGroupGui;
import vg.civcraft.mc.namelayer.command.commands.ListCurrentInvites;
import vg.civcraft.mc.namelayer.command.commands.ListGroups;
import vg.civcraft.mc.namelayer.command.commands.ListMembers;
import vg.civcraft.mc.namelayer.command.commands.ListPermissions;
import vg.civcraft.mc.namelayer.command.commands.ListPlayerTypes;
import vg.civcraft.mc.namelayer.command.commands.MergeGroups;
import vg.civcraft.mc.namelayer.command.commands.ModifyPermissions;
import vg.civcraft.mc.namelayer.command.commands.RemoveMember;
import vg.civcraft.mc.namelayer.command.commands.RenamePlayerType;
import vg.civcraft.mc.namelayer.command.commands.SetPassword;
import vg.civcraft.mc.namelayer.command.commands.ToggleAutoAcceptInvites;
import vg.civcraft.mc.namelayer.command.commands.TransferGroup;
import vg.civcraft.mc.namelayer.command.commands.PromotePlayer;
import vg.civcraft.mc.namelayer.command.commands.RevokeInvite;
import vg.civcraft.mc.namelayer.command.commands.SetDefaultGroup;
import vg.civcraft.mc.namelayer.command.commands.GetDefaultGroup;

public class NameLayerCommandHandler extends vg.civcraft.mc.civmodcore.command.CommandHandler {
	public Map<String, Command> commands = new HashMap<String, Command>();
	
	public void registerCommands(){
		addCommands(new AcceptInvite("AcceptInvite"));
		addCommands(new CreateGroup("CreateGroup"));
		addCommands(new DeleteGroup("DeleteGroup"));
		addCommands(new DisciplineGroup("DisiplineGroup"));
		addCommands(new GroupStats("GroupStats"));
		addCommands(new InvitePlayer("InvitePlayer"));
		addCommands(new JoinGroup("JoinGroup"));
		addCommands(new ListGroups("ListGroups"));
		addCommands(new ListMembers("ListMembers"));
		addCommands(new ListPermissions("ListPermissions"));
		addCommands(new MergeGroups("MergeGroups"));
		addCommands(new ModifyPermissions("ModifyPermissions"));
		addCommands(new RemoveMember("RemoveMember"));
		addCommands(new SetPassword("SetPassword"));
		addCommands(new TransferGroup("TransferGroup"));
		addCommands(new LeaveGroup("LeaveGroup"));
		addCommands(new ListPlayerTypes("ListPlayerTypes"));
		addCommands(new ListCurrentInvites("ListCurrentInvites"));
		addCommands(new ToggleAutoAcceptInvites("AutoAcceptInvites"));
		addCommands(new PromotePlayer("PromotePlayer"));
		addCommands(new RevokeInvite("RevokeInvite"));
		addCommands(new ChangePlayerName("ChangePlayerName"));
		addCommands(new SetDefaultGroup("SetDefaultGroup"));
		addCommands(new GetDefaultGroup("GetDefaultGroup"));
		addCommands(new NameLayerGroupGui("OpenGUI"));
		addCommands(new CreatePlayerType("CreatePlayerType"));
		addCommands(new DeletePlayerType("DeletePlayerType"));
		addCommands(new RenamePlayerType("RenamePlayerType"));
	}
}
