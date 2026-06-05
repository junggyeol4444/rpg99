package kr.reborn.craft.listener;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.StatType;
import kr.reborn.core.util.Msg;
import kr.reborn.craft.RebornCraft;
import kr.reborn.craft.data.CustomItem;
import kr.reborn.craft.event.RebornItemConsumeEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ConsumeListener implements Listener {

    private final RebornCraft plugin;
    /** uuid_itemId → 마지막 사용 시각 ms */
    private final Map<String, Long> cooldowns = new HashMap<>();

    public ConsumeListener(RebornCraft plugin) { this.plugin = plugin; }

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (!e.getAction().isRightClick()) return;
        ItemStack stack = e.getItem();
        if (stack == null) return;
        CustomItem ci = plugin.items().ofItem(stack);
        if (ci == null || ci.type != CustomItem.Type.CONSUMABLE) return;

        Player p = e.getPlayer();
        String key = p.getUniqueId() + "_" + ci.id;
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(key);
        if (last != null && ci.consumeCooldownSeconds > 0
                && now - last < ci.consumeCooldownSeconds * 1000L) {
            Msg.warn(p, "쿨타임이 끝나지 않았습니다.");
            e.setCancelled(true);
            return;
        }
        cooldowns.put(key, now);

        applyConsumeEffect(p, ci);

        // 1개 소모
        stack.setAmount(stack.getAmount() - 1);
        Bukkit.getPluginManager().callEvent(new RebornItemConsumeEvent(p, ci));
        e.setCancelled(true);
    }

    /** 영약·단약 효과 분기. */
    private void applyConsumeEffect(Player p, CustomItem ci) {
        if (ci.consumeType == null) return;
        double v = ci.consumeValue instanceof Number n ? n.doubleValue() : 0;
        String src = "ITEM:" + ci.id;
        UUID u = p.getUniqueId();
        switch (ci.consumeType) {
            case HEAL -> {
                p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + v));
                Msg.send(p, "&a회복: +" + (int) v);
            }
            case STAT_BOOST, ADD_ALL_COMMON -> {
                for (StatType st : StatType.COMMON_8) {
                    RebornCore.get().api().addStat(u, st, v, src);
                }
                Msg.send(p, "&b전 스탯 +" + (int) v);
            }
            case ADD_STAT -> {
                if (ci.consumeStat != null) {
                    RebornCore.get().api().addStat(u, ci.consumeStat, v, src);
                    Msg.send(p, "&b" + ci.consumeStat.name() + " +" + (int) v);
                }
            }
            case ADD_MULTI -> {
                for (var e : ci.consumeMultiStats.entrySet()) {
                    RebornCore.get().api().addStat(u, e.getKey(), e.getValue(), src);
                }
                Msg.send(p, "&b" + ci.consumeMultiStats.size() + "개 스탯 상승");
            }
            case ADD_RANDOM_COMMON -> {
                double bonus = ci.consumeMin + Math.random() * (ci.consumeMax - ci.consumeMin);
                for (StatType st : StatType.COMMON_8) {
                    RebornCore.get().api().addStat(u, st, bonus, src);
                }
                Msg.send(p, "&b각성 — 전 스탯 +" + (int) bonus);
            }
            case BUFF -> applyBuff(p, ci, v, false);
            case DEBUFF -> applyBuff(p, ci, v, true);
            case CURE_CURSE -> tryCureCurse(p, String.valueOf(ci.consumeValue));
            case ANTI_PARANOIA -> {
                RebornCore.get().api().addStat(u, StatType.MENTAL, v * 100, src);
                Msg.send(p, "&b심마 저항 — 정신 안정");
            }
            case RESTORE_MERIDIAN -> tryRestoreMeridian(p);
            case BUFF_RECIPE -> markStatus(p, "buff_recipe", (long) (ci.consumeDuration > 0 ? ci.consumeDuration : 3600));
            case BUFF_TRAIN -> markStatus(p, "buff_train", (long) (ci.consumeDuration > 0 ? ci.consumeDuration : 3600));
            case ALL_ELEMENTS -> tryBoostAllElements(p, v);
            case TIER_UP -> tryTierUp(p);
            case TIER_UP_CIRCLE -> tryTierUpCircle(p);
            case STOP_AGING -> markStatus(p, "stop_aging", 86400L * 30);
            case TRIBULATION_BOOST -> markStatus(p, "tribulation_boost", 3600L);
            case REVIVE -> markStatus(p, "revive_charge", 86400L * 7);
            case LEARN_SKILL -> tryLearnSkill(p, String.valueOf(ci.consumeValue));
            case LEARN_RANDOM_SPIRIT_SKILL -> tryLearnRandomSpiritSkill(p);
            case CUSTOM -> {
                if (ci.id != null && (ci.id.endsWith("_pill") || ci.id.endsWith("_pil"))) {
                    tryConsumePill(p, ci.id);
                }
            }
            case CURE, BLESS, ENERGY -> {
                // 일반 회복/축복/기력 — STAT_BOOST와 동일 처리
                for (StatType st : StatType.COMMON_8) {
                    RebornCore.get().api().addStat(u, st, v * 0.5, src);
                }
            }
        }
    }

    private void applyBuff(Player p, CustomItem ci, double v, boolean negative) {
        if (ci.consumeStat == null) return;
        double effective = negative ? -Math.abs(v) : Math.abs(v);
        long sec = ci.consumeDuration > 0 ? ci.consumeDuration : 600;
        RebornCore.get().api().addStat(p.getUniqueId(), ci.consumeStat, effective, "BUFF:" + ci.id);
        markStatus(p, "buff:" + ci.id + ":" + ci.consumeStat.name(), sec);
        Msg.send(p, (negative ? "&c" : "&b") + ci.consumeStat.name() + " "
                + (effective >= 0 ? "+" : "") + (int) effective + " (" + sec + "s)");
    }

    private void markStatus(Player p, String key, long durationSec) {
        try {
            var d = RebornCore.get().api().getPlayerData(p.getUniqueId());
            if (d != null) {
                long ticks = durationSec > 0 ? durationSec * 20L : Long.MAX_VALUE / 2;
                d.status().put(key,
                        new kr.reborn.core.data.PlayerData.StatusEffect(key, "BLESSING", ticks, 1));
            }
        } catch (Throwable ignored) {}
    }

    private void tryCureCurse(Player p, String curseType) {
        try {
            var cp = Bukkit.getPluginManager().getPlugin("RebornCurse");
            if (cp != null) {
                Object effects = cp.getClass().getMethod("effects").invoke(cp);
                // PlayerEffectManager.cure(Player, String)
                effects.getClass().getMethod("cure", Player.class, String.class)
                        .invoke(effects, p, curseType);
                Msg.send(p, "&a저주 정화: " + curseType);
            }
        } catch (Throwable ignored) {
            // Curse 플러그인 미설치 시 정화 마커만
            markStatus(p, "cured:" + curseType, 86400L);
        }
    }

    private void tryRestoreMeridian(Player p) {
        // 경맥 회복 — RebornSkill에 별도 시스템이 없으므로 PlayerData 마커만 사용
        // (외부 모듈이 status:meridian_restored 확인 가능)
        markStatus(p, "meridian_restored", 3600L);
        Msg.send(p, "&5경맥 회복");
    }

    private void tryBoostAllElements(Player p, double value) {
        // 오행단 — SpiritGrowth 거주자만 효과. 비-정령계는 마커만.
        try {
            var d = RebornCore.get().api().getPlayerData(p.getUniqueId());
            if (d == null) return;
            if (d.worldKey() != kr.reborn.core.data.WorldKey.SPIRIT) {
                Msg.send(p, "&6오행 — 정령계 외에서는 약효 절반.");
                RebornCore.get().api().addStat(p.getUniqueId(),
                        kr.reborn.core.data.StatType.SPIRIT_POWER, value * 50, "ITEM:five-element");
                return;
            }
            var sp = Bukkit.getPluginManager().getPlugin("RebornStat");
            if (sp == null) return;
            Object growth = sp.getClass().getMethod("growth").invoke(sp);
            Object strategy = growth.getClass().getMethod("of",
                    kr.reborn.core.data.WorldKey.class).invoke(growth, d.worldKey());
            if (strategy == null) return;
            Class<?> elementCls = Class.forName(
                    "kr.reborn.stat.growth.impl.SpiritGrowth$Element");
            Object[] elements = elementCls.getEnumConstants();
            var absorb = strategy.getClass().getMethod("absorbEssence",
                    Player.class, elementCls, double.class);
            for (Object el : elements) {
                // CHAOS 제외 — 일반 영약으로 카오스 친화도는 안 오름
                if ("CHAOS".equals(el.toString())) continue;
                absorb.invoke(strategy, p, el, value * 10);
            }
            Msg.send(p, "&6오행단 — 모든 원소 친화 +" + (int)(value * 10));
        } catch (Throwable ignored) {}
    }

    private void tryTierUp(Player p) {
        // 직접 경지 강제 승급은 위험 — 자연스러운 승급을 유도.
        // 보너스 스탯 부여 후 자동 승급 체크.
        try {
            var d = RebornCore.get().api().getPlayerData(p.getUniqueId());
            if (d == null) return;
            for (var st : kr.reborn.core.data.StatType.COMMON_8) {
                RebornCore.get().api().addStat(p.getUniqueId(), st, 100, "ITEM:gujeon-geumdan");
            }
            RebornCore.get().tierManager().checkAndAdvance(p, d);
            Msg.send(p, "&6구전금단 — 모든 스탯 +100, 경지 자동 평가");
        } catch (Throwable ignored) {}
    }

    private void tryTierUpCircle(Player p) {
        try {
            var d = RebornCore.get().api().getPlayerData(p.getUniqueId());
            if (d != null) {
                double cur = RebornCore.get().api().getStat(p.getUniqueId(), StatType.MANA);
                RebornCore.get().api().addStat(p.getUniqueId(), StatType.MANA,
                        Math.max(100, cur * 0.2), "ITEM:circle_up");
                Msg.send(p, "&5써클 돌파의 자격을 획득했다");
            }
        } catch (Throwable ignored) {}
    }

    private void tryLearnSkill(Player p, String skill) {
        try {
            var sp = Bukkit.getPluginManager().getPlugin("RebornSkill");
            if (sp != null) {
                sp.getClass().getMethod("learnByApi", java.util.UUID.class, String.class)
                        .invoke(sp, p.getUniqueId(), skill);
                Msg.send(p, "&d비급 습득: §f" + skill);
            }
        } catch (Throwable t) {
            Msg.error(p, "스킬 습득 실패: " + t.getMessage());
        }
    }

    private void tryLearnRandomSpiritSkill(Player p) {
        String[] spiritSkills = { "spirit_fire_lance", "spirit_water_orb",
                "spirit_earth_wall", "spirit_wind_blade", "spirit_light_heal" };
        String pick = spiritSkills[(int) (Math.random() * spiritSkills.length)];
        tryLearnSkill(p, pick);
        Msg.send(p, "&3정령왕의 축복 — 무작위 정령 스킬 습득");
    }

    /** 단약 사용 — 거주 세계에 따라 적절한 Growth.consumePill 호출. */
    private void tryConsumePill(Player p, String pillId) {
        try {
            var d = RebornCore.get().api().getPlayerData(p.getUniqueId());
            if (d == null) return;
            var sp = Bukkit.getPluginManager().getPlugin("RebornStat");
            if (sp == null) return;
            Object growth = sp.getClass().getMethod("growth").invoke(sp);
            Object strategy = growth.getClass().getMethod("of",
                    kr.reborn.core.data.WorldKey.class).invoke(growth, d.worldKey());
            if (strategy == null) return;
            // consumePill(Player, String) — MartialGrowth·ImmortalGrowth 모두 동일 시그니처
            strategy.getClass().getMethod("consumePill",
                    Player.class, String.class).invoke(strategy, p, pillId);
        } catch (Throwable t) {
            // 해당 세계에 consumePill 없으면 silent fail
        }
    }
}
