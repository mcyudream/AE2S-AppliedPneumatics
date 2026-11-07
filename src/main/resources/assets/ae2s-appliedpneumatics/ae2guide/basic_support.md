---
navigation:
  parent: index.md
  title: Basic Support
  icon: ae2:guide
  position: 10
---

# Basic Support

AE can now store air.  
Import Buses, Export Buses, and Storage Buses can all recognize air containers, and the UI can interact with air tanks.  
Additionally, **Air P2P** and **Temperature P2P** have been added.

<Row>
    <ItemImage id="ae2:import_bus" scale="4" />
    <ItemImage id="ae2:export_bus" scale="4" />
    <ItemImage id="ae2:storage_bus" scale="4" />
    <ItemImage id="air_p2p_tunel" scale="4" />
    <ItemImage id="heat_p2p_tunel" scale="4" />
</Row>

Details:
- <ItemLink id="ae2:export_bus" />: Outputs air into the linked air container. Automatically pressure-limited, will not cause the target to explode.
- <ItemLink id="ae2:import_bus" />: Extracts air from the linked air container. Can reduce down to 0 bar, but cannot replace a vacuum pump.
- <ItemLink id="ae2:storage_bus" />: Allows the ME network to recognize the linked air container. Negative-pressure containers are recognized as 0 mL.
- <ItemLink id="ae2s-appliedpneumatics:air_p2p_tunel" />: Receives input air and distributes it to the outputs.
- <ItemLink id="ae2s-appliedpneumatics:heat_p2p_tunel" />: Receives input heat and distributes it to the outputs.