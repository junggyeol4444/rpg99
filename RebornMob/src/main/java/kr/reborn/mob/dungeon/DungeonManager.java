package kr.reborn.mob.dungeon;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Msg;
import kr.reborn.mob.RebornMob;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 던전 매니저 — 정의 + 진행 추적.
 *
 * 플레이어가 입장 → 1층부터 시작 → 각 층 보스 처치 시 다음 층 진입.
 * 진척도는 영구 저장 (도달 최대 층 기록).
 *
 * 13개 던전 시드:
 *   - earth_labyrinth (지구 100층)
 *   - dragon_cavern (드래곤 5층)
 *   - demon_tower (마계 9층)
 *   - heaven_tower (천계 7층)
 *   - yokai_palace (요계 5층)
 *   - cyber_megacorp (사이버펑크 10층)
 *   - spirit_grove (정령계 6층)
 *   - immortal_seclusion (선계 4층)
 *   - martial_cliff (무협 9층)
 *   - magitech_workshop (마도공학 5층)
 *   - fantasy_castle (판타지 7층)
 *   - apocalypse_vault (아포 5층)
 *   - ocean_palace (해양 6층)
 */
public final class DungeonManager {

    private final RebornMob plugin;
    private final Map<String, Dungeon> dungeons = new HashMap<>();
    /** uuid → dungeonId → 도달 최대 층 */
    private final Map<UUID, Map<String, Integer>> progress = new ConcurrentHashMap<>();
    /** uuid → 진행 중인 dungeonId */
    private final Map<UUID, ActiveSession> activeSessions = new ConcurrentHashMap<>();

    public DungeonManager(RebornMob plugin) {
        this.plugin = plugin;
        seedDungeons();
    }

