# Arquitetura

Este documento descreve a arquitetura atual do protótipo e as extensões que ainda podem ser estudadas.

## Visão geral

O sistema possui dois programas:

- **servidor**, executado no computador controlado, responsável por produzir a imagem da tela e aplicar comandos de entrada;
- **cliente**, executado no computador controlador, responsável por exibir a tela remota e capturar interações do usuário.

Inicialmente, os dois programas poderão usar uma única conexão TCP. Mesmo compartilhando a conexão, existem dois fluxos conceitualmente separados:

```text
SERVER -> CLIENT
imagem da tela

CLIENT -> SERVER
comandos de entrada
```

O protocolo precisará identificar o tipo de cada mensagem para que o receptor saiba como interpretar os dados seguintes.

## Fluxo da imagem

```text
COMPUTADOR CONTROLADO
         │
         │ ScreenCapture
         ▼
   captura da tela
         │
         ▼
  divisão em blocos
         │
         ▼
 comparação com frame anterior
         │
         ▼
 serialização das mensagens
         │
         ▼
        TCP
         │
         ▼
COMPUTADOR CONTROLADOR
         │
         ▼
 leitura das mensagens
         │
         ▼
 RemoteScreenPanel
         │
         ▼
 reconstrução da imagem
```

O `ScreenCapture` captura a tela e produz blocos com posição, dimensões e dados de imagem. O cliente usa essas informações para desenhar cada região no local correto, formando visualmente a tela remota.

## Fluxo dos comandos do mouse

```text
CLIENTE
MouseListener / MouseMotionListener
             │
             ▼
  MOUSE_MOVE / MOUSE_CLICK
             │
             │ TCP
             ▼
SERVIDOR
RemoteInputController
             │
             ▼
      java.awt.Robot
             │
             ▼
Mouse do sistema operacional
```

O cliente transforma eventos do painel em mensagens do protocolo. O `RemoteInputController` recebe mensagens validadas e usa `java.awt.Robot` para executar a ação no computador controlado.

## Por que dividir a tela em blocos

Enviar a tela inteira a cada atualização desperdiçaria rede quando apenas uma pequena região mudasse. Uma grade permite identificar e enviar regiões independentes.

```text
Frame 1:          Frame 2:

[A][B][C]         [A][B][C]
[D][E][F]         [D][X][F]
```

Nesse exemplo, somente o bloco `X` precisaria ser retransmitido, pois `A`, `B`, `C`, `D` e `F` permaneceram iguais.

Cada bloco deverá carregar, no mínimo:

```text
x, y             posição na tela
width, height    dimensões do bloco
imageData         bytes da imagem
```

Blocos menores tornam as atualizações mais localizadas, mas aumentam a quantidade de metadados e comparações. Blocos maiores reduzem essa sobrecarga, mas podem retransmitir áreas inalteradas. O tamanho adequado deverá ser avaliado durante os estudos.

## Protocolo

TCP entrega um fluxo contínuo de bytes, não mensagens prontas. Por isso, cliente e servidor deverão concordar com uma estrutura e uma ordem de leitura. Uma mensagem de bloco poderá ser organizada conceitualmente assim:

```text
┌──────────────┬───┬───┬─────────┬────────┬─────────────┬────────────┐
│ tipo         │ x │ y │ largura │ altura │ nº de bytes │ imagem     │
└──────────────┴───┴───┴─────────┴────────┴─────────────┴────────────┘
```

`MessageType` representa a categoria da mensagem. Os campos são escritos e lidos na mesma ordem com `DataOutputStream` e `DataInputStream`.

## Responsabilidades

```text
server/
  ServerApplication         inicia e coordena o servidor
  ScreenCapture             captura e divide a tela
  RemoteInputController     aplica comandos de entrada

client/
  ClientApplication         inicia e coordena o cliente
  RemoteScreenPanel         reconstrói e exibe a tela

protocol/
  MessageType               identifica categorias de mensagem
  ProtocolMessage           representa uma mensagem conceitual
```

## Concorrência

Captura, leitura da rede e interface gráfica possuem ritmos diferentes. O servidor usa uma Thread para receber comandos enquanto transmite imagens, e o Swing mantém sua Event Dispatch Thread para a interface.

A separação utilizada é:

```text
Servidor: captura/transmissão + recebimento de comandos
Cliente:  recebimento de blocos + Event Dispatch Thread
```

## Fora do escopo atual

O protótipo ainda não possui autenticação, criptografia, suporte completo a vários monitores, múltiplos clientes ou controle de teclado. A codificação PNG é usada somente por bloco e pode ser otimizada futuramente.
