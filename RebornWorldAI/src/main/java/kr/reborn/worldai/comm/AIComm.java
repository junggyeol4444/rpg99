package kr.reborn.worldai.comm;

import kr.reborn.core.data.WorldKey;
import kr.reborn.worldai.RebornWorldAI;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 세계 AI 간 메시지 통신 + 인박스.
 *
 * 발신자는 send()로 받는 세계의 인박스에 push. 수신자는 다음 사이클 시작 시
 * processInbox()로 drain하여 메시지 의미에 따라 자기 state를 변동한다.
 *
 * 5가지 메시지 타입:
 *   TENSION_ALERT      이웃 긴장 경보 (+5 tension, -2 stability)
 *   WAR_DECLARATION    전쟁 선언 — 같은 연결권이면 동맹 참전, 적 연결권이면 대리전
 *   ECONOMY_REPORT     payload "boom" → tradeActivity +0.15, "crash" → -0.15
 *   QUEST_LINK         이웃 사건에 연동 — stability +3 (목적 부여)
 *   POLLUTION_ALERT    오염 확산 — mobBalance -0.15, stability -3
 */
public final class AIComm {

    private final RebornWorldAI plugin;
    private final Deque<Message> log = new ArrayDeque<>();
    private final Map<WorldKey, Deque<Message>> inbox = new EnumMap<>(WorldKey.class);

    public AIComm(RebornWorldAI p) { this.plugin = p; }

    public enum Type {
        TENSION_ALERT, QUEST_LINK, ECONOMY_REPORT, WAR_DECLARATION, POLLUTION_ALERT
    }

    public void send(WorldKey from, WorldKey to, Type type, String payload) {
        Message m = new Message(from, to, type, payload, System.currentTimeMillis());
        log.push(m);
        if (log.size() > 1000) log.pollLast();
        // 수신측 인박스에 push — 수신자가 다음 cycle에서 processInbox()로 소비.
        inbox.computeIfAbsent(to, k -> new ConcurrentLinkedDeque<>()).add(m);
        // 인박스 폭주 방지 (최근 50개만 유지)
        Deque<Message> q = inbox.get(to);
        while (q.size() > 50) q.pollFirst();
    }

    /** 수신자가 자기 인박스를 비우고 메시지 리스트를 받아간다. */
    public List<Message> drain(WorldKey self) {
        Deque<Message> q = inbox.get(self);
        if (q == null || q.isEmpty()) return List.of();
        List<Message> out = new ArrayList<>(q);
        q.clear();
        return out;
    }

    public java.util.List<Message> recent(int count) {
        var l = new java.util.ArrayList<Message>();
        int i = 0;
        for (Message m : log) { if (i++ >= count) break; l.add(m); }
        return l;
    }

    public static final class Message {
        public final WorldKey from, to; public final Type type;
        public final String payload; public final long when;
        Message(WorldKey from, WorldKey to, Type type, String payload, long when) {
            this.from = from; this.to = to; this.type = type;
            this.payload = payload; this.when = when;
        }
    }
}
