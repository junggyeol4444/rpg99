package kr.reborn.skill.signature;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * 시그니처 실행 엔진 — pattern별 실제 입자/사운드 렌더링.
 *
 * 각 패턴은 독자적인 시각 효과:
 *   RING: 시전자 둘레 원
 *   BEAM: 직선 입자 빔
 *   SPIRAL: 나선
 *   EXPLOSION: 중심 폭발
 *   STORM: 무작위 다수 입자
 *   METEOR_RAIN: 하늘에서 떨어지는 돌
 *   SLASH_ARC: 검기 호 (앞 90도 부채꼴)
 *   AURA_OUT: 자기 중심 발산 (위로 솟구침)
 *   AURA_IN: 시전자로 흡수
 *   WAVE_FRONT: 정면 파동 (밀어내기)
 *   TWIN_BEAM: 양손 쌍열 빔
 *   VERTICAL_PILLAR: 수직 기둥
 *   STAR_BURST: 별 모양 폭발
 *   DRAGON_BREATH: 콘 형태 화염
 *   SOUL_DRAIN: 대상→시전자 흐름
 *   TIME_RIPPLE: 시간 파동 (다중 원)
 */
public final class SignatureExecutor {

    /** 시전 시각 효과 — SkillSignature 기반. */
    public void renderCast(Player caster, SkillSignature sig) {
        if (sig == null) return;
        Location origin = caster.getLocation().add(0, 1, 0);
        // 사운드
        if (sig.castSound != null) {
            try { caster.getWorld().playSound(origin, sig.castSound, 1.0f, 1.0f); }
            catch (Throwable ignored) {}
        }
        // 패턴별 렌더
        switch (sig.primaryPattern) {
            case RING -> renderRing(origin, sig.primaryParticle);
            case BEAM -> renderBeam(caster, sig.primaryParticle);
            case SPIRAL -> renderSpiral(origin, sig.primaryParticle);
            case EXPLOSION -> renderExplosion(origin, sig.primaryParticle);
            case STORM -> renderStorm(caster, sig.primaryParticle);
            case METEOR_RAIN -> renderMeteorRain(caster, sig.primaryParticle);
            case SLASH_ARC -> renderSlashArc(caster, sig.primaryParticle);
            case AURA_OUT -> renderAuraOut(origin, sig.primaryParticle);
            case AURA_IN -> renderAuraIn(origin, sig.primaryParticle);
            case WAVE_FRONT -> renderWaveFront(caster, sig.primaryParticle);
            case TWIN_BEAM -> renderTwinBeam(caster, sig.primaryParticle);
            case VERTICAL_PILLAR -> renderVerticalPillar(origin, sig.primaryParticle);
            case STAR_BURST -> renderStarBurst(origin, sig.primaryParticle);
            case DRAGON_BREATH -> renderDragonBreath(caster, sig.primaryParticle);
            case SOUL_DRAIN -> renderSoulDrain(caster, sig.primaryParticle);
            case TIME_RIPPLE -> renderTimeRipple(origin, sig.primaryParticle);
        }
        // 보조 입자
        if (sig.secondaryParticle != null) {
            try { caster.getWorld().spawnParticle(sig.secondaryParticle, origin, 30, 0.5, 0.5, 0.5, 0.05); }
            catch (Throwable ignored) {}
        }
    }

