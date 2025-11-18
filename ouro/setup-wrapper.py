from setuptools import setup

setup(
    name='py-ouro-wrapper',
    version='0.1',
    packages=['ouro.wrapper'],
    entry_points={
        'console_scripts': [
            'ouro = ouro.wrapper.wrapper:main',
        ],
    },
    description='Wrapper for the Ouro build system',
    long_description=open('README.md').read(),
    package_dir={'': 'src'},
    include_dirs=['src/ouro/wrapper'],
)
