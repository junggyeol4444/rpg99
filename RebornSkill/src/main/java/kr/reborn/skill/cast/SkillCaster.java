package kr.reborn.skill.cast;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.util.Msg;
import kr.reborn.skill.RebornSkill;
import kr.reborn.skill.def.SkillDef;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SkillCaster {

    private final RebornSkill plugin;
    /** 쿨타임 만료 시각 (밀리초) */
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    public SkillCaster(RebornSkill p) { this.plugin = p; }

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

        // 캐스팅 시간 (피격 시 캔슬은 단순화 위해 생략)
        long castTicks = (long) (def.castSeconds * 20);
        Runnable apply = () -> applyEffect(p, def);
        if (castTicks > 0) {
            Msg.send(p, "&7시전 중...");
            RebornCore.get().scheduler().runTaskLater(apply, castTicks);
        } else apply.run();
    }

    private void applyEffect(Player p, SkillDef def) {
        PlayerData d = RebornCore.get().api().getPlayerData(p.getUniqueId());
        double dmg = Formula.eval(def.damageFormula, d);
        // 숙련도 보정
        int prof = plugin.store().prof(p.getUniqueId(), def.id);
        if (prof >= 76) dmg *= 1.20;
        else if (prof >= 51) dmg *= 1.10;
        plugin.store().addProf(p.getUniqueId(), def.id, 1);

        Msg.send(p, "&b" + def.name + " &f시전!");

        // 음수 = 회복
        if (dmg < 0) {
            p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() - dmg));
            return;
        }
        // 시야 6블록 내 가장 가까운 살아있는 엔티티에게 적용
        Entity target = null;
        double best = Double.MAX_VALUE;
        Vector dir = p.getLocation().getDirection();
        for (Entity e : p.getNearbyEntities(8, 4, 8)) {
            if (e == p) continue;
            if (!(e instanceof LivingEntity)) continue;
            Vector to = e.getLocation().toVector().subtract(p.getLocation().toVector()).normalize();
            double dot = dir.dot(to);
            if (dot < 0.6) continue;
            double dist = e.getLocation().distance(p.getLocation());
            if (dist < best) { best = dist; target = e; }
        }
        if (target instanceof LivingEntity le) {
            le.damage(dmg, p);
            Msg.send(p, "&c" + le.getName() + " &7→ " + String.format("%.1f", dmg) + " 피해");
        } else {
            Msg.warn(p, "대상이 없다.");
        }
    }
}
