#!/usr/bin/env python3
"""Verify both render namespaces in the actual installation/source archives."""
from pathlib import Path
from zipfile import ZipFile
import json
import sys

CURRENT = 'ae2s-appliedpneumatics'
PREVIOUS = 'appliedpneumatics'


def verify(path):
    with ZipFile(path) as archive:
        names = archive.namelist()
        assert len(names) == len(set(names)), (path, 'Duplicate ZIP entries')
        current_root = 'assets/' + CURRENT + '/'
        previous_root = 'assets/' + PREVIOUS + '/'
        render_files = [name for name in names if name.startswith(current_root)
                        and name[len(current_root):].split('/', 1)[0] in ('models', 'textures', 'blockstates')
                        and not name.endswith('/')]
        assert len(render_files) >= 100, (path, 'Render resources are incomplete')
        assert sum(name.startswith(current_root + 'models/item/') for name in render_files) == 35
        for name in render_files:
            alias = previous_root + name[len(current_root):]
            expected = archive.read(name)
            if name.endswith(('.json', '.mcmeta')):
                expected = expected.replace((CURRENT + ':').encode(), (PREVIOUS + ':').encode())
            assert archive.read(alias) == expected, (path, 'Missing or incorrect render alias', alias)
        assert not any(name.startswith(previous_root + 'recipes/') for name in names)
        assert not any(name.startswith(previous_root + 'lang/') for name in names)
        metadata = json.loads(archive.read('mcmod.info'))
        assert metadata[0]['modid'] == CURRENT
        print(f'PASS: {path.name}: {len(render_files)} render files available under both namespaces; 35 item models')


if __name__ == '__main__':
    assert len(sys.argv) > 1, 'Pass installation and source Jar paths'
    for argument in sys.argv[1:]:
        verify(Path(argument))
