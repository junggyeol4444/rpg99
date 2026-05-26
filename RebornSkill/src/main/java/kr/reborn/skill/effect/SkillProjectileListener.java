package kr.reborn.skill.effect;

import kr.reborn.skill.RebornSkill;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.metadata.MetadataValue;

/**
 * 스킬 투사체(ProjectileEffect)가 엔티티에 명중하면 메타데이터 기반으로 피해를 적용한다.
 *
 * Snowball/SmallFireball은 기본 피해가 미미하므로, 우리가 부여한 reborn_dmg를
 * Element 상성·상태이상과 함께 직접 적용한다.
 */
public final class SkillProjectileListener implements Listener {

    private final RebornSkill plugin;

    public SkillProjectileListener(RebornSkill plugin) { this.plugin = plugin; }

    @EventHandler
    public void onHit(ProjectileHitEvent e) {
        Projectile proj = e.getEntity();
        if (!proj.hasMetadata("reborn_dmg")) return;
        if (!(e.getHitEntity() instanceof LivingEntity target)) return;
        if (!(proj.getShooter() instanceof Player caster)) return;

        double base = meta(proj, "reborn_dmg");
        String elem = metaStr(proj, "reborn_elem");
        double mult = Element.multiplier(elem, target);
        target.damage(base * mult, caster);
        Element.applyStatus(target, elem, base);

        String tag = target.getCustomName() != null ? target.getCustomName() : target.getType().name();
        String suffix = mult > 1.0 ? " §a상성 우위 ×" + String.format("%.1f", mult)
                : mult < 1.0 ? " §c상성 불리 ×" + String.format("%.1f", mult) : "";
        kr.reborn.core.util.Msg.send(caster,
                "&b" + tag + " &7→ " + String.format("%.1f", base * mult) + " 피해" + suffix);
        proj.remove();
    }

    private double meta(Projectile p, String key) {
        for (MetadataValue v : p.getMetadata(key)) {
            if (v.getOwningPlugin() == plugin) return v.asDouble();
        }
        return 0;
    }

    private String metaStr(Projectile p, String key) {
        for (MetadataValue v : p.getMetadata(key)) {
            if (v.getOwningPlugin() == plugin) return v.asString();
        }
        return "";
    }
}
