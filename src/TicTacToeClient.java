import java.util.Scanner;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * A client for a multi-player tic tac toe game. Loosely based on an example in
 * Deitel and Deitel’s “Java How to Program” book. For this project I created a
 * new application-level protocol called TTTP (for Tic Tac Toe Protocol), which
 * is entirely plain text. The messages of TTTP are:
 *
 * Client -> Server MOVE <n> QUIT
 *
 * Server -> Client WELCOME <char> VALID_MOVE OTHER_PLAYER_MOVED <n>
 * OTHER_PLAYER_LEFT VICTORY DEFEAT TIE MESSAGE <text>
 */
public class TicTacToeClient {


    private Square[] board = new Square[9];
    private Socket socket;
    private Scanner in;
    private PrintWriter out;
    Scanner input = new Scanner(System.in);

    public TicTacToeClient(String serverAddress) throws Exception {

        socket = new Socket(serverAddress, 58901);
        in = new Scanner(socket.getInputStream());
        out = new PrintWriter(socket.getOutputStream(), true);

        for (var i = 0; i < board.length; i++) {
            board[i] = new Square();
        }

        System.out.println("Setting up connection");
    }

    private String Display(Square square) {
        if (square == null) {
            return " ";
        } 
        return square.text;
    }
    private void DisplayBoard() {
        System.out.println("Board:");
        System.out.println("   " + "1" + "   " + "2" + "   " + "3");
        System.out.println("A  " + Display(board[0]) + " | " + Display(board[1]) + " | " + Display(board[2]));
        System.out.println("   " + "-----------");
        System.out.println("B  " + Display(board[3]) + " | " + Display(board[4]) + " | " + Display(board[5]));
        System.out.println("   " + "-----------");
        System.out.println("C  " + Display(board[6]) + " | " + Display(board[7]) + " | " + Display(board[8]));
    }
    
    private void gameOver(String Player1, String Player2){
        var response = in.nextLine();
        if(response.startsWith("VICTORY")){
            System.out.println("You win!");
        }
        else if(response.startsWith("DEFEAT")){
            System.out.println("You lose!");
        }
        else if(response.startsWith("TIE")){
            System.out.println("Tied!");
        }

        input.close();
    }
    
    private int getPlayerInput() {
        System.out.print("Please enter your move (Like 2A, 3C, etc.): ");
        //Get user Intput
        String userInput = input.nextLine();

        switch (userInput) {
            case "1A":
                return 0;
            case "2A":
                return 1;
            case "3A":
                return 2;
            case "1B":
                return 3;
            case "2B":
                return 4;
            case "3B":
                return 5;
            case "1C":
                return 6;
            case "2C":
                return 7;
            case "3C":
                return 8;
            default:
                return getPlayerInput();
        }
    }
    /**
     * The main thread of the client will listen for messages from the server. The
     * first message will be a "WELCOME" message in which we receive our mark. Then
     * we go into a loop listening for any of the other messages, and handling each
     * message appropriately. The "VICTORY", "DEFEAT", "TIE", and
     * "OTHER_PLAYER_LEFT" messages will ask the user whether or not to play another
     * game. If the answer is no, the loop is exited and the server is sent a "QUIT"
     * message.
     */
    public void play() throws Exception {
        try {
            var response = in.nextLine();
            var mark = response.charAt(6);
            var opponentMark = mark == 'X' ? 'O' : 'X';
            System.out.println("Connected to server. Player: " + mark);
            DisplayBoard();
            while (in.hasNextLine()) {
                response = in.nextLine();
                if (response.startsWith("GAMEACK")) {

                    System.out.println("Game Start!");
                    int move = getPlayerInput();
                    board[move].setText(mark);
                    out.println("MOVE " + move);
                } else if (response.startsWith("MOVEACK")) {
                    System.out.print("\033[2J\033[1;1H");
                    System.out.println("Valid move, waiting for opponent move: ");
                    DisplayBoard();
                } else if (response.startsWith("MOVEDENIED")) {
                    System.out.print("\033[2J\033[1;1H");
                    System.out.println("Move denied. Try again.");
                    DisplayBoard();
                    int move = getPlayerInput();                    
                    board[move].setText(mark);
                    out.println("MOVE " + move);
                } else if (response.startsWith("MOVEDATA")) {
                    var loc = Integer.parseInt(response.substring(9));
                    board[loc].setText(opponentMark);
                    System.out.print("\033[2J\033[1;1H");
                    System.out.println("Opponent moved, your turn");
                    DisplayBoard();
                    int move = getPlayerInput();                    
                    board[move].setText(mark);
                    out.println("MOVE " + move);
                    
                } else if (response.startsWith("SERVERMESSAGE")) {
                    System.out.println(response.substring(15));
                } else if (response.startsWith("GAMESTATUS:WIN")) {
                    System.out.print("\033[2J\033[1;1H");
                    DisplayBoard();
                    System.out.println("Winner Winner Cicken Dinner!");
                    input.close();

                    break;
                } else if (response.startsWith("GAMESTATUS:DEFEAT")) {
                    System.out.print("\033[2J\033[1;1H");
                    DisplayBoard();
                    System.out.println("You lost, git gud");
                    input.close();
                    break;
                } else if (response.startsWith("GAMESTATUS:TIE")) {
                    System.out.print("\033[2J\033[1;1H");
                    DisplayBoard();
                    System.out.println("Tie");
                    break;
                } else if (response.startsWith("CLIENTEXIT")) {
                    System.out.println("Other player left");
                    break;
                }
            }
            out.println("CLIENTEXIT");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }
    }

    static class Square {
        String text = " ";
        
        public void setText(char text) {
            this.text = String.valueOf(text);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Pass the server IP as the sole command line argument");
            return;
        }
        TicTacToeClient client = new TicTacToeClient(args[0]);
        client.play();
    }
}