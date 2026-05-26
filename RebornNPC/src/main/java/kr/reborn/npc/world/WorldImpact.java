package kr.reborn.npc.world;

import kr.reborn.core.RebornCore;
import kr.reborn.core.event.RebornNpcWorldImpactEvent;
import kr.reborn.npc.RebornNPC;
import kr.reborn.npc.entity.RebornNpc;
import kr.reborn.npc.faction.Faction;
import kr.reborn.npc.soul.Goal;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;

import java.util.UUID;

/**
 * NPC 목표 달성을 "진짜" 세계 변화로 바꾼다.
 *
 *   FOUND_TOWN     → 정착지 구조물 건설 + 세력 영토 등록
 *   FOUND_RELIGION → 교단 명명 + 이벤트 발행 (RebornGod 연동용)
 *   ASCEND         → 직업·칭호 격상 (왕/마왕/선인/용왕…) + 엔티티 이름 갱신
 *   START_BUSINESS → 상점 구조물 건설 + 이벤트 발행 (RebornEconomy 연동용)
 *
 * 물리적 변화(블록)는 즉시 적용. 도메인 등록은 RebornNpcWorldImpactEvent로 위임.
 * 모든 블록·엔티티 접근은 Folia 안전을 위해 runRegionTask로 해당 리전 스레드에서.
 */
public final class WorldImpact {

    private final RebornNPC plugin;

    public WorldImpact(RebornNPC plugin) { this.plugin = plugin; }

    public void apply(RebornNpc npc, Goal g) {
        if (npc == null || g == null) return;
        switch (g.kind) {
            case FOUND_TOWN:     foundTown(npc); break;
            case FOUND_RELIGION: foundReligion(npc); break;
            case ASCEND:         ascend(npc); break;
            case START_BUSINESS: openShop(npc); break;
            default: break;
        }
    }

    // ───────────────────────── 마을 ─────────────────────────

    private void foundTown(RebornNpc npc) {
        Location center = npc.home != null ? npc.home : npc.location;
        if (center == null || center.getWorld() == null) return;
        // 세력 영토 등록 (세력 없으면 즉석 창설)
        Faction f = plugin.registry().factions().factionOf(npc.id);
        if (f == null) f = plugin.registry().factions().createFaction(npc, clean(npc.displayName) + " 마을");
        String townName = (f != null ? f.name : clean(npc.displayName)) + " 정착지";
        if (f != null) f.territoryCenter = center.clone();

        final Location c = center.clone();
        final String label = townName;
        RebornCore.get().scheduler().runRegionTask(c, () -> buildSettlement(c, label));
        Bukkit.broadcastMessage("§2§l[마을 건설] §f" + clean(npc.displayName)
                + "이(가) §a" + townName + "§f을(를) 세웠다!");
        fire(npc, RebornNpcWorldImpactEvent.Kind.TOWN_FOUNDED, townName, center);
    }

