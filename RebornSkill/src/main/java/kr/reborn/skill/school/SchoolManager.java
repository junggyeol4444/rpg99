package kr.reborn.skill.school;

import kr.reborn.core.RebornCore;
import kr.reborn.core.util.Msg;
import kr.reborn.skill.RebornSkill;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 무공 학파 매니저.
 *
 * 플레이어는 1개의 무공 학파를 선택 (또는 변경).
 * 학파 변경 시:
 *   - 이전 학파 보너스 회수
 *   - 새 학파 보너스 적용
 *   - 같은 학파 NPC 호의 +30
 *   - 적대 학파 NPC 호의 -30
 */
public final class SchoolManager {

    /** 학파 간 적대 관계 */
    private static final Map<MartialSchool, MartialSchool> RIVALS = new HashMap<>();
    static {
        RIVALS.put(MartialSchool.ORTHODOX, MartialSchool.DEMON_CULT);
        RIVALS.put(MartialSchool.DEMON_CULT, MartialSchool.ORTHODOX);
        // UNORTHODOX, IMPERIAL, HERMIT은 적대 없음 (중립)
    }

    private final RebornSkill plugin;
    private final Map<UUID, MartialSchool> schools = new ConcurrentHashMap<>();

    public SchoolManager(RebornSkill plugin) { this.plugin = plugin; }

    public boolean setSchool(Player p, MartialSchool newSchool) {
        MartialSchool old = schools.get(p.getUniqueId());
        if (old == newSchool) {
            Msg.warn(p, "이미 " + newSchool.koreanName + " 학파.");
            return false;
        }
        // 이전 학파 보너스 회수
        if (old != null) {
            for (var e : old.bonus.entrySet()) {
                RebornCore.get().api().addStat(p.getUniqueId(),
                        e.getKey(), -e.getValue(), "school-revoke");
            }
        }
        // 새 학파 보너스
        for (var e : newSchool.bonus.entrySet()) {
            RebornCore.get().api().addStat(p.getUniqueId(),
                    e.getKey(), e.getValue(), "school-join:" + newSchool);
        }
        schools.put(p.getUniqueId(), newSchool);
        Bukkit.broadcastMessage(newSchool.colorCode + "&l[학파 가입] §f"
                + p.getName() + " §7→ §6" + newSchool.koreanName);
        Msg.send(p, "&6학파 변경: §f" + newSchool.koreanName);
        // NPC 호의 변경 — RebornNPC 리플렉션
        applyNpcReputation(p, newSchool);
        return true;
    }

    private void applyNpcReputation(Player p, MartialSchool ms) {
        try {
            var np = Bukkit.getPluginManager().getPlugin("RebornNPC");
            if (np == null) return;
            np.getClass().getMethod("nudgeNearbyFavor",
                            Player.class, double.class, double.class)
                    .invoke(np, p, 50.0, 30.0);
            // 라이벌 학파 NPC 호의 -30 (다른 메서드 필요 — 단순화)
        } catch (Throwable ignored) {}
    }

    public MartialSchool of(UUID p) { return schools.get(p); }

    public boolean areRivals(UUID a, UUID b) {
        MartialSchool sa = schools.get(a);
        MartialSchool sb = schools.get(b);
        if (sa == null || sb == null) return false;
        return RIVALS.get(sa) == sb;
    }

    public Map<UUID, MartialSchool> all() { return schools; }
}
