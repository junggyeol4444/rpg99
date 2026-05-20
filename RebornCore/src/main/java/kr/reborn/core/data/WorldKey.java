package kr.reborn.core.data;

/**
 * 13개 일반 세계 + 히든 월드 + 특수 공간 키.
 */
public enum WorldKey {
    LOBBY,        // 환생의 월드
    TUTORIAL,     // 튜토리얼 차원 (세계별 분기는 sub-world로)
    FANTASY, DEMON, HEAVEN, SPIRIT, MARTIAL, IMMORTAL, YOKAI,
    EARTH, MAGITECH, APOCALYPSE, CYBERPUNK, DRAGON, OCEAN,
    // 히든
    ABYSS, UNDERWORLD, TIME_REALM, DREAM, VOID, GOD;

    public boolean isMain13() {
        switch (this) {
            case FANTASY: case DEMON: case HEAVEN: case SPIRIT:
            case MARTIAL: case IMMORTAL: case YOKAI: case EARTH:
            case MAGITECH: case APOCALYPSE: case CYBERPUNK:
            case DRAGON: case OCEAN: return true;
            default: return false;
        }
    }

    /** 일반/특이 차원 분류 (초기 스탯 가중치 결정용) */
    public boolean isSpecialWorld() {
        switch (this) {
            case DEMON: case HEAVEN: case SPIRIT: case YOKAI: case IMMORTAL: return true;
            default: return false;
        }
    }
}
