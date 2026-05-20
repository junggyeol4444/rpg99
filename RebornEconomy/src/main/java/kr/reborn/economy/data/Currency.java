package kr.reborn.economy.data;

import kr.reborn.core.data.WorldKey;
import org.bukkit.Material;

/** 13세계 통화 정의. */
public final class Currency {
    public final String id;
    public final String displayName;
    public final WorldKey world;
    public final Material icon;
    public final int model;

    public Currency(String id, String displayName, WorldKey world, Material icon, int model) {
        this.id = id;
        this.displayName = displayName;
        this.world = world;
        this.icon = icon;
        this.model = model;
    }
}
