#!/usr/bin/env python3
"""Package only the exact jar accepted in all four official Cleanroom 0.4.0 scenarios."""
import hashlib
import json
import re
from pathlib import Path
import shutil
import zipfile

ROOT = Path(__file__).resolve().parents[1]
VERSION = re.search(r"^version = '([^']+)'", (ROOT / 'build.gradle').read_text(), re.MULTILINE).group(1)
NAME = 'AE2S-AppliedPneumatics-' + VERSION


def digest(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def main():
    artifact = ROOT / 'build/libs' / (NAME + '.jar')
    summary = json.loads((ROOT / 'verification/summary.json').read_text())
    assert summary['cleanroom'] == '0.4.0'
    assert summary['artifact_sha256'] == digest(artifact), 'Accepted artifact differs from build'
    variants = [side + '-' + ponder + '-ponder' for side in ('client', 'server') for ponder in ('with', 'without')]
    ponder_runtime = set()
    crafttweaker_present = False
    for variant in variants:
        assert summary['scenarios'][variant]['result'] == 'FUNCTIONAL_PASS', variant
        evidence = ROOT / 'verification/cleanroom-0.4.0' / variant
        runtime = json.loads((evidence / 'runtime.json').read_text())
        result = json.loads((evidence / 'result.json').read_text())
        assert runtime['cleanroom'] == '0.4.0' and not runtime['userdev'], variant
        assert runtime['jars'][artifact.name] == digest(artifact), variant
        assert result['gameplay_report']['result'] == 'PASS', variant
        if '-with-ponder' in variant:
            ponder_runtime.update(name for name in runtime['jars'] if name.startswith('ponder-legacy-'))
            crafttweaker_present |= any('crafttweaker' in name.lower() for name in runtime['jars'])

    rows = '\n'.join('| ' + name + ' | ' + str(summary['scenarios'][name]['checks']) + ' | FUNCTIONAL_PASS |' for name in variants)
    ponder_names = '、'.join('`' + name + '`' for name in sorted(ponder_runtime))
    crafttweaker_status = '已安装' if crafttweaker_present else '未安装'
    verification_readme = f"""# 正式 Cleanroom 0.4.0 验收

最终安装包：`{artifact.name}`  
SHA-256：`{digest(artifact)}`

全部场景运行官方 **Cleanroom 0.4.0 正式版（不带 -alpha）**，Java 25.0.5、
固定 AE2S `1.1.0+a47dfe029` 和 PNC `1.12.2-0.11.15-398`。
安装器 SHA-256、实际加载的 Mod/运行库哈希保存在各场景的 `runtime.json`；未使用 userdev 启动。

| 场景 | 功能检查数 | 结果 |
| --- | ---: | --- |
{rows}

服务端覆盖 20 种元件的 long 存储、模拟操作与监听增量、256M 容量、真空卡，
PNC 原版充气站对超过 2³¹ mL 元件的持续充气、无线电池升级、真实三类总线，
气压/温度接口、空气/热 P2P、NBT 持久化、购物篮失败时原子拒绝、处理站优先级，
以及真实 PNC 无人机的手动交易、AE2S CPU 自动合成交易、流体付款与送货。

客户端在相同真实世界中完成上述检查，并逐项验证全部 35 个物品/方块的中英文显示名，
确认不含多余 `.name` 后缀；旧机器 ID 与旧空气资源类型的存档读取也经过检查。
随后通过真实鼠标事件、槽位点击和网络包验证
原版压力/温度按钮、普通/扩展处理站槽位、优先级子界面返回、中文商品搜索、购物篮、
空白样板编码、提交后真实送货、终端高度切换和样板槽持久化、便携终端三行上限。
有 Ponder 时还检查 33 个物品关联、全部三个完整时间线、结构无缺失、双语叙述与场景播放。
空气网络的 26 个关联复用同一个 storyboard，实际诊断无本模组重复场景 ID。
本次有 Ponder 的组合运行 {ponder_names}，{crafttweaker_status} CraftTweaker；
`dependency-lock.json` 的 Ponder 1.4.0 仍为历史编译依赖，运行安装包以 `runtime.json` 为准。
截图均检查 OpenGL 错误，并人工查看布局。

- [最终汇总](summary.json)
- [有 Ponder 客户端报告](cleanroom-0.4.0/client-with-ponder/gameplay/report.json)
- [有 Ponder 客户端截图](cleanroom-0.4.0/client-with-ponder/gameplay/screenshots/)
- [原版 UI 操作截图](cleanroom-0.4.0/client-without-ponder/gameplay/screenshots/wireless-cart-pattern-zh_cn.png)

`tools/verify-assets.py` 另核对 56 个模型、所有本地资源路径、方块状态与语言，
以及固定上游的 16 个 UI 贴图/样式/元数据文件：贴图字节保持一致，样式只替换资源命名空间。

日志保留依赖自身的 `minecraft:air#inventory` 模型提示和旧版 PNC DramaSplash 失效 URL 提示；
这些不属于本模组 GUI、模型或功能异常。验收源码在 `src/runtimeHarness/`，测试 Jar 不用于安装。

重现：`./tools/dev.sh build reobfRuntimeHarnessJar`，再运行
`DISPLAY=:0 python3 tools/verify.py --ponder-jar /path/to/ponder-legacy.jar`。
四组合及主包哈希均一致后，`python3 tools/package.py` 才会生成分发目录。
"""
    (ROOT / 'verification/README.md').write_text(verification_readme)

    destination = ROOT / 'dist'  / (NAME + '-cleanroom-0.4.0')
    destination.mkdir(parents=True, exist_ok=True)
    for name in (NAME + '.jar', NAME + '-sources.jar'):
        shutil.copy2(ROOT / 'build/libs' / name, destination / name)
    for name in ('README.md', 'AGENTS.md', 'LICENSE', 'NOTICE.md', 'upstream-lock.json', 'dependency-lock.json', 'build-dependencies.json'):
        shutil.copy2(ROOT / name, destination / name)
    shutil.copytree(ROOT / 'docs', destination / 'docs', dirs_exist_ok=True)
    shutil.copytree(ROOT / 'verification', destination / 'verification', dirs_exist_ok=True)

    archive = destination / (NAME + '-source.zip')
    roots = ('src', 'gradle', 'tools', 'docs')
    with zipfile.ZipFile(archive, 'w', zipfile.ZIP_DEFLATED) as output:
        for name in roots:
            for path in sorted((ROOT / name).rglob('*')):
                if path.is_file() and '__pycache__' not in path.parts:
                    output.write(path, str(path.relative_to(ROOT)))
        for name in ('build.gradle', 'settings.gradle', 'gradle.properties', 'gradlew', 'gradlew.bat',
                     '.gitignore', 'README.md', 'AGENTS.md', 'LICENSE', 'NOTICE.md', 'upstream-lock.json', 'dependency-lock.json', 'build-dependencies.json'):
            output.write(ROOT / name, name)
    checksums = {str(path.relative_to(destination)): digest(path)
                 for path in sorted(destination.rglob('*')) if path.is_file() and path.name != 'SHA256SUMS'}
    (destination / 'SHA256SUMS').write_text(''.join(value + '  ' + name + '\n' for name, value in checksums.items()))
    print(destination)
    print(artifact.name + ' SHA-256 ' + digest(artifact))


if __name__ == '__main__':
    main()
