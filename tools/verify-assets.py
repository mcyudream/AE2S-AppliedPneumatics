#!/usr/bin/env python3
"""Validate migrated resource paths, registration identities and optional client API boundaries."""
import json
import hashlib
from pathlib import Path

root = Path(__file__).resolve().parents[1]
resources = root / 'src/main/resources'
assets = resources / 'assets/ae2s-appliedpneumatics'
models = 0
for path in resources.rglob('*.json'):
    value = json.loads(path.read_text())
    if '/models/' not in str(path):
        continue
    models += 1
    for texture in value.get('textures', {}).values():
        if texture.startswith('ae2s-appliedpneumatics:'):
            assert (assets / 'textures' / (texture.split(':', 1)[1] + '.png')).is_file(), (path, texture)
    parent = value.get('parent', '')
    if parent.startswith('ae2s-appliedpneumatics:'):
        assert (assets / 'models' / (parent.split(':', 1)[1] + '.json')).is_file(), (path, parent)
for path in (assets / 'blockstates').glob('*.json'):
    variants = json.loads(path.read_text())['variants']
    assert len(variants) == 3
    for value in variants.values():
        name = value['model'].split(':', 1)[1]
        assert (assets / 'models/block' / (name + '.json')).is_file(), (path, name)
for path in (assets / 'lang').glob('*.lang'):
    keys = set()
    for line in path.read_text().splitlines():
        if not line or line.startswith('#'):
            continue
        key = line.split('=', 1)[0]
        assert key not in keys, (path, key)
        keys.add(key)
        if key.startswith('item.'):
            assert key.endswith('.name'), (path, 'Bare item translation would create a visible .name suffix', key)
    for key in ('ae2s-appliedpneumatics.ui.pressure_title', 'ae2s-appliedpneumatics.ui.amadron_title', 'ae2s-appliedpneumatics.ponder.shared.air_power'):
        assert key in keys, (path, key)
for path in (root / 'src/main/java').rglob('*.java'):
    source = path.read_text()
    assert 'net.neoforged' not in source and 'net.minecraft.world.item' not in source, path
    if 'net.createmod' in source:
        assert '/compat/ponder/' in str(path), path
assert len(json.loads((assets / 'data/machine-recipes.json').read_text())) == 29
assert json.loads((resources / 'pack.mcmeta').read_text())['pack']['pack_format'] == 3
for config in resources.glob('mixins.*.json'):
    mixin=json.loads(config.read_text())
    for name in mixin.get('mixins',[])+mixin.get('client',[]):
        assert (root / 'src/main/java' / Path(mixin['package'].replace('.','/')) / (name+'.java')).is_file(), (config,name)
ui_lock=json.loads((assets / 'data/ui-source-lock.json').read_text())
for name,digest in ui_lock['files'].items():
    payload=(resources / name).read_bytes()
    assert hashlib.sha256(payload).hexdigest()==digest, name
    source_name=name.replace('ae2s-appliedpneumatics','appliedpneumatics')
    if name.endswith('.json'):
        payload=payload.replace(b'ae2s-appliedpneumatics',b'appliedpneumatics')
    assert hashlib.sha256(payload).hexdigest()==ui_lock['source_files'][source_name], name
print(f'PASS: {models} model files, all local paths, block states, languages optional API isolation and {len(ui_lock["files"])} original UI resources')