    private void seedDungeons() {
        // 지구 미궁 100층
        Dungeon labyrinth = new Dungeon("earth_labyrinth", "지구 미궁", WorldKey.EARTH, 100, 1000);
        for (int i = 1; i <= 100; i++) {
            String label = i % 10 == 0 ? (i + "층 (중간보스)") : (i + "층");
            Dungeon.Floor f = new Dungeon.Floor(i, label);
            // 일반층 몹
            if (i <= 25) f.mobIds.add("earth_gate_d");
            else if (i <= 50) f.mobIds.add("earth_gate_c");
            else if (i <= 75) f.mobIds.add("earth_gate_b");
            else f.mobIds.add("earth_gate_a");
            // 중간보스
            if (i % 10 == 0) {
                if (i == 100) f.bossId = "earth_labyrinth_100f";
                else if (i >= 50) f.bossId = "earth_gate_ss_boss";
                else f.bossId = "earth_gate_s_boss";
                f.goldReward = i * 100L;
            } else {
                f.goldReward = i * 10L;
            }
            labyrinth.floors.add(f);
        }
        labyrinth.completionRewards.add("title:dungeon_master");
        labyrinth.completionRewards.add("stat:STRENGTH:500");
        labyrinth.completionRewards.add("stat:ENDURANCE:500");
        dungeons.put(labyrinth.id, labyrinth);

        // 드래곤 동굴 5층
        Dungeon dragonCavern = new Dungeon("dragon_cavern", "고룡의 동굴", WorldKey.DRAGON, 5, 2000);
        for (int i = 1; i <= 5; i++) {
            Dungeon.Floor f = new Dungeon.Floor(i, i + "층");
            f.mobIds.add("dragon_hatchling");
            f.mobIds.add("dragon_drake");
            f.bossId = i == 5 ? "dragon_ancient_king" : "dragon_elder";
            f.goldReward = i * 500L;
            dragonCavern.floors.add(f);
        }
        dragonCavern.completionRewards.add("stat:DRAGON_POWER:1000");
        dragonCavern.completionRewards.add("title:dragon_lord_t");
        dungeons.put(dragonCavern.id, dragonCavern);

        // 마계 탑 9층
        Dungeon demonTower = new Dungeon("demon_tower", "마계 7대 마왕 탑", WorldKey.DEMON, 9, 3000);
        String[] marwangs = {
            "marwang_mammon", "marwang_leviathan", "marwang_satan",
            "demon_archdemon", "demon_balrog", "demon_lord_baal",
            "marwang_lucifer", "demon_lord_lucifer", "demon_corpse_giant"
        };
        for (int i = 1; i <= 9; i++) {
            Dungeon.Floor f = new Dungeon.Floor(i, i + "층 마왕");
            f.mobIds.add("demon_hellhound");
            f.bossId = marwangs[(i - 1) % marwangs.length];
            f.goldReward = i * 1000L;
            demonTower.floors.add(f);
        }
        demonTower.completionRewards.add("stat:DEMON_KI:2000");
        demonTower.completionRewards.add("title:demon_god");
        dungeons.put(demonTower.id, demonTower);

        // 천계 탑 7층
        Dungeon heavenTower = new Dungeon("heaven_tower", "천계 칠층 보탑", WorldKey.HEAVEN, 7, 2500);
        for (int i = 1; i <= 7; i++) {
            Dungeon.Floor f = new Dungeon.Floor(i, i + "층");
            f.mobIds.add(i <= 3 ? "heaven_angel_novice" : "heaven_angel");
            f.bossId = i == 7 ? "heaven_cheonje" : (i == 4 ? "heaven_fallen_archangel" : "heaven_archangel");
            f.goldReward = i * 800L;
            heavenTower.floors.add(f);
        }
        heavenTower.completionRewards.add("stat:HEAVEN_KI:2000");
        heavenTower.completionRewards.add("title:heaven_god");
        dungeons.put(heavenTower.id, heavenTower);

        // 요계 궁 5층
        Dungeon yokaiPalace = new Dungeon("yokai_palace", "요왕 궁전", WorldKey.YOKAI, 5, 2000);
        for (int i = 1; i <= 5; i++) {
            Dungeon.Floor f = new Dungeon.Floor(i, i + "층");
            f.mobIds.add(i <= 2 ? "yokai_dokkaebi" : "yokai_oni");
            f.bossId = i == 5 ? "yokai_emperor" : "kumiho_grand";
            f.goldReward = i * 600L;
            yokaiPalace.floors.add(f);
        }
        yokaiPalace.completionRewards.add("stat:YOKAI_KI:1500");
        yokaiPalace.completionRewards.add("title:yokai_emperor_t");
        dungeons.put(yokaiPalace.id, yokaiPalace);

        // 사이버펑크 메가코프 10층
        Dungeon megacorp = new Dungeon("cyber_megacorp", "메가코프 본사", WorldKey.CYBERPUNK, 10, 1500);
        for (int i = 1; i <= 10; i++) {
            Dungeon.Floor f = new Dungeon.Floor(i, i + "층");
            f.mobIds.add("cyber_combat_drone");
            f.mobIds.add("cyber_psycho");
            f.bossId = i == 10 ? "cyber_rogue_ai" : (i == 5 ? "cyber_corp_mech" : null);
            f.goldReward = i * 200L;
            megacorp.floors.add(f);
        }
        megacorp.completionRewards.add("stat:CYBER_ADAPTATION:500");
        dungeons.put(megacorp.id, megacorp);

        // 정령 숲 6층
        Dungeon spiritGrove = new Dungeon("spirit_grove", "원소 정령의 숲", WorldKey.SPIRIT, 6, 1500);
        String[] kingBosses = {"elemental_king_fire", "elemental_king_water",
                "elemental_king_earth", "elemental_king_wind",
                "spirit_corruptor", "spirit_predator"};
        for (int i = 1; i <= 6; i++) {
            Dungeon.Floor f = new Dungeon.Floor(i, i + "층 원소왕");
            f.mobIds.add("spirit_wild_fire");
            f.bossId = kingBosses[i - 1];
            f.goldReward = i * 400L;
            spiritGrove.floors.add(f);
        }
        spiritGrove.completionRewards.add("stat:SPIRIT_POWER:2000");
        dungeons.put(spiritGrove.id, spiritGrove);

        // 선계 4층 폐관
        Dungeon seclusion = new Dungeon("immortal_seclusion", "태허선궁", WorldKey.IMMORTAL, 4, 3000);
        for (int i = 1; i <= 4; i++) {
            Dungeon.Floor f = new Dungeon.Floor(i, i + "층");
            f.mobIds.add("immortal_seongwi");
            f.bossId = i == 4 ? "immortal_taeheo_master" : "immortal_demon_sage";
            f.goldReward = i * 1500L;
            seclusion.floors.add(f);
        }
        seclusion.completionRewards.add("stat:IMMORTAL_KI:3000");
        dungeons.put(seclusion.id, seclusion);

        // 무협 절벽 9층
        Dungeon martialCliff = new Dungeon("martial_cliff", "전설의 무벽", WorldKey.MARTIAL, 9, 2500);
        for (int i = 1; i <= 9; i++) {
            Dungeon.Floor f = new Dungeon.Floor(i, i + "층");
            f.mobIds.add("martial_kangshi_white");
            f.bossId = i == 9 ? "martial_cheonma_descend"
                    : i == 5 ? "martial_cult_master"
                    : i == 3 ? "martial_sapa_lord"
                    : "martial_sect_leader";
            f.goldReward = i * 500L;
            martialCliff.floors.add(f);
        }
        martialCliff.completionRewards.add("stat:INNER_KI:3000");
        martialCliff.completionRewards.add("title:martial_god");
        dungeons.put(martialCliff.id, martialCliff);

        // 마도공학 5층
        Dungeon magitechWorkshop = new Dungeon("magitech_workshop", "마도 공학 폐허",
                WorldKey.MAGITECH, 5, 1500);
        for (int i = 1; i <= 5; i++) {
            Dungeon.Floor f = new Dungeon.Floor(i, i + "층");
            f.mobIds.add("magitech_runaway_drone");
            f.bossId = i == 5 ? "magitech_runaway_core" : "magitech_runaway_golem";
            f.goldReward = i * 300L;
            magitechWorkshop.floors.add(f);
        }
        magitechWorkshop.completionRewards.add("stat:MAGITECH_ENERGY:1500");
        dungeons.put(magitechWorkshop.id, magitechWorkshop);

        // 판타지 성 7층
        Dungeon fantasyCastle = new Dungeon("fantasy_castle", "흑마법사의 성", WorldKey.FANTASY, 7, 800);
        for (int i = 1; i <= 7; i++) {
            Dungeon.Floor f = new Dungeon.Floor(i, i + "층");
            f.mobIds.add(i <= 3 ? "fantasy_zombie" : "fantasy_skeleton_knight");
            f.bossId = i == 7 ? "dungeon_core"
                    : i == 5 ? "fantasy_lich"
                    : i == 3 ? "fantasy_hydra" : null;
            f.goldReward = i * 200L;
            fantasyCastle.floors.add(f);
        }
        fantasyCastle.completionRewards.add("stat:MANA:1000");
        fantasyCastle.completionRewards.add("stat:INTELLIGENCE:200");
        dungeons.put(fantasyCastle.id, fantasyCastle);

        // 아포 금고 5층
        Dungeon apocVault = new Dungeon("apocalypse_vault", "버려진 금고", WorldKey.APOCALYPSE, 5, 1500);
        for (int i = 1; i <= 5; i++) {
            Dungeon.Floor f = new Dungeon.Floor(i, i + "층");
            f.mobIds.add("apoc_mutant_rat");
            f.bossId = i == 5 ? "apoc_radiation_titan" : "apoc_warlord";
            f.goldReward = i * 800L;
            apocVault.floors.add(f);
        }
        apocVault.completionRewards.add("stat:ENDURANCE:300");
        apocVault.completionRewards.add("stat:LUCK:50");
        dungeons.put(apocVault.id, apocVault);

        // 해양 궁 6층
        Dungeon oceanPalace = new Dungeon("ocean_palace", "해왕의 궁전", WorldKey.OCEAN, 6, 1500);
        for (int i = 1; i <= 6; i++) {
            Dungeon.Floor f = new Dungeon.Floor(i, i + "층");
            f.mobIds.add(i <= 2 ? "ocean_jellyfish" : "ocean_giant_shark");
            f.bossId = i == 6 ? "ocean_sea_king"
                    : i == 3 ? "ocean_kraken_ancient" : "ocean_pirate_captain";
            f.goldReward = i * 500L;
            oceanPalace.floors.add(f);
        }
        oceanPalace.completionRewards.add("stat:OCEAN_POWER:2000");
        oceanPalace.completionRewards.add("title:sea_king_t");
        dungeons.put(oceanPalace.id, oceanPalace);
    }

