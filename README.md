# AE2S 应用气动 · Applied Pneumatics

**应用能源 2 超巨星（AE2S）× 气动工艺：重制版（PneumaticCraft: Repressurized）** 的附属模组，
将压缩空气存储、稳压温控和亚马龙交易自动化接入 ME 网络。

面向 **Minecraft 1.12.2 / Cleanroom 0.4.0 正式版 / Java 25**。
仓库名为 `AE2S-AppliedPneumatics`，Mod ID 与资源命名空间为 `ae2s-appliedpneumatics`。

## 功能概览

- **空气存储**：普通与便携空气元件各 10 档，容量从 1K 到 256M；便携元件也可作为气罐使用。
- **ME 气压接口**：向 ME 网络存取空气以维持目标气压，并遵循气动工艺由高压向低压扩散的规则。
- **ME 温控接口**：同时消耗 AE 能量与压缩空气来维持设定温度，并与相邻机器自然交换热量。
- **P2P 通道**：新增空气与温度两类通道，用于跨区域传输和分配。
- **亚马龙交易**：无线终端配合处理站下单；不限量报价可以保存为样板，由 AE2S 合成 CPU 自动执行。
- **总线兼容**：输入、输出和存储总线可以操作气动机器及容器中的空气。
- **新增报价**：补充基础资源的亚马龙买卖交易。

## 安装

