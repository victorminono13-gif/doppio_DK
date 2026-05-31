"""
Servidor Python WebSocket para Behavioral Cloning Mod
Recebe dados dos jogadores e processa com IA
"""

import asyncio
import json
import logging
from datetime import datetime
from websockets.server import serve
from pathlib import Path

# Configuração de logging
logging.basicConfig(
    level=logging.INFO,
    format='[%(asctime)s] %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('clonemod_server.log'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

# Armazenamento em memória (em produção, use banco de dados)
active_sessions = {}
memory_slots = {}


class PlayerAnalysisSession:
    """Representa uma sessão de análise de um jogador"""
    
    def __init__(self, player_name):
        self.player_name = player_name
        self.start_time = datetime.now()
        self.data_count = 0
        self.confidence = 0.0
        self.clone_id = None
        self.neural_weights = None
        self.chat_database = []
    
    def add_data(self, data):
        """Adiciona um ponto de dados à sessão"""
        self.data_count += 1
        # Atualiza confiança (simples: min(1.0, count / 1000))
        self.confidence = min(1.0, self.data_count / 1000.0)
        
        # Se for chat, armazena
        if data.get('chatMessage'):
            self.chat_database.append({
                'message': data['chatMessage'],
                'timestamp': data.get('timestamp'),
                'position': data.get('position')
            })
    
    def to_dict(self):
        return {
            'player_name': self.player_name,
            'start_time': self.start_time.isoformat(),
            'data_count': self.data_count,
            'confidence': self.confidence,
            'clone_id': self.clone_id,
            'chat_messages': len(self.chat_database)
        }


async def handle_client(websocket, path):
    """Handler para conexões de clientes"""
    
    client_id = f"{websocket.remote_address[0]}:{websocket.remote_address[1]}"
    logger.info(f"[CONEXÃO] Cliente conectado: {client_id}")
    
    try:
        async for message in websocket:
            try:
                data = json.loads(message)
                action = data.get('action')
                
                logger.debug(f"[MENSAGEM] {client_id} - Ação: {action}")
                
                # Processa diferentes ações
                if action == 'handshake':
                    await handle_handshake(websocket, data, client_id)
                
                elif action == 'start_analysis':
                    await handle_start_analysis(websocket, data, client_id)
                
                elif action == 'stop_analysis':
                    await handle_stop_analysis(websocket, data, client_id)
                
                elif action == 'player_data':
                    await handle_player_data(websocket, data, client_id)
                
                elif action == 'chat_message':
                    await handle_chat_message(websocket, data, client_id)
                
                elif action == 'spawn_clone':
                    await handle_spawn_clone(websocket, data, client_id)
                
                elif action == 'memory_save':
                    await handle_memory_save(websocket, data, client_id)
                
                elif action == 'memory_load':
                    await handle_memory_load(websocket, data, client_id)
                
                elif action == 'memory_list':
                    await handle_memory_list(websocket, data, client_id)
                
                else:
                    logger.warning(f"[AVISO] {client_id} - Ação desconhecida: {action}")
                    await websocket.send(json.dumps({
                        'action': 'error',
                        'message': f'Ação desconhecida: {action}'
                    }))
            
            except json.JSONDecodeError as e:
                logger.error(f"[ERRO] {client_id} - Erro ao decodificar JSON: {e}")
                await websocket.send(json.dumps({
                    'action': 'error',
                    'message': 'Erro ao decodificar JSON'
                }))
    
    except asyncio.CancelledError:
        logger.info(f"[DESCONEXÃO] Cliente desconectado: {client_id}")
    except Exception as e:
        logger.error(f"[ERRO] {client_id} - {e}")
    finally:
        logger.info(f"[CONEXÃO FINALIZADA] {client_id}")


async def handle_handshake(websocket, data, client_id):
    """Processa handshake inicial"""
    mod_name = data.get('mod_name')
    version = data.get('version')
    
    logger.info(f"[HANDSHAKE] {mod_name} v{version}")
    
    await websocket.send(json.dumps({
        'action': 'handshake',
        'server_name': 'CloneModServer',
        'server_version': '1.0',
        'status': 'ready'
    }))


async def handle_start_analysis(websocket, data, client_id):
    """Inicia análise de um jogador"""
    player_name = data.get('player')
    
    if player_name in active_sessions:
        logger.warning(f"[ANÁLISE] Sessão já existe para {player_name}")
        await websocket.send(json.dumps({
            'action': 'error',
            'message': f'Sessão já existe para {player_name}'
        }))
    else:
        session = PlayerAnalysisSession(player_name)
        active_sessions[player_name] = session
        
        logger.info(f"[ANÁLISE INICIADA] {player_name}")
        
        await websocket.send(json.dumps({
            'action': 'analysis_started',
            'player': player_name,
            'timestamp': datetime.now().isoformat()
        }))


async def handle_stop_analysis(websocket, data, client_id):
    """Para análise de um jogador"""
    player_name = data.get('player')
    
    if player_name in active_sessions:
        session = active_sessions.pop(player_name)
        logger.info(f"[ANÁLISE PARADA] {player_name} - {session.data_count} registros")
        
        await websocket.send(json.dumps({
            'action': 'analysis_stopped',
            'player': player_name,
            'summary': session.to_dict()
        }))
    else:
        await websocket.send(json.dumps({
            'action': 'error',
            'message': f'Nenhuma sessão ativa para {player_name}'
        }))


async def handle_player_data(websocket, data, client_id):
    """Processa dados de movimento/ação do jogador"""
    player_name = data.get('player')
    
    if player_name not in active_sessions:
        await websocket.send(json.dumps({
            'action': 'error',
            'message': f'Nenhuma sessão ativa para {player_name}'
        }))
        return
    
    session = active_sessions[player_name]
    session.add_data(data)
    
    # A cada 100 registros, envia atualização
    if session.data_count % 100 == 0:
        await websocket.send(json.dumps({
            'action': 'analysis_update',
            'player': player_name,
            'data_count': session.data_count,
            'confidence': round(session.confidence, 4)
        }))


async def handle_chat_message(websocket, data, client_id):
    """Processa mensagem de chat do jogador"""
    player_name = data.get('player')
    message = data.get('chatMessage')
    
    if player_name not in active_sessions:
        logger.warning(f"[CHAT] Nenhuma sessão ativa para {player_name}")
        return
    
    session = active_sessions[player_name]
    session.add_data(data)
    
    logger.info(f"[CHAT] {player_name}: {message}")


async def handle_spawn_clone(websocket, data, client_id):
    """Cria um clone do jogador"""
    player_name = data.get('player')
    force = data.get('force', False)
    
    if player_name not in active_sessions:
        await websocket.send(json.dumps({
            'action': 'error',
            'message': f'Nenhuma sessão ativa para {player_name}'
        }))
        return
    
    session = active_sessions[player_name]
    
    # Verifica se há dados suficientes (ou força)
    if not force and session.confidence < 0.3:
        await websocket.send(json.dumps({
            'action': 'error',
            'message': f'Dados insuficientes. Confiança: {session.confidence:.2%}'
        }))
        return
    
    # Simula spawn do clone
    clone_id = f"clone_{player_name}_{datetime.now().timestamp()}"
    session.clone_id = clone_id
    
    logger.info(f"[CLONE SPAWNED] {clone_id} para {player_name}")
    
    await websocket.send(json.dumps({
        'action': 'clone_spawned',
        'clone_id': clone_id,
        'player': player_name,
        'confidence': session.confidence
    }))


async def handle_memory_save(websocket, data, client_id):
    """Salva perfil em slot de memória"""
    slot = data.get('slot')
    
    # Coleta dados de todas as sessões ativas
    if not active_sessions:
        await websocket.send(json.dumps({
            'action': 'error',
            'message': 'Nenhuma sessão ativa para salvar'
        }))
        return
    
    # Cria backup de todas as sessões
    memory_data = {
        'timestamp': datetime.now().isoformat(),
        'sessions': {}
    }
    
    for player_name, session in active_sessions.items():
        memory_data['sessions'][player_name] = session.to_dict()
    
    memory_slots[slot] = memory_data
    
    logger.info(f"[MEMÓRIA SALVA] Slot: {slot}")
    
    await websocket.send(json.dumps({
        'action': 'memory_saved',
        'slot': slot,
        'sessions_saved': len(active_sessions)
    }))


async def handle_memory_load(websocket, data, client_id):
    """Carrega perfil de um slot de memória"""
    slot = data.get('slot')
    
    if slot not in memory_slots:
        await websocket.send(json.dumps({
            'action': 'error',
            'message': f'Slot não encontrado: {slot}'
        }))
        return
    
    memory_data = memory_slots[slot]
    
    logger.info(f"[MEMÓRIA CARREGADA] Slot: {slot}")
    
    await websocket.send(json.dumps({
        'action': 'memory_loaded',
        'slot': slot,
        'data': memory_data
    }))


async def handle_memory_list(websocket, data, client_id):
    """Lista todos os slots de memória"""
    slots_list = [
        {
            'slot': slot,
            'timestamp': info['timestamp'],
            'sessions': len(info.get('sessions', {}))
        }
        for slot, info in memory_slots.items()
    ]
    
    logger.info(f"[MEMÓRIA LIST] {len(slots_list)} slots")
    
    await websocket.send(json.dumps({
        'action': 'memory_list',
        'slots': slots_list
    }))


async def main():
    """Inicia o servidor WebSocket"""
    logger.info("=" * 50)
    logger.info("CloneModServer iniciando...")
    logger.info("=" * 50)
    
    # Inicia servidor WebSocket
    async with serve(handle_client, "localhost", 8765):
        logger.info("✓ Servidor WebSocket rodando em ws://localhost:8765")
        logger.info("Aguardando conexões...")
        await asyncio.Future()  # Run forever


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        logger.info("\n[SHUTDOWN] Servidor encerrado pelo usuário")
    except Exception as e:
        logger.error(f"[ERRO FATAL] {e}")
