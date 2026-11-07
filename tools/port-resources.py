#!/usr/bin/env python3
"""Reproduce recipes, translations and original UI using the pinned upstream checkout."""
import hashlib
import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
UPSTREAM = ROOT.parent.parent / 'upstream/AppliedPneumatics'
MODID = 'ae2s-appliedpneumatics'
SOURCE_NAMESPACE = 'appliedpneumatics'
DEST = ROOT / 'src/main/resources/assets' / MODID
ALIASES = {
    'pneumaticcraft:reinforced_pressure_tube': 'pneumaticcraft:advanced_pressure_tube',
    'megacells:sky_steel_ingot': 'pneumaticcraft:ingot_iron_compressed',
    'extendedae:entro_budding_fully': 'ae2:entro_budding_fully',
    'extendedae:entro_budding_mostly': 'ae2:entro_budding_mostly',
    'extendedae:entro_budding_half': 'ae2:entro_budding_half',
    'extendedae:entro_budding_hardly': 'ae2:entro_budding_hardly',
}


def namespace(value):
    return value.replace(SOURCE_NAMESPACE, MODID)


def convert(value):
    if isinstance(value, list):
        return [convert(v) for v in value]
    if not isinstance(value, dict):
        return namespace(ALIASES.get(value, value)) if isinstance(value, str) else value
    if 'tag' in value:
        return {'type': 'forge:ore_dict', 'ore': {'c:glass_blocks': 'blockGlass', 'c:ingots': 'ingotIron'}[value['tag']]}
    result = {('item' if k == 'id' else k): convert(v) for k, v in value.items() if k not in ('category', 'neoforge:conditions')}
    if result.get('item') == 'pneumaticcraft:network_data_storage':
        result.update(item='pneumaticcraft:network_component', data=2)
    return result


def main():
    screens = ROOT / 'src/main/resources/assets/ae2/screens'
    shutil.copytree(UPSTREAM / 'src/main/resources/assets/ae2/screens', screens, dirs_exist_ok=True)
    for path in screens.rglob('*.json'):
        path.write_text(namespace(path.read_text()))
    recipes = UPSTREAM / 'src/generated/resources/data/appliedpneumatics/recipe'
    machine = []
    for path in sorted(recipes.rglob('*.json')):
        data = json.loads(path.read_text())
        recipe_type = data['type']
        if recipe_type in ('pneumaticcraft:amadron', 'pneumaticcraft:assembly_drill', 'pneumaticcraft:assembly_laser', 'pneumaticcraft:pressure_chamber'):
            machine.append({'source': str(path.relative_to(recipes)), **convert(data)})
        elif recipe_type.startswith('minecraft:crafting_'):
            if 'neoforge:conditions' in data or path.name == 'amadron_process_upgrade.json':
                continue
            output = DEST / 'recipes' / path.relative_to(recipes)
            output.parent.mkdir(parents=True, exist_ok=True)
            output.write_text(json.dumps(convert(data), ensure_ascii=False, indent=2) + '\n')
    (DEST / 'data').mkdir(exist_ok=True)
    (DEST / 'data/machine-recipes.json').write_text(json.dumps(machine, ensure_ascii=False, indent=2) + '\n')
    for path in (UPSTREAM / 'src/main/resources/assets/appliedpneumatics/lang').glob('*.json'):
        original = {namespace(k): v for k, v in json.loads(path.read_text()).items()}
        translated = {}
        for key, value in original.items():
            if key.startswith('item.'):
                # Item#getTranslationKeyInefficiently translates the bare key before appending .name.
                # Keeping both spellings makes the visible name become "translated name.name".
                translated[key + '.name'] = value
            else:
                translated[key] = value
                if key.startswith('block.'):
                    translated['tile.' + key.removeprefix('block.') + '.name'] = value
        extra = json.loads((ROOT / 'tools' / ('lang-zh.json' if path.stem == 'zh_cn' else 'lang-en.json')).read_text())
        translated.update({MODID + '.ui.' + key: value for key, value in extra.items()})
        translated.update(json.loads((ROOT / 'tools' / ('ponder-' + ('zh_cn' if path.stem == 'zh_cn' else 'en_us') + '.json')).read_text()))
        (DEST / 'lang' / (path.stem + '.lang')).write_text('\n'.join(k + '=' + v.replace('\n', '\\n') for k, v in translated.items()) + '\n')
    for name in ('me_pressure_interface_block', 'me_temperature_interface', 'me_amadron_process_station', 'me_amadron_extended_process_station'):
        variants = {}
        for state, texture in enumerate(('room_temperature', 'low_temperature', 'high_temperature')):
            model = 'me_temperature_interface/' + texture if name == 'me_temperature_interface' else name
            variants['temperature=' + str(state)] = {'model': MODID + ':' + model}
        (DEST / 'blockstates' / (name + '.json')).write_text(json.dumps({'variants': variants}) + '\n')
    shutil.copytree(DEST / 'models/me_temperature_interface', DEST / 'models/block/me_temperature_interface', dirs_exist_ok=True)
    tiers = ('1k','4k','16k','64k','256k','1m','4m','16m','64m','256m')
    for i, tier in enumerate(tiers):
        if i < 5:
            base = [{'item':MODID + ':air_cell_shell'}, {'item':'ae2:cell_component_' + tier}]
            portable = base + [{'item':'ae2:chest'}, {'item':'ae2:energy_cell'}]
        else:
            base = [{'item':MODID + ':mega_air_cell_shell'}] + [{'item':MODID + ':air_cell_' + tiers[i-1]} for _ in range(4)]
            portable = [{'item':MODID + ':air_cell_' + tier}, {'item':'ae2:chest'}, {'item':'ae2:energy_cell'}]
        for prefix, components in [('air_cell_', base), ('portable_air_cell_', portable)]:
            output = DEST / 'recipes/disassembly' / (prefix + tier + '.json')
            output.parent.mkdir(parents=True, exist_ok=True)
            output.write_text(json.dumps({'type':'ae2:storage_cell_disassembly','cell':MODID + ':' + prefix + tier,'cell_disassembly_items':components}, indent=2) + '\n')
    source_lock = json.loads((ROOT / 'tools/ui-source-lock.json').read_text())
    lock = {**source_lock, 'namespace_mapping': {SOURCE_NAMESPACE: MODID}, 'source_files': source_lock['files'], 'files': {}}
    for name in source_lock['files']:
        output = ROOT / 'src/main/resources' / namespace(name)
        lock['files'][namespace(name)] = hashlib.sha256(output.read_bytes()).hexdigest()
    (DEST / 'data/ui-source-lock.json').write_text(json.dumps(lock, indent=2) + '\n')
    print(f'Converted {len(machine)} PneumaticCraft recipes, translations and original UI')


if __name__ == '__main__':
    main()
