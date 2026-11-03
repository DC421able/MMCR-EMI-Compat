Gradle 构建所需的编译期依赖（不会被打进产物）。

把下面两个 jar 复制到本目录（文件名需与 gradle.properties 里的 mmcr_jar / jei_jar 一致）：

  mmcr-neoforge-mc1.21.1-0.1.4.jar
  jei-1.21.1-neoforge-19.56.0.441.jar

来源：
  * MMCR：https://modrinth.com/mod/modular-machinery-community-refoxed
  * JEI ：https://modrinth.com/mod/jei

说明：
  * MMCR 与 JEI 只用于编译（compileOnly），运行时由玩家实例里的模组提供，
    本补丁的 jar 不会内嵌它们。
  * 本机若无法访问 maven.neoforged.net，请改用离线构建：
        powershell -NoProfile -ExecutionPolicy Bypass -File ..\tools\build.ps1
    该脚本会直接从本机实例读取 MMCR / JEI，不需要本目录。
