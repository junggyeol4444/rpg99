package kr.reborn.core.data;

import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PlayerData {

    private final UUID uuid;
    private String name;
    private WorldKey worldKey = WorldKey.LOBBY;
    private String tier = "";
    private String titleId = "";
    private String clanId = "";
    private String lineage = "";
    private int dragonAge;
    private int deaths;
    private int reincarnations;
    private boolean gymUsed;
    private boolean childStart;
    private long firstJoin;
    private long lastJoin;
    private long playtime;

    private final EnumMap<StatType, Double> stats = new EnumMap<>(StatType.class);
    private final Set<WorldKey> visited = new HashSet<>();
    private final Map<String, StatusEffect> status = new java.util.concurrent.ConcurrentHashMap<>();

    /** dirty flag: write-back 최적화 */
    private final AtomicBoolean dirty = new AtomicBoolean(false);

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID uuid() { return uuid; }
    public String name() { return name; }
    public WorldKey worldKey() { return worldKey; }
    public String tier() { return tier; }
    public String titleId() { return titleId; }
    public String clanId() { return clanId; }
    public String lineage() { return lineage; }
    public int dragonAge() { return dragonAge; }
    public int deaths() { return deaths; }
    public int reincarnations() { return reincarnations; }
    public boolean gymUsed() { return gymUsed; }
    public boolean childStart() { return childStart; }
    public long firstJoin() { return firstJoin; }
    public long lastJoin() { return lastJoin; }
    public long playtime() { return playtime; }

    public void name(String s) { this.name = s; markDirty(); }
    public void worldKey(WorldKey w) { this.worldKey = w; markDirty(); }
    public void tier(String s) { this.tier = s; markDirty(); }
    public void titleId(String s) { this.titleId = s; markDirty(); }
    public void clanId(String s) { this.clanId = s; markDirty(); }
    public void lineage(String s) { this.lineage = s; markDirty(); }
    public void dragonAge(int v) { this.dragonAge = v; markDirty(); }
    public void deaths(int v) { this.deaths = v; markDirty(); }
    public void reincarnations(int v) { this.reincarnations = v; markDirty(); }
    public void gymUsed(boolean v) { this.gymUsed = v; markDirty(); }
    public void childStart(boolean v) { this.childStart = v; markDirty(); }
    public void firstJoin(long v) { this.firstJoin = v; markDirty(); }
    public void lastJoin(long v) { this.lastJoin = v; markDirty(); }
    public void playtime(long v) { this.playtime = v; markDirty(); }

    public Map<StatType, Double> statsView() { return stats; }

    public double getStat(StatType t) {
        return stats.getOrDefault(t, 0.0);
    }

    public void setStat(StatType t, double v) {
        stats.put(t, v);
        markDirty();
    }

    public void addStat(StatType t, double delta) {
        stats.merge(t, delta, Double::sum);
        markDirty();
    }

    public Set<WorldKey> visited() { return visited; }
    public Map<String, StatusEffect> status() { return status; }

    /** 세계 방문 기록 — markDirty 자동 호출 (save에서 visited 누락 방지). */
    public void visit(WorldKey w) {
        if (w == null) return;
        if (visited.add(w)) markDirty();
    }

    /** 상태 효과 추가 — markDirty 자동 호출. */
    public void addStatus(StatusEffect e) {
        if (e == null || e.id == null) return;
        status.put(e.id, e);
        markDirty();
    }

    /** 상태 효과 제거 — markDirty 자동 호출. */
    public void removeStatus(String id) {
        if (status.remove(id) != null) markDirty();
    }

    public boolean isDirty() { return dirty.get(); }
    public void clearDirty() { dirty.set(false); }
    public void markDirty() { dirty.set(true); }

    public static final class StatusEffect {
        public final String id;
        public final String type; // BLESSING / CURSE
        public long remainingTicks;
        public int stacks;

        public StatusEffect(String id, String type, long remainingTicks, int stacks) {
            this.id = id; this.type = type;
            this.remainingTicks = remainingTicks; this.stacks = stacks;
        }
    }

    @Nullable
    public StatusEffect getStatus(String id) {
        return status.get(id);
    }
}
