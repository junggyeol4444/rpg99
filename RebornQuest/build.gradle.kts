dependencies {
    compileOnly(project(":RebornCore"))
    compileOnly(project(":RebornNPC"))
    compileOnly(project(":RebornStat"))
    compileOnly(project(":RebornEconomy"))
    // RebornWorldAI는 이벤트로만 통신 — 컴파일 의존 없음
}
