import sys
import io
import subprocess


def run_script(code: str) -> str:
    """
    Execute Python code and return stdout/stderr as a string.
    """
    old_stdout, old_stderr = sys.stdout, sys.stderr
    buf = io.StringIO()
    sys.stdout = buf
    sys.stderr = buf
    try:
        exec(code, {})
    except Exception as e:
        print(f"[ERROR] {e}")
    finally:
        sys.stdout = old_stdout
        sys.stderr = old_stderr
    return buf.getvalue()


def pip_install(pkg: str) -> str:
    if not pkg:
        return "[ERROR] empty package name"
    try:
        cmd = [sys.executable, "-m", "pip", "install", pkg]
        completed = subprocess.run(cmd, capture_output=True, text=True, check=False)
        return (completed.stdout + "\n" + completed.stderr).strip()
    except Exception as e:
        return f"[ERROR] {e}"
