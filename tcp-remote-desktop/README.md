# TCP Remote Desktop

Projeto acadêmico em Java para estudar a construção gradual de um software simples de acesso remoto via TCP, inspirado conceitualmente em VNC e RDP.

**Status: Estrutura inicial / Em desenvolvimento**

## Tecnologias

- Java 17
- Maven
- APIs nativas do Java, futuramente incluindo TCP, Swing/AWT, `Robot` e `BufferedImage`

## Funcionalidades planejadas

- captura e divisão da tela em blocos;
- transmissão dos blocos por TCP;
- reconstrução da tela no cliente;
- detecção de blocos alterados;
- envio de movimentos e cliques do mouse ao servidor.

Essas funcionalidades ainda **não estão implementadas**. O projeto contém somente o esqueleto inicial para estudo.

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

Por enquanto, cada aplicação apenas imprime uma mensagem de identificação e encerra.
