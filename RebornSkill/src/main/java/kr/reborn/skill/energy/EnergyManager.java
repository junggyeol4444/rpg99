package kr.reborn.skill.energy;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.skill.RebornSkill;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 13세계 에너지 시스템 — 스킬이 소모한 양(deficit)만 자연 회복한다.
 *
 * 특수 스탯(MANA·DEMON_KI 등)은 "영구 성장 스탯"이자 "에너지 풀"을 겸한다.
 * 따라서 회복은 절대 스탯을 키우지 않고, consume()으로 깎인 만큼만 되돌린다.
 * (이전 구현은 초당 +1 무조건 가산이라 가만히 있어도 성장 시스템을 우회해
 *  스탯이 무한 증가하던 결함이 있었음.)
 *
 * deficit은 휘발성 — 재시작 시 비어있음 = "충분히 쉰 상태"로 친화적 처리.
 */
public final class EnergyManager {

    private final RebornSkill plugin;
    /** uuid → (stat → 소모 누적치). 회복은 이 빚을 갚는 것까지만. */
    private final Map<UUID, Map<StatType, Double>> deficit = new ConcurrentHashMap<>();

    public EnergyManager(RebornSkill p) { this.plugin = p; }

    public void tickAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerData d = RebornCore.get().api().getPlayerData(p.getUniqueId());
            if (d == null) continue;
            recover(p, d);
        }
    }

    private void recover(Player p, PlayerData d) {
        Map<StatType, Double> debts = deficit.get(p.getUniqueId());
        if (debts == null || debts.isEmpty()) return;
        long t = p.getWorld().getTime();
        boolean night = t >= 13000 && t <= 23000;
        for (var e : debts.entrySet()) {
            double owed = e.getValue();
            if (owed <= 0) continue;
            double rate = recoveryRate(p, d, e.getKey(), night);
            if (rate <= 0) continue;
            double restore = Math.min(rate, owed);
            d.addStat(e.getKey(), restore);
            e.setValue(owed - restore);
        }
        debts.entrySet().removeIf(en -> en.getValue() <= 0);
    }

    /** 세계·스탯별 초당 회복량. 거주 세계의 고유 에너지는 빠르게, 그 외는 30%. */
    private double recoveryRate(Player p, PlayerData d, StatType t, boolean night) {
        double base = switch (t) {
            case MANA -> 1.0;
            case DEMON_KI -> 1.0;
            case HEAVEN_KI -> 1.0;
            case IMMORTAL_KI -> 1.0;
            case SPIRIT_POWER -> 1.0;
            case YOKAI_KI -> night ? 3.0 : 0.5;
            case OCEAN_POWER -> p.isInWater() ? 1.5 : 0.3;
            case DRAGON_POWER -> 0.3;
            case INNER_KI, TAO_POWER -> 0.8;
            case AURA -> 1.0;
            case MAGITECH_ENERGY, CYBER_ADAPTATION -> 0.8;
            default -> 0.5;
        };
        // 거주 세계 보너스 — 자기 세계 에너지는 ×1, 타 세계에서는 ×0.3
        boolean home = switch (d.worldKey()) {
            case FANTASY -> t == StatType.MANA || t == StatType.AURA;
            case DEMON -> t == StatType.DEMON_KI;
            case HEAVEN -> t == StatType.HEAVEN_KI;
            case IMMORTAL -> t == StatType.IMMORTAL_KI || t == StatType.TAO_POWER;
            case SPIRIT -> t == StatType.SPIRIT_POWER;
            case YOKAI -> t == StatType.YOKAI_KI;
            case OCEAN -> t == StatType.OCEAN_POWER;
            case DRAGON -> t == StatType.DRAGON_POWER;
            case MARTIAL -> t == StatType.INNER_KI || t == StatType.TAO_POWER;
            case MAGITECH -> t == StatType.MAGITECH_ENERGY;
            case CYBERPUNK -> t == StatType.CYBER_ADAPTATION;
            default -> false;
        };
        return home ? base : base * 0.3;
    }

    /** 스킬 시전 비용 차감. 부족하면 false. 차감분은 deficit에 기록 → 자연 회복 대상. */
    public boolean consume(Player p, StatType t, double amount) {
        if (t == null || amount <= 0) return true;
        PlayerData d = RebornCore.get().api().getPlayerData(p.getUniqueId());
        if (d == null) return false;  // 오프라인이거나 데이터 로드 실패
        if (d.getStat(t) < amount) return false;
        d.addStat(t, -amount);
        deficit.computeIfAbsent(p.getUniqueId(), k -> new ConcurrentHashMap<>())
                .merge(t, amount, Double::sum);
        return true;
    }

    /** 외부 시스템이 에너지 강제 소모를 기록할 때 (예: 저주·환경 드레인). */
    public void recordDrain(UUID uuid, StatType t, double amount) {
        if (t == null || amount <= 0) return;
        deficit.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                .merge(t, amount, Double::sum);
    }

    public double deficitOf(UUID uuid, StatType t) {
        Map<StatType, Double> m = deficit.get(uuid);
        return m == null ? 0 : m.getOrDefault(t, 0.0);
    }
}
