package me.libraryaddict.Hungergames.Managers;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Properties;

import org.bukkit.Bukkit;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ReflectionManager
{
    private SimpleCommandMap commandMap;
    private String currentVersion;
    private String currentCraftBukkitVersion;
    private boolean gameProfile = false;
    private Class itemClass;
    private Properties properties;
    private Object propertyManager;
    private Object serverSettings;

    public ReflectionManager()
    {
        try
        {
            commandMap = (SimpleCommandMap) Bukkit.getServer().getClass().getDeclaredMethod("getCommandMap")
                    .invoke(Bukkit.getServer());
            Object obj = Bukkit.getServer().getClass().getDeclaredMethod("getServer").invoke(Bukkit.getServer());
            serverSettings = obj.getClass().getField("y").get(obj); //DedicatedServerSettings
            propertyManager = serverSettings.getClass().getDeclaredMethod("a").invoke(serverSettings); // DedicatedServerProperties
            properties = (Properties) propertyManager.getClass().getField("Y").get(propertyManager); // Properties
            currentCraftBukkitVersion = Bukkit.getServer().getClass().getPackage().getName();
            currentVersion = currentCraftBukkitVersion.replace("org.bukkit.craftbukkit.", "net.minecraft.server.");
            itemClass = getCraftClass("inventory.CraftItemStack");
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        try
        {
            Class.forName("com.mojang.authlib.GameProfile");
            gameProfile = true;
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    public ItemStack getBukkitItem(Object nmsItem)
    {
        try
        {
            return (ItemStack) itemClass.getMethod("asCraftMirror", getNmsClass("ItemStack")).invoke(null, nmsItem);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public SimpleCommandMap getCommandMap()
    {
        return commandMap;
    }

    public Class getCraftClass(String className)
    {
        try
        {
            return Class.forName(currentCraftBukkitVersion + "." + className);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public Class getNmsClass(String className)
    {
        try
        {
            return Class.forName(currentVersion + "." + className);
        }
        catch (Exception e)
        {
            // e.printStackTrace();
        }
        return null;
    }

    public Object getNmsItem(ItemStack itemstack)
    {
        try
        {
            return itemClass.getMethod("asNMSCopy", ItemStack.class).invoke(null, itemstack);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    private Properties getProperties()
    {
        if (properties == null)
        {
            try
            {
                properties = (Properties) propertyManager.getClass().getField("Y").get(propertyManager); // Properties
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
        }
        return properties;
    }

    public String getPropertiesConfig(String name, String obj)
    {
        return getProperties().getProperty(name, obj);
    }

    public boolean hasGameProfiles()
    {
        return gameProfile;
    }

    public void savePropertiesConfig()
    {
        try
        {
            serverSettings.getClass().getMethod("b").invoke(serverSettings); //forceSave()
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void sendChunk(Player p, int x, int z)
    {
        try
        {
            Object obj = p.getClass().getDeclaredMethod("getHandle").invoke(p);
            List list = (List) obj.getClass().getField("chunkCoordIntPairQueue").get(obj);
            Constructor con = Class.forName(currentVersion + ".ChunkCoordIntPair").getConstructor(int.class, int.class);
            list.add(con.newInstance(x, z));
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    public void setPropertiesConfig(String name, Object obj)
    {
        getProperties().setProperty(name, obj.toString());
    }

    public void setBoundingBox(Player p, boolean spectator)
    {
        try
        {
            Method handle = p.getClass().getMethod("getHandle");
            Object player = handle.invoke(p);

            Class c = Class.forName(currentVersion + ".Entity");
            
            try {
                c.getField("height");
            } catch (Exception ex) {
                return;
            }
            
            Field field2 = c.getField("width");
            Field field3 = c.getField("length");
            field2.setFloat(player, spectator ? 0 : 0.6F);
            field3.setFloat(player, spectator ? 0 : 1.8F);

        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
}
