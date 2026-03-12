package com.howlstudio.friendsystem;
import com.hypixel.hytale.component.Ref; import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.nio.file.*; import java.util.*;
public class FriendManager {
    private final Path dataDir;
    private final Map<UUID,Set<UUID>> friends=new HashMap<>();
    private final Map<UUID,Set<UUID>> requests=new HashMap<>();
    private final Map<UUID,String> names=new HashMap<>();
    public FriendManager(Path d){this.dataDir=d;try{Files.createDirectories(d);}catch(Exception e){}load();}
    public int getPlayerCount(){return friends.size();}
    public void onJoin(UUID uid,String name){
        names.put(uid,name);
        friends.computeIfAbsent(uid,k->new HashSet<>());
        // notify friends
        try{for(PlayerRef p:Universe.get().getPlayers()){
            if(!p.getUuid().equals(uid)&&isFriend(uid,p.getUuid()))
                p.sendMessage(Message.raw("[Friends] §a"+name+" §7is now online."));
        }}catch(Exception e){}
    }
    public void onLeave(UUID uid){
        String name=names.getOrDefault(uid,"?");
        try{for(PlayerRef p:Universe.get().getPlayers()){
            if(!p.getUuid().equals(uid)&&isFriend(uid,p.getUuid()))
                p.sendMessage(Message.raw("[Friends] §7"+name+" went offline."));
        }}catch(Exception e){}
        save();
    }
    public boolean isFriend(UUID a,UUID b){return friends.getOrDefault(a,Set.of()).contains(b);}
    public void sendRequest(UUID from,String toName,PlayerRef fromRef){
        PlayerRef target=null;try{for(PlayerRef p:Universe.get().getPlayers())if(p.getUsername().equalsIgnoreCase(toName)){target=p;break;}}catch(Exception e){}
        if(target==null){fromRef.sendMessage(Message.raw("[Friends] "+toName+" is not online."));return;}
        if(isFriend(from,target.getUuid())){fromRef.sendMessage(Message.raw("[Friends] Already friends with "+toName+"."));return;}
        requests.computeIfAbsent(target.getUuid(),k->new HashSet<>()).add(from);
        target.sendMessage(Message.raw("[Friends] §e"+fromRef.getUsername()+"§7 sent you a friend request. /friend accept "+fromRef.getUsername()));
        fromRef.sendMessage(Message.raw("[Friends] Friend request sent to "+toName+"."));
    }
    public void accept(UUID uid,String fromName,PlayerRef ref){
        UUID fromId=null;UUID n=names.entrySet().stream().filter(e->e.getValue().equalsIgnoreCase(fromName)).map(Map.Entry::getKey).findFirst().orElse(null);
        if(n==null){ref.sendMessage(Message.raw("[Friends] Not found: "+fromName));return;}
        fromId=n;
        Set<UUID> pending=requests.getOrDefault(uid,Set.of());
        if(!pending.contains(fromId)){ref.sendMessage(Message.raw("[Friends] No request from "+fromName+"."));return;}
        pending.remove(fromId);
        friends.computeIfAbsent(uid,k->new HashSet<>()).add(fromId);
        friends.computeIfAbsent(fromId,k->new HashSet<>()).add(uid);
        ref.sendMessage(Message.raw("[Friends] §aNow friends with "+fromName+"!"));
        try{for(PlayerRef p:Universe.get().getPlayers())if(p.getUuid().equals(fromId))p.sendMessage(Message.raw("[Friends] §a"+ref.getUsername()+" accepted your friend request!"));}catch(Exception e){}
        save();
    }
    public void remove(UUID uid,String targetName,PlayerRef ref){
        UUID tid=names.entrySet().stream().filter(e->e.getValue().equalsIgnoreCase(targetName)).map(Map.Entry::getKey).findFirst().orElse(null);
        if(tid==null||!isFriend(uid,tid)){ref.sendMessage(Message.raw("[Friends] Not friends with "+targetName+"."));return;}
        friends.getOrDefault(uid,new HashSet<>()).remove(tid);
        friends.getOrDefault(tid,new HashSet<>()).remove(uid);
        ref.sendMessage(Message.raw("[Friends] Removed "+targetName+" from friends."));
        save();
    }
    public void msg(UUID from,String toName,String message,PlayerRef ref){
        PlayerRef target=null;try{for(PlayerRef p:Universe.get().getPlayers())if(p.getUsername().equalsIgnoreCase(toName)){target=p;break;}}catch(Exception e){}
        if(target==null){ref.sendMessage(Message.raw("[Friends] "+toName+" is not online."));return;}
        if(!isFriend(from,target.getUuid())){ref.sendMessage(Message.raw("[Friends] Not friends with "+toName+"."));return;}
        target.sendMessage(Message.raw("[FM] §e"+ref.getUsername()+"§7→§eyou§7: "+message));
        ref.sendMessage(Message.raw("[FM] §eyou§7→§e"+toName+"§7: "+message));
    }
    public void save(){try{StringBuilder sb=new StringBuilder();for(Map.Entry<UUID,Set<UUID>> e:friends.entrySet()){String line=e.getKey()+":"+e.getValue().stream().map(UUID::toString).reduce((a,b)->a+","+b).orElse("");sb.append(line).append("\n");}Files.writeString(dataDir.resolve("friends.txt"),sb.toString());}catch(Exception e){}}
    private void load(){try{Path f=dataDir.resolve("friends.txt");if(!Files.exists(f))return;for(String l:Files.readAllLines(f)){String[]p=l.split(":");if(p.length<1)continue;UUID uid=UUID.fromString(p[0]);Set<UUID> set=new HashSet<>();if(p.length>1&&!p[1].isBlank())for(String u:p[1].split(","))try{set.add(UUID.fromString(u));}catch(Exception e){}friends.put(uid,set);}}catch(Exception e){}}
    public AbstractPlayerCommand getFriendCommand(){
        return new AbstractPlayerCommand("friend","Friend management. /friend add|accept|remove|list|msg"){
            @Override protected void execute(CommandContext ctx,Store<EntityStore> store,Ref<EntityStore> ref,PlayerRef playerRef,World world){
                UUID uid=playerRef.getUuid();String raw=ctx.getInputString().trim();
                String[]args=raw.isEmpty()?new String[0]:raw.split("\\s+",3);
                if(args.length==0){playerRef.sendMessage(Message.raw("Cmds: /friend add|accept|remove|list|msg"));return;}
                switch(args[0].toLowerCase()){
                    case"add","request"->{if(args.length<2)break;sendRequest(uid,args[1],playerRef);}
                    case"accept"->{if(args.length<2)break;accept(uid,args[1],playerRef);}
                    case"remove","unfriend"->{if(args.length<2)break;remove(uid,args[1],playerRef);}
                    case"list"->{
                        Set<UUID> fl=friends.getOrDefault(uid,Set.of());
                        if(fl.isEmpty()){playerRef.sendMessage(Message.raw("[Friends] No friends yet. /friend add <player>"));break;}
                        playerRef.sendMessage(Message.raw("[Friends] Your friends ("+fl.size()+"):"));
                        Set<UUID> online=new HashSet<>();try{for(PlayerRef p:Universe.get().getPlayers())online.add(p.getUuid());}catch(Exception e){}
                        for(UUID fid:fl){String n=names.getOrDefault(fid,fid.toString().substring(0,8));playerRef.sendMessage(Message.raw("  "+n+(online.contains(fid)?" §a[online]":"§7 [offline]")));}
                    }
                    case"msg"->{if(args.length<3)break;msg(uid,args[1],args[2],playerRef);}
                    default->playerRef.sendMessage(Message.raw("Cmds: /friend add|accept|remove|list|msg"));
                }
            }
        };
    }
}
