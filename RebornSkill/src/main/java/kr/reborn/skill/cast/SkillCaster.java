package kr.reborn.skill.cast;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.util.Msg;
import kr.reborn.skill.RebornSkill;
import kr.reborn.skill.def.SkillDef;
import kr.reborn.skill.effect.EffectExecutor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SkillCaster {

    private final RebornSkill plugin;
    private final EffectExecutor effects;
    /** 쿨타임 만료 시각 (밀리초) */
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    public SkillCaster(RebornSkill p) {
        this.plugin = p;
        this.effects = new EffectExecutor(p);
    }

    public void cast(Player p, String skillId) {
        SkillDef def = plugin.registry().get(skillId);
        if (def == null) { Msg.error(p, "스킬 정보 없음: " + skillId); return; }
        if (!plugin.store().has(p.getUniqueId(), skillId)) {
            Msg.error(p, "이 스킬을 보유하고 있지 않다."); return;
        }
        long now = System.currentTimeMillis();
        long cdEnd = cooldowns.computeIfAbsent(p.getUniqueId(), x -> new HashMap<>())
                .getOrDefault(skillId, 0L);
        if (now < cdEnd) {
            Msg.warn(p, "쿨타임 " + (cdEnd - now) / 1000 + "초"); return;
        }
        if (!plugin.energy().consume(p, def.costType, def.costAmount)) {
            Msg.error(p, "에너지 부족 — 필요 " + def.costType + " " + def.costAmount); return;
        }
        cooldowns.get(p.getUniqueId()).put(skillId, now + (long) (def.cooldownSeconds * 1000));

        long castTicks = (long) (def.castSeconds * 20);
        if (castTicks > 0) beginCast(p, def, castTicks);
        else applyEffect(p, def);
    }

    /** 캐스팅 — 진행 바 표시 + 피격 시 시전 중단. */
    private void beginCast(Player p, SkillDef def, long totalTicks) {
        Msg.send(p, "&7" + def.name + " &8시전 시작...");
        castStep(p, def, totalTicks, 0, p.getHealth());
    }

    private void castStep(Player p, SkillDef def, long total, long elapsed, double startHp) {
        if (!p.isOnline()) return;
        if (p.getHealth() < startHp - 0.5) {  // 피격 → 중단
            actionBar(p, "§c✘ 시전 중단!");
            Msg.warn(p, "피격으로 시전이 중단되었다.");
            return;
        }
        if (elapsed >= total) {
            actionBar(p, "");
            applyEffect(p, def);
            return;
        }
        actionBar(p, castBar(def.name, (double) elapsed / total));
        long step = Math.max(2, total / 10);
        RebornCore.get().scheduler().runTaskLater(
                () -> castStep(p, def, total, elapsed + step, startHp), step);
    }

    /** 데미지 계산(숙련도 포함) 후 종류별 효과 실행. 초식이 있으면 적용. */
    private void applyEffect(Player p, SkillDef def) {
        PlayerData d = RebornCore.get().api().getPlayerData(p.getUniqueId());
        double power = Formula.eval(def.damageFormula, d);
        // 숙련도 보정
        int prof = plugin.store().prof(p.getUniqueId(), def.id);
        if (prof >= 76) power *= 1.20;
        else if (prof >= 51) power *= 1.10;
        plugin.store().addProf(p.getUniqueId(), def.id, 1);

        // 초식 선택 — 비급에 초식 데이터가 있으면 다음 초식을 골라 배수·속성 적용
        var tech = plugin.techniques().nextFor(p.getUniqueId(), def.id, prof);
        if (tech != null) {
            power *= tech.mult;
            Msg.send(p, "&5[" + def.name + "] §d→ §f" + tech.name);
            if (!tech.description.isEmpty()) Msg.send(p, "&7  " + tech.description);
            if (tech.elementOverride != null) {
                // 속성 덮어쓰기 — 임시 SkillDef를 생성해 EffectExecutor에 넘김
                SkillDef shadow = withElement(def, tech.elementOverride);
                // 콤보 배수 적용
                power *= plugin.combo().onCast(p, shadow);
                effects.execute(p, shadow, power);
                return;
            }
        }
        // 콤보 배수 적용
        power *= plugin.combo().onCast(p, def);
        effects.execute(p, def, power);
    }

    /** 초식이 속성을 덮어쓸 때 사용 — 한 번 캐스팅에만 쓰이는 그림자 SkillDef. */
    private SkillDef withElement(SkillDef d, String element) {
        return new SkillDef(d.id, d.name, d.world, d.category, d.costType, d.costAmount,
                d.cooldownSeconds, d.castSeconds, d.damageFormula, element, d.learnMethod,
                d.type, d.radius, d.range, d.projectileSpeed, d.durationTicks, d.summonMob);
    }

    private String castBar(String name, double progress) {
        int filled = (int) Math.round(progress * 10);
        StringBuilder sb = new StringBuilder("§e" + name.replaceAll("&.", "") + " §8[");
        for (int i = 0; i < 10; i++) sb.append(i < filled ? "§a▰" : "§7▱");
        sb.append("§8]");
        return sb.toString();
    }

    private void actionBar(Player p, String text) {
        try {
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacyText(text));
        } catch (Throwable ignored) {}
    }
}
