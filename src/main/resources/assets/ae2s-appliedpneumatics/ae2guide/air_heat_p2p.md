---
navigation:
  parent: index.md
  title: New P2P
  icon: ae2s-appliedpneumatics:air_p2p_tunel
  position: 30
item_ids:
  - air_p2p_tunel
  - heat_p2p_tunel
---

# Air / Temperature P2P

<Row>
    <ItemImage id="air_p2p_tunel" scale="4" />
    <ItemImage id="heat_p2p_tunel" scale="4" />
</Row>

**Air P2P Tunnel** lets **compressed air** be transmitted across the AE network between one input and multiple outputs.

**Temperature P2P Tunnel** lets **heat** be exchanged between one input and multiple outputs.

## How to Use

1. Place a **P2P tunnel** and tune it to **Air P2P Tunnel** by right-clicking with any item that has an air capability, or tune it to **Temperature P2P Tunnel** by right-clicking with a Compressed Iron Block, Heat Pipe, or Heat Sink.
2. Place one or more **P2P tunnels** as outputs, then link them to the input with a Memory Card.
3. Place a block with temperature or air capability adjacent to begin transmission.

## Notes

### Air P2P
- Tune by right-clicking with any item that can store air. The Air P2P input exposes to the outside the **average pressure** of all outputs. When a higher-pressure pneumatic system is connected to it, air will flow into the input; the input then distributes the incoming air to the outputs **weighted by the pressure difference** between the average and each output.

### Temperature P2P
- Tune by right-clicking with a Compressed Iron Block, Heat Pipe, or Heat Sink. The Temperature P2P input exposes to the outside the **heat-capacity-weighted average temperature** of all outputs. When it connects to another thermal system, the input exchanges heat with it and then distributes the exchanged heat to the outputs **weighted by (each output’s heat capacity × the temperature difference between input and that output)**.