    /** 명중 시각 효과. */
    public void renderHit(Player caster, LivingEntity target, SkillSignature sig) {
        if (sig == null) return;
        Location hit = target.getLocation().add(0, 1, 0);
        if (sig.hitSound != null) {
            try { hit.getWorld().playSound(hit, sig.hitSound, 1.0f, 1.0f); }
            catch (Throwable ignored) {}
        }
        try {
            hit.getWorld().spawnParticle(sig.primaryParticle, hit, 30, 0.5, 0.5, 0.5, 0.1);
            if (sig.secondaryParticle != null) {
                hit.getWorld().spawnParticle(sig.secondaryParticle, hit, 15, 0.3, 0.3, 0.3, 0.05);
            }
        } catch (Throwable ignored) {}
        // 추가 상태이상 부여
        for (var status : sig.extraStatuses) {
            try {
                target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        status, sig.statusDuration, sig.statusAmplifier));
            } catch (Throwable ignored) {}
        }
    }

    /* ───────────── 패턴별 렌더 구현 ───────────── */

    private void renderRing(Location c, Particle p) {
        for (int a = 0; a < 360; a += 10) {
            double rad = Math.toRadians(a);
            Location point = c.clone().add(Math.cos(rad) * 2, 0, Math.sin(rad) * 2);
            try { c.getWorld().spawnParticle(p, point, 2, 0.05, 0.05, 0.05, 0); }
            catch (Throwable ignored) {}
        }
    }

    private void renderBeam(Player caster, Particle p) {
        Location origin = caster.getEyeLocation();
        Vector dir = origin.getDirection().normalize();
        for (int i = 0; i < 30; i++) {
            Location point = origin.clone().add(dir.clone().multiply(i));
            try { caster.getWorld().spawnParticle(p, point, 3, 0.1, 0.1, 0.1, 0); }
            catch (Throwable ignored) {}
        }
    }

    private void renderSpiral(Location c, Particle p) {
        for (double y = 0; y < 4; y += 0.2) {
            double rad = y * 2;
            double r = 1.5 - y * 0.3;
            Location point = c.clone().add(Math.cos(rad) * r, y, Math.sin(rad) * r);
            try { c.getWorld().spawnParticle(p, point, 2, 0, 0, 0, 0); }
            catch (Throwable ignored) {}
        }
    }

    private void renderExplosion(Location c, Particle p) {
        try { c.getWorld().spawnParticle(p, c, 80, 2, 1.5, 2, 0.1); }
        catch (Throwable ignored) {}
        try { c.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, c, 3, 0.5, 0.5, 0.5); }
        catch (Throwable ignored) {}
    }

    private void renderStorm(Player caster, Particle p) {
        Location origin = caster.getLocation();
        for (int i = 0; i < 50; i++) {
            double dx = (Math.random() - 0.5) * 12;
            double dy = Math.random() * 4;
            double dz = (Math.random() - 0.5) * 12;
            try { origin.getWorld().spawnParticle(p, origin.clone().add(dx, dy, dz), 3, 0.1, 0.1, 0.1, 0.05); }
            catch (Throwable ignored) {}
        }
    }

    private void renderMeteorRain(Player caster, Particle p) {
        Location origin = caster.getLocation();
        for (int i = 0; i < 12; i++) {
            double dx = (Math.random() - 0.5) * 20;
            double dz = (Math.random() - 0.5) * 20;
            Location top = origin.clone().add(dx, 15, dz);
            Location bottom = origin.clone().add(dx, 0, dz);
            // 떨어지는 입자 라인
            for (double y = top.getY(); y > bottom.getY(); y -= 1) {
                Location point = top.clone();
                point.setY(y);
                try { origin.getWorld().spawnParticle(p, point, 2, 0.2, 0, 0.2, 0); }
                catch (Throwable ignored) {}
            }
        }
    }

    private void renderSlashArc(Player caster, Particle p) {
        Location origin = caster.getEyeLocation();
        Vector forward = origin.getDirection().normalize();
        Vector right = new Vector(-forward.getZ(), 0, forward.getX()).normalize();
        for (int a = -60; a <= 60; a += 6) {
            double rad = Math.toRadians(a);
            Vector v = forward.clone().multiply(Math.cos(rad)).add(right.clone().multiply(Math.sin(rad))).multiply(2);
            Location point = origin.clone().add(v);
            try { caster.getWorld().spawnParticle(p, point, 4, 0.1, 0.1, 0.1, 0); }
            catch (Throwable ignored) {}
        }
    }

    private void renderAuraOut(Location c, Particle p) {
        for (int i = 0; i < 80; i++) {
            double rad = Math.random() * Math.PI * 2;
            double r = Math.random() * 3;
            double y = Math.random() * 3;
            Location point = c.clone().add(Math.cos(rad) * r, y, Math.sin(rad) * r);
            try { c.getWorld().spawnParticle(p, point, 1, 0, 0.1, 0, 0.02); }
            catch (Throwable ignored) {}
        }
    }

    private void renderAuraIn(Location c, Particle p) {
        for (int i = 0; i < 60; i++) {
            double rad = Math.random() * Math.PI * 2;
            double r = 3 - (i / 60.0) * 3;
            double y = Math.random() * 3;
            Location point = c.clone().add(Math.cos(rad) * r, y, Math.sin(rad) * r);
            try { c.getWorld().spawnParticle(p, point, 1, 0, 0, 0, 0); }
            catch (Throwable ignored) {}
        }
    }

    private void renderWaveFront(Player caster, Particle p) {
        Location origin = caster.getLocation();
        Vector forward = origin.getDirection().setY(0).normalize();
        for (double r = 0; r < 8; r += 0.5) {
            Vector point = forward.clone().multiply(r);
            for (int side = -3; side <= 3; side++) {
                Vector rightV = new Vector(-forward.getZ(), 0, forward.getX()).multiply(side);
                Location loc = origin.clone().add(point).add(rightV).add(0, 1, 0);
                try { origin.getWorld().spawnParticle(p, loc, 1, 0, 0, 0, 0); }
                catch (Throwable ignored) {}
            }
        }
    }

    private void renderTwinBeam(Player caster, Particle p) {
        Location origin = caster.getEyeLocation();
        Vector dir = origin.getDirection().normalize();
        Vector right = new Vector(-dir.getZ(), 0, dir.getX()).normalize().multiply(0.5);
        for (int i = 0; i < 30; i++) {
            Location left = origin.clone().add(dir.clone().multiply(i)).add(right.clone().multiply(-1));
            Location rightP = origin.clone().add(dir.clone().multiply(i)).add(right);
            try {
                caster.getWorld().spawnParticle(p, left, 2, 0.1, 0.1, 0.1, 0);
                caster.getWorld().spawnParticle(p, rightP, 2, 0.1, 0.1, 0.1, 0);
            } catch (Throwable ignored) {}
        }
    }

    private void renderVerticalPillar(Location c, Particle p) {
        for (double y = 0; y < 20; y += 0.3) {
            Location point = c.clone().add(0, y, 0);
            try { c.getWorld().spawnParticle(p, point, 4, 0.4, 0, 0.4, 0); }
            catch (Throwable ignored) {}
        }
    }

    private void renderStarBurst(Location c, Particle p) {
        // 5각형 별 모양
        for (int a = 0; a < 360; a += 72) {
            double rad = Math.toRadians(a);
            for (double r = 0; r < 4; r += 0.3) {
                Location point = c.clone().add(Math.cos(rad) * r, 0.5, Math.sin(rad) * r);
                try { c.getWorld().spawnParticle(p, point, 2, 0.05, 0.05, 0.05, 0); }
                catch (Throwable ignored) {}
            }
        }
        try { c.getWorld().spawnParticle(p, c, 50, 2, 1, 2, 0.1); }
        catch (Throwable ignored) {}
    }

    private void renderDragonBreath(Player caster, Particle p) {
        Location origin = caster.getEyeLocation();
        Vector forward = origin.getDirection().normalize();
        for (double d = 1; d < 12; d += 0.5) {
            for (int i = 0; i < 5; i++) {
                double offset = d * 0.2;
                double dx = (Math.random() - 0.5) * offset;
                double dy = (Math.random() - 0.5) * offset;
                double dz = (Math.random() - 0.5) * offset;
                Location point = origin.clone().add(forward.clone().multiply(d)).add(dx, dy, dz);
                try { caster.getWorld().spawnParticle(p, point, 1, 0, 0, 0, 0.01); }
                catch (Throwable ignored) {}
            }
        }
    }

    private void renderSoulDrain(Player caster, Particle p) {
        Location origin = caster.getEyeLocation();
        Vector dir = origin.getDirection().normalize();
        // 직선 라인 (drain 흐름)
        for (int i = 0; i < 15; i++) {
            Location point = origin.clone().add(dir.clone().multiply(i));
            try { caster.getWorld().spawnParticle(p, point, 4, 0.2, 0.2, 0.2, 0.05); }
            catch (Throwable ignored) {}
        }
    }

    private void renderTimeRipple(Location c, Particle p) {
        // 3겹의 원
        for (double r = 1; r <= 5; r += 1.5) {
            for (int a = 0; a < 360; a += 10) {
                double rad = Math.toRadians(a);
                Location point = c.clone().add(Math.cos(rad) * r, 0, Math.sin(rad) * r);
                try { c.getWorld().spawnParticle(p, point, 1, 0, 0, 0, 0); }
                catch (Throwable ignored) {}
            }
        }
    }
}
