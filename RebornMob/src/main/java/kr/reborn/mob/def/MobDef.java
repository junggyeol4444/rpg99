package kr.reborn.mob.def;

import kr.reborn.core.data.WorldKey;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

public final class MobDef {
    public final String id;
    public final EntityType base;
    public final String name;
    public final WorldKey world;
    public final double hp, damage, speed;
    public final MobAI ai;
    public final boolean boss;
    public final List<DropEntry> drops = new ArrayList<>();
    public final int phases;

    public MobDef(String id, EntityType base, String name, WorldKey world,
                  double hp, double damage, double speed, MobAI ai,
                  boolean boss, int phases) {
        this.id = id; this.base = base; this.name = name; this.world = world;
        this.hp = hp; this.damage = damage; this.speed = speed;
        this.ai = ai; this.boss = boss; this.phases = phases;
    }

    public static final class DropEntry {
        public final String item;
        public final double chance;
        public final int min, max;
        public DropEntry(String item, double chance, int min, int max) {
            this.item = item; this.chance = chance; this.min = min; this.max = max;
        }
    }
}
