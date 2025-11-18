from setuptools import setup

setup(
    name='py-ouro',
    version='0.1',
    packages=['ouro'],
    entry_points={
        'console_scripts': [
            'ouro = ouro.__main__:main',
        ],
    },
    description='Ouro build system',
    author='Ultreon Studios',
    maintainers=['Qubix'],
    url='https://github.com/Ultreon/python-vm',
    long_description=open('README.md').read(),
    package_dir={'': 'src'},
    include_dirs=['src/ouro'],
)
