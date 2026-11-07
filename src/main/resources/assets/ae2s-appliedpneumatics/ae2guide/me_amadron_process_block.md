---
navigation:
  parent: index.md
  title: Amadron Processing Station
  icon: ae2s-appliedpneumatics:me_amadron_process_station
  position: 70
item_ids:
  - ae2s-appliedpneumatics:me_amadron_process_station
  - ae2s-appliedpneumatics:me_amadron_extended_process_station
  - ae2s-appliedpneumatics:amadron_pattern
  - ae2s-appliedpneumatics:amadron_process_upgrade
---

# Amadron Processing Station

<Row>
  <BlockImage id="me_amadron_process_station" scale="4" />
  <BlockImage id="me_amadron_extended_process_station" scale="4" />
  <ItemImage id="amadron_pattern" scale="4" />
  <ItemImage id="amadron_process_upgrade" scale="4" />
</Row>

Processes orders coming from the **Amadron Wireless Terminal** and **ME network auto-crafting**.  
Once an order enters the queue, the station sends a drone request to the Amadron Shopping Center every 200 game ticks.

Amadron company drones will arrive to collect resources and deliver goods.  
Each drone only handles a single type of order.

During this process, you must always ensure that the top of the station is **unobstructed**; otherwise, Amadron drones may refuse to deploy.  
If this happens, your processing station will become clogged with resources and require manual clearing.

If the processing station is destroyed while the order queue is not empty, all uncompleted order resources will be returned to the ME network.  
If the ME network is unavailable or has insufficient space, the remaining resources will be ejected as item drops.

## Available Upgrade Cards
- **Acceleration Card**: Increases both the maximum number of drones available per dispatch and the maximum number of orders each drone can process.