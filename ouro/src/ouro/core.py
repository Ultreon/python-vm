# ouro/core.py
"""
Core build system logic.
"""
import os
import platform
import sys
import traceback
import types
from argparse import Namespace
from collections import OrderedDict
from pathlib import Path


class OuroException(Exception):
    pass


class Task:
    def __init__(self, name, fn, depends=None, description=None):
        self.name = name
        self.fn = fn
        self.depends = list(depends) if depends else []
        self.description = description or ""
        self._ran = False

    def run(self, project, ctx):
        if self._ran:
            return
        # Run dependencies first
        for dep in self.depends:
            if isinstance(dep, Task):
                dep.run(project, ctx)
            elif isinstance(dep, str):
                dep_task = ctx['tasks'].get(dep)
                if not dep_task:
                    raise RuntimeError(f"Task '{self.name}' depends on unknown task '{dep}'")
                dep_task.run(project, ctx)
            else:
                raise RuntimeError("Dependency must be Task or task-name string")
        if not self._ran:
            print(f"[ouro] Executing task: {self.name}")
            try:
                # task function may accept (project) or (project, ctx) or no args
                import inspect
                sig = inspect.signature(self.fn)
                if len(sig.parameters) == 0:
                    self.fn()
                elif len(sig.parameters) == 1:
                    self.fn(project)
                else:
                    self.fn(project, ctx)
            except Exception:
                print(f"[ouro] Task {self.name} failed:")
                traceback.print_exc()
                raise
            self._ran = True


def task(name=None, depends=None, description=None):
    """
    Decorator to declare a task in the build file.

    Usage:
    ```python
      @task("compile", depends=["prepare"])
      def compile(project):
          ...
    ```
    """

    def deco(fn):
        tname = name or fn.__name__
        return Task(tname, fn, depends=depends, description=description)

    return deco


class Project:
    def __init__(self, root_dir: Path):
        self.root_dir = root_dir
        self.name = root_dir.name
        self.properties = {}
        self.extensions = {}
        self.tasks = OrderedDict()
        self.options = {}
        self.java_home = Path(os.environ.get('JAVA_HOME'))
        if not self.java_home.exists():
            raise RuntimeError("JAVA_HOME environment variable not set")

    def set_property(self, k, v):
        self.properties[k] = v

    def apply(self, pyfile_path: str):
        # Execute a Python file in the context where `project` is available
        path = (self.root_dir / pyfile_path).resolve()
        if not path.exists():
            raise FileNotFoundError(f"{path} not found")
        ctx = {
            'project': self,
            'task': task,  # decorator helper
            'Task': Task,  # type
            'tasks': self.tasks,  # allow build file to directly register tasks
            '__file__': str(path),
            '__name__': '__build__'
        }
        code = path.read_text(encoding='utf-8')
        module = types.ModuleType('__build__')
        module.__dict__.update(ctx)
        try:
            exec(compile(code, str(path), 'exec'), module.__dict__)
        except Exception:
            print(f"[ouro] Error executing {path}:")
            traceback.print_exc()
            raise
        # After exec, collect Task instances from module globals
        for name, val in module.__dict__.items():
            if isinstance(val, Task):
                if val.name in self.tasks:
                    raise RuntimeError(f"Duplicate task '{val.name}'")
                self.tasks[val.name] = val

    def register_task(self, task_obj: Task):
        if task_obj.name in self.tasks:
            raise RuntimeError(f"Duplicate task '{task_obj.name}'")
        self.tasks[task_obj.name] = task_obj


def load_settings(project_root: Path, settings_filename: str):
    """
    Executes settings.<newname>.py which is expected to set project-wide properties.
    The settings file will get 'project' object.
    """
    settings_path = project_root / settings_filename
    if not settings_path.exists():
        return None
    ctx = {'project': Project(project_root)}
    code = settings_path.read_text(encoding='utf-8')
    module = types.ModuleType('__settings__')
    module.__dict__.update(ctx)
    try:
        exec(compile(code, str(settings_path), 'exec'), module.__dict__)
    except Exception:
        print(f"[ouro] Error executing settings at {settings_path}")
        traceback.print_exc()
        raise
    # settings file may modify the project instance or return it
    proj = module.__dict__.get('project', ctx['project'])
    return proj


