package Controller;

import com.java.xiangqi.Board;
import coordinate.Move;
import coordinate.Position;
import pieces.ChessPiece;
import helper.PieceType;
import helper.Colour;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = "*")
public class Controller {

    private final Board board = new Board();

    @GetMapping("/hello")
    public String hello() {
        return "Hello, Xiangqi!";
    }

    // Equivalent to /reset in python (which returns FEN there, here returns state)
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startGame() {
        board.reset();
        return ResponseEntity.ok(getBoardStatus());
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetGame() {
        board.clearAllStates();
        return ResponseEntity.ok(getBoardStatus());
    }

    @PostMapping("/move")
    public ResponseEntity<Map<String, Object>> makeMove(@RequestBody GameRequest request) {
        try {
            String move = request.getPayload();
            board.saveState();
            board.playTurn(move);
            return ResponseEntity.ok(getBoardStatus());
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Internal server error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/undo")
    public ResponseEntity<Map<String, Object>> undoMove() {
        board.restoreBoardState();
        return ResponseEntity.ok(getBoardStatus());
    }

    // Equivalent to /board in python
    @GetMapping("/board")
    public ResponseEntity<Map<String, Object>> getBoard() {
        return ResponseEntity.ok(getBoardStatus());
    }
    
    // Alias for compatibility with previous version
    @GetMapping("/state")
    public ResponseEntity<Map<String, Object>> getState() {
        return getBoard();
    }

    @GetMapping("/legal_moves/{x}/{y}")
    public ResponseEntity<List<Map<String, Integer>>> getLegalMoves(@PathVariable("x") int x, @PathVariable("y") int y) {
        System.out.println("Fetching legal moves for position: " + x + ", " + y);
        List<Map<String, Integer>> legalMoves = new ArrayList<>();
        try {
             ChessPiece piece = board.getPieceAtPosition(x, y);
             if (piece != null) {
                 System.out.println("Piece found: " + piece.getSymbol());
                 Position pos = board.getPosition(x, y);
                 List<Move> validMoves = board.getValidMoves(piece, pos);
                 System.out.println("Found " + validMoves.size() + " valid moves");
                 for (Move move : validMoves) {
                     Map<String, Integer> moveMap = new HashMap<>();
                     moveMap.put("x", move.getDestination().getX());
                     moveMap.put("y", move.getDestination().getY());
                     legalMoves.add(moveMap);
                     System.out.println("Valid move to: " + move.getDestination().getX() + ", " + move.getDestination().getY());
                 }
             } else {
                 System.out.println("No piece at position " + x + ", " + y);
             }
             return ResponseEntity.ok(legalMoves);
        } catch (Exception e) {
            System.err.println("Error fetching legal moves: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    private Map<String, Object> getBoardStatus() {
        Map<String, Object> state = new HashMap<>();
        
        // Match structure of chessAi.py get_board_status
        // "board" in python is FEN. Here we provide the full grid object.
        state.put("board", board.getBoard()); 
        
        state.put("playerTurn", board.getCurrentTurn().toString().toLowerCase());
        state.put("isGameOver", board.isGameOver());
        state.put("isCheck", board.isInCheck(board.getCurrentTurn()));
        state.put("isCheckmate", board.isInCheckMate(board.getCurrentTurn()));
        state.put("isStalemate", false); // Not explicitly tracked
        
        String result = null;
        if (board.isGameOver()) {
            if (board.isInCheckMate(Colour.RED)) result = "black_wins";
            else if (board.isInCheckMate(Colour.BLACK)) result = "red_wins";
            else result = "draw"; // Simplified
        }
        state.put("result", result);

        state.put("pieces", getPiecesList());
        state.put("promotionNeeded", false); 

        return state;
    }

    private List<Map<String, Object>> getPiecesList() {
        List<Map<String, Object>> pieces = new ArrayList<>();
        List<List<Position>> grid = board.getBoard();
        
        for (int i = 0; i < grid.size(); i++) {
            for (int j = 0; j < grid.get(i).size(); j++) {
                Position pos = grid.get(i).get(j);
                if (!pos.isEmpty()) {
                    ChessPiece piece = pos.getPiece();
                    Map<String, Object> pieceMap = new HashMap<>();
                    // id logic: unique identifier
                    pieceMap.put("id", piece.getSymbol() + "-" + i + "-" + j); 
                    pieceMap.put("x", i);
                    pieceMap.put("y", j);
                    pieceMap.put("type", getPieceTypeString(piece.getPieceType()));
                    pieceMap.put("player", piece.getColour().toString().toLowerCase());
                    pieces.add(pieceMap);
                }
            }
        }
        return pieces;
    }

    private String getPieceTypeString(PieceType type) {
        switch (type) {
            case GENERAL: return "general";
            case ADVISOR: return "advisor";
            case ELEPHANT: return "elephant";
            case CHARIOT: return "chariot";
            case HORSE: return "horse";
            case CANNON: return "cannon";
            case SOLDIER: return "soldier";
            default: return "unknown";
        }
    }
}
