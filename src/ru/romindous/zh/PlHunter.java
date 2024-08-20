package ru.romindous.zh;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import ru.komiss77.modules.player.Oplayer;
import ru.komiss77.utils.TCUtil;
import ru.romindous.zh.Commands.KitsCmd;
import ru.romindous.zh.Game.Arena;

public class PlHunter extends Oplayer {

    private Arena ar = null;
    private boolean orgZomb = false;
    private boolean zombie = false;

    public PlHunter(final HumanEntity p) {
        super(p);
    }

    public Arena arena() {return ar;}
    public void arena(final Arena ar) {this.ar = ar;}

    public boolean zombie() {return zombie;}
    public void zombie(final boolean inf) {this.zombie = inf;}

    public boolean orgZomb() {return orgZomb;}
    public void orgZomb(final boolean org) {this.orgZomb = org;}

    public void taq(final String pfx, final String afx, final String sfx) {
        color(zombie ? NamedTextColor.RED : NamedTextColor.GREEN);
        final Player p = getPlayer();
        tabPrefix(pfx, p);
        tabSuffix(sfx, p);
        beforeName(afx, p);
        tag(pfx, sfx);
    }

    public void survKit(final String kit) {
        mysqlData.put(Arena.SKIT, kit);
        score.getSideBar().update(Arena.SKIT, Arena.SURV_CLR + "Игрока: " + TCUtil.P + kit);
    }
    public String survKit() {
        final String def = KitsCmd.firstOf("kits.player");
        final String data = mysqlData.get(Arena.SKIT);
        return data == null || data.isEmpty() ? def : data;
    }

    public void zombKit(final String kit) {
        mysqlData.put(Arena.ZKIT, kit);
        score.getSideBar().update(Arena.ZKIT, Arena.ZOMB_CLR + "Зомби: " + TCUtil.P + kit);
    }
    public String zombKit() {
        final String def = KitsCmd.firstOf("kits.zombie");
        final String data = mysqlData.get(Arena.ZKIT);
        return data == null || data.isEmpty() ? def : data;
    }

    int kills = 0;
    public void kills0() {this.kills = 0;}
    public void killsI() {this.kills += 1;}
    public int kills() {return kills;}
}
