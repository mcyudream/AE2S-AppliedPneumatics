#!/usr/bin/env python3
"""Verify packaged SRG jars on official Cleanroom 0.4.0, with and without optional Ponder."""
import argparse
import hashlib
import json
import os
import re
from pathlib import Path
import shutil
import subprocess
import sys
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
WORKSPACE = ROOT.parent.parent
GRADLE = Path(os.environ.get('GRADLE_USER_HOME', Path.home() / '.gradle'))
LOCK = json.loads((ROOT / 'dependency-lock.json').read_text())


def digest(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def dependency(name):
    entry = LOCK[name]
    target = ROOT / 'build/dependencies/verification' / entry['file']
    candidates = [target]
    if name == 'ponder':
        candidates.append(ROOT / 'build/dependencies/ponder' / entry['file'])
    if name == 'pneumaticcraft':
        candidates.extend((GRADLE / 'caches/modules-2/files-2.1/curse.maven/pneumaticcraft-repressurized-281849/2978408').glob('*/*.jar'))
    elif 'group' in entry:
        candidates.extend((GRADLE / 'caches/modules-2/files-2.1' / entry['group'] / entry['module'] / entry['version']).glob('*/*.jar'))
    for candidate in candidates:
        if candidate.is_file() and digest(candidate) == entry['sha256']:
            return candidate
    target.parent.mkdir(parents=True, exist_ok=True)
    partial = target.with_suffix('.part')
    with urllib.request.urlopen(entry['url'], timeout=90) as response, partial.open('wb') as output:
        shutil.copyfileobj(response, output)
    if digest(partial) != entry['sha256']:
        raise RuntimeError('Pinned dependency differs: ' + name)
    partial.replace(target)
    return target


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--side', choices=['server', 'client', 'both'], default='both')
    parser.add_argument('--ponder', choices=['with', 'without', 'both'], default='both')
    parser.add_argument('--port', type=int, default=25893)
    parser.add_argument('--name', default='accepted')
    parser.add_argument('--ponder-jar', type=Path, help='Test an independently built Ponder installation jar instead of the pinned historical runtime')
    args = parser.parse_args()
    if args.side != 'server' and not os.environ.get('DISPLAY'):
        parser.error('Client verification requires DISPLAY (Xvfb is supported)')
    version = re.search(r"^version = '([^']+)'", (ROOT / 'build.gradle').read_text(), re.MULTILINE).group(1)
    artifact = ROOT / 'build/libs' / ('AE2S-AppliedPneumatics-' + version + '.jar')
    harness = ROOT / 'build/verification/applied-pneumatics-verification.jar'
    ae2s = ROOT / 'build/dependencies/ae2-supergiant' / LOCK['ae2s']['runtime']['file']
    for path in (artifact, harness, ae2s):
        if not path.is_file():
            parser.error('Build first: ./tools/dev.sh build reobfRuntimeHarnessJar')
    if digest(ae2s) != LOCK['ae2s']['runtime']['sha256']:
        raise RuntimeError('AE2S differs from the pinned runtime')
    artifact_digest = digest(artifact)
    environment = os.environ.copy()
    environment['ALSOFT_DRIVERS'] = 'null'
    summary_path = ROOT / 'verification/summary.json'
    summary = json.loads(summary_path.read_text()) if summary_path.exists() else {}
    if summary.get('artifact_sha256') != artifact_digest:
        summary = {'cleanroom': '0.4.0', 'artifact_sha256': artifact_digest, 'scenarios': {}}
    sides = ('server', 'client') if args.side == 'both' else (args.side,)
    variants = (False, True) if args.ponder == 'both' else (args.ponder == 'with',)
    for ponder in variants:
        for side in sides:
            variant = side + '-' + ('with' if ponder else 'without') + '-ponder'
            name = args.name + '-' + variant
            jars = [artifact, ae2s, dependency('pneumaticcraft'), harness]
            if ponder:
                jars += [args.ponder_jar.resolve(strict=True)] if args.ponder_jar else [dependency('ponder'), dependency('crafttweaker')]
            command = [sys.executable, str(ROOT / 'tools/verify-cleanroom.py'), side, '--name', name,
                       '--port', str(args.port), '--timeout', '480', '--report', 'apneumatics-verification/report.json']
            for jar in jars:
                command += ['--mod', str(jar)]
            subprocess.run(command, cwd=ROOT, env=environment, check=True)
            run = ROOT / 'run/cleanroom-0.4.0' / name / side
            if digest(artifact) != artifact_digest:
                raise RuntimeError('Artifact changed during verification; rebuild and run again')
            result = json.loads((run / 'result.json').read_text())
            if result['result'] != 'FUNCTIONAL_PASS':
                raise RuntimeError('Functional acceptance failed: ' + variant)
            destination = ROOT / 'verification/cleanroom-0.4.0' / variant
            destination.mkdir(parents=True, exist_ok=True)
            for file in ('result.json', 'runtime.json', 'console.log'):
                shutil.copy2(run / file, destination / file)
            shutil.copytree(run / 'apneumatics-verification', destination / 'gameplay', dirs_exist_ok=True)
            if summary_path.exists():
                fresh = json.loads(summary_path.read_text())
                if fresh.get('artifact_sha256') == artifact_digest:
                    summary = fresh
            summary['scenarios'][variant] = {'result': result['result'], 'checks': len(result['gameplay_report']['checks'])}
            summary_path.write_text(json.dumps(summary, indent=2) + '\n')
    print(json.dumps(summary, indent=2))


if __name__ == '__main__':
    main()
