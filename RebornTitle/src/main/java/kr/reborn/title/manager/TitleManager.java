package kr.reborn.title.manager;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.util.Items;
import kr.reborn.core.util.Msg;
import kr.reborn.title.RebornTitle;
import kr.reborn.title.data.Title;
import kr.reborn.title.event.RebornTitleChangeEvent;
import kr.reborn.title.event.RebornTitleGrantEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TitleManager {

    private static final String NS = "RebornTitle.player";

    private final RebornTitle plugin;
    private final Map<String, Title> titles = new HashMap<>();
    /** uuid → 보유 칭호 id 집합 */
    private final Map<UUID, Set<String>> owned = new ConcurrentHashMap<>();
    /** uuid → 대표 칭호 id */
    private final Map<UUID, String> active = new ConcurrentHashMap<>();
    /** uuid → 누적 킬 카운트 */
    private final Map<UUID, Integer> kills = new ConcurrentHashMap<>();
    private final java.util.Set<UUID> loaded = ConcurrentHashMap.newKeySet();

    public TitleManager(RebornTitle plugin) {
        this.plugin = plugin;
        load();
    }

    private void ensureLoaded(UUID p) {
        if (loaded.add(p)) {
            var all = RebornCore.get().kv().loadAll(NS, p);
            String og = all.get("owned");
            if (og != null && !og.isEmpty()) {
                Set<String> s = new HashSet<>();
                for (String c : og.split(",")) if (!c.isEmpty()) s.add(c);
                owned.put(p, s);
            }
            String ac = all.get("active");
            if (ac != null && !ac.isEmpty()) active.put(p, ac);
            String k = all.get("kills");
            if (k != null) try { kills.put(p, Integer.parseInt(k)); } catch (Throwable ignored) {}
        }
    }

    private void load() {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("titles");
        if (sec == null) return;
        for (String id : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(id);
            if (s == null) continue;
            try { titles.put(id, Title.fromConfig(id, s)); }
            catch (Exception e) { plugin.getLogger().warning("칭호 로드 실패: " + id + " " + e.getMessage()); }
        }
    }

    public Collection<Title> all() { return titles.values(); }

    public Title get(String id) { return titles.get(id); }

    public Set<String> owned(UUID p) {
        ensureLoaded(p);
        return owned.computeIfAbsent(p, k -> new HashSet<>());
    }

    public String active(UUID p) { ensureLoaded(p); return active.get(p); }

    public boolean grant(Player p, String id) {
        Title t = titles.get(id);
        if (t == null) return false;
        Set<String> s = owned(p.getUniqueId());
        if (s.contains(id)) return false;
        s.add(id);
        RebornCore.get().kv().put(NS, p.getUniqueId(), "owned", String.join(",", s));
        Bukkit.getPluginManager().callEvent(new RebornTitleGrantEvent(p, t));
        // Title 종류별 고유 연출
        renderGrant(p, t);
        return true;
    }

    private void renderGrant(Player p, Title t) {
        org.bukkit.Sound sound;
        org.bukkit.Particle particle;
        String prefix;
        boolean broadcast = false;
        switch (t.type) {
            case TIER -> {
                sound = org.bukkit.Sound.BLOCK_BEACON_POWER_SELECT;
                particle = org.bukkit.Particle.END_ROD;
                prefix = "&5&l✦ 경지 칭호 ✦ &r";
            }
            case ACHIEVEMENT -> {
                sound = org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE;
                particle = org.bukkit.Particle.TOTEM;
                prefix = "&6&l[업적] ";
            }
            case CLAN -> {
                sound = org.bukkit.Sound.BLOCK_BELL_USE;
                particle = org.bukkit.Particle.CRIT_MAGIC;
                prefix = "&3&l[가문 명예] ";
            }
            case NPC -> {
                sound = org.bukkit.Sound.ENTITY_VILLAGER_TRADE;
                particle = org.bukkit.Particle.HEART;
                prefix = "&d&l[인연] ";
            }
            case WORLD_QUEST -> {
                sound = org.bukkit.Sound.BLOCK_BELL_RESONATE;
                particle = org.bukkit.Particle.PORTAL;
                prefix = "&6&l[세계의 영웅] ";
                broadcast = true;
            }
            case AI -> {
                sound = org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_CHIME;
                particle = org.bukkit.Particle.SPELL_INSTANT;
                prefix = "&7&l[행적의 증거] ";
            }
            default -> {
                sound = org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE;
                particle = org.bukkit.Particle.TOTEM;
                prefix = "&6&l[칭호 획득] ";
            }
        }
        Msg.send(p, prefix + t.name);
        try {
            p.playSound(p.getLocation(), sound, 1.0f, 1.2f);
            p.getWorld().spawnParticle(particle, p.getLocation().add(0, 1.5, 0), 40, 0.5, 0.8, 0.5, 0.1);
            p.sendTitle("§6✦ 칭호 ✦", "§f" + t.name, 10, 50, 20);
        } catch (Throwable ignored) {}
        if (broadcast) {
            Bukkit.broadcastMessage(Msg.PREFIX + Msg.c(prefix + p.getName() + " §7가 " + t.name + " §7칭호 획득!"));
        }
    }

    public void revoke(Player p, String id) {
        Set<String> s = owned(p.getUniqueId());
        if (!s.remove(id)) return;
        RebornCore.get().kv().put(NS, p.getUniqueId(), "owned", String.join(",", s));
        if (id.equals(active.get(p.getUniqueId()))) {
            // 활성 칭호였으면 스탯 회수도 (이전엔 active만 비우고 stat 회수 안 함)
            Title t = titles.get(id);
            if (t != null) removeEffects(p, t);
            active.remove(p.getUniqueId());
            RebornCore.get().kv().remove(NS, p.getUniqueId(), "active");
        }
        Msg.warn(p, "&7칭호 회수: " + id);
    }

    public void setActive(Player p, String id) {
        if (!owned(p.getUniqueId()).contains(id)) {
            Msg.error(p, "보유하지 않은 칭호입니다.");
            return;
        }
        Title t = titles.get(id);
        if (t == null) return;
        String prev = active.put(p.getUniqueId(), id);
        RebornCore.get().kv().put(NS, p.getUniqueId(), "active", id);
        // 이전 칭호 효과 회수 — 이전엔 누락돼 칭호 교체 시마다 스탯이 누적됐음
        // (A 장착 +50, B 장착 +50 → STR +100 영구. 무한 사이클 가능)
        if (prev != null && !prev.equals(id)) {
            Title prevT = titles.get(prev);
            if (prevT != null) removeEffects(p, prevT);
        }
        applyEffects(p, t);
        Bukkit.getPluginManager().callEvent(new RebornTitleChangeEvent(p, prev, id));
        PlayerData d = RebornCore.get().api().getPlayerData(p.getUniqueId());
        if (d != null) d.titleId(id);
        Msg.send(p, "&a대표 칭호 변경: " + t.name);
    }

    /** 대표 칭호 효과 적용 — 단순 스탯 가산. */
    public void applyEffects(Player p, Title t) {
        for (var e : t.statBonuses.entrySet()) {
            RebornCore.get().api().addStat(p.getUniqueId(), e.getKey(), e.getValue(), "TITLE:" + t.id);
        }
        if ("HIDDEN_MASTER_REVEAL".equals(t.special)) {
            Bukkit.broadcastMessage(Msg.PREFIX + Msg.c("&c&l은둔고수 " + p.getName() + "님이 정체를 드러냈다!"));
        }
    }

    /** 칭호 효과 회수 — 비활성화·revoke 시 호출. */
    private void removeEffects(Player p, Title t) {
        for (var e : t.statBonuses.entrySet()) {
            RebornCore.get().api().addStat(p.getUniqueId(), e.getKey(), -e.getValue(), "TITLE-REVOKE:" + t.id);
        }
    }

    /** 킬 카운트 증가 — 칭호 진행 트리거. */
    public void incrementKill(Player p) {
        ensureLoaded(p.getUniqueId());
        int n = kills.merge(p.getUniqueId(), 1, Integer::sum);
        RebornCore.get().kv().putInt(NS, p.getUniqueId(), "kills", n);
        for (Title t : titles.values()) {
            if (t.reqType == Title.ReqType.KILL_COUNT && t.reqValue instanceof Number num) {
                if (n >= num.intValue()) grant(p, t.id);
            }
        }
    }

    /** 세계 방문 트리거. */
    public void onWorldVisit(Player p) {
        int visited = RebornCore.get().api().getVisitedWorlds(p.getUniqueId()).size();
        for (Title t : titles.values()) {
            if (t.reqType == Title.ReqType.WORLDS_VISITED && t.reqValue instanceof Number num) {
                if (visited >= num.intValue()) grant(p, t.id);
            }
        }
    }

    /** 경지 도달 트리거. */
    public void onTier(Player p, String tier) {
        for (Title t : titles.values()) {
            if (t.reqType == Title.ReqType.TIER && tier.equals(String.valueOf(t.reqValue))) {
                grant(p, t.id);
            }
        }
    }

    public void openListGui(Player p) {
        var b = plugin.gui().builder("&6칭호 목록", 6);
        int slot = 0;
        for (Title t : titles.values()) {
            if (slot >= 54) break;
            boolean has = owned(p.getUniqueId()).contains(t.id);
            boolean isActive = t.id.equals(active(p.getUniqueId()));
            Material mat = isActive ? Material.NETHER_STAR : (has ? Material.NAME_TAG : Material.GRAY_DYE);
            String prefix = isActive ? "&e[활성] " : has ? "&a[보유] " : "&8[미획득] ";
            var icon = Items.of(mat, prefix + t.name,
                    "&7" + t.description,
                    "&7유형: " + t.type,
                    has ? "&a클릭: 대표 칭호로 설정" : "&8요구: " + t.reqType + " " + t.reqValue);
            String id = t.id;
            b.set(slot++, icon, e -> { if (has) setActive(p, id); });
        }
        b.open(p);
    }
}
