---
navigation:
  parent: index.md
  title: 基础支持
  icon: ae2:guide
  position: 10
---

# 基础支持

AE现在能够存储空气，输入总线、输出总线、存储总线均能识别空气容器，UI中能与气罐进行交互。并且添加了空气P2P
和温度P2P。

<Row>
    <ItemImage id="ae2:import_bus" scale="4" />
    <ItemImage id="ae2:export_bus" scale="4" />
    <ItemImage id="ae2:storage_bus" scale="4" />
    <ItemImage id="air_p2p_tunel" scale="4" />
    <ItemImage id="heat_p2p_tunel" scale="4" />
</Row>

详细介绍：
 - <ItemLink id="ae2:export_bus" />：向链接的空气容器中输出空气，自动限压，不会使对方爆炸；
 - <ItemLink id="ae2:import_bus" />：从链接的空气容器中抽取空气，最低抽到0bar，不能替代真空泵；
 - <ItemLink id="ae2:storage_bus" />：让ME网络识别其链接的空气容器，负压容器会被识别为0ml；
 - <ItemLink id="ae2s-appliedpneumatics:air_p2p_tunel" />：接收输入的空气，分配到输出端。
 - <ItemLink id="ae2s-appliedpneumatics:heat_p2p_tunel" />：接收输入的传递的热量，分配到输出端。