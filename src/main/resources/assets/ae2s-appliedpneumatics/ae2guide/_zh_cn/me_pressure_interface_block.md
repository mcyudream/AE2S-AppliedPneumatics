---
navigation:
  parent: index.md
  title: ME气压接口
  icon: ae2s-appliedpneumatics:me_pressure_interface_block
  position: 40
item_ids:
  - ae2s-appliedpneumatics:me_pressure_interface_block
---

# ME气压接口

<Row>
  <BlockImage id="me_pressure_interface_block" scale="4" />
</Row>

ME 气压接口提供了极为便捷的空气交互功能。  
你只需设置一个 **期望气压**，接口就会通过从 **ME 网络** 中 **取出/插入空气**，尽可能将气压维持在设定值。

对外，它可以通过 **气动工艺的压力管道** 传输空气，并遵循“高气压向低气压流动”的逻辑。  
同时，它还提供了一个物品槽，你可以放入 **气罐** 等物品。接口会为其充气或抽取空气，取决于双方的气压高低。

## 可用升级卡
- **[容积卡](upgrades.md)**：提高气压接口的基础容积。在相同气压下，容积越大，存储的空气总量越多。
- **[安全卡](upgrades.md)**：为气压接口提供额外保护。当瞬时输入气压过大，或 ME 网络不可用导致接口即将超出危险气压时，安全卡会阻止爆炸并排出多余空气。
- **[真空卡](upgrades.md)**：允许气压接口的期望气压为负值。不仅能替代真空泵的功能，还能连接未闭合的管道，从世界中源源不断地抽取空气。虽然速率有限，但几乎不消耗任何资源。