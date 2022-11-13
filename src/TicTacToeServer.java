import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.Executors;

/**
 * A server for a multi-player tic tac toe game. Loosely based on an example in
 * Deitel and Deitel’s “Java How to Program” book. For this project I created a
 * new application-level protocol called TTTP (for Tic Tac Toe Protocol), which
 * is entirely plain text. The messages of TTTP are:
 *
 * Client -> Server MOVE <n> QUIT
 *
 * Server -> Client WELCOME <char> VALID_MOVE OTHER_PLAYER_MOVED <n>
 * OTHER_PLAYER_LEFT VICTORY DEFEAT TIE MESSAGE <text>
 */
public class TicTacToeServer {

    public static void main(String[] args) throws Exception {
        try (var listener = new ServerSocket(58901)) {
            System.out.println("Game Server is Ready...");
            var pool = Executors.newFixedThreadPool(200);
            while (true) {
                Game game = new Game();
                pool.execute(game.new Player(listener.accept(), 'X'));
                pool.execute(game.new Player(listener.accept(), 'O'));
            }
        }
    }
}

class Game {

    // Board cells numbered 0-8, top to bottom, left to right; null if empty
    private Player[] board = new Player[9];

    Player currentPlayer;

    public boolean hasWinner() {
        // horizontal
        for (int i = 0; i < 3; i++) {
            boolean empty = false;

            for (int j = 0; j < 3; j++) {
                if (board[i + j] == null) {
                    empty = true;
                    break;
                }
            }
            if (!empty && board[i] == board[i + 1] && board [i + 1] == board[i + 2])
                return true;
        }
        //vertical
        for (int i = 0; i < 3; i++) {
            boolean empty = false;

            for (int j = 0; j < 3; j++) {
                if (board[i + j*3] == null) {
                    empty = true;
                    break;
                }
            }
            if (!empty && board[0 + i] == board[3 + i] && board [3 + i] == board[6 + i])
                return true;
        }
        // check diagonal
        int[] diagonal1 = {0, 4, 8};
        int[] diagonal2 = {3, 4, 6};
        boolean empty = false;

        for (int i : diagonal1) {
            if (board[i] == null) {
                empty = true;
                break;
            }
        }
        if (!empty && board[diagonal1[0]] == board[diagonal1[1]] && board[diagonal1[1]] == board[diagonal1[2]]) {
            return true;
        }
        empty = false;

        for (int i : diagonal1) {
            if (board[i] == null) {
                empty = true;
                break;
            }
        }
        if (!empty && board[diagonal2[0]] == board[diagonal2[1]] && board[diagonal2[1]] == board[diagonal2[2]]) {
            return true;
        }
        return false;
    }

    public boolean boardFilledUp() {
        return Arrays.stream(board).allMatch(p -> p != null);
    }

    public synchronized void move(int location, Player player) {
        if (player != currentPlayer) {
            throw new IllegalStateException("Not your turn");
        } else if (player.opponent == null) {
            throw new IllegalStateException("You don't have an opponent yet");
        } else if (board[location] != null) {
            throw new IllegalStateException("MOVEDENIED " + getBoard());
        }
        board[location] = currentPlayer;
        currentPlayer = currentPlayer.opponent;
    }
    public String getBoard() {
        String boardString = "";

        for (int i = 0; i < board.length; i++) {
            Player player = board[i];

            if (player == null) {
                boardString += String.valueOf('0') + " ";
                System.out.println("Null found");
            } else {
                boardString += String.valueOf(player.mark) + " ";
            }
        }
        
        return boardString.trim();
    }

    /**
     * A Player is identified by a character mark which is either 'X' or 'O'. For
     * communication with the client the player has a socket and associated Scanner
     * and PrintWriter.
     */
    class Player implements Runnable {
        char mark;
        Player opponent;
        Socket socket;
        Scanner input;
        PrintWriter output;

        public Player(Socket socket, char mark) {
            this.socket = socket;
            this.mark = mark;
        }

        @Override
        public void run() {
            try {
                setup();
                processCommands();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (opponent != null && opponent.output != null) {
                    opponent.output.println("CLIENTEXIT");
                }
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }

        private void setup() throws IOException {
            input = new Scanner(socket.getInputStream());
            output = new PrintWriter(socket.getOutputStream(), true);
            output.println("ACK:  " + mark);
            if (mark == 'X') {
                currentPlayer = this;
                output.println("SERVERMESSAGE: Waiting for opponent to connect");
            } else {
                opponent = currentPlayer;
                opponent.opponent = this;
                opponent.output.println("GAMEACK");
            }
        }

        private void processCommands() {
            while (input.hasNextLine()) {
                var command = input.nextLine();
                if (command.startsWith("CLIENTEXIT")) {
                    return;
                } else if (command.startsWith("MOVE")) {
                    processMoveCommand(Integer.parseInt(command.substring(5)));
                }
            }
        }

        private void processMoveCommand(int location) {
            try {
                move(location, this);
                output.println("MOVEACK");
                opponent.output.println("MOVEDATA " + location);
                if (hasWinner()) {
                    output.println("GAMESTATUS:WIN");
                    opponent.output.println("GAMESTATUS:DEFEAT");
                } else if (boardFilledUp()) {
                    output.println("GAMESTATUS:TIE");
                    opponent.output.println("GAMESTATUS:TIE");
                }
            } catch (IllegalStateException e) {
                output.println(e.getMessage());
            }
        }
    }
}