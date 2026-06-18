package kr.reborn.core.event;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * NPC가 장기 목표를 달성해 세계에 "실제" 영향을 남길 때 발생.
 *
 * RebornNPC가 발행하고, 다른 플러그인이 구독해 교단(RebornGod)·왕국(RebornClan)·
 * 상점(RebornEconomy) 등 자기 도메인 데이터를 등록한다.
 * 리스너가 아직 없어도 무해 — 추상 상태가 아닌 진짜 사건의 단일 진입점.
 */
public final class RebornNpcWorldImpactEvent extends Event {

    public enum Kind { TOWN_FOUNDED, RELIGION_FOUNDED, ASCENDED, SHOP_OPENED }

    private static final HandlerList HANDLERS = new HandlerList();

    public final String npcId;
    public final Kind kind;
    /** 교단명·왕국 칭호·상점명 등. */
    public final String payload;
    public final Location location;

    public RebornNpcWorldImpactEvent(String npcId, Kind kind, String payload, Location location) {
        this.npcId = npcId;
        this.kind = kind;
        this.payload = payload == null ? "" : payload;
        this.location = location;
    }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
