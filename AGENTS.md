# AE2S Applied Pneumatics 开发约定

- 仓库名：`AE2S-AppliedPneumatics`；Mod ID 与资源命名空间：`ae2s-appliedpneumatics`。
- 项目展示名与成品文件名按用户命名，不强制添加 Legacy 标识。
- 目标平台为 Minecraft 1.12.2、Java 25、Cleanroom **0.4.0 正式版（不带 -alpha）**。
- AE2 支持目标为 AE2S / Supergiant，实际依赖版本见 `dependency-lock.json`；不能据此宣称兼容 AE2UEL 或原版 AE2。
- 保留上游 UI 的贴图、布局与交互。样式文件允许转换资源命名空间，不重新绘制界面。
- 物品翻译使用 1.12.2 的 `item.<namespace>.<path>.name`；不要同时生成裸物品键，避免再次出现可见的 `.name` 后缀。
- Ponder 为可选兼容，直接 API 引用隔离到 `compat/ponder`，客户端演示保持客户端隔离。
- 修改注册名时维护已有存档读取，并检查样板、空气资源类型、Ponder 结构与模型引用。
- 完成构建后，使用正式 Cleanroom 0.4.0 的安装包验证客户端和专用服务器、有无 Ponder 的组合；旧包的验收结果不能代替新成品。
- 保留原作者、美术资源署名及 LGPL / CC BY-NC-SA 许可说明。
