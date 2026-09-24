# TCP Remote Desktop

Projeto acadêmico em Java para estudar a construção gradual de um software simples de acesso remoto via TCP, inspirado conceitualmente em VNC e RDP.

**Status: Protótipo funcional / Em desenvolvimento**

## Tecnologias

- Java 17
- Maven
- APIs nativas do Java: TCP, Swing/AWT, `Robot` e `BufferedImage`

## Funcionalidades implementadas

- captura e divisão da tela em blocos;
- transmissão dos blocos por TCP;
- reconstrução da tela no cliente;
- detecção de blocos alterados;
- envio de movimentos e cliques do mouse ao servidor.

Cada bloco é enviado separadamente com posição, dimensões e bytes PNG. A tela
completa nunca é serializada ou transmitida como um único objeto.

## Compilar

Na raiz do projeto:

```bash
mvn compile
```

## Executar o servidor

Após compilar:

```bash
java -cp target/classes com.remotedesktop.server.ServerApplication
```

## Executar o cliente

Após compilar, em outro terminal:

```bash
java -cp target/classes com.remotedesktop.client.ClientApplication
```

Para conectar em outro computador, informe o IP e opcionalmente a porta:

```bash
java -cp target/classes com.remotedesktop.client.ClientApplication 192.168.0.10 5000
```

O servidor aceita um cliente por execução. A janela do cliente mostra a tela
remota; movimentos e cliques feitos sobre ela são reproduzidos no servidor.

## Limitações e segurança

- captura apenas a tela principal;
- atende somente um cliente por execução;
- não possui autenticação nem criptografia;
- foi desenvolvido para estudo e deve ser usado somente em uma rede local confiável;
- firewall e permissões do sistema operacional podem bloquear a porta ou o uso de `Robot`.

## Regra de commits

O projeto possui um hook Git em `.githooks/pre-commit` que permite commits
somente na branch `main` e bloqueia o commit quando o build falha. O hook usa o
Maven Wrapper ou Maven quando disponível e, como alternativa, compila os fontes
com Java 17. Para ativá-lo depois de clonar o repositório, execute na raiz do
repositório:

```bash
git config core.hooksPath .githooks
```