def run_task_by_name(project: Project, task_name: str, cli_ctx=None):
    ctx = {'tasks': project.tasks, 'project': project}
    if task_name not in project.tasks:
        raise RuntimeError(f"Task '{task_name}' not found. Available tasks: {', '.join(project.tasks.keys())}")
    # reset run markers
    for t in project.tasks.values():
        t._ran = False
    project.tasks[task_name].run(project, ctx)


def list_tasks(project: Project):
    print("Available tasks:")
    for name, t in project.tasks.items():
        desc = f" - {t.description}" if getattr(t, 'description', None) else ""
        print(f"  {name}{desc}")


def help_task(project):
    print("Usage: ouro [--root <dir>] [--settings <file>] [--build-file <file>] <task-name> [args...]")
    print("Options:")
    print("  --root <dir> - Project root directory (default: current dir)")
    print("  --settings <file> - Settings file name (default: settings.ouro.py)")
    print("  --build-file <file> - Build file name (default: build.ouro.py)")
    print("  --help -h - Show this help")
    print("")
    print("Arguments:")
    print("  <task-name> - Task name to run")
    print("  [args...] - Optional arguments passed to the task")
    print("")

    list_tasks(project)


def clean(project):
    import shutil
    shutil.rmtree(project.root_dir.joinpath('build'), ignore_errors=True)


def compile_project(project: Project):
    import subprocess
    import platform
    import os
    java_exec = 'java'
    match platform.system():
        case 'Windows':
            java_exec = 'java.exe'
        case _:
            pass

    failing = False
    for path in os.walk(project.root_dir / 'src'):
        for file in path[2]:
            proc = subprocess.run(
                [
                    project.java_home / 'bin' / java_exec,
                    '-jar',
                    project.options['pyvm_compiler'],
                    "--input",
                    path[0] + '/' + file,
                    "--output",
                    project.root_dir / 'build' / 'classes'
                                                 "--package",
                    os.path.relpath(path[0], project.root_dir / 'src').replace('\\', '.').replace('/', '.')
                ]
            )

            if proc.returncode != 0:
                failing = True

    if failing:
        raise Exception("PyVM compilation failed")


def jar_project(project):
    import subprocess

    # Create ZIP archive
    proc = subprocess.run(
        [
            'jar',
            'cf',
            project.root_dir / 'build' / f'{project.name}.jar',
            'classes',
        ],
        cwd=project.root_dir / 'build'
    )

    if proc.returncode != 0:
        raise Exception("Jar creation failed")


def build(project):
    pass


def wrapper(project):
    batch = """"
@echo off
setlocal
set HERE=%~dp0
set PYZ=%HERE%ouro\wrapper\ouro.pyz
if not exist "%PYZ%" (
  echo ouro.pyz not found at %PYZ%
  exit /b 1
)
rem Use whatever 'python' is on PATH
python "%PYZ%" %*
endlocal
    """

    shell = """
#!/usr/bin/env bash
# ouro - wrapper to run the ouro.pyz shipped in ouro/wrapper/
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
PYZ="$HERE/ouro/wrapper/ouro.pyz"

if [ ! -f "$PYZ" ]; then
  echo "ouro.pyz not found at $PYZ"
  exit 1
fi

# Use same python that runs this script (if invoked via shebang), or fallback to 'python3'
PYTHON="${PYTHON:-$(command -v python3 || command -v python || true)}"
if [ -z "$PYTHON" ]; then
  echo "No python found in PATH"
  exit 2
fi

exec "$PYTHON" "$PYZ" "$@"
"""
    with open(project.root_dir / 'ouro.bat', 'w') as f:
        f.write(batch)
    with open(project.root_dir / 'ouro.sh', 'w') as f:
        f.write(shell)

    if platform.system() == 'Linux' or platform.system() == 'Darwin':
        os.chmod(project.root_dir / 'ouro.sh', 0o755)  # make executable


    wrapper = Path(__file__).parent / 'ouro' / 'wrapper'

    def filter(path: Path):
        if path.name.endswith('.pyc'):
            return False
        if not path.is_relative_to(wrapper):
            return str(path) == '__main__.py'
        return True

    wrapper_ = project.root_dir / 'ouro' / 'wrapper'
    wrapper_.mkdir(parents=True, exist_ok=True)
    with open(wrapper_ / 'ouro.pyz', 'wb') as f:
        # Check if ran as zipapp
        is_zipapp = getattr(sys, 'frozen', False)
        if not is_zipapp:
            import zipapp
            zipapp.create_archive(Path(__file__).parent, target=f, interpreter='#!/usr/bin/env python3')
        else:
            raise Exception("ouro.pyz is already a zipapp")


