import os
import sys
from pathlib import Path

if __name__ == '__main__':
    PYVM_COMPILER_PATH = Path(__file__).parent.absolute() / "pyvm-compiler.jar"

    def compile(path: str):
        for file in os.listdir(os.path.join(sys.argv[1], path)):
            if file.endswith(".py"):
                print(f'Compiling {file}')
                if os.system(f'{sys.executable} "{os.environ["PYVM_PARSER"]}" -o "{sys.argv[2]}" -p "{path.replace("/", ".")}" "{path}/{file}"') != 0:
                    # exit(1)
                    continue
                a = f'java -cp {str(PYVM_COMPILER_PATH).replace("\\", "/")}{os.pathsep}{os.environ.get("PYVM_CLASSPATH").replace("\\", "/")} dev.ultreon.pyvm.compiler.AstCompiler {sys.argv[2].replace("\\", "/")}/{sys.argv[3].replace(".", "/")}/{file[:file.rfind(".")]}.ast.json "{sys.argv[3].replace("\\", "/").replace("/", ".")}.{file[:file.rfind(".")]}" {sys.argv[2].replace("\\", "/")}/classes/'

                import shlex
                for c in shlex.split(a):
                    print(c)
                if os.system(a) != 0:
                    # exit(1)
                    continue
            elif os.path.isdir(f'{path}/{file}'):
                print(f'Entering {file}')
                compile(f'{path}/{file}')
                print(f'Exiting {file}')


    for file in os.listdir(sys.argv[1]):
        if os.path.isdir(f'{sys.argv[1]}/{file}'):
            print(f'Entering {file}')
            compile(f'{sys.argv[1]}/{file}')
            print(f'Exiting {file}')
        elif file.endswith(".py"):
            print(f'Compiling {file}')
            if os.system(f'{sys.executable} "{os.environ["PYVM_PARSER"]}" -o "{sys.argv[2]}" -p "{sys.argv[3]}" "{file}"') != 0:
                # exit(1)
                continue
            a = f'java -cp {str(PYVM_COMPILER_PATH).replace("\\", "/")}{os.pathsep}{os.environ.get("PYVM_CLASSPATH").replace("\\", "/")} dev.ultreon.pyvm.compiler.AstCompiler {sys.argv[2].replace("\\", "/")}/{sys.argv[3].replace(".", "/")}/{file[:file.rfind(".")]}.ast.json "{sys.argv[3].replace("\\", "/").replace("/", ".")}.{file[:file.rfind(".")]}" {sys.argv[2].replace("\\", "/")}/classes/'

            # import shlex
            # for c in shlex.split(a):
            #     print(c)
            if os.system(a) != 0:
                # exit(1)
                continue
