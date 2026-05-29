package kr.reborn.hiddenclass.event;

import kr.reborn.hiddenclass.data.HiddenClass;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * 히든 클래스 해금 이벤트.
 *
 * 다른 플러그인이 후처리 가능: 칭호 부여, 스킬 자동 학습, 신앙 누적, 클랜 알림 등.
 * 발생 시 ConditionEngine이 이미 statBonuses·statOverrides 적용을 마친 상태.
 */
public final class RebornHiddenClassUnlockEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final HiddenClass hiddenClass;

    public RebornHiddenClassUnlockEvent(Player player, HiddenClass hc) {
        this.player = player;
        this.hiddenClass = hc;
    }

    public Player getPlayer() { return player; }
    public HiddenClass hiddenClass() { return hiddenClass; }
    public String id() { return hiddenClass.id; }

    @Override @NotNull public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
