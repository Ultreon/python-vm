from pathlib import Path


def main():
    import argparse, sys, os

    parser = argparse.ArgumentParser(description='Python to Java compiler')
    parser.add_argument('file', help='Python file to compile')
    parser.add_argument('-o', '--output', type=str, help='Output file')
    parser.add_argument('-p', '--package', type=str, help='Package name')
    args = parser.parse_args()

    print(args.file)
    print(args.output)
    print(args.package)

    import pythonvm.compiler as compiler
    replace = Path(args.file).name
    sys.exit(compiler.main(args.file, os.path.join(args.output, args.package.replace(".", "/") + "/" + replace[:replace.rindex(".")] + ".ast.json"), args.package))

if __name__ == '__main__':
    main()
