package com.java.xiangqi;

import java.util.*;

import coordinate.Move;
import coordinate.Position;
import helper.Colour;
import pieces.ChessPiece;
import pieces.PieceFactory;
public class Board {
    /*
    Class representing a board

     */
    /*
    For reference
    車 馬 象 士 將 士 象 馬 車
    · · · · · · · · ·
    · 砲 · · · · · 砲 ·
    卒 · 卒 · 卒 · 卒 · 卒
    · · · · · · · · ·
    · · · · · · · · ·
    兵 · 兵 · 兵 · 兵 · 兵
    · 炮 · · · · · 炮 ·
    · · · · · · · · ·
    俥 傌 相 仕 帥 仕 相 傌 俥

     */
    private List<List<Position>> board;

    // Dictionary to track which general is in Check
    private HashMap<Colour, Boolean> playerInCheck = new HashMap<>();
    private HashMap<Colour, Boolean> playerInCheckMate = new HashMap<>();
    // Track a stack of board states
    private Stack<List<List<Position>>> boardStates = new Stack<>();
    // List of chess pieces
    private ArrayList<ChessPiece> pieces = new ArrayList<>();

    private final String[][] initialSetup = {
            {"車", "馬", "象", "士", "將", "士", "象", "馬", "車"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "砲", "·", "·", "·", "·", "·", "砲", "·"},
            {"卒", "·", "卒", "·", "卒", "·", "卒", "·", "卒"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"兵", "·", "兵", "·", "兵", "·", "兵", "·", "兵"},
            {"·", "炮", "·", "·", "·", "·", "·", "炮", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"俥", "傌", "相", "仕", "帥", "仕", "相", "傌", "俥"},
    };

    // Player turn
    // Red starts first
    private Colour currentTurn = Colour.RED;

    private boolean gameOver = false;

    private boolean inCheck = false;
    // com.java.xiangqi.Board with vanilla constructor setup
    public Board() {
        this.board = new ArrayList<>();
        generateBoard(initialSetup);
    }
    // It's possible to create an instance of a board with a custom board input
    public Board(String[][] boardInput, Colour startingTurn) {
        this.board = new ArrayList<>();
        generateBoard(boardInput);
        this.currentTurn = startingTurn;
    }
    /**
     * Generates the board based on the provided input.
     */
    public void generateBoard(String[][] boardInput){
        int rows = boardInput.length;
        int cols = boardInput[0].length;
        for (int i = 0; i < rows; i++) {
            List<Position> row = new ArrayList<>();
            for (int j = 0; j < cols; j++) {
                String pieceSymbol = boardInput[i][j];
                Position position;
                if (!pieceSymbol.equals("·")) {
                    ChessPiece piece = PieceFactory.createPiece(pieceSymbol);
                    // Add piece to pieces list
                    pieces.add(piece);
                    position = new Position(i, j, piece);
                    if (piece.getPieceType() == helper.PieceType.GENERAL) {
                        // New Player to track for check
                        playerInCheck.put(piece.getColour(), false);
                        playerInCheckMate.put(piece.getColour(), false);

                    }
                } else {
                    position = new Position(i, j);
                }
                row.add(position);
            }
            board.add(row);
        }
    }
    /**
     * Restores the board state to the last saved state.
     */
    public void restoreBoardState(){
        if (!boardStates.isEmpty()) {
            board = boardStates.pop();
        } else {
            System.out.println("No previous board state to restore.");
        }
    }
    /**
     * Saves the current state of the board to the stack.
     */
    public void saveState(){
        // Saves the current state of the board and push it into the stack
        List<List<Position>> currentState = new ArrayList<>();
        for (List<Position> row : board) {
            List<Position> rowCopy = new ArrayList<>();
            for (Position pos : row) {
                rowCopy.add(new Position(pos.getX(), pos.getY(), pos.getPiece()));
            }
            currentState.add(rowCopy);
        }
        boardStates.push(currentState);
    }

    /**
     * Evaluates if the current player is in checkmate.
     * If the player is in check or in checkmate, we update the states for which player is in check or checkmate.
     */
    public void evaluateCheckMate(){
        // We first evaluate the current board that if the current player is in check
        boolean playerChecked = evaluateCheck(currentTurn);
        if (playerChecked){
            playerInCheck.put(currentTurn, true);
        } else {
            playerInCheck.put(currentTurn, false);
        }

        if (playerInCheck.get(currentTurn) == null || !playerInCheck.get(currentTurn)) {
            return; // If the player is not in check, no need to evaluate checkmate
        } else{
            /* Player is in check, we need to evaluate if the player is in checkmate
             Get any valid moves for the current player. If there are valid moves, it's not checkmate.
             */
            for (List<Position> row : board){
                for (Position pos : row){
                    if (pos.isEmpty()){
                        continue; // Skip empty positions
                    }
                    ChessPiece piece = pos.getPiece();
                    if (piece.getColour() != currentTurn){
                        continue; // Skip pieces that do not belong to the current player
                    }
                    List<Move> validMoves = getValidMoves(piece, pos);
                    
                    // Since getValidMoves now ensures moves do not result in self-check,
                    // any returned move is a valid escape.
                    if (!validMoves.isEmpty()) {
                        playerInCheckMate.put(currentTurn, false);
                        System.out.println("Player " + currentTurn + " is not in checkmate.");
                        return; // Player is not in checkmate
                    }
                }
            }
            // If we reach here, it means the player has no valid moves to escape check
            playerInCheckMate.put(currentTurn, true);
        }
    }