def main(argv=None):
    if argv is None:
        argv = sys.argv[1:]
    # Simple CLI
    import argparse
    parser = argparse.ArgumentParser(prog='ouro', description='ouro build system')
    parser.add_argument('--root', '-r', default='.', help='Project root dir')
    parser.add_argument('--settings', '-s', default='settings.ouro.py', help='Settings file name')
    parser.add_argument('--build-file', '-b', default='build.ouro.py', help='Build file name')
    parser.add_argument('--pyvm-compiler', '-c', default='ouro/wrapper/pyvm_compiler.jar', help='Path to PythonVM compiler')
    parser.add_argument('command', nargs='?', default='help', help='Command or task name (default: tasks)')
    parser.add_argument('extra', nargs=argparse.REMAINDER)
    args: Namespace = parser.parse_args(argv)

    root: Path = Path(args.root).resolve()
    settings_file = args.settings
    build_file = args.build_file

    # load settings -> gets a project object (default to new Project)
    project = load_settings(root, settings_file)
    if project is None:
        project = Project(root)

    # allow command-line overrides in project.options
    project.options['cli_args'] = args.extra
    project.options['pyvm_compiler'] = args.pyvm_compiler

    if args.command == 'downloadCompiler':
        import urllib.request
        try:
            urllib.request.urlretrieve(
                "https://github.com/ultreon/python-vm/releases/download/latest/pyvm_compiler.jar",
                project.options['pyvm_compiler'])
        except Exception as e:
            print(f"[ouro] Error downloading PythonVM compiler: {e}")
            return 1
        return 0

    if not os.path.exists(project.options['pyvm_compiler']) and args.command not in ('tasks', 'help', '-h', '--help', 'downloadCompiler', 'wrapper'):
        print(f"[ouro] Warning: No PythonVM compiler found at {project.options['pyvm_compiler']}", file=sys.stderr)

    # load build file (executes and registers tasks)
    if not os.path.exists(build_file):
        if args.command not in ('tasks', 'help', '-h', '--help', 'downloadCompiler', 'wrapper'):
            print(f"[ouro] Warning: No build file found at {build_file}", file=sys.stderr)
    else:
        project.apply(build_file)

    project.tasks['wrapper'] = Task('wrapper', wrapper, description="Create ouro.bat wrapper")
    project.tasks['clean'] = Task('clean', clean, description="Clean build directory")
    project.tasks['compile'] = Task('compile', compile_project, description="Compile project")
    project.tasks['jar'] = Task('jar', jar_project, description="Create JAR")
    project.tasks['build'] = Task('build', build, description="Build project")
    project.tasks['tasks'] = Task('tasks', list_tasks, description="List available tasks")
    project.tasks['help'] = Task('help', help_task, description="Show this help")

    if args.command in ('tasks',):
        list_tasks(project)
        return 0

    if args.command in ('help', '-h', '--help'):
        help_task(project)
        return 0

    # otherwise treat command as a task name
    try:
        run_task_by_name(project, args.command)
    except Exception as e:
        print("[ouro] Build failed:", e)
        return 2
    return 0


if __name__ == '__main__':
    sys.exit(main())
