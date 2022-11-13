import java.util.Scanner;
import java.io.PrintWriter;
import java.net.Socket;

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
    /**
     * Display the current board state, with 1, 2, 3 and a, b, c labels. To let player know what to enter in user input
     */
    private void DisplayBoard() {
        System.out.println("Board:");
        System.out.println("   " + "1" + "   " + "2" + "   " + "3");
        System.out.println("A  " + Display(board[0]) + " | " + Display(board[1]) + " | " + Display(board[2]));
        System.out.println("   " + "-----------");
        System.out.println("B  " + Display(board[3]) + " | " + Display(board[4]) + " | " + Display(board[5]));
        System.out.println("   " + "-----------");
        System.out.println("C  " + Display(board[6]) + " | " + Display(board[7]) + " | " + Display(board[8]));
    }
    /**
     * This method is used to get the user input and return the index of the array.
     * The switch statement is used to check the user input and return the index of the array.
     * If the user input is not valid, the method will be called again.
     */
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
     * This code is the main game loop. It listens for messages from the server and responds accordingly. 
     * The first if statement checks to see if the message starts with GAMEACK, which means that both players have connected and it's time to start playing. 
     * If this is true then we get player input, set their mark on the board, and send a move request to the server. 
     * MOVEACK, which means that our last move was valid so we display an updated board and wait for opponent moves. 
     * MOVEDENIED which means our last move was invalid so we need to redraw the board with any new marks that were placed since our last turn as well as tell them they made an invalid move and ask them again what they want to do.
     * MOVEDATA which tells us where our opponent moved so we update our board with their mark in that location and ask for player input again because it's now their turn again. 
     * SERVERMESSAGE Prints out whatever message the server sends us. 
     * GAMESTATUS:WIN displays a winning message when the player wins 
     * GAMESTATUS:DEFEAT displays a losing message when the player loses 
     * GAMESTATUS:TIE displays a tie message when there are no more moves left but nobody has won yet
     * @throws Exception
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
                    String boardText = response.substring(11);
                    String[] boardData = boardText.split(" ");
                    for (int i = 0; i < boardData.length; i++) {
                        if (boardData[i].toCharArray()[0] != '0') {
                            board[i].setText(boardData[i].toCharArray()[0]);
                        } else {
                            board[i].setText(' ');
                        }
                    }
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