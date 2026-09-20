# Roteiro de implementação

Este roteiro organiza o desenvolvimento em etapas pequenas e verificáveis. Avance somente depois de compreender e testar a etapa anterior.

## Etapa 1 — Estrutura inicial

- [x] Criar o projeto Maven.
- [x] Criar os packages de cliente, servidor e protocolo.
- [x] Criar as classes principais como um esqueleto compilável.
- [x] Garantir que `ClientApplication` e `ServerApplication` executem separadamente.
- [ ] Manter as próximas etapas sem implementação até o momento de estudá-las.

## Etapa 2 — Comunicação TCP básica

- Estudar a função de `ServerSocket` no servidor e de `Socket` no cliente.
- Escolher uma porta local para os primeiros testes.
- Fazer o servidor aguardar uma conexão.
- Fazer o cliente se conectar a `localhost`.
- Criar streams de entrada e saída nos dois lados.
- Enviar inicialmente apenas texto simples.
- Testar comunicação bidirecional com o seguinte diálogo:

```text
CLIENT -> HELLO
SERVER -> OK
```

- Fechar sockets e streams de forma segura, preferencialmente com `try-with-resources`.
- Não misturar esta etapa com captura ou envio de imagens.

## Etapa 3 — Captura da tela

- Estudar `java.awt.Robot` e as permissões exigidas pelo sistema operacional.
- Descobrir a resolução ou os limites da tela usando as APIs AWT.
- Criar um `Rectangle` correspondente à área que será capturada.
- Usar `Robot.createScreenCapture(...)` para obter um `BufferedImage`.
- Fazer um teste exclusivamente local, salvando uma captura em arquivo.
- Verificar o comportamento em ambientes sem interface gráfica e com mais de um monitor.
- Ainda não transmitir a imagem pela rede.

## Etapa 4 — Divisão da tela em blocos

A tela não deverá ser tratada como uma única unidade de transmissão. Ela será dividida em regiões menores:

```text
Tela
┌────┬────┬────┐
│ A  │ B  │ C  │
├────┼────┼────┤
│ D  │ E  │ F  │
└────┴────┴────┘
```

- Estudar como obter regiões de um `BufferedImage`.
- Experimentar tamanhos de bloco como `64x64`, `100x100` e `128x128`.
- Tratar corretamente os blocos das bordas quando a resolução não for múltipla do tamanho escolhido.
- Criar futuramente uma representação de bloco contendo pelo menos:

```text
x
y
width
height
imageData
```

- Testar a divisão e a remontagem apenas em memória antes de envolver a rede.

## Etapa 5 — Criar protocolo de transmissão

- Definir o formato de cada mensagem enviada pela conexão.
- Usar `MessageType` para distinguir blocos de tela, comandos do mouse e mensagens de controle.
- Estudar `DataInputStream` e `DataOutputStream`.
- Definir uma ordem fixa para leitura e escrita dos campos.
- Para um bloco de imagem, transmitir primeiro os metadados:

```text
tipo
x
y
largura
altura
quantidade de bytes
bytes da imagem
```

- Validar tamanhos antes de alocar buffers no receptor.
- Documentar o formato para que cliente e servidor permaneçam compatíveis.
- Não usar serialização Java de objetos como substituto para compreender o protocolo.

## Etapa 6 — Enviar um único bloco

Antes de transmitir a tela inteira:

1. Capturar uma única região da tela.
2. Converter o bloco para bytes em um formato de imagem adequado.
3. Escrever seus metadados e bytes no stream TCP.
4. Ler a mensagem no cliente.
5. Reconstruir um `BufferedImage`.
6. Salvar ou exibir o bloco recebido para conferir o resultado.

- Verificar se o número de bytes escrito é igual ao número lido.
- Manter o teste pequeno para facilitar a depuração do protocolo.

## Etapa 7 — Transmitir vários blocos

- Percorrer a grade de blocos da tela.
- Enviar cada bloco como uma mensagem individual.
- No cliente, identificar a posição de destino por `x` e `y`.
- Conferir especialmente os blocos menores das bordas.
- Medir o volume de dados e observar o efeito do tamanho escolhido para os blocos.

> **Nunca enviar a tela completa como um único objeto ou uma única imagem pela rede.** A divisão em blocos é parte central do estudo e permitirá atualizar apenas regiões alteradas posteriormente.

## Etapa 8 — Interface gráfica do cliente

