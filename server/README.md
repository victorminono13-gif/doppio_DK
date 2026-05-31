# Python WebSocket Server - CloneModServer

Este diretório contém o servidor Python que processa os dados enviados pelo mod Minecraft.

## 📋 Pré-requisitos

- Python 3.8+
- pip

## 🚀 Como executar

### Opção 1: Com script automático

```bash
python run.py
```

### Opção 2: Manual

```bash
# 1. Instalar dependências
pip install -r requirements.txt

# 2. Executar servidor
python server.py
```

## 🔌 Especificação do Servidor

O servidor escuta em **`ws://localhost:8765`** e processa os seguintes eventos:

### 1. Handshake (Inicial)
```json
{
  "action": "handshake",
  "mod_name": "CloneMod",
  "version": "1.0"
}
```

### 2. Análise
```json
{
  "action": "start_analysis",
  "player": "nome_jogador"
}
```

```json
{
  "action": "stop_analysis",
  "player": "nome_jogador"
}
```

### 3. Dados de Jogador
```json
{
  "action": "player_data",
  "player": "nome_jogador",
  "position": {"x": 100.5, "y": 64, "z": 200.3},
  "rotation": {"yaw": 45.0, "pitch": 0.0},
  "keyInput": {
    "forward": true,
    "backward": false,
    "jump": false,
    ...
  }
}
```

### 4. Mensagem de Chat
```json
{
  "action": "chat_message",
  "player": "nome_jogador",
  "chatMessage": "Olá mundo!"
}
```

### 5. Spawn de Clone
```json
{
  "action": "spawn_clone",
  "player": "nome_jogador",
  "force": false
}
```

### 6. Memória
```json
{
  "action": "memory_save",
  "slot": "slot_1"
}
```

```json
{
  "action": "memory_load",
  "slot": "slot_1"
}
```

```json
{
  "action": "memory_list"
}
```

## 📊 Fluxo de Funcionamento

1. Mod Minecraft conecta ao servidor Python via WebSocket
2. Mod envia `/clone analisar <jogador>`
3. Servidor cria sessão de análise
4. Mod coleta dados do jogador e envia periodicamente
5. Servidor atualiza confiança e padrões
6. Quando confiança é suficiente, permite spawn do clone
7. Clone recebe ações do servidor para imitar o jogador

## 📝 Logs

Os logs do servidor são salvos em `clonemod_server.log` e também exibidos no console.

## 🛠️ Desenvolvedor

Atualmente é um servidor básico. Fases futuras implementarão:
- PyTorch/TensorFlow para Imitation Learning
- Integração com Ollama (Llama 3) para chat
- Persistência em banco de dados
- Sistema de gerenciamento de clones
