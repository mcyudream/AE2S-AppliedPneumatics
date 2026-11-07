---
navigation:
  parent: index.md
  title: ME Pressure Interface
  icon: ae2s-appliedpneumatics:me_pressure_interface_block
  position: 40
item_ids:
  - ae2s-appliedpneumatics:me_pressure_interface_block
---

# ME Pressure Interface

<Row>
  <BlockImage id="me_pressure_interface_block" scale="4" />
</Row>

The ME Pressure Interface provides an easy way to interact with air.  
You only need to set a **target pressure**, and the interface will insert or extract air from the **ME network** to keep the pressure as close to your desired value as possible.

Externally, it can transfer air through **PneumaticCraft pressure pipes**, following the logic of air naturally flowing from high pressure to low pressure.  
It also provides an item slot where you can insert **air containers** such as air tanks. The interface will either charge them with air or drain air from them, depending on the relative pressure levels.

## Available Upgrade Cards
- **[Volume Card](upgrades.md)**: Increases the base volume of the pressure interface. At the same pressure, a larger volume means more total air storage.
- **[Security Card](upgrades.md)**: Provides an extra layer of protection. If too much pressure is suddenly input or the ME network becomes unavailable, causing the interface to exceed its danger threshold, the Security Card prevents explosions and vents the excess air.
- **[Vacuum Card](upgrades.md)**: Allows the target pressure to be negative. This not only replaces the function of a vacuum pump but also enables the interface to draw air continuously from the world through open pipes. The rate is limited, but it consumes almost no resources.