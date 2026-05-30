package kr.reborn.skill.combo;

import kr.reborn.core.util.Msg;
import kr.reborn.skill.def.SkillDef;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 스킬 콤보 트래커.
 *
 * 한 플레이어가 5초 이내에 연속 시전한 스킬 시퀀스를 추적.
 * 미리 정의된 콤보 시퀀스(예: ["fireball", "fire_chain", "meteor"])와 일치하면
 * 최종 스킬에 보너스 효과(damage ×2, 추가 폭발).
 *
 * 콤보 정의:
 *   - 같은 카테고리 3연속 → element bonus (각 카테고리 첫 명중에 +30%)
 *   - 같은 element 3연속 → 마지막 명중에 광역 폭발 1회
 *   - 다른 카테고리 5종 사용 → "만능" 칭호 1회 broadcast
 */
public final class ComboTracker {

    private static final long COMBO_WINDOW_MS = 5_000;
    private static final int MAX_CHAIN = 10;

    /** uuid → 최근 시전 기록 (timestamp, skillId, category, element) */
    private final Map<UUID, Deque<CastRecord>> history = new ConcurrentHashMap<>();
    /** uuid → 누적 다른 카테고리 set (만능 칭호) */
    private final Map<UUID, java.util.Set<String>> categoriesUsed = new ConcurrentHashMap<>();

    /** 시전 직후 호출 — 보너스 multiplier 반환. */
    public double onCast(Player p, SkillDef def) {
        long now = System.currentTimeMillis();
        Deque<CastRecord> dq = history.computeIfAbsent(p.getUniqueId(), k -> new java.util.ArrayDeque<>());
        // 5초 지난 기록 제거
        while (!dq.isEmpty() && now - dq.peekFirst().at > COMBO_WINDOW_MS) dq.pollFirst();
        dq.offerLast(new CastRecord(now, def.id, def.category, elementOf(def)));
        while (dq.size() > MAX_CHAIN) dq.pollFirst();

        double mult = 1.0;
        // 같은 카테고리 3연속
        if (sameLast(dq, 3, r -> r.category)) {
            mult *= 1.3;
            Msg.send(p, "&e&l같은 류 3연속 콤보! 데미지 +30%");
        }
        // 같은 element 3연속
        if (sameLast(dq, 3, r -> r.element)) {
            mult *= 1.2;
            Msg.send(p, "&5&l원소 3연속 콤보! 데미지 +20% + 폭발");
            try {
                p.getWorld().createExplosion(p.getLocation(), 2.0f, false, false);
            } catch (Throwable ignored) {}
        }
        // 다른 카테고리 5종 누적
        var set = categoriesUsed.computeIfAbsent(p.getUniqueId(), k -> new java.util.HashSet<>());
        set.add(def.category == null ? "?" : def.category);
        if (set.size() == 5) {
            Bukkit.broadcastMessage("§6§l[만능] §f" + p.getName()
                    + " §7가 5종 카테고리 스킬을 자유자재로 다룬다!");
            // 칭호 1회 — 영구 보너스 + 알림. 후속 호출 방지 위해 set에 sentinel 추가.
            set.add("__BROADCASTED__");
        }
        return mult;
    }

    private boolean sameLast(Deque<CastRecord> dq, int n, java.util.function.Function<CastRecord, String> fn) {
        if (dq.size() < n) return false;
        Object[] arr = dq.toArray();
        String first = fn.apply((CastRecord) arr[arr.length - 1]);
        if (first == null) return false;
        for (int i = arr.length - n; i < arr.length; i++) {
            if (!first.equals(fn.apply((CastRecord) arr[i]))) return false;
        }
        return true;
    }

    private String elementOf(SkillDef def) {
        // SkillDef에 element 필드가 없는 경우 카테고리로 추정
        try {
            var f = def.getClass().getField("element");
            Object v = f.get(def);
            return v == null ? null : v.toString();
        } catch (Throwable ignored) {}
        return def.category;
    }

    public Deque<CastRecord> recentOf(UUID p) {
        return history.getOrDefault(p, new java.util.ArrayDeque<>());
    }

    public static final class CastRecord {
        public final long at;
        public final String skillId;
        public final String category;
        public final String element;
        public CastRecord(long at, String skillId, String category, String element) {
            this.at = at; this.skillId = skillId;
            this.category = category; this.element = element;
        }
    }
}
