#!/bin/bash
# Script rapido per eseguire i test in Docker

set -e

# Colori per output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== StateMachine Test Runner ===${NC}"
echo ""

# Verificare che Docker sia installato
if ! command -v docker &> /dev/null; then
    echo -e "${RED}Errore: Docker non è installato${NC}"
    echo "Installare Docker da: https://docs.docker.com/get-docker/"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}Errore: Docker Compose non è installato${NC}"
    echo "Installare Docker Compose da: https://docs.docker.com/compose/install/"
    exit 1
fi

# Andare nella directory docker
cd "$(dirname "$0")"

# Mostrare menu
echo "Seleziona un'opzione:"
echo "1) Eseguire tutti i test"
echo "2) Ambiente di sviluppo interattivo"
echo "3) Eseguire test specifico"
echo "4) Generare report di copertura"
echo "5) Build del progetto"
echo "6) Pulire tutto"
echo ""
read -p "Scelta [1-6]: " choice

case $choice in
    1)
        echo -e "${GREEN}Eseguendo tutti i test...${NC}"
        docker-compose up test
        ;;
    2)
        echo -e "${GREEN}Avviando ambiente di sviluppo...${NC}"
        docker-compose up -d dev
        echo -e "${GREEN}Entrando nel container...${NC}"
        docker-compose exec dev bash
        ;;
    3)
        echo ""
        echo "Test disponibili:"
        echo "  - StateMachineTest"
        echo "  - StateMachineContextTest"
        echo "  - TransitionTest"
        echo "  - StateMachineExceptionTest"
        echo "  - StateMachineIntegrationTest"
        echo ""
        read -p "Nome del test: " test_name
        echo -e "${GREEN}Eseguendo $test_name...${NC}"
        TEST_CLASS=$test_name docker-compose up test-single
        ;;
    4)
        echo -e "${GREEN}Generando report di copertura...${NC}"
        docker-compose up coverage
        echo -e "${GREEN}Report generato in: ../target/site/jacoco/index.html${NC}"
        ;;
    5)
        echo -e "${GREEN}Building progetto...${NC}"
        docker-compose up build
        echo -e "${GREEN}JAR creato in: ../target/nexum-1.0.0.jar${NC}"
        ;;
    6)
        echo -e "${RED}Pulendo container, immagini e volumi...${NC}"
        docker-compose down -v
        echo -e "${GREEN}Pulizia completata${NC}"
        ;;
    *)
        echo -e "${RED}Scelta non valida${NC}"
        exit 1
        ;;
esac

echo ""
echo -e "${GREEN}Operazione completata!${NC}"