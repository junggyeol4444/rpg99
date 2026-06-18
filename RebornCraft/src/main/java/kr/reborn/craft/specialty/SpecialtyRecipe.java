package kr.reborn.craft.specialty;

import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

/**
 * 특화 레시피 — 재료 → 결과물 + 난이도.
 *
 * difficultyTier: 1~5 — 높을수록 필요 스탯·숙련 ↑, 실패시 재료 손실 ↑
 */
public final class SpecialtyRecipe {

    public final String id;
    public final SpecialtyType type;
    public final String name;
    public final Map<Material, Integer> ingredients = new HashMap<>();
    /** 결과물 — Material 또는 customItemId (RebornCraft.ItemRegistry) */
    public final String resultId;
    public final Material resultMaterial;
    public final int resultAmount;
    public final int difficultyTier;
    public final double requiredPrimaryStat;
    public final double requiredSecondaryStat;
    public final int requiredProficiency;

    public SpecialtyRecipe(String id, SpecialtyType type, String name,
                           String resultId, Material resultMat, int resultAmount,
                           int difficultyTier, double reqPrim, double reqSec, int reqProf) {
        this.id = id; this.type = type; this.name = name;
        this.resultId = resultId; this.resultMaterial = resultMat;
        this.resultAmount = resultAmount; this.difficultyTier = difficultyTier;
        this.requiredPrimaryStat = reqPrim; this.requiredSecondaryStat = reqSec;
        this.requiredProficiency = reqProf;
    }
}
