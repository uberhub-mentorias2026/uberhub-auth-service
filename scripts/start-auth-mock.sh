#!/bin/sh
# Inicia o Auth Service com o perfil postman-mock, somente em 127.0.0.1.
# Uso: sh scripts/start-auth-mock.sh
# Não usar em produção. As interfaces externas são fictícias (ver docs/POSTMAN-TESTES.md).
set -eu

PORT=8081
HOST=127.0.0.1
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
PROJECT_DIR=$(cd "$SCRIPT_DIR/.." && pwd)

# 1. Java 21 disponível
if ! command -v java >/dev/null 2>&1; then
    echo "Erro: Java não encontrado no PATH. Instale o JDK 21." >&2
    exit 1
fi
JAVA_MAJOR=$(java -version 2>&1 | awk -F'"' '/version/ {split($2, p, "."); print p[1]; exit}')
if [ "$JAVA_MAJOR" != "21" ]; then
    echo "Erro: Java 21 é necessário; versão encontrada: ${JAVA_MAJOR:-desconhecida}." >&2
    exit 1
fi

# 2. Porta local livre
if command -v lsof >/dev/null 2>&1 && lsof -nP -iTCP:"$PORT" -sTCP:LISTEN >/dev/null 2>&1; then
    echo "Erro: a porta $PORT já está em uso em $HOST. Encerre o processo que a ocupa e tente novamente." >&2
    exit 1
fi

# 3. Inicialização com o perfil de testes e endereço local
echo "Iniciando o Auth Service (perfil postman-mock) em http://$HOST:$PORT"
echo "Para encerrar: pressione Ctrl+C neste terminal."
echo "Dados fictícios apenas. Nenhum token real é usado."
cd "$PROJECT_DIR"
exec sh ./mvnw -B -Plocal-mock spring-boot:run -Dspring-boot.run.profiles=postman-mock
