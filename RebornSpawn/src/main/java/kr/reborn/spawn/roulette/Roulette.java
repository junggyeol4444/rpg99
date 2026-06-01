package kr.reborn.spawn.roulette;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Msg;
import kr.reborn.core.util.Rand;
import kr.reborn.spawn.RebornSpawn;
import kr.reborn.spawn.event.RebornRouletteResultEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class Roulette {

    private final RebornSpawn plugin;

    public Roulette(RebornSpawn p) { this.plugin = p; }

    public void spin(Player p) {
        var c = plugin.getConfig();
        List<String> raw = c.getStringList("roulette.worlds");
        List<WorldKey> worlds = new ArrayList<>();
        for (String w : raw) try { worlds.add(WorldKey.valueOf(w)); } catch (Exception ignored) {}
        if (worlds.isEmpty()) {
            Msg.error(p, "룰렛 세계 목록 없음.");
            return;
        }

        double dur = c.getDouble("roulette.duration-seconds", 7);
        long ticks = (long) (dur * 20L);
        // 시각 효과: 빠른 타이틀 회전
        long step = 4L;
        for (long t = 0; t < ticks; t += step) {
            long delay = t;
            int idx = (int) ((t * 3 + System.nanoTime()) % worlds.size());
            if (idx < 0) idx = -idx;
            int finalIdx = idx;
            RebornCore.get().scheduler().runTaskLater(() -> {
                if (p.isOnline()) p.sendTitle("§6환생 룰렛", "§f" + worlds.get(finalIdx % worlds.size()), 0, 8, 0);
            }, delay);
        }
        RebornCore.get().scheduler().runTaskLater(() -> finish(p, worlds), ticks);
    }

    private void finish(Player p, List<WorldKey> worlds) {
        // 럭 가중치
        PlayerData d = RebornCore.get().api().getPlayerData(p.getUniqueId());
        double luck = d == null ? 0 : d.getStat(StatType.LUCK);
        double bias = plugin.getConfig().getDouble("roulette.luck-bias", 0.0001);
        WorldKey result = Rand.weighted(worlds, w -> 1.0 + luck * bias);

        d.worldKey(result);
        d.visited().add(result);

        applyInitialStats(d, result);

        Bukkit.getPluginManager().callEvent(new RebornRouletteResultEvent(p, result));

        // 종족 자동 배정
        try {
            plugin.races().assignRandom(p, result);
        } catch (Throwable ignored) {}

        // RebornHiddenClass INITIAL 클래스 후보 굴림 (있으면)
        try {
            var hcPlugin = Bukkit.getPluginManager().getPlugin("RebornHiddenClass");
            if (hcPlugin != null) {
                Object engine = hcPlugin.getClass().getMethod("engine").invoke(hcPlugin);
                engine.getClass().getMethod("rollInitial",
                        org.bukkit.entity.Player.class, WorldKey.class)
                        .invoke(engine, p, result);
            }
        } catch (Throwable ignored) {}
        // 세계별 고유 환생 메시지
        String worldFlavor = worldFlavorOf(result);
        p.sendTitle("§6운명이 결정되었다", "§f→ " + result, 5, 60, 20);
        p.sendMessage("");
        p.sendMessage("§5§l═══ 환생 ═══");
        p.sendMessage("§7" + worldFlavor);
        p.sendMessage("");
        try {
            org.bukkit.Sound s = worldSoundOf(result);
            org.bukkit.Particle pp = worldParticleOf(result);
            if (s != null) p.getWorld().playSound(p.getLocation(), s, 1.5f, 1.0f);
            if (pp != null) p.getWorld().spawnParticle(pp, p.getLocation().add(0, 1.5, 0), 80, 1.5, 2, 1.5, 0.1);
        } catch (Throwable ignored) {}

        int cd = plugin.getConfig().getInt("roulette.count-down-seconds", 30);
        for (int i = 0; i <= cd; i++) {
            int sec = cd - i;
            RebornCore.get().scheduler().runTaskLater(() -> {
                if (p.isOnline()) p.sendActionBar("§e" + result + " 세계 진입까지 " + sec + "초");
            }, i * 20L);
        }
        RebornCore.get().scheduler().runTaskLater(() -> teleportToTutorial(p, result), cd * 20L);

        // NPC 자녀 시작 확률 판정
        rollChildStart(p, d, result);
    }

    private void applyInitialStats(PlayerData d, WorldKey w) {
        var c = plugin.getConfig();
        int min, max;
        if (w == WorldKey.DRAGON) {
            min = c.getInt("initial-stats.dragon-bonus.min", 15);
            max = c.getInt("initial-stats.dragon-bonus.max", 25);
        } else if (w.isSpecialWorld()) {
            min = c.getInt("initial-stats.special-bonus.min", 10);
            max = c.getInt("initial-stats.special-bonus.max", 15);
        } else {
            min = c.getInt("initial-stats.normal-bonus.min", 1);
            max = c.getInt("initial-stats.normal-bonus.max", 10);
        }
        for (StatType t : StatType.COMMON_8) {
            d.setStat(t, 1 + Rand.range(min, max));
        }
        // 특수 스탯
        double special = c.getDouble("initial-stats.special-stat-default", 5);
        StatType ss = specialStatOf(w);
        if (ss != null) d.setStat(ss, special);
        if (w == WorldKey.DRAGON) {
            d.setStat(StatType.DRAGON_POWER, c.getInt("initial-stats.dragon-power", 10));
        }
    }

    private StatType specialStatOf(WorldKey w) {
        switch (w) {
            case FANTASY: return StatType.MANA;
            case DEMON: return StatType.DEMON_KI;
            case HEAVEN: return StatType.HEAVEN_KI;
            case SPIRIT: return StatType.SPIRIT_POWER;
            case MARTIAL: return StatType.INNER_KI;
            case IMMORTAL: return StatType.IMMORTAL_KI;
            case YOKAI: return StatType.YOKAI_KI;
            case EARTH: return StatType.LEVEL;
            case MAGITECH: return StatType.MAGITECH_ENERGY;
            case CYBERPUNK: return StatType.CYBER_ADAPTATION;
            case OCEAN: return StatType.OCEAN_POWER;
            default: return null;
        }
    }

    private void teleportToTutorial(Player p, WorldKey result) {
        var w = Bukkit.getWorld("tutorial_" + result.name().toLowerCase());
        if (w == null) w = Bukkit.getWorld("tutorial");
        if (w == null) {
            Msg.error(p, "튜토리얼 월드가 없습니다. 관리자에게 문의.");
            return;
        }
        p.teleport(w.getSpawnLocation());
    }

    private void rollChildStart(Player p, PlayerData d, WorldKey w) {
        var c = plugin.getConfig();
        double base = c.getDouble("child-start.base-chance", 0.005);
        double max = c.getDouble("child-start.max-chance", 0.02);
        double scale = c.getDouble("child-start.luck-scale", 0.0001);
        double chance = Math.min(max, base + d.getStat(StatType.LUCK) * scale);
        if (!Rand.chance(chance)) return;
        var clans = c.getMapList("child-start.npc-clans." + w.name());
        if (clans.isEmpty()) return;
        int idx = Rand.range(0, clans.size() - 1);
        var clan = clans.get(idx);
        String clanId = String.valueOf(clan.get("id"));
        double parent = ((Number) clan.get("parent")).doubleValue();
        String lineage = String.valueOf(clan.get("lineage"));
        double bonusPct = c.getDouble("child-start.parent-stat-percent", 5) / 100.0;
        double perStat = (parent / 8.0) * bonusPct;
        for (StatType t : StatType.COMMON_8) d.addStat(t, perStat);
        d.clanId(clanId);
        d.lineage(lineage);
        d.childStart(true);
        Msg.send(p, "&d[NPC 자녀 시작] &f" + clan.get("name") + "의 자녀로 태어났다.");
    }

    /** 세계별 환생 플레이버 텍스트. */
    private String worldFlavorOf(WorldKey w) {
        switch (w) {
            case FANTASY:    return "마법과 검의 땅 — 학파를 선택하고 종족의 운명을 살아라.";
            case DEMON:      return "마기가 흐르는 어둠의 세계 — 너의 영혼이 마기에 잠긴다.";
            case HEAVEN:     return "신성한 빛의 세계 — 살생을 금하고 신의 길을 따르라.";
            case SPIRIT:     return "원소가 살아 숨쉬는 정령의 세계 — 정령왕의 호의를 얻어라.";
            case MARTIAL:    return "무공이 천하를 움직이는 무림 — 정사대전이 너를 기다린다.";
            case IMMORTAL:   return "선계 — 천기를 모으고 천겁을 넘어 선인이 되어라.";
            case YOKAI:      return "요계의 밤 — 보름달이 너의 요기를 일깨운다.";
            case EARTH:      return "지구 — 게이트가 열린 현대. 헌터의 길을 가라.";
            case MAGITECH:   return "마도공학의 세계 — 마법과 기계가 융합한다.";
            case APOCALYPSE: return "폐허의 세상 — 방사능 속에서 살아남아라.";
            case CYBERPUNK:  return "네온이 흐르는 메가코프의 세계 — 임플란트로 무장하라.";
            case DRAGON:     return "용의 세계 — 잠과 보물 속에서 나이를 먹어가라.";
            case OCEAN:      return "끝없는 바다 — 해적인가, 해왕의 신민인가?";
            default:         return "운명이 너를 이 땅으로 인도했다.";
        }
    }

    private org.bukkit.Sound worldSoundOf(WorldKey w) {
        switch (w) {
            case FANTASY: return org.bukkit.Sound.BLOCK_ENCHANTMENT_TABLE_USE;
            case DEMON: return org.bukkit.Sound.ENTITY_WITHER_AMBIENT;
            case HEAVEN: return org.bukkit.Sound.BLOCK_BELL_RESONATE;
            case SPIRIT: return org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_CHIME;
            case MARTIAL: return org.bukkit.Sound.ITEM_TRIDENT_THUNDER;
            case IMMORTAL: return org.bukkit.Sound.BLOCK_BEACON_AMBIENT;
            case YOKAI: return org.bukkit.Sound.ENTITY_FOX_AGGRO;
            case EARTH: return org.bukkit.Sound.BLOCK_PISTON_EXTEND;
            case MAGITECH: return org.bukkit.Sound.BLOCK_END_PORTAL_FRAME_FILL;
            case APOCALYPSE: return org.bukkit.Sound.ENTITY_RAVAGER_ROAR;
            case CYBERPUNK: return org.bukkit.Sound.BLOCK_BEACON_POWER_SELECT;
            case DRAGON: return org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL;
            case OCEAN: return org.bukkit.Sound.ENTITY_CONDUIT_AMBIENT;
            default: return org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE;
        }
    }

    private org.bukkit.Particle worldParticleOf(WorldKey w) {
        switch (w) {
            case FANTASY: return org.bukkit.Particle.SPELL_INSTANT;
            case DEMON: return org.bukkit.Particle.SQUID_INK;
            case HEAVEN: return org.bukkit.Particle.END_ROD;
            case SPIRIT: return org.bukkit.Particle.SPELL_MOB_AMBIENT;
            case MARTIAL: return org.bukkit.Particle.CRIT;
            case IMMORTAL: return org.bukkit.Particle.SOUL_FIRE_FLAME;
            case YOKAI: return org.bukkit.Particle.SPELL_WITCH;
            case EARTH: return org.bukkit.Particle.PORTAL;
            case MAGITECH: return org.bukkit.Particle.ELECTRIC_SPARK;
            case APOCALYPSE: return org.bukkit.Particle.SMOKE_LARGE;
            case CYBERPUNK: return org.bukkit.Particle.REDSTONE;
            case DRAGON: return org.bukkit.Particle.DRAGON_BREATH;
            case OCEAN: return org.bukkit.Particle.WATER_BUBBLE;
            default: return org.bukkit.Particle.TOTEM;
        }
    }
}
