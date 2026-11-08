#!/usr/bin/env python3
"""Keep pre-rename render resource locations usable without registering duplicate content."""
from pathlib import Path
import shutil

ROOT = Path(__file__).resolve().parents[1]
CURRENT = 'ae2s-appliedpneumatics'
PREVIOUS = 'appliedpneumatics'
RENDER_DIRECTORIES = ('blockstates', 'models', 'textures')


def main():
    source = ROOT / 'src/main/resources/assets' / CURRENT
    output = ROOT / 'build/generated/resource-aliases/assets' / PREVIOUS
    if output.exists():
        shutil.rmtree(output)
    count = 0
    for directory in RENDER_DIRECTORIES:
        for path in sorted((source / directory).rglob('*')):
            if not path.is_file():
                continue
            destination = output / path.relative_to(source)
            destination.parent.mkdir(parents=True, exist_ok=True)
            payload = path.read_bytes()
            if path.suffix in ('.json', '.mcmeta'):
                payload = payload.replace((CURRENT + ':').encode(), (PREVIOUS + ':').encode())
            destination.write_bytes(payload)
            count += 1
    print(f'Generated {count} original-namespace render aliases; registries and recipes keep {CURRENT}')


if __name__ == '__main__':
    main()
