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

    private static final String NS = "RebornWorldAI.history";

    private final RebornWorldAI plugin;
    private final Map<WorldKey, Deque<Entry>> records = new EnumMap<>(WorldKey.class);
    private static final int CAP = 200;
    /** 세계별 카운터 — KV 키 충돌 방지 위한 순번. */
    private final EnumMap<WorldKey, Long> seq = new EnumMap<>(WorldKey.class);

    public WorldHistory(RebornWorldAI plugin) {
        this.plugin = plugin;
        for (WorldKey w : WorldKey.values()) {
            records.put(w, new ArrayDeque<>());
            seq.put(w, 0L);
        }
        loadAll();
    }

    private void loadAll() {
        try {
            var all = kr.reborn.core.RebornCore.get().kv().loadAll(NS, null);
            // key: "<world>:<seq>" → "kind|when|text"
            // 각 월드별로 모아 정렬 후 push (오래된 것 먼저 → push로 newest first)
            EnumMap<WorldKey, java.util.TreeMap<Long, Entry>> sorted = new EnumMap<>(WorldKey.class);
            for (var e : all.entrySet()) {
                int colon = e.getKey().indexOf(':');
                if (colon < 0) continue;
                try {
                    WorldKey w = WorldKey.valueOf(e.getKey().substring(0, colon));
                    long s = Long.parseLong(e.getKey().substring(colon + 1));
                    String[] parts = e.getValue().split("\\|", 3);
                    if (parts.length < 3) continue;
                    EventKind kind = EventKind.valueOf(parts[0]);
                    long when = Long.parseLong(parts[1]);
                    Entry entry = new Entry(w, kind, parts[2], when);
                    sorted.computeIfAbsent(w, k -> new java.util.TreeMap<>()).put(s, entry);
                    if (s > seq.get(w)) seq.put(w, s);
                } catch (Throwable ignored) {}
            }
            // descending order (newest first matches the deque push pattern)
            for (var e : sorted.entrySet()) {
                var dq = records.get(e.getKey());
                for (Entry entry : e.getValue().values()) {
                    dq.push(entry);
                    if (dq.size() > CAP) dq.pollLast();
                }
            }
        } catch (Throwable ignored) {}
    }

    public void record(WorldKey w, EventKind kind, String text) {
        var dq = records.get(w);
        if (dq == null) return;
        long when = System.currentTimeMillis();
        Entry entry = new Entry(w, kind, text, when);
        dq.push(entry);
        long s = seq.merge(w, 1L, Long::sum);
        try {
            // text의 | 문자는 공백으로 치환해 디코딩 충돌 방지
            String safe = text == null ? "" : text.replace('|', ' ');
            kr.reborn.core.RebornCore.get().kv().put(NS, null,
                    w.name() + ":" + s, kind.name() + "|" + when + "|" + safe);
        } catch (Throwable ignored) {}
        if (dq.size() > CAP) {
            Entry removed = dq.pollLast();
            // CAP 초과 시 가장 오래된 항목 KV에서도 삭제 시도 (정확한 seq 모를 수 있으므로 best-effort)
            // 단순화: 주기적 prune은 별도로 처리 가능. 지금은 CAP을 약간 초과해도 무방.
        }
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
