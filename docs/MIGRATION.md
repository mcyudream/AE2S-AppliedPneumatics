# Applied Pneumatics 1.0.9 → 1.12.2

固定上游 `29e33cbf5df5aa5b6c83c212338b3f7635cd0742`（1.21.1 分支）。
Mod ID 为 `ae2s-appliedpneumatics`；保留 35 个方块/物品的路径与原始资源署名。
旧命名空间 `appliedpneumatics` 的方块/物品通过缺失映射迁移；机器标签与空气资源类型提供读取兼容。
代码沿用 LGPL-3.0-or-later，艺术资源沿用 CC BY-NC-SA 3.0。

## 平台映射

| 上游接口 | 移植实现 |
| --- | --- |
| NeoForge 注册、能力、组件与菜单 | Forge 1.12 注册事件、旧接口、NBT 和客户端隔离的菜单/网络包 |
| `appeng` 资源类型与存储 | AE2S `ae2` API；空气仍为单例 AEKey，250 mL/byte |
| 普通 StorageCell | 实现 AE2S StorageCell/StorageCellStatistics，插入与提取同步发布有符号增量 |
| PNC `IAirHandlerMachine` 能力 | PNC 0.11.15 的 `IPneumaticMachine` / `IAirHandler` |
| 物品空气能力 | PNC 1.12 `IPressurizable`；便携元件内部仍使用 long |
| 部件空气与热能力 | 仅给 AE2S TileCableBus 增加旧 PNC 机器/热交换接口，按面委托到本模组 P2P |
| 现代 Amadron ResourceLocation 报价 ID | 序列化完整的旧 PNC 报价，并由服务端报价管理器重新验证身份与库存 |
| GuideME | 可选 Ponder：空气网络、温控与亚马龙三个完整流程、33 个物品关联、中英文叙述 |
| 现代数据包机器配方 | 固定 JSON 转换后调用旧 PNC 公开配方 API；11 报价 + 16 装配 + 2 压力室 |

所有用户动作在服务器当前容器校验后执行。无线报价采用固定快照同步，购物篮和编码动作绑定窗口及快照身份，服务器重新验证交易；
NaN/无穷设定值被拒绝，交易次数与队列有上限。

## 版本差异

- AE2 仅针对 AE2S `1.1.0+a47dfe029`，使用 Java 25；没有声称支持原版 AE2 或 AE2UEL。
- Cleanroom 0.4.0 正式版专服的 Kirino 客户端发现调用通过版本/专服双重条件兼容；
  较新加载器不应用该修正。Mixin 采用正式 0.4.0 注解 API，无 Minecraft 字符串注入点。
- 原版 PNC 旧空气量/容积 API 使用 int。20 种元件容量及便携元件压力计算使用 long；
  单次旧接口传输最多 Integer.MAX_VALUE mL，旧接口容积视图饱和为 Integer.MAX_VALUE / 20，
  避免 PNC 充气站将 `20 × 容积` 转为 int 时溢出，并继续允许超过 2³¹ mL 的元件充放气；
  存储容量与满载 20 bar 不受影响。
- 空气 P2P 输出端的旧接口空气/容积比固定为 50 bar，防止 PNC 旧扩散公式倒灌；
  输入端按实际输出邻居聚合视图和压差分配，每 mL 都转移，接收设备的超压由 PNC 自身处理。
- 热 P2P 使用热容加权视图和热容比例分配；输出端及离线通道采用绝热阻值，防止重复交换。
- 1.12.2 缺少当前现代 MEGA Cells / ExtendedAE 运行库，因此高阶元件与扩展站提供本地配方路线。
  原版只影响配方启用的可选库不会变成必装前置。
- MEGA 外壳：强化压力管映射为高级压力管，钢锭映射为压缩铁；数据存储组件映射为
  `pneumaticcraft:network_component` 的 metadata 2；压力要求保留 4.95 bar。
  ExtendedAE 的四档福鲁伊克斯晶芽装配配方映射至 AE2S 内置的同类 `entro_budding_*` 方块。
- 原版机器界面与无线终端直接复用上游样式 JSON、贴图、尺寸、按钮位置和交互。
  普通站 9 格、扩展站 36 格样板，各有 9 格付款输入与 9 格到货输出；流体缓冲 64000 mB。
  待派队列最多 512 次交易；普通增减最多 64，Shift 翻倍最多 256，与原版卡片一致。
  购物篮提交先合计物品/流体付款与共享缓冲空间，完整校验后再扣款。
  便携空气元件沿用原版终端样式；客户端适配补回 AE2S 样式解析暂缺的 `maxRows: 3` 上限，仅影响本模组空气元件。真实无人机损失会按 PNC 原生行为影响交易，
  合成 CPU 仍会等待未收到的产物。保持处理站区块加载与顶部通畅。
- 取消按钮明确只取消尚未派单的请求；它不会撤回已在飞行的无人机。
- 无线终端直接绑定处理站，使用上游双列卡片、可伸缩高度、购物篮与样板编码界面；AE2S 万用终端合并不属于本移植的功能。

## 重现

`tools/port-resources.py` 从固定源码重建配方/语言、界面样式与 1.12 模型路径；
`tools/generate-structures.py` 重建三份 Ponder 结构。
`tools/verify-assets.py` 检查模型、纹理、方块状态、语言、原版 UI 文件哈希与命名空间转换与可选 API 隔离。
`tools/verify.py` 使用实际 SRG 成品和官方 Cleanroom 0.4.0 安装配方验收，
开发测试 Jar 不打入正式成品。

## 命名与翻译修复

`1.0.9-cleanroom.3` 使用 `AE2S-AppliedPneumatics` 仓库名和成品名，Mod ID 为 `ae2s-appliedpneumatics`。
1.12.2 的物品显示名处理会先翻译裸键再添加 `.name`；原生成器同时保留两种键，导致中文名称后仍出现 `.name`。
现在只生成物品的 `.name` 键，并在真实中英文客户端检查全部 35 个物品/方块显示名。
UI 贴图的字节保持上游原样；JSON 只映射命名空间，校验同时保留原始与映射后的哈希。

## 改名后的渲染资源兼容

`1.0.9-cleanroom.4` 将内容注册名与渲染路径分开。物品、方块状态映射、驱动器元件、P2P 与 GUI
继续读取原始 `appliedpneumatics` 渲染路径，同时在成品中保留 `ae2s-appliedpneumatics` 下的对应资源。
`tools/generate-resource-aliases.py` 在构建时生成旧命名空间的模型、贴图与方块状态，二进制贴图保持原样，
JSON 中的自有资源引用同步转换。配方、语言、Ponder、注册 ID 和存档写入仍使用新 Mod ID。
主包和源码 Jar 都由 `tools/verify-packaged-resources.py` 检查两套资源完整性。

实际客户端逐项检查全部 35 个物品、12 个方块状态、10 档驱动器元件和两类 P2P 的模型、面与 atlas sprite，
并检查新旧命名空间的 70 个物品模型；资源重载后重复检查，另保存全物品渲染截图。
此前 `.3` 包在独立环境、VintageFix、VintageFix + StellarCore 的定点检查中未复现全量缺材质；
本次兼容修正覆盖了上版缺少旧资源路径的问题，不将这些定点检查当作用户整合包问题已经复现的证据。
