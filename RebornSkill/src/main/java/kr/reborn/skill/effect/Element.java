package kr.reborn.skill.effect;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.Set;

/**
 * 속성 상성 + 속성별 부가 효과.
 *
 * 상성: 강점 속성으로 때리면 ×1.5, 약점이면 ×0.66 (그 외 ×1.0).
 * 대상의 "속성"은 몹 종류로 추정 (블레이즈=불, 좀비=암흑 등).
 * 부가 효과: 불=연소, 빙=둔화, 독=중독, 암흑=위더, 번개=낙뢰 등.
 */
public final class Element {

    private Element() {}

    /** atk 속성이 강한 대상 속성들. (atk → 강점 대상 집합) */
    private static final Map<String, Set<String>> STRONG = Map.of(
            "FIRE",      Set.of("ICE", "NATURE"),
            "ICE",       Set.of("NATURE", "WIND"),
            "WATER",     Set.of("FIRE"),
            "NATURE",    Set.of("WATER", "EARTH"),
            "LIGHTNING", Set.of("WATER", "WIND"),
            "HOLY",      Set.of("DARK"),
            "DARK",      Set.of("HOLY"),
            "POISON",    Set.of("NATURE")
    );

    /** 몹 종류 → 추정 속성. */
    private static final Set<EntityType> UNDEAD = Set.of(
            EntityType.ZOMBIE, EntityType.ZOMBIE_VILLAGER, EntityType.HUSK, EntityType.DROWNED,
            EntityType.SKELETON, EntityType.STRAY, EntityType.WITHER_SKELETON, EntityType.WITHER,
            EntityType.PHANTOM, EntityType.ZOMBIFIED_PIGLIN, EntityType.ZOGLIN, EntityType.SKELETON_HORSE);

    public static String inferTargetElement(LivingEntity e) {
        switch (e.getType()) {
            case BLAZE: case MAGMA_CUBE: case GHAST: case STRIDER: return "FIRE";
            case SNOW_GOLEM: return "ICE";
            case GUARDIAN: case ELDER_GUARDIAN: case SQUID: case GLOW_SQUID:
            case DOLPHIN: case TURTLE: case COD: case SALMON: case PUFFERFISH: return "WATER";
            case SPIDER: case CAVE_SPIDER: case BEE: case SILVERFISH: return "POISON";
            default:
                if (UNDEAD.contains(e.getType())) return "DARK";
                return "PHYSICAL";
        }
    }

    /** 공격 속성 vs 대상 → 데미지 배수. */
    public static double multiplier(String atkElement, LivingEntity target) {
        if (atkElement == null) return 1.0;
        String atk = atkElement.toUpperCase();
        String def = inferTargetElement(target);
        // 신성 vs 언데드는 항상 강점
        if (atk.equals("HOLY") && UNDEAD.contains(target.getType())) return 1.5;
        Set<String> strongFor = STRONG.get(atk);
        if (strongFor != null && strongFor.contains(def)) return 1.5;
        Set<String> defStrong = STRONG.get(def);
        if (defStrong != null && defStrong.contains(atk)) return 0.66;
        return 1.0;
    }

    /** 속성별 상태이상 부여. power로 강도/지속 스케일. */
    public static void applyStatus(LivingEntity target, String element, double power) {
        if (element == null) return;
        try {
            int dur = (int) Math.min(140, 40 + power);  // 2~7초
            switch (element.toUpperCase()) {
                case "FIRE":
                    target.setFireTicks(Math.max(target.getFireTicks(), (int) Math.min(120, 40 + power)));
                    break;
                case "ICE": case "WATER":
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, dur, 1));
                    break;
                case "POISON": case "NATURE":
                    target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, dur, 0));
                    break;
                case "DARK":
                    target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, dur, 0));
                    break;
                case "LIGHTNING":
                    if (target.getWorld() != null) target.getWorld().strikeLightningEffect(target.getLocation());
                    break;
                case "HOLY":
                    target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, dur, 0));
                    break;
                case "ARCANE":
                    target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, dur, 0));
                    break;
                default: break;
            }
        } catch (Throwable ignored) {}
    }
}
