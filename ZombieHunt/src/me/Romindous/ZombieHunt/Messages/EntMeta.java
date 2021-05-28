package me.Romindous.ZombieHunt.Messages;

import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.Metadatable;

import me.Romindous.ZombieHunt.Main;

public class EntMeta {

	public static void chngMeta(final Metadatable ent, final String meta, final byte val) {
		ent.setMetadata(meta, new FixedMetadataValue(Main.plug, val + (ent.hasMetadata(meta) ? ent.getMetadata(meta).get(0).asByte() : 0)));
	}

	public static boolean checkBol(final Metadatable ent, final String meta, final Object value) {
		if (!ent.hasMetadata(meta) || ent.getMetadata(meta).size() < 1) {
			ent.setMetadata(meta, new FixedMetadataValue(Main.plug, value));
			return true;
		}
		return ent.getMetadata(meta).get(0).value().equals(value);
	}
	/*
	public static void setOnKD(final Metadatable ent, final String meta, final Object rpl, String msg, final int kd, final Main plug) {
		final Object o = ent.getMetadata(meta).get(0).value();
		ent.setMetadata(meta, new FixedMetadataValue(plug, rpl));
		Bukkit.getScheduler().scheduleSyncDelayedTask(plug, new Runnable() {
			@Override
			public void run() {
				ent.setMetadata(meta, new FixedMetadataValue(plug, o));
				if (ent instanceof Player) {
					TitleManager.sendActionBar(((Player) ent), msg);
				}
			}
		}, kd);
	}*/
}
