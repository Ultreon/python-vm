# __main__.py - entrypoint inside the .pyz
try:
    from ouro.core import main
except ImportError:
    from ouro.wrapper.wrapper import main
import sys

if __name__ == '__main__':
    sys.exit(main())
