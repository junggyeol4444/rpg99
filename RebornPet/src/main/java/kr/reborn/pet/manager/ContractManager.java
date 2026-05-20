package kr.reborn.pet.manager;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Msg;
import kr.reborn.pet.RebornPet;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 정령계 핵심 계약 시스템 + 일반 계약.
 * 등급: HALF / MEDIUM / HIGH / SOUL
 */
public final class ContractManager {

    private final RebornPet plugin;
    private final Map<UUID, Contract> contracts = new HashMap<>();

    public ContractManager(RebornPet p) { this.plugin = p; }

    public boolean propose(Player owner, String targetMobOrSpiritId, Grade grade) {
        if (contracts.containsKey(owner.getUniqueId())) {
            Msg.error(owner, "이미 계약 중"); return false;
        }
        contracts.put(owner.getUniqueId(), new Contract(targetMobOrSpiritId, grade));
        Msg.send(owner, "&b계약 성립: " + targetMobOrSpiritId + " (" + grade + ")");
        return true;
    }

    public void breakContract(Player owner) {
        Contract c = contracts.remove(owner.getUniqueId());
        if (c == null) return;
        PlayerData d = RebornCore.get().api().getPlayerData(owner.getUniqueId());
        if (d.worldKey() == WorldKey.SPIRIT) {
            // 정령력 50% 영구 감소
            double cur = d.getStat(StatType.SPIRIT_POWER);
            d.setStat(StatType.SPIRIT_POWER, cur * (1 - plugin.getConfig().getDouble("contract.spirit-spirit-power-loss-percent", 50) / 100.0));
            Msg.error(owner, "&5계약 파기 — 정령력 50% 영구 감소.");
        } else {
            double pct = plugin.getConfig().getDouble("contract.break-stat-loss-percent", 10) / 100.0;
            for (StatType t : StatType.COMMON_8) d.setStat(t, d.getStat(t) * (1 - pct));
            Msg.error(owner, "계약 파기 — 모든 스탯 " + (pct * 100) + "% 감소.");
        }
    }

    public Contract of(UUID owner) { return contracts.get(owner); }

    public enum Grade { HALF, MEDIUM, HIGH, SOUL }

    public static final class Contract {
        public final String partnerId;
        public Grade grade;
        public Contract(String partnerId, Grade grade) {
            this.partnerId = partnerId; this.grade = grade;
        }
    }
}
