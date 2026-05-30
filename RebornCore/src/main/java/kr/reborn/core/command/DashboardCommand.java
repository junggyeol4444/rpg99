package kr.reborn.core.command;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * 통합 대시보드 — 플레이어의 모든 플러그인 상태 한 화면 표시.
 *
 * RebornCore의 모든 외부 플러그인 hook을 reflection으로 호출.
 * 각 플러그인이 없으면 해당 섹션은 표시하지 않음.
 */
public final class DashboardCommand implements CommandExecutor {

    private final RebornCore plugin;

    public DashboardCommand(RebornCore plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) { Msg.error(s, "플레이어 전용"); return true; }
        PlayerData d = plugin.api().getPlayerData(p.getUniqueId());
        if (d == null) { Msg.error(p, "데이터 없음"); return true; }

        Msg.send(p, "&6&l╔══════════════════════════════╗");
        Msg.send(p, "&6&l║ §f대시보드: " + p.getName() + "&6&l ║");
        Msg.send(p, "&6&l╚══════════════════════════════╝");

        // 기본 정보
        p.sendMessage("§7세계: §e" + d.worldKey()
                + " §7| 경지: §e" + d.tier()
                + " §7| 환생: §e" + d.reincarnations() + "회");
        p.sendMessage("§7체력: §c" + (int) p.getHealth() + "/" + (int) p.getMaxHealth()
                + " §7스탯 총합: §6" + (long) plugin.api().getTotalStats(p.getUniqueId()));

        // 공통 8스탯
        StringBuilder sb = new StringBuilder("§7§8 STR ");
        sb.append("§c").append((int) d.getStat(StatType.STRENGTH))
                .append(" §7DEX §a").append((int) d.getStat(StatType.AGILITY))
                .append(" §7END §6").append((int) d.getStat(StatType.ENDURANCE))
                .append(" §7INT §b").append((int) d.getStat(StatType.INTELLIGENCE))
                .append(" §7MEN §d").append((int) d.getStat(StatType.MENTAL))
                .append(" §7LCK §e").append((int) d.getStat(StatType.LUCK))
                .append(" §7CHR §5").append((int) d.getStat(StatType.CHARISMA))
                .append(" §7CHM §6").append((int) d.getStat(StatType.CHARM));
        p.sendMessage(sb.toString());

        // 세계별 특수 스탯
        showSpecialStat(p, d, StatType.MANA);
        showSpecialStat(p, d, StatType.INNER_KI);
        showSpecialStat(p, d, StatType.IMMORTAL_KI);
        showSpecialStat(p, d, StatType.DEMON_KI);
        showSpecialStat(p, d, StatType.HEAVEN_KI);
        showSpecialStat(p, d, StatType.YOKAI_KI);
        showSpecialStat(p, d, StatType.SPIRIT_POWER);
        showSpecialStat(p, d, StatType.DRAGON_POWER);
        showSpecialStat(p, d, StatType.OCEAN_POWER);
        showSpecialStat(p, d, StatType.MAGITECH_ENERGY);
        showSpecialStat(p, d, StatType.CYBER_ADAPTATION);
        showSpecialStat(p, d, StatType.DIVINITY);
        showSpecialStat(p, d, StatType.UNDERWORLD_KI);

        // 외부 플러그인 hook
        showSection(p, "RebornHiddenClass", "히든 클래스",
                this::hiddenClassInfo);
        showSection(p, "RebornCurse", "활성 효과", this::curseInfo);
        showSection(p, "RebornEconomy", "재정", this::economyInfo);
        showSection(p, "RebornClan", "가문", this::clanInfo);
        showSection(p, "RebornSpawn", "종족", this::raceInfo);
        showSection(p, "RebornTime", "환생력", this::calendarInfo);

        return true;
    }

    private void showSpecialStat(Player p, PlayerData d, StatType t) {
        double v = d.getStat(t);
        if (v > 0) {
            p.sendMessage("§7" + t + ": §f" + (int) v);
        }
    }

    private void showSection(Player p, String pluginName, String label,
                             java.util.function.BiConsumer<Player, org.bukkit.plugin.Plugin> fn) {
        var pl = Bukkit.getPluginManager().getPlugin(pluginName);
        if (pl == null) return;
        try {
            fn.accept(p, pl);
        } catch (Throwable ignored) {}
    }

    private void hiddenClassInfo(Player p, org.bukkit.plugin.Plugin pl) {
        try {
            Object prog = pl.getClass().getMethod("progress").invoke(pl);
            Object set = prog.getClass().getMethod("unlocked", java.util.UUID.class)
                    .invoke(prog, p.getUniqueId());
            if (set instanceof java.util.Collection<?> col && !col.isEmpty()) {
                p.sendMessage("§5히든 클래스: §f" + col.size() + " 종 — " + col);
            }
        } catch (Throwable ignored) {}
    }

    private void curseInfo(Player p, org.bukkit.plugin.Plugin pl) {
        try {
            Object effects = pl.getClass().getMethod("effects").invoke(pl);
            Object map = effects.getClass().getMethod("of", java.util.UUID.class)
                    .invoke(effects, p.getUniqueId());
            if (map instanceof java.util.Map<?, ?> m && !m.isEmpty()) {
                p.sendMessage("§c활성 효과: §f" + m.size() + " 건");
            }
        } catch (Throwable ignored) {}
    }

    private void economyInfo(Player p, org.bukkit.plugin.Plugin pl) {
        try {
            Object cur = pl.getClass().getMethod("currencies").invoke(pl);
            Object bal = cur.getClass().getMethod("balance", java.util.UUID.class, String.class)
                    .invoke(cur, p.getUniqueId(), "GOLD_COIN");
            if (bal instanceof Number n && n.longValue() > 0) {
                p.sendMessage("§6GOLD: §f" + n.longValue());
            }
        } catch (Throwable ignored) {}
    }

    private void clanInfo(Player p, org.bukkit.plugin.Plugin pl) {
        try {
            Object cm = pl.getClass().getMethod("clans").invoke(pl);
            Object clan = cm.getClass().getMethod("ofPlayer", java.util.UUID.class)
                    .invoke(cm, p.getUniqueId());
            if (clan != null) {
                String name = (String) clan.getClass().getField("name").get(clan);
                int level = clan.getClass().getField("level").getInt(clan);
                p.sendMessage("§9가문: §f" + name + " §7Lv " + level);
            }
        } catch (Throwable ignored) {}
    }

    private void raceInfo(Player p, org.bukkit.plugin.Plugin pl) {
        try {
            Object races = pl.getClass().getMethod("races").invoke(pl);
            Object r = races.getClass().getMethod("raceOf", java.util.UUID.class)
                    .invoke(races, p.getUniqueId());
            if (r != null) {
                java.lang.reflect.Field f = r.getClass().getField("koreanName");
                String nameKor = (String) f.get(r);
                p.sendMessage("§e종족: §f" + nameKor);
            }
        } catch (Throwable ignored) {}
    }

    private void calendarInfo(Player p, org.bukkit.plugin.Plugin pl) {
        try {
            Object cal = pl.getClass().getMethod("calendar").invoke(pl);
            String now = (String) cal.getClass().getMethod("formatNow").invoke(cal);
            p.sendMessage("§3환생력: §f" + now);
        } catch (Throwable ignored) {}
    }
}