    public Dungeon get(String id) { return dungeons.get(id); }
    public java.util.Collection<Dungeon> all() { return dungeons.values(); }

    /** 입장 시도. */
    public boolean enter(Player p, String dungeonId) {
        Dungeon d = dungeons.get(dungeonId);
        if (d == null) { Msg.error(p, "던전 없음: " + dungeonId); return false; }
        double total = RebornCore.get().api().getTotalStats(p.getUniqueId());
        if (total < d.minTotalStats) {
            Msg.error(p, "최소 스탯 " + d.minTotalStats + " 필요 (현재 " + (long) total + ")");
            return false;
        }
        if (activeSessions.containsKey(p.getUniqueId())) {
            Msg.error(p, "다른 던전 진행 중 — /dungeon exit");
            return false;
        }
        // 진입 전 위치 저장 (exit 시 복귀용)
        ActiveSession sess = new ActiveSession(d.id, 1);
        sess.entryLocation = p.getLocation().clone();
        activeSessions.put(p.getUniqueId(), sess);
        Msg.send(p, "&5&l[" + d.name + "] §7진입 — 1층에서 시작.");
        try {
            p.sendTitle("§5§l" + d.name, "§71층 / " + d.totalFloors, 10, 60, 20);
            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_END_PORTAL_FRAME_FILL, 1.5f, 0.5f);
            p.getWorld().spawnParticle(org.bukkit.Particle.PORTAL,
                    p.getLocation().add(0, 1, 0), 100, 1, 2, 1);
        } catch (Throwable ignored) {}
        spawnFloorMobs(p, d, 1);
        return true;
    }

    public void exit(Player p) {
        ActiveSession sess = activeSessions.remove(p.getUniqueId());
        if (sess == null) return;
        Msg.send(p, "&7던전 퇴장.");
        // 진입 전 위치로 복귀 — Folia: 엔티티 스케줄러
        if (sess.entryLocation != null) {
            kr.reborn.core.RebornCore.get().scheduler().runEntityTask(p,
                    () -> { try { p.teleport(sess.entryLocation); } catch (Throwable ignored) {} });
        }
    }

    /** 보스 처치 시 호출 — 다음 층 또는 완료. */
    public void onBossKill(Player p, String bossId) {
        ActiveSession sess = activeSessions.get(p.getUniqueId());
        if (sess == null) return;
        Dungeon d = dungeons.get(sess.dungeonId);
        if (d == null) return;
        Dungeon.Floor floor = d.floor(sess.currentFloor);
        if (floor == null || !bossId.equals(floor.bossId)) return;
        // 진척 갱신
        int newMax = progress.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>())
                .merge(d.id, sess.currentFloor, Math::max);
        persistProgress(p.getUniqueId(), d.id, newMax);
        // 보상
        applyFloorReward(p, floor);
        if (sess.currentFloor >= d.totalFloors) {
            // 완료
            applyCompletionRewards(p, d);
            activeSessions.remove(p.getUniqueId());
            Bukkit.broadcastMessage("§6§l[던전 클리어] §f" + p.getName()
                    + " §7가 " + d.name + " 클리어!");
        } else {
            sess.currentFloor++;
            Msg.send(p, "&a다음 층 진입: " + sess.currentFloor + "/" + d.totalFloors);
            spawnFloorMobs(p, d, sess.currentFloor);
        }
    }

    private void applyFloorReward(Player p, Dungeon.Floor f) {
        if (f.goldReward > 0) {
            try {
                var ep = Bukkit.getPluginManager().getPlugin("RebornEconomy");
                if (ep != null) {
                    Object cur = ep.getClass().getMethod("currencies").invoke(ep);
                    cur.getClass().getMethod("deposit", UUID.class, String.class, long.class)
                            .invoke(cur, p.getUniqueId(), "GOLD_COIN", f.goldReward);
                }
            } catch (Throwable ignored) {}
            Msg.send(p, "&6보상: " + f.goldReward + " GOLD");
        }
        for (String item : f.rewardItems) {
            try {
                Material mat = Material.matchMaterial(item);
                if (mat != null) p.getInventory().addItem(new ItemStack(mat, 1));
            } catch (Throwable ignored) {}
        }
    }

    private void applyCompletionRewards(Player p, Dungeon d) {
        for (String reward : d.completionRewards) {
            String[] parts = reward.split(":");
            if (parts.length < 2) continue;
            try {
                switch (parts[0]) {
                    case "stat" -> {
                        if (parts.length < 3) break;
                        var st = kr.reborn.core.data.StatType.valueOf(parts[1].toUpperCase());
                        RebornCore.get().api().addStat(p.getUniqueId(), st,
                                Double.parseDouble(parts[2]), "dungeon:" + d.id);
                    }
                    case "title" -> {
                        var tp = Bukkit.getPluginManager().getPlugin("RebornTitle");
                        if (tp != null) {
                            Object tm = tp.getClass().getMethod("titles").invoke(tp);
                            tm.getClass().getMethod("grant", Player.class, String.class)
                                    .invoke(tm, p, parts[1]);
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }
        Msg.send(p, "&6&l[완주 보상] §7" + d.completionRewards.size() + "건 적용.");
    }

    private void spawnFloorMobs(Player p, Dungeon d, int floor) {
        Dungeon.Floor f = d.floor(floor);
        if (f == null) return;
        Location l = p.getLocation();
        // 일반 몹 3마리
        for (String mobId : f.mobIds) {
            for (int i = 0; i < 3; i++) {
                double dx = (Math.random() - 0.5) * 10;
                double dz = (Math.random() - 0.5) * 10;
                try {
                    plugin.registry().get(mobId);
                    Location pos = l.clone().add(dx, 0, dz);
                    plugin.bosses().summon(mobId, pos);
                } catch (Throwable ignored) {}
            }
        }
        // 보스
        if (f.bossId != null) {
            try {
                Location pos = l.clone().add(0, 0, 8);
                plugin.bosses().summon(f.bossId, pos);
            } catch (Throwable ignored) {}
        }
    }

    private static final String NS = "RebornMob.dungeon";

    public int maxFloorOf(UUID p, String dungeonId) {
        Map<String, Integer> map = progress.get(p);
        if (map != null && map.containsKey(dungeonId)) return map.get(dungeonId);
        int stored = kr.reborn.core.RebornCore.get().kv().getInt(NS, p, dungeonId, 0);
        if (stored > 0) {
            progress.computeIfAbsent(p, k -> new java.util.HashMap<>())
                    .put(dungeonId, stored);
        }
        return stored;
    }

    /** 보스 처치 시 호출되는 onBossKill에서 진척 업데이트 후 KV에 저장. */
    private void persistProgress(UUID p, String dungeonId, int floor) {
        try { kr.reborn.core.RebornCore.get().kv().putInt(NS, p, dungeonId, floor); }
        catch (Throwable ignored) {}
    }

    public ActiveSession activeOf(UUID p) { return activeSessions.get(p); }

    public static final class ActiveSession {
        public final String dungeonId;
        public int currentFloor;
        public final long startedAt;
        public org.bukkit.Location entryLocation;
        public ActiveSession(String d, int f) {
            this.dungeonId = d; this.currentFloor = f;
            this.startedAt = System.currentTimeMillis();
        }
    }
}
