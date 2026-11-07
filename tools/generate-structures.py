#!/usr/bin/env python3
from pathlib import Path
from structure import Structure

root = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/ae2s-appliedpneumatics/ponder'
root.mkdir(parents=True, exist_ok=True)
for scene, machine in [('air_network', 'me_pressure_interface_block'), ('temperature', 'me_temperature_interface'), ('amadron', 'me_amadron_process_station')]:
    s = Structure()
    s.machine(1, 1, 2, 'vibration_chamber').machine(2, 1, 3, 'drive', forward='NORTH')
    s.machine(3, 1, 2, '64k_crafting_storage')
    s.cable(3, 1, 3, connections=('west', 'east', 'north', 'south'))
    s.block(4, 1, 3, 'ae2s-appliedpneumatics:' + machine, nbt={'id': 'ae2s-appliedpneumatics:' + machine, 'x': 4, 'y': 1, 'z': 3})
    if scene == 'air_network':
        s.block(1, 1, 3, 'pneumaticcraft:air_compressor')
        s.block(5, 1, 3, 'pneumaticcraft:pressure_tube')
    elif scene == 'temperature':
        s.block(5, 1, 3, 'pneumaticcraft:refinery')
    else:
        s.chest(5, 1, 3)
    (root / (scene + '.nbt')).write_bytes(s.data())
