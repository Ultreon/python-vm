# one-time build script to create ouro.pyz
import zipapp, pathlib

if __name__ == '__main__':
    def filter(path: pathlib.Path):
        if path.name.endswith('.pyc'):
            return False
        if not path.is_relative_to(wrapper):
            return str(path) == '__main__.py'
        return True

    source_dir = pathlib.Path('src')
    out = pathlib.Path('build/wrapper.pyz')
    wrapper = pathlib.Path('ouro') / 'wrapper'
    out.parent.mkdir(parents=True, exist_ok=True)
    zipapp.create_archive(source_dir, target=out, interpreter='#!/usr/bin/env python3', filter=filter)
    print("Created", out)
