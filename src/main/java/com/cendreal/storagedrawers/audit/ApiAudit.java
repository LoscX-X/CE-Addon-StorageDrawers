package com.cendreal.storagedrawers.audit;
import com.cendreal.storagedrawers.StorageDrawersCE;
import org.bukkit.*;
import org.bukkit.entity.Player;
import java.net.*;
import java.nio.file.Path;
import java.util.List;
/** Loads the separately compiled API-only consumer, not an internal implementation test. */
public final class ApiAudit {
    @SuppressWarnings("unchecked")public static List<String> run(StorageDrawersCE plugin,World world,Player player,Location normal,Location compact,Location framed,Location table,Location controller)throws Exception{
        var path=Path.of(System.getProperty("storagedrawers.ce.apiProbe"));
        try(var loader=new URLClassLoader(new URL[]{path.toUri().toURL()},plugin.getClass().getClassLoader())){
            var type=loader.loadClass("consumer.ApiContractProbe");
            try{return (List<String>)type.getMethod("run",org.bukkit.plugin.java.JavaPlugin.class,World.class,Player.class,Location.class,Location.class,Location.class,Location.class,Location.class).invoke(null,plugin,world,player,normal,compact,framed,table,controller);}catch(java.lang.reflect.InvocationTargetException failure){throw new IllegalStateException("External SDK consumer failed",failure.getCause());}
        }
    }
    private ApiAudit(){}
}
