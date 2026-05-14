package kr.reborn.skill.create;

import kr.reborn.skill.RebornSkill;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * "AI가 판단하여 스킬로 창조" — 행동 패턴 카운팅 + 임계치 도달 시 알림.
 * 실제 자동 매칭 템플릿은 config.yml에 추가 가능.
 */
public final class SkillCreator {

    private final RebornSkill plugin;
    private final Map<UUID, Map<String, Integer>> patternCount = new HashMap<>();

    public SkillCreator(RebornSkill p) { this.plugin = p; }

    /** 행동 로그 1건 추가. 동일 patternKey가 100회 이상 누적되면 통보. */
    public void log(Player p, String patternKey) {
        var m = patternCount.computeIfAbsent(p.getUniqueId(), x -> new HashMap<>());
        int n = m.merge(patternKey, 1, Integer::sum);
        if (n == 100) {
            p.sendMessage("§5[스킬 창조 후보] §f반복 패턴 감지: " + patternKey);
            // TODO: 사전 템플릿 매칭 시 자동 생성. 현재는 관리자 알림.
            org.bukkit.Bukkit.getOnlinePlayers().stream()
                    .filter(Player::isOp)
                    .forEach(op -> op.sendMessage("§5[AI] §f" + p.getName() + " 행동 패턴: " + patternKey + " (100회)"));
        }
    }
}
