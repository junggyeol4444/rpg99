package kr.reborn.economy.bank;

import java.util.UUID;

/**
 * 은행 계좌 — 예금 + 대출.
 *
 * 예금: 일 0.1% 이자 (config). 만기 7/30/90일 선택 → 만기까지 인출 금지하면 보너스.
 * 대출: 최대 보유 자산의 3배, 일 0.3% 이자. 미상환 시 신용 등급 -.
 *
 * 신용 등급 0~100: 대출 한도·이자율 결정.
 */
public final class BankAccount {

    public final UUID owner;
    public final String currency;
    public long deposit;
    public long loan;
    public int credit;          // 0~100
    public final long openedAt;
    /** 마지막 이자 정산 시각 */
    public long lastInterestAt;
    /** 정기예금 만기 시각 (0 = 일반예금) */
    public long maturityAt;

    public BankAccount(UUID owner, String currency) {
        this.owner = owner;
        this.currency = currency;
        this.openedAt = System.currentTimeMillis();
        this.lastInterestAt = openedAt;
        this.credit = 70; // 시작 70
    }

    public long netWorth() { return deposit - loan; }
}