- Criar uma janela Swing (`JFrame`).
- Adicionar um `RemoteScreenPanel` à janela.
- Manter em memória uma imagem que represente o estado atual da tela remota.
- Ao receber um bloco, desenhá-lo nas coordenadas corretas dessa imagem.
- Solicitar a repintura do painel.
- Garantir que atualizações visuais respeitem a Event Dispatch Thread do Swing.

O painel deverá receber blocos, posicioná-los e reconstruir visualmente a tela remota.

## Etapa 9 — Atualização contínua

- Criar um ciclo controlado de atualização:

```text
capturar
   ↓
dividir
   ↓
enviar
   ↓
exibir
   ↓
capturar novamente
```

- Começar com uma frequência baixa para facilitar testes.
- Evitar um loop sem pausa que consuma toda a CPU.
- Medir taxa de quadros e quantidade de dados transmitida.
- Considerar Threads somente após o fluxo sequencial estar correto.

## Etapa 10 — Detecção de blocos alterados

Esta é uma otimização bônus e deve ser implementada somente depois que a transmissão completa por blocos funcionar.

- Guardar informações dos blocos da captura anterior.
- Comparar cada posição da grade com a mesma posição do frame anterior:

```text
bloco atual == bloco anterior
    não envia

bloco atual != bloco anterior
    envia
```

- Estudar alternativas simples:
  - comparação direta de pixels;
  - conversão para arrays e uso de `Arrays.equals`;
  - hash ou checksum calculado por bloco.
- Começar pela abordagem mais fácil de validar, sem otimização prematura.
- Confirmar que uma alteração localizada gera apenas o envio dos blocos afetados.

## Etapa 11 — Controle do mouse

- Adicionar listeners ao painel remoto para observar `mouseMoved` e `mouseClicked`.
- Transformar cada evento em uma mensagem do protocolo, por exemplo:

```text
MOUSE_MOVE x y
MOUSE_CLICK x y LEFT
```

- Enviar os comandos do cliente para o servidor pela conexão TCP.
- No servidor, validar o tipo e os valores recebidos.
- Somente então estudar o uso de:

```java
Robot.mouseMove(...);
Robot.mousePress(...);
Robot.mouseRelease(...);
```

- Criar uma forma segura de desativar o controle durante os testes.

## Etapa 12 — Conversão de coordenadas

- Considerar o caso em que a resolução do servidor difere do tamanho do painel no cliente:

```text
resolução do servidor != tamanho da janela do cliente
```

- Registrar a resolução original da tela remota.
- Calcular a escala horizontal e vertical usada para desenhar a imagem.
- Converter as coordenadas do evento no painel para coordenadas da tela remota.
- Considerar barras ou margens quando a proporção da imagem for preservada.
- Testar cliques nos quatro cantos e no centro antes de permitir controle contínuo.

## Etapa 13 — Threads

- Separar as responsabilidades que podem bloquear umas às outras:

```text
Thread de captura/transmissão
Thread de recebimento de comandos
Thread da interface gráfica
```

- Estudar concorrência, interrupção e visibilidade de dados entre Threads.
- Evitar modificar componentes Swing fora da Event Dispatch Thread (EDT).
- Usar `SwingUtilities.invokeLater(...)` quando uma atualização visual vier da rede.
- Planejar como encerrar todas as Threads quando a conexão terminar.

## Etapa 14 — Tratamento de desconexão

- Tratar o cliente encerrando normalmente.
- Tratar o servidor sendo fechado.
- Tratar perda inesperada da conexão.
- Detectar fim de stream e exceções de leitura/escrita.
- Fechar streams e sockets sem deixá-los abertos.
- Interromper loops e Threads relacionados à sessão.
- Atualizar a interface com um estado de desconectado.
- Permitir um novo teste sem precisar reiniciar todo o ambiente, se fizer sentido.

## Etapa 15 — Testes finais

- Testar servidor e cliente em `localhost`.
- Testar os dois programas no mesmo computador.
- Testar em dois computadores da mesma rede.
- Verificar regras de firewall e o endereço IP usado na conexão.
- Testar atualização de toda a tela e de pequenas regiões.
- Testar movimentação e clique do mouse com segurança.
- Confirmar a conversão de coordenadas em tamanhos de janela diferentes.
- Confirmar que regiões estáticas não são retransmitidas após a otimização.
- Testar desconexões normais e inesperadas.
- Registrar limitações conhecidas e ideias para estudos futuros.
