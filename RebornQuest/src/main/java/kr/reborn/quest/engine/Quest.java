package kr.reborn.quest.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class Quest {
    public final String id;
    public final String name;
    public final String type;       // KILL, GATHER, TALK, MOVE, ESCORT, CRAFT, SKILL_USE, SURVIVE, EXPLORE, DEFEND, DELIVER, CUSTOM, WORLD
    public final String target;
    public final int amount;
    public final String world;
    public final String linkedQuestId;
    public final Map<String, Object> rewards;
    public final List<Phase> phases;

    public Quest(String id, String name, String type, String target, int amount,
                 String world, String linked, Map<String, Object> rewards) {
        this.id = id; this.name = name; this.type = type; this.target = target;
        this.amount = amount; this.world = world; this.linkedQuestId = linked;
        this.rewards = rewards; this.phases = new ArrayList<>();
    }

    public static final class Phase {
        public final String name;
        public final String target;
        public final int amount;
        public final String targetId;
        public Phase(String name, String target, int amount, String targetId) {
            this.name = name; this.target = target; this.amount = amount; this.targetId = targetId;
        }
    }
}
