package com.howlstudio.friendsystem;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
/**
 * FriendSystem — Player friend lists with online notifications and private messaging.
 * /friend add|remove|list|msg. Join/leave notifications to friends.
 */
public final class FriendPlugin extends JavaPlugin {
    private FriendManager manager;
    public FriendPlugin(JavaPluginInit init){super(init);}
    @Override protected void setup(){
        System.out.println("[Friends] Loading...");
        manager=new FriendManager(getDataDirectory());
        new FriendListener(manager).register();
        CommandManager cmd=CommandManager.get();
        cmd.register(manager.getFriendCommand());
        System.out.println("[Friends] Ready. "+manager.getPlayerCount()+" players.");
    }
    @Override protected void shutdown(){if(manager!=null)manager.save();System.out.println("[Friends] Stopped.");}
}
