package kr.reborn.skill.energy;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.data.WorldKey;
import kr.reborn.skill.RebornSkill;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * 13세계 에너지 시스템. 자연 회복·소모 처리.
 * 단순화: PlayerData의 특수 스탯 = 현재 에너지로 사용.
 * (실제 운영 시 max_energy 별도 트래킹 필요)
 */
public final class EnergyManager {

    private final RebornSkill plugin;

    public EnergyManager(RebornSkill p) { this.plugin = p; }

    public void tickAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerData d = RebornCore.get().api().getPlayerData(p.getUniqueId());
            if (d == null) continue;
            recover(p, d);
        }
    }

    private void recover(Player p, PlayerData d) {
        long t = p.getWorld().getTime();
        boolean night = t >= 13000 && t <= 23000;
        switch (d.worldKey()) {
            case FANTASY:
                addUpTo(d, StatType.MANA, +1, computeMaxMana(d));
                break;
            case DEMON:
                // 마계: 1%/초, 타 세계 0.3%
                addUpTo(d, StatType.DEMON_KI, +1, 9999);
                break;
            case HEAVEN:
                addUpTo(d, StatType.HEAVEN_KI, +1, 9999);
                break;
            case IMMORTAL:
                addUpTo(d, StatType.IMMORTAL_KI, +1, 9999);
                break;
            case SPIRIT:
                addUpTo(d, StatType.SPIRIT_POWER, +1, 9999);
                break;
            case YOKAI:
                addUpTo(d, StatType.YOKAI_KI, night ? 3 : 0.5, 9999);
                break;
            case OCEAN:
                if (p.isInWater()) addUpTo(d, StatType.OCEAN_POWER, +1, 9999);
                break;
            case DRAGON:
                addUpTo(d, StatType.DRAGON_POWER, +0.05, 9999);
                break;
            case EARTH:
                addUpTo(d, StatType.LEVEL, 0, 99999); // 레벨은 자동 회복 X
                break;
            default: break;
        }
    }

    private double computeMaxMana(PlayerData d) {
        double s = d.getStat(StatType.MANA);
        return Math.max(50, s * 1.5);
    }

    private void addUpTo(PlayerData d, StatType t, double delta, double max) {
        if (d.getStat(t) >= max) return;
        d.addStat(t, delta);
    }

    /** 스킬 시전 비용 차감. 부족하면 false. */
    public boolean consume(Player p, StatType t, double amount) {
        if (t == null || amount <= 0) return true;
        PlayerData d = RebornCore.get().api().getPlayerData(p.getUniqueId());
        if (d.getStat(t) < amount) return false;
        d.addStat(t, -amount);
        return true;
    }
}