从 [Releases](https://github.com/mcyudream/AE2S-AppliedPneumatics/releases) 下载
`AE2S-AppliedPneumatics-1.0.9-cleanroom.3.jar`，客户端与服务器均放入 `mods`。

| 依赖 | 验证版本 |
| --- | --- |
| Minecraft | 1.12.2 |
| Cleanroom | **0.4.0 正式版**，不带 `-alpha` |
| Java | **25** |
| AE2S / Supergiant | `1.1.0+a47dfe029`，Mod ID 为 `ae2` |
| 气动工艺：重制版 | `1.12.2-0.11.15-398`，[下载](https://www.curseforge.com/minecraft/mc-mods/pneumaticcraft-repressurized/files/2978408) |
| Ponder（可选） | 1.6.2，用于游戏内分步演示 |

AE2S 为本项目的目标实现；原版 AE2 与 AE2UEL 未纳入支持范围。
Ponder 独立安装，不安装也能使用全部核心玩法；Ponder 1.6.2 无需 CraftTweaker。
精确依赖来源与哈希见 [dependency-lock.json](dependency-lock.json)。
固定 AE2S 编译包、运行包及对应源码另存于 [构建依赖快照](https://github.com/mcyudream/AE2S-AppliedPneumatics/releases/tag/build-dependencies)，
避免上游滚动更新 dev 附件后无法复现；清单见 [build-dependencies.json](build-dependencies.json)。

## 使用方法

**存储与稳压**：把空气元件装入 ME 驱动器，用输入、输出或存储总线连接气动设备。
总线的资源类型需启用空气。气压接口使用原版 `-1 / -0.1 / +0.1 / +1` 按钮调节目标，
按住 Shift 可五倍调整；物品槽可以给气动工具、气罐和便携空气元件充放气。

**温度控制**：把温控接口放在机器旁，通过原版 `-10 / -1 / +1 / +10` 按钮设置目标摄氏温度，
Shift 可十倍调整。网络需要同时提供空气与 AE 能量。

**P2P 传输**：使用压力管协调空气 P2P，使用烈焰棒协调温度 P2P，
再用 AE2S 存储卡绑定输入与输出。通道需要网络供电与频道。

**亚马龙自动化**：处理站接入有电、有频道的 ME 网络，顶部保持露天，供无人机取货与送货。
普通处理站提供 9 格样板，扩展处理站提供 36 格。
把不限量报价编码为亚马龙样板并放入处理站后，合成 CPU 就能将交易纳入合成链。
物品和流体交易均由真实气动无人机完成，货物自动返回 ME 库存。

**无线终端**：充电后，潜行右键处理站绑定；再次右键打开商品界面。
双列交易卡片支持搜索与滚动：右键加一、左键减一，Shift 右键翻倍、Shift 左键减半，中键清空。
点击“提交订单”从 ME 库存付款；把空白样板放入终端槽，选择不限量报价，再点击箭头编码。
侧栏按钮可以切换终端高度。支持访问其他维度中已经加载的处理站，不会主动加载区块。

处理站界面的取消按钮会取消尚未派出无人机的订单并退回付款；已经派出的无人机继续执行。
对没有待派订单、保留付款和缓冲资源的普通站使用升级物品，即可保留样板与升级卡并升为扩展站。

## 升级与教程

- **安全卡**：保护满载空气元件，并让气压接口在 20 bar 上限排气。
- **真空卡**：在元件中销毁溢出空气，在接口中允许最低 -1 bar。
- **容积卡**：提高气压接口容积与温控接口热容。
- **加速卡**：提高温控和处理站派单速度。
- **充气卡**：让便携空气元件为背包内的气动工具供气。

元件可在元件工作台安装升级。空元件潜行右键可拆解，并返还升级卡。
安装 Ponder 后，可以观看空气网络、温控和亚马龙自动化三个中英文分步演示。

## 界面与版本适配

界面沿用上游原版贴图、布局、控件位置和交互；样式文件仅替换资源命名空间。
[查看实机截图](verification/cleanroom-0.4.0/client-without-ponder/gameplay/screenshots/wireless-cart-pattern-zh_cn.png)。

1M–256M 元件与扩展处理站使用本项目提供的合成路线，无需现代版 MEGA Cells 或 ExtendedAE。
高阶普通元件由四个上一级空元件和 MEGA 空气外壳合成，便携版另加 ME 箱子和能量元件；
可在 `ae2s-appliedpneumatics.cfg` 中关闭 `enableLargeCells`。
现代材料到 1.12.2 的映射、运行差异见 [迁移说明](docs/MIGRATION.md)。

`1.0.9-cleanroom.3` 修复物品名称多出 `.name` 的问题，统一项目命名和 Mod ID，
并提供旧注册名、机器存档及空气资源类型的读取兼容。

## 构建与验证

```bash
./tools/dev.sh build reobfRuntimeHarnessJar
DISPLAY=:0 python3 tools/verify.py --ponder-jar /path/to/ponder.jar
python3 tools/package.py
```

构建需要 Java 25，可通过 `AP_JAVA_HOME` 指定 JDK。
编译 SDK 为 Cleanroom `0.7.14-alpha`，运行验收基线为 **0.4.0 正式版**。
正式安装使用 SRG 主 Jar，`-mcp.jar` 为开发包，`-sources.jar` 为源码；依赖不打入本体。

验收覆盖真实 AE2S 网络、空气总线、P2P、合成 CPU、PNC 无人机物品/流体交易、
全部物品中英文名称、原版 UI 操作和可选 Ponder 播放。
具体成品哈希与客户端/专服、有/无 Ponder 四组合结果见 [验收记录](verification/README.md)。

## 来源与许可

功能与代码基于 [Frostbite-time/AppliedPneumatics](https://github.com/Frostbite-time/AppliedPneumatics)
1.0.9，固定提交 `29e33cbf5df5aa5b6c83c212338b3f7635cd0742`。
本页功能介绍参考 [FormlessDragon/AppliedPneumatics](https://github.com/FormlessDragon/AppliedPneumatics)，
翻译为中文并按本项目的实际版本、依赖与功能调整。

- 代码：Frostbite，**LGPL-3.0-or-later**；1.12.2 / AE2S 移植维护：yudream。
- 美术资源：MHanHanBing 及上游贡献者，**CC BY-NC-SA 3.0**。
- 便携元件侧面资源包含 Applied Energistics 2 与 MEGA Cells 的署名。

详见 [LICENSE](LICENSE)、[NOTICE.md](NOTICE.md) 与 [upstream-lock.json](upstream-lock.json)。
