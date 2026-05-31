#!/usr/bin/env python3
"""
Script para setup e execução do servidor Python
"""

import subprocess
import sys
import os
from pathlib import Path

def install_requirements():
    """Instala dependências Python"""
    print("📦 Instalando dependências Python...")
    requirements_file = Path(__file__).parent / "requirements.txt"
    
    try:
        subprocess.check_call([sys.executable, "-m", "pip", "install", "-r", str(requirements_file)])
        print("✓ Dependências instaladas com sucesso")
        return True
    except subprocess.CalledProcessError as e:
        print(f"✗ Erro ao instalar dependências: {e}")
        return False

def run_server():
    """Executa o servidor"""
    print("\n🚀 Iniciando servidor...")
    server_file = Path(__file__).parent / "server.py"
    
    try:
        subprocess.run([sys.executable, str(server_file)])
    except KeyboardInterrupt:
        print("\n\n⛔ Servidor parado")
    except Exception as e:
        print(f"✗ Erro: {e}")

def main():
    print("=" * 60)
    print("CloneModServer - Setup e Execução")
    print("=" * 60)
    
    # Verifica Python version
    if sys.version_info < (3, 8):
        print("✗ Python 3.8+ é necessário")
        return
    
    print(f"✓ Python {sys.version.split()[0]}")
    
    # Instala dependências
    if not install_requirements():
        return
    
    # Executa servidor
    run_server()

if __name__ == "__main__":
    main()
