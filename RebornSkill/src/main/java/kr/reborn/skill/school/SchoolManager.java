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

    private static final String NS = "RebornSkill.school";

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

    private void persist(UUID p, MartialSchool ms) {
        try { RebornCore.get().kv().put(NS, p, "school", ms.name()); }
        catch (Throwable ignored) {}
    }

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
        persist(p.getUniqueId(), newSchool);
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
            // 같은 학파 NPC 호의 +30 (반경 50)
            np.getClass().getMethod("nudgeNearbyFavor",
                            Player.class, double.class, double.class)
                    .invoke(np, p, 50.0, 30.0);
            // 라이벌 학파 NPC 호의 -30 — 직접 라이벌 학파 가입자 그룹에 broadcast
            MartialSchool rival = RIVALS.get(ms);
            if (rival != null) {
                // 같은 서버에 라이벌 학파인 다른 플레이어가 있으면 NPC 호의도 영향
                // (구체 라이벌 NPC 직접 -30은 NPC 학파 정보 부족으로 일단 글로벌 -3 으로 근사)
                np.getClass().getMethod("nudgeGlobalFavor", double.class)
                        .invoke(np, -3.0);
            }
        } catch (Throwable ignored) {}
    }

    public MartialSchool of(UUID p) {
        MartialSchool ms = schools.get(p);
        if (ms != null) return ms;
        String stored = RebornCore.get().kv().get(NS, p, "school");
        if (stored != null) {
            try {
                ms = MartialSchool.valueOf(stored);
                schools.put(p, ms);
                return ms;
            } catch (IllegalArgumentException ignored) {}
        }
        return null;
    }

    public boolean areRivals(UUID a, UUID b) {
        // schools 캐시 비어있으면 KV에서 로드 (lazy)
        MartialSchool sa = of(a);
        MartialSchool sb = of(b);
        if (sa == null || sb == null) return false;
        return RIVALS.get(sa) == sb;
    }

    public Map<UUID, MartialSchool> all() { return schools; }
}
