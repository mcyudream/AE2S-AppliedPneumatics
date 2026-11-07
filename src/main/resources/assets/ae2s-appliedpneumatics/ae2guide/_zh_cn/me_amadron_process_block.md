---
navigation:
  parent: index.md
  title: 亚马龙处理站
  icon: ae2s-appliedpneumatics:me_amadron_process_station
  position: 70
item_ids:
  - ae2s-appliedpneumatics:me_amadron_process_station
  - ae2s-appliedpneumatics:me_amadron_extended_process_station
  - ae2s-appliedpneumatics:amadron_pattern
  - ae2s-appliedpneumatics:amadron_process_upgrade
---

# 亚马龙处理站

<Row>
  <BlockImage id="me_amadron_process_station" scale="4" />
  <BlockImage id="me_amadron_extended_process_station" scale="4" />
  <ItemImage id="amadron_pattern" scale="4" />
  <ItemImage id="amadron_process_upgrade" scale="4" />
</Row>

处理来自 **亚马龙无线终端** 和 **ME 网络自动合成** 的订单。  
当订单进入队列后，处理站会每隔 200 游戏刻向亚马龙购物中心发送一次无人机请求。

亚马龙公司的无人机会前来收取资源并派送货物。  
每台无人机只会处理 **一种类型的订单**。

在此过程中，你必须始终确保处理站的顶部 **无遮挡物**；  
否则亚马龙无人机可能会拒绝出动。  
这会导致处理站出现资源堵塞，需要你手动清理。

如果处理站在订单队列非空时被破坏，所有未完成的订单资源都会返还到 ME 网络。  
若 ME 网络不可用或空间不足，剩余的资源会作为掉落物弹出。

## 可用升级卡
- **加速卡**：提升每次派单时可用无人机的最大数量，以及每台无人机可处理的最大订单数。