    /**
    Evaluates if the current player is in check.
    This method checks if the current player's general is under threat from any opposing pieces.
    @param player The colour of the current player to see if is in check.
    @return true if the player is in check, false otherwise.
     */
    public boolean evaluateCheck(Colour player){
        return evaluateCheck(player, this.board);
    }

    public boolean evaluateCheck(Colour player, List<List<Position>> boardToCheck){
        // Check if the current player's general is in check
        // Iterate the board and generate a list of valid moves for the current player
        for (List<Position> row : boardToCheck) {
            for (Position pos : row) {
                if (pos.isEmpty()) {
                    continue; // Skip empty positions
                }
                ChessPiece piece = pos.getPiece();
                if (piece != null && piece.getColour() != player) {
                    //System.out.println("Evaluating piece: " + piece.getSymbol() + " at position (" + pos.getX() + "," + pos.getY() + ")");
                    List<Move> validMoves = piece.getValidMoves(pos, boardToCheck);
                    // If the general is in check, return true
                    for (Move move : validMoves) {
                        Position targetPos = move.getDestination();
                        if (targetPos.isEmpty()) {
                            continue; // Skip empty positions
                        }
                        ChessPiece targetPiece = targetPos.getPiece();
                        if (targetPiece != null && targetPiece.getPieceType() == helper.PieceType.GENERAL) {

                            // We found a valid move, now we terminate
                            return true;
                        }
                    }
                }
            }
        }
        // Player is safe if no valid moves can capture the general
        return false;

    }
    /**
     * Returns a boolean if a player is in checkmate
     */
    public boolean isInCheckMate(Colour player){
        return playerInCheckMate.getOrDefault(player, false);
    }
    public boolean isInCheck(Colour player){
        return playerInCheck.getOrDefault(player, false);
    }
    /**
     * Switches the turn to the next player.
     * If the current turn is RED, it switches to BLACK, and vice versa.
     * This method is called after a valid move is made.
     */
    public void switchTurn (){
        currentTurn = (currentTurn == Colour.RED) ? Colour.BLACK : Colour.RED;
    }

    /**
     * Returns the current player's turn.
     */
    public Colour getCurrentTurn() {
        return currentTurn;
    }
    public Position getPosition(int x, int y) {
        return board.get(x).get(y);
    }
    public ChessPiece getPieceAtPosition(int x, int y) {
        Position targetPos = getPosition(x, y);
        if (targetPos.isEmpty()) {
            return null;
        }
        return targetPos.getPiece();
    }
    public List<List<Position>> getBoard() {
        return board;
    }

