"
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
    