package kr.reborn.npc.ai.utility;

/**
 * 입력값(원시 데이터)을 0~1 utility 값으로 변환하는 곡선.
 *
 * 예시:
 *   - "HP 비율" raw=0.2 (20%) → linear(invert) → utility=0.8 (HP 낮을수록 도주 utility↑)
 *   - "분노" raw=80 → sigmoid → utility=0.9 (임계 60 넘으면 급격히 상승)
 *   - "거리" raw=5블록 → exponential(invert) → utility=0.95 (가까울수록↑)
 */
public final class ResponseCurve {

    private ResponseCurve() {}

    /** raw를 [min, max] 범위로 정규화 → [0, 1]. clamp. */
    public static double normalize(double raw, double min, double max) {
        if (max <= min) return 0;
        return Math.max(0, Math.min(1, (raw - min) / (max - min)));
    }

    /** 선형. */
    public static double linear(double raw, double min, double max) {
        return normalize(raw, min, max);
    }

    /** 선형 반전 (낮을수록 utility↑). HP·욕구만족도 같은 데 사용. */
    public static double linearInverted(double raw, double min, double max) {
        return 1.0 - normalize(raw, min, max);
    }

    /** 시그모이드 — 임계점 근처에서 급격히 변화. midpoint 기준 +-5의 sharpness. */
    public static double sigmoid(double raw, double midpoint) {
        return sigmoid(raw, midpoint, 0.1);
    }

    public static double sigmoid(double raw, double midpoint, double sharpness) {
        return 1.0 / (1.0 + Math.exp(-sharpness * (raw - midpoint)));
    }

    /** 지수 (이른 값이 빠르게 큰 utility). */
    public static double exponential(double raw, double min, double max, double power) {
        double t = normalize(raw, min, max);
        return Math.pow(t, power);
    }

    /** 지수 반전 (낮은 값일수록 utility↑, 빠르게 감소). */
    public static double exponentialInverted(double raw, double min, double max, double power) {
        return 1.0 - exponential(raw, min, max, power);
    }

    /** 종 모양 — 중간값(midpoint)에서 최대, 양 끝에서 0. */
    public static double bell(double raw, double midpoint, double width) {
        double d = (raw - midpoint) / width;
        return Math.exp(-d * d);
    }

    /** 부울 — 조건 만족 시 1, 아니면 0. */
    public static double bool(boolean cond) {
        return cond ? 1.0 : 0.0;
    }
}
