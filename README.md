## Getting Started

Welcome to the VS Code Java world. Here is a guideline to help you get started to write Java code in Visual Studio Code.

## Folder Structure

The workspace contains two folders by default, where:

- `src`: the folder to maintain sources
- `lib`: the folder to maintain dependencies

## Building

1. Compile and run "TiTacToeServer.java."

2. Once the server is waiting for connection, each player must compile and run "TicTacToeClient."

3. The moves are applied by entering a number to represent the columns, then a letter that represents the rows. There are no spaces between the number and letter.

4. Player X will have the first turn, Player O must wait for X to finish turn.

5. After Player X finishes first move, X cannot make another move until O has finished move. 

6. If an invalid move is entered, the server will return the same board status.

7. If either player gets 3 in a row vertically, horizontally, or diagonally, they will receive a message from the server that they have won.

9. The player that lost will receive message from the server to say that they have lost the game. 

8. If the board is full and there are no moves left, the game will end with a draw. 