    /**
    Returns game over state
     */
    public boolean isGameOver() {
        return gameOver;
    }
    /**
    Resets the board to the initial setup
     */
    public void reset(){
        board.clear();
        generateBoard(initialSetup);
        currentTurn = Colour.RED;
        gameOver = false;
        inCheck = false;
    }
    /**
     * Returns a list of all chess pieces on the board.
     */
    public ArrayList<ChessPiece> getAllPieces() {
        return pieces;
    }
    /**
    Starts the game loop
     */
    public void playGame(){
        reset();
        while (!isGameOver()) {
            DisplayBoard();
            playUserTurn();
            // Check for game over conditions
            evaluateCheckMate();
            // Check is any player is in check
            // If in check, evaluate if the player is in checkmate
            // Save the state of the board in a stack
            saveState();


        }
    }
    /**
     * Plays a single turn for the current player.
     * This method handles user input, validates the move, and applies it to the board.
     */
    public void playUserTurn(){
        //Get user input and convert the input string to get a Move object
        Scanner scanner = new Scanner(System.in);
        boolean validMove = false;
        while (!validMove){
            try {
                // We expect a move in digits (1234) == (1,2,3,4) ==(FromX, FromY, ToX, ToY)
                System.out.println("Enter your move:");
                String input = scanner.nextLine();
                // End the game if the input is "c"
                if (input.equalsIgnoreCase("c")) {
                    System.out.println("Game ended by user.");
                    gameOver = true;
                    return;
                }
                // Split the input into a list
                String[] parts = input.split("");

                if (parts.length != 4) {
                    throw new IllegalArgumentException("Invalid input format. Please enter 4 numbers not separated by spaces.");
                }
                // Convert to 0-based index
                int fromX = Integer.parseInt(parts[0]);
                int fromY = Integer.parseInt(parts[1]);
                int toX = Integer.parseInt(parts[2]);
                int toY = Integer.parseInt(parts[3]);
                //Validate this input
                if (validateInput(fromX, fromY, toX, toY)){
                    //Now we validate the move
                    Position fromPos = board.get(fromX).get(fromY);
                    Position toPos = board.get(toX).get(toY);
                    ChessPiece piece = fromPos.getPiece();
                    List<Move> validMoves = getValidMoves(piece, fromPos);
                    // Check if the move within the valid moves
                    for (Move move : validMoves) {
                        if (move.equals(fromPos, toPos)) {
                            // If the move is valid, apply the move
                            move.applyMove();
                            validMove = true;
                            System.out.println(
                                    "Move made: " + piece.getSymbol() +
                                    " from (" + (fromX + 1) + "," + (fromY + 1) +
                                    ") to (" + (toX + 1) + "," + (toY + 1) + ")"
                            );
                            break;
                        }
                    }
                    if (!validMove) {
                        System.out.println("Invalid move attempted. Please try again.");
                    }

                }

            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());

            }
        }
        //Switch the turn after the move is made
        switchTurn();
    }
    /**
    Validates the input for the move
     */
    private boolean validateInput(int fromX, int fromY, int toX, int toY){
        if (fromX < 0 || fromX >= board.size() || fromY < 0 || fromY >= board.get(0).size()) {
            System.out.println("Invalid position");
            return false; // Invalid position
        }
        if (toX < 0 || toX >= board.size() || toY < 0 || toY >= board.get(0).size()) {
            System.out.println("Invalid position");
            return false; // Invalid position
        }
        // We get the position from this input
        Position fromPos = board.get(fromX).get(fromY);
        //Check if the position is empty
        if (fromPos.isEmpty()) {
            System.out.println("No piece at the selected position.");
            return false; // No piece to move
        }
        // Check if the piece belongs to the current player
        ChessPiece piece = fromPos.getPiece();
        if (piece.getColour() != currentTurn) {
            System.out.println("It's not your piece to move.");
            return false; // Not the player's piece
        }

        return true;


    }

    /**
     * Plays a turn for the current player.
     * @param: input of string representing the move.
     */
    public void playTurn(String stringInput){

            String[] parts = stringInput.split("");
            System.out.println(parts.toString());
            if (parts.length != 4) {
                throw new IllegalArgumentException("Invalid input format. Please enter 4 numbers not separated by spaces.");
            }
            // Convert to 0-based index
            int fromX = Integer.parseInt(parts[0]);
            int fromY = Integer.parseInt(parts[1]);
            int toX = Integer.parseInt(parts[2]);
            int toY = Integer.parseInt(parts[3]);
            //Validate this input
            if (validateInput(fromX, fromY, toX, toY)) {
                System.out.println("Input validated");
                //Now we validate the move
                Position fromPos = board.get(fromX).get(fromY);
                Position toPos = board.get(toX).get(toY);
                ChessPiece piece = fromPos.getPiece();
                List<Move> validMoves = getValidMoves(piece, fromPos);
                // Check if the move within the valid moves
                for (Move move : validMoves) {
                    if (move.equals(fromPos, toPos)) {
                        // If the move is valid, apply the move
                        move.applyMove();
                        switchTurn();
                        evaluateCheckMate();

                        return;
                    }
                }
                throw new IllegalArgumentException("Invalid move attempted. No valid moves");
            }
            else{
                throw new IllegalArgumentException("Invalid move attempted.");
            }
        }

    /**
     * Returns a list of valid moves for a given piece at a specific position.
     * @param piece The chess piece to check for valid moves.
     * @param pos The position of the piece on the board.
     * @return A list of valid moves for the piece.
     */
    public List<Move> getValidMoves(ChessPiece piece, Position pos){
        List<Move> validMoves = piece.getValidMoves(pos, board);
        List<Move> legalMoves = new ArrayList<>();
        for (Move move : validMoves) {
            // Duplicate board
            List<List<Position>> tempBoard = new ArrayList<>();
            for (List<Position> row : board) {
                List<Position> rowCopy = new ArrayList<>();
                for (Position p : row) {
                    rowCopy.add(new Position(p.getX(), p.getY(), p.getPiece()));
                }
                tempBoard.add(rowCopy);
            }

            // Apply move
            Position fromPos = tempBoard.get(move.getSource().getX()).get(move.getSource().getY());
            Position toPos = tempBoard.get(move.getDestination().getX()).get(move.getDestination().getY());

            toPos.setPiece(fromPos.getPiece());
            fromPos.emptyPiece();

            // Check
            if (!evaluateCheck(piece.getColour(), tempBoard)) {
                legalMoves.add(move);
            }
        }
        return legalMoves;
    }

    public void DisplayBoard(){
        String output = "";
        int rowDisplay = 0;
        for (List<Position> row : board) {
            output += rowDisplay++ + " ";

            for (Position pos : row) {
                output += pos.display() + " ";
            }
            output += "\n";
        }
        // Add column headers
        output += "  ";
        for (int i = 0; i <= board.get(0).size(); i++) {
            output += i + " ";
        }
        System.out.println(output);
    }
}
