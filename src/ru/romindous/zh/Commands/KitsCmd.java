package ru.romindous.zh.Commands;

import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;
import ru.komiss77.OConfig;
import ru.komiss77.modules.player.PM;
import ru.komiss77.utils.ItemBuilder;
import ru.komiss77.utils.ItemUtil;
import ru.komiss77.utils.TCUtil;
import ru.romindous.zh.Game.Arena;
import ru.romindous.zh.Game.GameState;
import ru.romindous.zh.Main;
import ru.romindous.zh.PlHunter;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class KitsCmd implements CommandExecutor, TabCompleter{

	public static final String split = "¶";
	private static final String[] esa = {};
	public static final OConfig kits = new OConfig(new File(
		Main.plug.getDataFolder() + File.separator + "kits.yml"), 0);

	public static String firstOf(final String path) {
		final ConfigurationSection cs = kits.getConfigurationSection(path);
		return cs == null ? "" : cs.getKeys(false).iterator().next();
	}

	@Override
	public List<String> onTabComplete(CommandSender send, Command cmd, String al, String[] args) {
		LinkedList<String> sugg = new LinkedList<String>();
		if (send instanceof Player) {
			if (send.isOp()) {
				if (args.length == 1) {
					sugg.add("add");
					sugg.add("edit");
					sugg.add("del");
					sugg.add("choose");
				} else if (args.length == 2) {
					switch (args[0]) {
					case "add", "edit", "del":
						sugg.add("zombie");
						sugg.add("player");
					default:
						break;
					}
				} else if (args.length == 3) {
					switch (args[0]) {
					case "edit", "del":
						switch (args[1]) {
						case "zombie":
							if (kits.getConfigurationSection("kits.zombie") == null) break;
							sugg.addAll(kits.getConfigurationSection("kits.zombie").getKeys(false));
							break;
						case "player":
							if (kits.getConfigurationSection("kits.player") == null) break;
							sugg.addAll(kits.getConfigurationSection("kits.player").getKeys(false));
							break;
						default:
							break;
						}
					default:
						break;
					}
				}
			} else {
				if (args.length == 1) {
					sugg.add("choose");
				}
			}
		}
		return sugg;
	}
	
	@Override
	public boolean onCommand(CommandSender send, Command cmd, String label, String[] args) {
		if (label.equalsIgnoreCase("zkits") && send instanceof final Player p) {
			if (send.isOp()) {
				if (args.length == 3 && (args[1].equalsIgnoreCase("player") || args[1].equalsIgnoreCase("zombie"))) {
					if (args[0].equalsIgnoreCase("add")) {
						//есть ли уже набор?
						if (kits.contains("kits." + args[1] + "." + args[2])) {
							p.sendMessage("§cТакой набор уже есть");
							return true;
						}
						Inventory inv = Bukkit.createInventory(p, 27, TCUtil.form("§3Создание Набора"));
						inv.setContents(fillAddInv());
						if (args[1].equalsIgnoreCase("player")) {
							inv.setItem(13, new ItemBuilder(Material.VILLAGER_SPAWN_EGG).name(args[2]).build());
						} else {
							inv.setItem(13, new ItemBuilder(Material.ZOMBIE_SPAWN_EGG).name(args[2]).build());
						}
						p.openInventory(inv);
					} else if (args[0].equalsIgnoreCase("edit") && kits.contains("kits." + args[1] + "." + args[2])) {
						Inventory inv = Bukkit.createInventory(p, 27, TCUtil.form("§3Создание Набора"));
						inv.setContents(fillEditInv("kits." + args[1] + "." + args[2]));
						if (args[1].equalsIgnoreCase("player")) {
							inv.setItem(13, new ItemBuilder(Material.VILLAGER_SPAWN_EGG).name(args[2]).build());
						} else {
							inv.setItem(13, new ItemBuilder(Material.ZOMBIE_SPAWN_EGG).name(args[2]).build());
						}
						p.openInventory(inv);
					} else if (args[0].equalsIgnoreCase("del") && kits.contains("kits." + args[1] + "." + args[2])) {
						kits.set("kits." + args[1] + "." + args[2], null);
						kits.saveConfig();
						p.sendMessage(Main.PRFX + "§7Набор §6" + args[2] + "§7 успешно удален!");
						return true;
					} else {
						p.sendMessage(Main.PRFX + "§cИспользование: /zkits add | edit | del  zombie | player  название");
						return true;
					}
				} else if (args.length == 1 && args[0].equalsIgnoreCase("choose")) {
					final Arena ar = PM.getOplayer(p, PlHunter.class).arena();
					if (ar != null && ar.getState() == GameState.RUNNING) {
						p.sendMessage(Main.PRFX + "§cВы не можете изпользовать эту комманду во время игры!");
						return true;
					}
					final Inventory inv = Bukkit.createInventory(p, 27, TCUtil.form("§3Доступные Наборы"));
					inv.setContents(fillChooseInv());
					p.openInventory(inv);
				} else {
					final Arena ar = PM.getOplayer(p, PlHunter.class).arena();
					if (ar != null && ar.getState() == GameState.RUNNING) {
						p.sendMessage(Main.PRFX + "§cВы не можете изпользовать эту комманду во время игры!");
						return true;
					}
					p.sendMessage(Main.PRFX + "§cИспользование: /zkits add | edit | del  zombie | player  название");
					return true;
				}
			} else if (args.length == 1 && args[0].equalsIgnoreCase("choose")) {
				final Inventory inv = Bukkit.createInventory(p, 27, TCUtil.form("§3Доступные Наборы"));
				inv.setContents(fillChooseInv());
				p.openInventory(inv);
			} else {
				p.sendMessage(Main.PRFX + "§cИспользование: /zkits add | edit | del  zombie | player  название");
				return true;
			}
		}
		return true;
	}

	private ItemStack[] fillChooseInv() {
		ItemStack[] loot = new ItemStack[27];
		byte pused = 0;
		byte zused = 0;
		for (byte i = 0; i < 27; i++) {
			final String[] kns;
			switch (i) {
			case 0, 2, 3, 4, 5, 6, 8, 13, 22:
				loot[i] = new ItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE).name("§0.").build();
				break;
			case 1:
				loot[i] = new ItemBuilder(Material.VILLAGER_SPAWN_EGG).name(TCUtil.N + "Наборы для " + Arena.SURV_CLR + "Игроков").build();
				break;
			case 7:
				loot[i] = new ItemBuilder(Material.ZOMBIE_SPAWN_EGG).name(TCUtil.N + "Наборы для " + Arena.ZOMB_CLR + "Зомби").build();
				break;
			case 9, 10, 11, 12, 18, 19, 20, 21:
				if (kits.getConfigurationSection("kits.player") == null) break;
				kns = kits.getConfigurationSection("kits.player").getKeys(false).toArray(esa);
				if (pused < kns.length) {
					loot[i] = new ItemBuilder(getItemStack("kits.player." + kns[pused] + ".0"))
						.name(Arena.SURV_CLR + "§n" + kns[pused]).deLore()
						.lore(" ").lore(TCUtil.P + "ЛКМ §7- выбрать набор")
						.lore(TCUtil.A + "ПКМ §7- посмотреть набор").build();
					pused++;
				}
				break;
			case 14, 15, 16, 17, 23, 24, 25, 26:
				if (kits.getConfigurationSection("kits.zombie") == null) break;
				kns = kits.getConfigurationSection("kits.zombie").getKeys(false).toArray(esa);
				if (zused < kns.length) {
					loot[i] = new ItemBuilder(getItemStack("kits.zombie." + kns[zused] + ".0"))
						.name(Arena.ZOMB_CLR + "§n" + kns[zused]).deLore()
						.lore(" ").lore(TCUtil.P + "ЛКМ §7- выбрать набор")
						.lore(TCUtil.A + "ПКМ §7- посмотреть набор").build();
					zused++;
				}
				break;
			default:
				break;
			}
		}
		return loot;
	}

	public static ItemStack[] fillEditInv(final String path) {
		final ItemStack[] loot = new ItemStack[27];
		if (kits.getConfigurationSection(path) == null) return loot;
		for (int i = 0; i < 27; i++) {
			switch (i) {
			case 0:
				loot[i] = getItemStack(path + ".helm");
				break;
			case 1:
				loot[i] = getItemStack(path + ".chest");
				break;
			case 2:
				loot[i] = getItemStack(path + ".leggs");
				break;
			case 3:
				loot[i] = getItemStack(path + ".boots");
				break;
			case 5:
				loot[i] = new ItemBuilder(Material.SPIDER_EYE).name("§6Здоровье: §4-1").amount(kits.getInt(path + ".hp")).build();
				break;
			case 6:
				loot[i] = new ItemBuilder(Material.BEETROOT).name("§6Здоровье: §2+1").amount(kits.getInt(path + ".hp")).build();
				break;
			case 8:
				loot[i] = new ItemBuilder(Material.GREEN_WOOL).name("§aГотово").build();
				break;
			case 17:
				if (kits.getString(path + ".perm").contains("w")) {
					loot[i] = new ItemBuilder(Material.IRON_INGOT).name("§6Воин и выше").build();
				} else if (kits.getString(path + ".perm").contains("h")) {
					loot[i] = new ItemBuilder(Material.GOLD_INGOT).name("§6Герой и выше").build();
				} else if (kits.getString(path + ".perm").contains("l")) {
					loot[i] = new ItemBuilder(Material.NETHERITE_INGOT).name("§6Легенда и выше").build();
				} else {
					loot[i] = new ItemBuilder(Material.BRICK).name("§6Без привилегий").build();
				}
				break;
			case 19, 20, 21, 22, 23, 24, 25:
				if (kits.contains(path + "." + (i - 19))) {
					loot[i] = getItemStack(path + "." + (i - 19));
				}
				break;
			default:
				loot[i] = new ItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE).name("§8-=-=-=-=-").build();
				break;
			}
		}
		return loot;
	}

	private ItemStack[] fillAddInv() {
		ItemStack[] loot = new ItemStack[27];
		for (byte i = 0; i < 27; i++) {
			switch (i) {
			case 0:
				loot[i] = new ItemStack(Material.LEATHER_HELMET);
				break;
			case 1:
				loot[i] = new ItemStack(Material.LEATHER_CHESTPLATE);
				break;
			case 2:
				loot[i] = new ItemStack(Material.LEATHER_LEGGINGS);
				break;
			case 3:
				loot[i] = new ItemStack(Material.LEATHER_BOOTS);
				break;
			case 5:
				loot[i] = new ItemBuilder(Material.SPIDER_EYE).name("§6Здоровье: §4-1").amount(20).build();
				break;
			case 6:
				loot[i] = new ItemBuilder(Material.BEETROOT).name("§6Здоровье: §2+1").amount(20).build();
				break;
			case 8:
				loot[i] = new ItemBuilder(Material.GREEN_WOOL).name("§aГотово").amount(20).build();
				break;
			case 17:
				loot[i] = new ItemBuilder(Material.BRICK).name("§6Без привилегий").build();
				break;
			case 19:
				loot[i] = new ItemStack(Material.WOODEN_SWORD);
				break;
			case 20, 21, 22, 23, 24, 25:
				break;
			default:
				loot[i] = new ItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE).name("§8-=-=-=-=-").build();
				break;
			}
		}
		return loot;
	}

	@SuppressWarnings("deprecation")
	public static @Nullable ItemStack getItemStack(final String path) {
		final ConfigurationSection cs = kits.getConfigurationSection(path);
		if (cs == null || !cs.contains("mat")) {
//			Bukkit.broadcast(TCUtil.form("get " + path + " to " + kits.getString(path)));
			return ItemUtil.parseItem(kits.getString(path), split);
		}
		//результат
		final ItemStack result = new ItemStack(Material.getMaterial(cs.getString("mat")));
		final ItemMeta meta = result.getItemMeta();
		//имя предметв
		if (cs.getString("name").length() > 1) {
			meta.setDisplayName(cs.getString("name"));
		}
		//неразрушимость
		meta.setUnbreakable(true);
		meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
		//кол-во предмета
		result.setAmount(cs.getInt("amt"));
		//описание предмета
		if (cs.contains("lore")) {
			meta.setLore(Arrays.asList(cs.getString("lore").split(":")));
		}
		//зачарования предмета
		if (cs.contains("enchs")) {
			if (!cs.getString("lvls").contains(":")) {
				meta.addEnchant(Enchantment.getByName(cs.getString("enchs")), 
						Integer.parseInt(cs.getString("lvls")), true);
			} else {
				for (byte i = 0; i < cs.getString("lvls").split(":").length; i++) {
					meta.addEnchant(Enchantment.getByName(cs.getString("enchs").split(":")[i]), 
							Integer.parseInt(cs.getString("lvls").split(":")[i]), true);
				}
			}
		}
		//цвет предмета
		if (cs.contains("color")) {
			LeatherArmorMeta lmeta = (LeatherArmorMeta) meta;
			lmeta.setColor(Color.fromRGB(cs.getInt("color")));
			result.setItemMeta(lmeta);
		} else if (cs.contains("pot")) {
			PotionMeta pmeta = (PotionMeta) meta;
			pmeta.setBasePotionType(Registry.POTION.get(Key.key("pot.base")));
			if (cs.contains("pot.color")) {
				pmeta.setColor(Color.fromRGB(Integer.parseInt(cs.getString("pot.color"))));
			}
			if (cs.contains("pot.types")) {
				if (!cs.getString("pot.durs").contains(":")) {
					pmeta.addCustomEffect(new PotionEffect(PotionEffectType.getByName(cs.getString("pot.types")), 
						Integer.parseInt(cs.getString("pot.durs")), 
						Integer.parseInt(cs.getString("pot.amps"))), true);
				} else {
					for (byte i = 0; i < cs.getString("pot.durs").split(":").length; i++) {
						pmeta.addCustomEffect(new PotionEffect(PotionEffectType.getByName(cs.getString("pot.types").split(":")[i]), 
							Integer.parseInt(cs.getString("pot.durs").split(":")[i]), 
							Integer.parseInt(cs.getString("pot.amps").split(":")[i])), true);
					}
				}
			}
			result.setItemMeta(pmeta);
		} else {
			result.setItemMeta(meta);
		}
		return result;
	}
}