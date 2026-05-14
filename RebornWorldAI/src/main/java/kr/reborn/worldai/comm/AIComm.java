package kr.reborn.worldai.comm;

import kr.reborn.core.data.WorldKey;
import kr.reborn.worldai.RebornWorldAI;

import java.util.ArrayDeque;
import java.util.Deque;

/** 세계 AI 간 메시지 통신. */
public final class AIComm {

    private final RebornWorldAI plugin;
    private final Deque<Message> log = new ArrayDeque<>();

    public AIComm(RebornWorldAI p) { this.plugin = p; }

    public enum Type {
        TENSION_ALERT, QUEST_LINK, ECONOMY_REPORT, WAR_DECLARATION, POLLUTION_ALERT
    }

    public void send(WorldKey from, WorldKey to, Type type, String payload) {
        Message m = new Message(from, to, type, payload, System.currentTimeMillis());
        log.push(m);
        if (log.size() > 1000) log.pollLast();
        // 수신측 AI에 전달
        var ai = plugin.of(to);
        if (ai != null) {
            // 향후: AI 사이클에서 처리. 여기서는 간단히 tension만 살짝 조정.
            if (type == Type.TENSION_ALERT) ai.state().tension += 5;
            if (type == Type.WAR_DECLARATION) ai.state().tension += 20;
        }
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
