package kr.reborn.title.manager;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Items;
import kr.reborn.core.util.Msg;
import kr.reborn.title.RebornTitle;
import kr.reborn.title.event.RebornFirstPlaceEvent;
import kr.reborn.title.event.RebornRankingUpdateEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 세계별·분야별·크로스월드 랭킹. */
public final class RankingManager {

    public static final class RankEntry {
        public final UUID id;
        public final String name;
        public final double score;
        public RankEntry(UUID id, String name, double score) {
            this.id = id; this.name = name; this.score = score;
        }
    }

    private final RebornTitle plugin;
    private final Map<WorldKey, List<RankEntry>> worldRanks = new EnumMap<>(WorldKey.class);
    private final List<RankEntry> crossWorld = new ArrayList<>();
    /** uuid → 마지막 1위였던 세계 (1위 알림 중복 방지) */
    private final Map<UUID, WorldKey> lastFirsts = new java.util.concurrent.ConcurrentHashMap<>();

    public RankingManager(RebornTitle plugin) { this.plugin = plugin; }

    /** 5분마다 호출. 모든 온라인 플레이어 + DB 캐시 기반 갱신. */
    public void refresh() {
        EnumMap<WorldKey, List<RankEntry>> next = new EnumMap<>(WorldKey.class);
        List<RankEntry> cross = new ArrayList<>();
        double crossThreshold = plugin.getConfig().getDouble("ranking.cross-world-threshold", 5000);
        int perPage = plugin.getConfig().getInt("ranking.per-page", 100);

        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerData d = RebornCore.get().api().getPlayerData(p.getUniqueId());
            if (d == null) continue;
            double total = RebornCore.get().api().getTotalStats(p.getUniqueId());
            RankEntry e = new RankEntry(p.getUniqueId(), p.getName(), total);
            next.computeIfAbsent(d.worldKey(), k -> new ArrayList<>()).add(e);
            if (total >= crossThreshold) cross.add(e);
        }
        // TODO: NPC + 오프라인 플레이어 캐시 결합 (RebornNPC hook)

        for (var entry : next.entrySet()) {
            entry.getValue().sort(Comparator.comparingDouble((RankEntry r) -> r.score).reversed());
            if (entry.getValue().size() > perPage) {
                entry.setValue(new ArrayList<>(entry.getValue().subList(0, perPage)));
            }
        }
        cross.sort(Comparator.comparingDouble((RankEntry r) -> r.score).reversed());

        synchronized (worldRanks) {
            worldRanks.clear();
            worldRanks.putAll(next);
            crossWorld.clear();
            crossWorld.addAll(cross);
        }

        // 1위 변동 공지
        for (var entry : next.entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            RankEntry top = entry.getValue().get(0);
            WorldKey prev = lastFirsts.get(top.id);
            if (prev != entry.getKey()) {
                lastFirsts.put(top.id, entry.getKey());
                Player p = Bukkit.getPlayer(top.id);
                if (p != null) {
                    Bukkit.broadcastMessage(Msg.PREFIX + Msg.c("&e&l[랭킹] " + p.getName()
                            + " 님이 " + entry.getKey() + " 1위에 올랐습니다!"));
                    Bukkit.getPluginManager().callEvent(new RebornFirstPlaceEvent(p, entry.getKey()));
                }
            }
        }
        Bukkit.getPluginManager().callEvent(new RebornRankingUpdateEvent());
    }

    public List<RankEntry> world(WorldKey w) {
        synchronized (worldRanks) {
            return worldRanks.getOrDefault(w, List.of());
        }
    }

    public List<RankEntry> crossWorld() {
        synchronized (worldRanks) { return new ArrayList<>(crossWorld); }
    }

    public void open(Player p) {
        var b = plugin.gui().builder("&6랭킹 — 세계 선택", 4);
        int slot = 0;
        for (WorldKey w : WorldKey.values()) {
            if (slot >= 27) break;
            int size = world(w).size();
            var icon = Items.of(Material.PAPER, "&e" + w.name(),
                    "&7등록: &f" + size + "명",
                    "&7클릭으로 상세 보기");
            b.set(slot++, icon, e -> openWorld(p, w));
        }
        b.set(31, Items.of(Material.NETHER_STAR, "&6크로스 월드 랭킹",
                "&7절대자급 5000+ 통합"), e -> openCross(p));
        b.open(p);
    }

    private void openWorld(Player p, WorldKey w) {
        var b = plugin.gui().builder("&6랭킹 — " + w, 6);
        List<RankEntry> list = world(w);
        for (int i = 0; i < Math.min(54, list.size()); i++) {
            RankEntry e = list.get(i);
            var icon = Items.of(Material.PLAYER_HEAD, "&e#" + (i + 1) + " " + e.name,
                    "&7스탯: &f" + (long) e.score);
            b.set(i, icon, evt -> {});
        }
        b.open(p);
    }

    private void openCross(Player p) {
        var b = plugin.gui().builder("&6크로스 월드 랭킹", 6);
        List<RankEntry> list = crossWorld();
        for (int i = 0; i < Math.min(54, list.size()); i++) {
            RankEntry e = list.get(i);
            var icon = Items.of(Material.NETHER_STAR, "&e#" + (i + 1) + " " + e.name,
                    "&7총 스탯: &f" + (long) e.score);
            b.set(i, icon, evt -> {});
        }
        b.open(p);
    }
}
