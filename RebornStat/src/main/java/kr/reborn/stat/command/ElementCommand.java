package kr.reborn.stat.command;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Msg;
import kr.reborn.stat.RebornStat;
import kr.reborn.stat.growth.GrowthStrategy;
import kr.reborn.stat.growth.impl.SpiritGrowth;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * /element — 정령계 환생 직후 주 속성 선택 (기획서 5-4).
 *
 * 4대 원소(FIRE/WATER/EARTH/WIND): 즉시 선택 가능.
 * 12 소원소(LIGHTNING/ICE/POISON/METAL/CRYSTAL/LAVA/MIST/SAND/WOOD/SOUND): 즉시 선택 가능.
 * 빛(LIGHT)·어둠(DARK): 태초의 정령 시험(ancient_spirit_test 퀘스트) 통과 시에만.
 * 혼돈(CHAOS): 명령으로 선택 불가 — 기연(13번째 숲)으로만.
 */
public final class ElementCommand implements CommandExecutor {

    private final RebornStat plugin;
    public ElementCommand(RebornStat p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        var d = RebornCore.get().api().getPlayerData(p.getUniqueId());
        if (d == null) return true;
        if (d.worldKey() != WorldKey.SPIRIT) {
            Msg.error(p, "주 속성 선택은 정령계 거주자만 가능.");
            return true;
        }
        GrowthStrategy strategy = plugin.growth().of(WorldKey.SPIRIT);
        if (!(strategy instanceof SpiritGrowth spirit)) {
            Msg.error(p, "정령계 성장 strategy 없음.");
            return true;
        }
        if (a.length == 0) {
            Msg.send(p, "&3=== 주 속성 선택 ===");
            Msg.send(p, "&7/element <속성>");
            Msg.send(p, "&a4대 원소: &fFIRE, WATER, EARTH, WIND");
            Msg.send(p, "&b12 소원소: &fLIGHT, DARK, LIGHTNING, ICE, POISON, METAL,");
            Msg.send(p, "&b                  CRYSTAL, LAVA, MIST, SAND, WOOD, SOUND");
            Msg.send(p, "&8(LIGHT/DARK은 태초의 정령 시험 통과 필요)");
            Msg.send(p, "&5(CHAOS는 13번째 숲의 기연으로만)");
            // 현재 주 속성 표시
            SpiritGrowth.Element cur = currentPrimary(spirit, p.getUniqueId());
            if (cur != null) {
                Msg.send(p, "&7현재 주 속성: &f" + cur + " &7(친화도 "
                        + (int) spirit.affinityOf(p.getUniqueId(), cur) + ")");
            }
            return true;
        }
        SpiritGrowth.Element element;
        try { element = SpiritGrowth.Element.valueOf(a[0].toUpperCase()); }
        catch (Exception e) {
            Msg.error(p, "알 수 없는 속성: " + a[0]);
            return true;
        }
        if (element == SpiritGrowth.Element.CHAOS) {
            Msg.error(p, "&5혼돈 속성은 명령으로 선택 불가 — 13번째 숲의 기연으로만 가능.");
            return true;
        }
        // 이미 다른 속성으로 친화도 형성된 경우 차단 (환생 직후 1회만 허용)
        SpiritGrowth.Element cur = currentPrimary(spirit, p.getUniqueId());
        if (cur != null && spirit.affinityOf(p.getUniqueId(), cur) >= 50) {
            Msg.error(p, "이미 &f" + cur + " &c속성이 주속성으로 형성됨 ("
                    + (int) spirit.affinityOf(p.getUniqueId(), cur)
                    + "). 변경 불가 — 새 환생 필요.");
            return true;
        }
        // LIGHT/DARK은 태초의 정령 시험 통과 검증 (RebornQuest 리플렉션)
        if (element == SpiritGrowth.Element.LIGHT || element == SpiritGrowth.Element.DARK) {
            if (!hasPassedAncientTest(p)) {
                Msg.error(p, "&5빛/어둠은 태초의 정령 시험(/quest accept ancient_spirit_test) 통과 후만 가능.");
                Msg.warn(p, "&7시험 실패 시 4대 원소(FIRE/WATER/EARTH/WIND) 중 랜덤 배정.");
                return true;
            }
        }
        // 친화도 초기화 — 선택한 원소에 100 부여 (다른 원소는 0)
        spirit.absorbEssence(p, element, 100.0);
        Msg.send(p, "&3&l✦ 주 속성 결정 ✦ &r&7" + element + " &7정령으로 각성!");
        try {
            p.getWorld().spawnParticle(particleOf(element), p.getLocation().add(0, 1.5, 0),
                    80, 1, 1.5, 1, 0.1);
            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 1.0f);
        } catch (Throwable ignored) {}
        return true;
    }

    private SpiritGrowth.Element currentPrimary(SpiritGrowth s, java.util.UUID p) {
        SpiritGrowth.Element best = null;
        double bestV = 0;
        for (SpiritGrowth.Element e : SpiritGrowth.Element.values()) {
            double v = s.affinityOf(p, e);
            if (v > bestV) { bestV = v; best = e; }
        }
        return best;
    }

    /** RebornQuest 리플렉션 — ancient_spirit_test 완료 여부. */
    private boolean hasPassedAncientTest(Player p) {
        try {
            var qp = org.bukkit.Bukkit.getPluginManager().getPlugin("RebornQuest");
            if (qp == null) return false;
            Object engine = qp.getClass().getMethod("engine").invoke(qp);
            // QuestEngine.activeFor(uuid)는 진행 중 quest 반환 — 완료 여부는 별도 필요
            // 단순화: PlayerData.status에 "quest_complete:ancient_spirit_test" 마커 확인
            var d = RebornCore.get().api().getPlayerData(p.getUniqueId());
            if (d == null) return false;
            return d.status().containsKey("quest_complete:ancient_spirit_test");
        } catch (Throwable t) { return false; }
    }

    private org.bukkit.Particle particleOf(SpiritGrowth.Element e) {
        return switch (e) {
            case FIRE, LAVA -> org.bukkit.Particle.FLAME;
            case WATER, MIST -> org.bukkit.Particle.WATER_SPLASH;
            case EARTH, SAND -> org.bukkit.Particle.BLOCK_DUST;
            case WIND -> org.bukkit.Particle.CLOUD;
            case LIGHT -> org.bukkit.Particle.END_ROD;
            case DARK -> org.bukkit.Particle.SQUID_INK;
            case LIGHTNING -> org.bukkit.Particle.ELECTRIC_SPARK;
            case ICE -> org.bukkit.Particle.SNOWFLAKE;
            case POISON -> org.bukkit.Particle.SPELL_MOB;
            case METAL -> org.bukkit.Particle.CRIT;
            case CRYSTAL -> org.bukkit.Particle.GLOW;
            case WOOD -> org.bukkit.Particle.VILLAGER_HAPPY;
            case SOUND -> org.bukkit.Particle.NOTE;
            case CHAOS -> org.bukkit.Particle.PORTAL;
        };
    }
}
