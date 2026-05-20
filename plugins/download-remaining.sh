#!/usr/bin/env bash
#
# 환생의 월드 — 남은 5개 외부 플러그인 다운로드 스크립트
#
# 본 저장소에는 GitHub Releases에서 직접 받을 수 있는 5개가 이미 포함되어 있고,
# 자체 CDN 또는 SpigotMC에 배포되는 다음 5개는 이 스크립트로 받는다.
#
# 사용:
#   chmod +x plugins/download-remaining.sh
#   ./plugins/download-remaining.sh
#
# 결과: plugins/ 디렉토리에 5개 jar 추가
#
# 주의: 각 플러그인 사이트가 버전을 새로 올리면 URL이 변할 수 있다.
# 실패 시 아래 LINK에서 수동 다운로드.

set -e
cd "$(dirname "$0")"

echo "=========================================="
echo "  환생의 월드 외부 플러그인 다운로드"
echo "=========================================="
echo

# ─── LuckPerms ─────────────────────────────────────
# 사이트: https://luckperms.net/download
# CI:    https://ci.lucko.me/job/LuckPerms/lastSuccessfulBuild/artifact/bukkit/loader/build/libs/
echo "[1/5] LuckPerms..."
LP_URL="https://download.luckperms.net/1572/bukkit/loader/LuckPerms-Bukkit-5.5.5.jar"
curl -fLo LuckPerms.jar "$LP_URL" \
  && echo "  ✓ LuckPerms.jar 다운로드 완료" \
  || echo "  ✗ 실패 — https://luckperms.net/download 에서 수동 다운로드"
echo

# ─── CoreProtect ───────────────────────────────────
# 사이트: https://www.coreprotect.net/downloads/
# Modrinth: https://modrinth.com/plugin/coreprotect
echo "[2/5] CoreProtect..."
CP_URL="https://www.coreprotect.net/downloads/CoreProtect-23.2.jar"
curl -fLo CoreProtect.jar "$CP_URL" \
  && echo "  ✓ CoreProtect.jar 다운로드 완료" \
  || echo "  ✗ 실패 — https://www.coreprotect.net/downloads/ 에서 수동 다운로드"
echo

# ─── Chunky ────────────────────────────────────────
# 사이트: https://hangar.papermc.io/pop4959/Chunky
# Modrinth: https://modrinth.com/plugin/chunky
echo "[3/5] Chunky..."
CH_URL="https://hangarcdn.papermc.io/plugins/pop4959/Chunky/versions/1.4.36/PAPER/Chunky-Bukkit-1.4.36.jar"
curl -fLo Chunky.jar "$CH_URL" \
  && echo "  ✓ Chunky.jar 다운로드 완료" \
  || echo "  ✗ 실패 — https://hangar.papermc.io/pop4959/Chunky 에서 수동 다운로드"
echo

# ─── WorldEdit ─────────────────────────────────────
# 사이트: https://enginehub.org/worldedit/
# Modrinth: https://modrinth.com/plugin/worldedit
echo "[4/5] WorldEdit..."
WE_URL="https://dev.bukkit.org/projects/worldedit/files/latest"
curl -fLo WorldEdit.jar "$WE_URL" \
  && echo "  ✓ WorldEdit.jar 다운로드 완료" \
  || echo "  ✗ 실패 — https://enginehub.org/worldedit/ 에서 수동 다운로드"
echo

# ─── VoidGen ───────────────────────────────────────
# 사이트: https://www.spigotmc.org/resources/voidgen.74599/
# (Spigot 리소스 — 직접 다운로드 URL은 변동, 수동 받기 권장)
echo "[5/5] VoidGen..."
echo "  ℹ️ VoidGen은 SpigotMC에 배포 — 자동 다운로드 불가."
echo "    https://www.spigotmc.org/resources/voidgen.74599/ 에서 수동 다운로드"
echo

echo "=========================================="
echo "  완료. plugins/ 디렉토리 확인:"
ls -la *.jar 2>/dev/null | awk '{printf "  %-40s %s\n", $NF, $5" bytes"}'
echo "=========================================="
