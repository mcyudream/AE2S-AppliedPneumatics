# 正式 Cleanroom 0.4.0 验收

最终安装包：`AE2S-AppliedPneumatics-1.0.9-cleanroom.4.jar`

SHA-256：`82b6e90d4901e5d37be035fef79dc4c5f6167a6b8287139d1add95a49ea49f43`

全部场景运行官方 **Cleanroom 0.4.0 正式版（不带 -alpha）**，Java 25.0.5、
固定 AE2S `1.1.0+a47dfe029` 和 PNC `1.12.2-0.11.15-398`。
安装器 SHA-256、实际加载的 Mod/运行库哈希保存在各场景的 `runtime.json`；未使用 userdev 启动。

| 场景 | 功能检查数 | 结果 |
| --- | ---: | --- |
| client-with-ponder | 40 | FUNCTIONAL_PASS |
| client-without-ponder | 34 | FUNCTIONAL_PASS |
| server-with-ponder | 10 | FUNCTIONAL_PASS |
| server-without-ponder | 10 | FUNCTIONAL_PASS |

服务端覆盖 20 种元件的 long 存储、模拟操作与监听增量、256M 容量、真空卡，
PNC 原版充气站对超过 2³¹ mL 元件的持续充气、无线电池升级、真实三类总线，
气压/温度接口、空气/热 P2P、NBT 持久化、购物篮失败时原子拒绝、处理站优先级，
以及真实 PNC 无人机的手动交易、AE2S CPU 自动合成交易、流体付款与送货。

客户端在相同真实世界中完成上述检查，并逐项验证全部 35 个物品/方块的中英文显示名，
确认不含多余 `.name` 后缀；旧机器 ID 与旧空气资源类型的存档读取也经过检查。
逐项检查 35 个物品、12 个方块状态、10 档驱动器元件、两类 P2P 的真实烘焙模型与 atlas sprite，
并验证新旧命名空间的 70 个物品模型；资源重载后再次检查，保存全物品渲染截图。
随后通过真实鼠标事件、槽位点击和网络包验证
原版压力/温度按钮、普通/扩展处理站槽位、优先级子界面返回、中文商品搜索、购物篮、
空白样板编码、提交后真实送货、终端高度切换和样板槽持久化、便携终端三行上限。
有 Ponder 时还检查 33 个物品关联、全部三个完整时间线、结构无缺失、双语叙述与场景播放。
空气网络的 26 个关联复用同一个 storyboard，实际诊断无本模组重复场景 ID。
本次有 Ponder 的组合运行 `ponder-legacy-1.6.2.jar`，未安装 CraftTweaker；
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
