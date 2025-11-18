try:
    from .core import main
except ImportError:
    from .wrapper.wrapper import main
import sys

if __name__ == '__main__':
    sys.exit(main())
