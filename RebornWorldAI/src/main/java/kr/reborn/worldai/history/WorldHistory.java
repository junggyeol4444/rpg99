package kr.reborn.worldai.history;

import kr.reborn.core.data.WorldKey;
import kr.reborn.worldai.RebornWorldAI;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 세계 역사 기록 — AI 결정·전쟁·재해·축제 등 큰 사건을 누적 저장.
 *
 * 각 항목은 epoch 추정에 사용되며 /worldai history <world> 명령으로 조회.
 */
public final class WorldHistory {

    public enum EventKind {
        WAR_START, WAR_END, FESTIVAL, DISASTER, REVOLT, BOSS_DESCENT,
        ECON_CRISIS, MIGRATION, DIPLOMACY, FACTION_RISE, FACTION_FALL,
        WEATHER, MARKET_EXTREME, SPECIAL
    }

    private final RebornWorldAI plugin;
    private final Map<WorldKey, Deque<Entry>> records = new EnumMap<>(WorldKey.class);
    private static final int CAP = 200;

    public WorldHistory(RebornWorldAI plugin) {
        this.plugin = plugin;
        for (WorldKey w : WorldKey.values()) records.put(w, new ArrayDeque<>());
    }

    public void record(WorldKey w, EventKind kind, String text) {
        var dq = records.get(w);
        if (dq == null) return;
        dq.push(new Entry(w, kind, text, System.currentTimeMillis()));
        if (dq.size() > CAP) dq.pollLast();
    }

    public List<Entry> recent(WorldKey w, int count) {
        var dq = records.get(w);
        if (dq == null) return java.util.Collections.emptyList();
        var l = new java.util.ArrayList<Entry>();
        int i = 0;
        for (Entry e : dq) { if (i++ >= count) break; l.add(e); }
        return l;
    }

    /** 최근 N개 사건에서 특정 종류 개수 계산. */
    public int countRecent(WorldKey w, EventKind kind, int window) {
        var dq = records.get(w);
        if (dq == null) return 0;
        int found = 0, i = 0;
        for (Entry e : dq) {
            if (i++ >= window) break;
            if (e.kind == kind) found++;
        }
        return found;
    }

    public static final class Entry {
        public final WorldKey world;
        public final EventKind kind;
        public final String text;
        public final long when;

        public Entry(WorldKey w, EventKind k, String t, long when) {
            this.world = w; this.kind = k; this.text = t; this.when = when;
        }
    }
}
