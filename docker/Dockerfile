# Dockerfile per ambiente di sviluppo e test Nexum
FROM maven:3.9.11-sapmachine-25

# Metadata
LABEL maintainer="Nexum Project"
LABEL description="Development and testing environment for Nexum"

# Installare utility utili
RUN apt-get update && apt-get install -y \
    git vim nano tree ssh bash-completion sudo whois \
    zip curl unzip ca-certificates \
    && rm -rf /var/lib/apt/lists/*

RUN apt-get clean && rm -rf /tmp/* /var/tmp/*

RUN usermod -p $(mkpasswd -m sha-512 ciao) -a -G sudo $(id -un 1000)

USER ubuntu
WORKDIR /home/ubuntu/workspace

# COPY pom.xml .
# RUN mvn dependency:go-offline -B

# Comando predefinito: eseguire i test
CMD ["tail", "-f", "/dev/null"]