    private void buildSettlement(Location c, String name) {
        try {
            World w = c.getWorld();
            if (w == null) return;
            int bx = c.getBlockX(), by = c.getBlockY(), bz = c.getBlockZ();
            // 3x3 광장 바닥
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++)
                    w.getBlockAt(bx + dx, by - 1, bz + dz).setType(Material.COBBLESTONE);
            w.getBlockAt(bx, by, bz).setType(Material.CAMPFIRE);          // 중심 모닥불
            w.getBlockAt(bx + 1, by, bz).setType(Material.BELL);          // 마을 종
            w.getBlockAt(bx - 1, by, bz).setType(Material.CRAFTING_TABLE);
            w.getBlockAt(bx, by, bz + 1).setType(Material.BARREL);
            // 마을 표지판 (코블 위 standing sign — 지지됨)
            Block sb = w.getBlockAt(bx, by, bz - 1);
            sb.setType(Material.OAK_SIGN);
            applySign(sb, "§6[마을]", name);
        } catch (Throwable ignored) {}
    }

    // ───────────────────────── 종교 ─────────────────────────

    private void foundReligion(RebornNpc npc) {
        String religionName = clean(npc.displayName) + "교";
        npc.aiData.put("religion", religionName);
        Bukkit.broadcastMessage("§5§l[종교 창시] §f" + clean(npc.displayName)
                + "이(가) §d" + religionName + "§f를 창시했다!");
        fire(npc, RebornNpcWorldImpactEvent.Kind.RELIGION_FOUNDED, religionName, npc.location);
    }

    // ───────────────────────── 초월 ─────────────────────────

    private void ascend(RebornNpc npc) {
        String newJob = "KING", title = "왕";
        if (npc.world != null) switch (npc.world) {
            case DEMON:    newJob = "DEMON_LORD";  title = "마왕";   break;
            case HEAVEN:   newJob = "ARCHANGEL";   title = "대천사"; break;
            case IMMORTAL: newJob = "IMMORTAL";    title = "선인";   break;
            case SPIRIT:   newJob = "SPIRIT_KING"; title = "정령왕"; break;
            case DRAGON:   newJob = "DRAGON_LORD"; title = "용왕";   break;
            default: break;
        }
        String base = clean(npc.displayName);
        npc.job = newJob;
        npc.displayName = "§6[" + title + "] §f" + base;
        renameEntity(npc);
        Bukkit.broadcastMessage("§6§l[초월] §f" + base + "이(가) §e" + title + "§f의 자리에 올랐다!");
        fire(npc, RebornNpcWorldImpactEvent.Kind.ASCENDED, title, npc.location);
    }

    private void renameEntity(RebornNpc npc) {
        if (npc.bukkitEntityId == null || npc.location == null) return;
        final UUID eid = npc.bukkitEntityId;
        final String nm = npc.displayName;
        RebornCore.get().scheduler().runRegionTask(npc.location, () -> {
            var ent = Bukkit.getEntity(eid);
            if (ent != null) ent.setCustomName(nm);
        });
    }

    // ───────────────────────── 상점 ─────────────────────────

    private void openShop(RebornNpc npc) {
        Location at = npc.workplace != null ? npc.workplace : npc.location;
        if (at == null || at.getWorld() == null) return;
        String shopName = clean(npc.displayName) + " 상점";
        npc.aiData.put("shop", shopName);
        final Location c = at.clone();
        final String label = shopName;
        RebornCore.get().scheduler().runRegionTask(c, () -> buildShop(c, label));
        Bukkit.broadcastMessage("§e§l[개업] §f" + clean(npc.displayName)
                + "이(가) §6" + shopName + "§f을(를) 열었다!");
        fire(npc, RebornNpcWorldImpactEvent.Kind.SHOP_OPENED, shopName, at);
    }

    private void buildShop(Location c, String name) {
        try {
            World w = c.getWorld();
            if (w == null) return;
            int bx = c.getBlockX(), by = c.getBlockY(), bz = c.getBlockZ();
            w.getBlockAt(bx, by - 1, bz).setType(Material.COBBLESTONE);
            w.getBlockAt(bx, by, bz).setType(Material.BARREL);            // 판매대
            Block sb = w.getBlockAt(bx, by + 1, bz);                       // 통 위 standing sign
            sb.setType(Material.OAK_SIGN);
            applySign(sb, "§6[상점]", name);
        } catch (Throwable ignored) {}
    }

    // ───────────────────────── 보조 ─────────────────────────

    private void applySign(Block block, String header, String name) {
        try {
            if (block.getState() instanceof Sign sign) {
                sign.setLine(0, header);
                sign.setLine(1, trim(name, 15));
                sign.update(true);
            }
        } catch (Throwable ignored) {}
    }

    private void fire(RebornNpc npc, RebornNpcWorldImpactEvent.Kind kind, String payload, Location loc) {
        try {
            Bukkit.getPluginManager().callEvent(
                    new RebornNpcWorldImpactEvent(npc.id, kind, payload, loc));
        } catch (Throwable ignored) {}
    }

    private String clean(String s) { return s == null ? "" : s.replaceAll("§.", ""); }

    private String trim(String s, int max) { return s.length() <= max ? s : s.substring(0, max); }
}
