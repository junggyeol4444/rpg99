# 환생의 월드 리소스팩

CustomModelData를 이용한 커스텀 모델·텍스처 패치.

## 구조

```
resourcepack/
├── pack.mcmeta
├── pack.png            (배포 시 추가)
└── assets/
    └── minecraft/
        ├── models/
        │   └── item/
        │       ├── iron_sword.json       # CMD 2001~2099
        │       ├── netherite_sword.json  # CMD 2001~2099 (전설/창세 무기)
        │       ├── gold_nugget.json      # CMD 4001~4099 (장신구)
        │       ├── written_book.json     # CMD 5001~5099 (비급·마법서)
        │       ├── glow_berries.json     # CMD 6001~6099 (영약)
        │       ├── enchanted_golden_apple.json  # CMD 7001~7099 (선계 단약)
        │       └── ...
        └── textures/
            └── item/
                └── reborn/
                    ├── excalibur.png
                    ├── flame_sword.png
                    ├── cheonma_blade.png
                    └── ...
```

## CustomModelData 매핑

| 범위        | 분류         |
|-------------|--------------|
| 1001~1099   | 화폐         |
| 2001~2099   | 무기 (검·도) |
| 3001~3099   | 갑옷         |
| 4001~4099   | 장신구       |
| 5001~5099   | 비급·마법서  |
| 6001~6099   | 무협 영약    |
| 7001~7099   | 선계 단약    |
| 8001~8099   | 기연 아이템  |
| 9001~9099   | 화폐         |

## 모델 JSON 템플릿 예시 (item/netherite_sword.json)

```json
{
  "parent": "item/handheld",
  "textures": {
    "layer0": "item/netherite_sword"
  },
  "overrides": [
    { "predicate": { "custom_model_data": 2002 }, "model": "item/reborn/flame_sword" },
    { "predicate": { "custom_model_data": 2003 }, "model": "item/reborn/excalibur" },
    { "predicate": { "custom_model_data": 2004 }, "model": "item/reborn/cheonma_blade" }
  ]
}
```

## 배포

서버 server.properties에 `resource-pack=` 설정 또는 플레이어에게
`/resourcepack` 명령으로 적용.
