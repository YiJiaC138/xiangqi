package com.java.xiangqi.logic;

import com.java.xiangqi.Board;
import coordinate.Move;
import coordinate.Position;
import helper.Colour;
import org.junit.jupiter.api.Test;
import pieces.ChessPiece;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PieceMovementTest {

    // --- EXISTING TESTS FROM TestRunner.java MIGRATED AND ADAPTED ---

    @Test
    void testValidMovesFromTestRunner() {
        Board board = new Board();
        // Move Red Soldier from (6,0) to (5,0)
        board.playTurn("6050");
        // Check if the piece has moved
        assertEquals("兵", board.getPieceAtPosition(5, 0).getSymbol());
        
        String [][] test1 = {
                {"·", "·", "·", "·", "將", "·", "·", "·", "·"},
                {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
                {"·", "·", "·", "·", "·", "·", "砲", "·", "·"},
                {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
                {"·", "·", "傌", "·", "·", "·", "·", "·", "·"},
                {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
                {"·", "·", "·", "·", "兵", "·", "·", "·", "·"},
                {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
                {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
                {"·", "·", "·", "·", "帥", "·", "·", "·", "·"},
        };
        Board testBoard1 = new Board(test1, Colour.RED);
        ChessPiece horse = testBoard1.getPieceAtPosition(4,2);
        // Get valid moves for the horse at (4,2)
        List<Move> validMoves = horse.getValidMoves(testBoard1.getPosition(4,2), testBoard1.getBoard());
        assertEquals(8, validMoves.size(), "Horse should have 8 valid moves");
        
        // Horse should be able to move to (2,1), (2,3), (3,0), (3,4), (5,0), (5,4), (6,1), (6,3)
        List<Position> expectedPositions = List.of(
                testBoard1.getPosition(2,1),
                testBoard1.getPosition(2,3),
                testBoard1.getPosition(3,0),
                testBoard1.getPosition(3,4),
                testBoard1.getPosition(5,0),
                testBoard1.getPosition(5,4),
                testBoard1.getPosition(6,1),
                testBoard1.getPosition(6,3)
        );
        for (Move move : validMoves) {
            Position targetPos = move.getDestination();
            assertTrue(expectedPositions.contains(targetPos), "Horse should be able to move to " + targetPos.display());
        }
    }

    @Test
    void testInvalidMovesFromTestRunner(){
        Board board = new Board();
        // We expect to throw an exception when an invalid move is attempted
        assertThrows(IllegalArgumentException.class, () -> {
             // Attempt to move a piece in an invalid way
             board.playTurn("1121");
        });
    }

    // --- OTHER PIECE MOVEMENT TESTS ---

    @Test
    void testHorseMovementAndBlocking() {
        String[][] customBoard = {
            {"·", "·", "·", "·", "將", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "傌", "兵", "·", "·", "·", "·", "·"}, // Horse at 4,2, Soldier at 4,3 (blocks moves to right)
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "帥", "·", "·", "·", "·"},
        };
        
        Board board = new Board(customBoard, Colour.RED);
        ChessPiece horse = board.getPieceAtPosition(4, 2);
        List<Move> validMoves = horse.getValidMoves(board.getPosition(4, 2), board.getBoard());
        
        List<Position> destinations = validMoves.stream().map(Move::getDestination).toList();
        
        // Blocked directions: Right (because of leg at 4,3). 
        boolean hasRightUp = destinations.stream().anyMatch(p -> p.getX() == 3 && p.getY() == 4);
        boolean hasRightDown = destinations.stream().anyMatch(p -> p.getX() == 5 && p.getY() == 4);
        
        assertFalse(hasRightUp, "Horse should be blocked moving Right-Up");
        assertFalse(hasRightDown, "Horse should be blocked moving Right-Down");
        
        // Other moves should be valid (assuming empty)
        //System.out.println(validMoves);
        assertTrue(destinations.stream().anyMatch(p -> p.getX() == 2 && p.getY() == 1), "Up-Left should be valid");
    }

    @Test
    void testCannonCapture() {
         String[][] customBoard = {
            {"·", "·", "·", "·", "將", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "炮", "·", "兵", "·", "士", "·", "·", "·"}, // Cannon(4,1), Screen(4,3), Target(4,5)
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "帥", "·", "·", "·", "·"},
        };
        
        Board board = new Board(customBoard, Colour.RED);
        ChessPiece cannon = board.getPieceAtPosition(4, 1);
        List<Move> validMoves = cannon.getValidMoves(board.getPosition(4, 1), board.getBoard());
        
        List<Position> destinations = validMoves.stream().map(Move::getDestination).toList();
        
        assertTrue(destinations.stream().anyMatch(p -> p.getX() == 4 && p.getY() == 5), "Cannon should capture with screen");
        assertTrue(destinations.stream().anyMatch(p -> p.getX() == 4 && p.getY() == 2), "Cannon should move to empty space");
        assertFalse(destinations.stream().anyMatch(p -> p.getX() == 4 && p.getY() == 3), "Cannon cannot capture friendly screen");
    }
    
    @Test
    void testElephantRiverCrossing() {
        String[][] riverBoard = {
            {"·", "·", "·", "·", "將", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"}, 
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"}, 
            {"·", "·", "相", "·", "·", "·", "·", "·", "·"}, // Red Elephant at 5,2
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "帥", "·", "·", "·", "·"}, 
        };
        
        Board board = new Board(riverBoard, Colour.RED);
        ChessPiece elephant = board.getPieceAtPosition(5, 2);
        List<Move> validMoves = elephant.getValidMoves(board.getPosition(5, 2), board.getBoard());
        
        List<Position> destinations = validMoves.stream().map(Move::getDestination).toList();
        
        boolean crossedRiver = destinations.stream().anyMatch(p -> p.getX() < 5);
        assertFalse(crossedRiver, "Elephant should not cross the river");
    }

    // --- NEW TESTS FOR OTHER PIECES ---

    @Test
    void testGeneralMovementAndPalaceRestriction() {
        // Place General at center of palace (8,4) for Red.
        // Valid moves: 7,4 (Up), 9,4 (Down), 8,3 (Left), 8,5 (Right).
        // Palace for Red: Rows 7-9, Cols 3-5.
        
        String[][] boardConfig = {
            {"·", "·", "·", "·", "將", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "相", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "帥", "·", "·", "·", "·"}, // General at 8,4
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
        };
        
        Board board = new Board(boardConfig, Colour.RED);
        ChessPiece general = board.getPieceAtPosition(8, 4);
        List<Move> validMoves = general.getValidMoves(board.getPosition(8, 4), board.getBoard());
        List<Position> destinations = validMoves.stream().map(Move::getDestination).toList();

        // Check standard orthogonal moves
        assertTrue(destinations.stream().anyMatch(p -> p.getX() == 7 && p.getY() == 4), "General should move Up");
        assertTrue(destinations.stream().anyMatch(p -> p.getX() == 9 && p.getY() == 4), "General should move Down");
        assertTrue(destinations.stream().anyMatch(p -> p.getX() == 8 && p.getY() == 3), "General should move Left");
        assertTrue(destinations.stream().anyMatch(p -> p.getX() == 8 && p.getY() == 5), "General should move Right");
        
        // Test Palace Restriction
        // Place General at edge of palace (7,3) -> Can move to 7,4 or 8,3. Cannot move to 7,2 (Out) or 6,3 (Out).
        String[][] edgeBoardConfig = {
            {"·", "·", "·", "·", "將", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "帥", "·", "·", "·", "·", "·"}, // General at 7,3
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
        };
        Board edgeBoard = new Board(edgeBoardConfig, Colour.RED);
        ChessPiece edgeGeneral = edgeBoard.getPieceAtPosition(7, 3);
        List<Position> edgeDest = edgeGeneral.getValidMoves(edgeBoard.getPosition(7, 3), edgeBoard.getBoard())
                                            .stream().map(Move::getDestination).toList();
                                            
        // Only one move for red General
        assertEquals(1, edgeDest.size(), "General at edge should have 1 move");
        assertFalse(edgeDest.stream().anyMatch(p -> p.getX() == 7 && p.getY() == 2), "Should NOT move left outside palace");
        assertFalse(edgeDest.stream().anyMatch(p -> p.getX() == 6 && p.getY() == 3), "Should NOT move up outside palace");
        // Cannot move to the center of the palace due to flying general rule
        assertFalse(edgeDest.stream().anyMatch(p -> p.getX() == 7 && p.getY() == 4), "Should NOT move to center of palace due to flying general rule");
    }

    @Test
    void testAdvisorMovementAndPalaceRestriction() {
        // Red Advisor at 8,4 (Center Palace). Moves: 7,3; 7,5; 9,3; 9,5.
        String[][] boardConfig = {
            {"·", "·", "·", "·", "將", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "仕", "·", "·", "·", "·"}, // Advisor at 8,4
            {"·", "·", "·", "·", "帥", "·", "·", "·", "·"},
        };
        Board board = new Board(boardConfig, Colour.RED);
        ChessPiece advisor = board.getPieceAtPosition(8, 4);
        List<Position> dests = advisor.getValidMoves(board.getPosition(8, 4), board.getBoard())
                                      .stream().map(Move::getDestination).toList();
        
        assertEquals(4, dests.size(), "Advisor at center should have 4 moves");
        assertTrue(dests.stream().anyMatch(p -> p.getX() == 7 && p.getY() == 3));
        assertTrue(dests.stream().anyMatch(p -> p.getX() == 9 && p.getY() == 5));
        
        // Test Advisor at Corner of Palace (9,3) -> Only 8,4 valid.
        // 9,3 -> Moves: 8,4 (in). 8,2 (out - invalid). 10,2 (out). 10,4 (out).
         String[][] cornerBoardConfig = {
            {"·", "·", "·", "·", "將", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"}, 
            {"·", "·", "·", "仕", "帥", "·", "·", "·", "·"}, // Advisor at 9,3
        };
        Board cornerBoard = new Board(cornerBoardConfig, Colour.RED);
        ChessPiece cornerAdvisor = cornerBoard.getPieceAtPosition(9, 3);
        List<Position> cornerDests = cornerAdvisor.getValidMoves(cornerBoard.getPosition(9, 3), cornerBoard.getBoard())
                                                  .stream().map(Move::getDestination).toList();
                                                  
        assertTrue(cornerDests.stream().anyMatch(p -> p.getX() == 8 && p.getY() == 4), "Should move to center");
        assertFalse(cornerDests.stream().anyMatch(p -> p.getX() == 8 && p.getY() == 2), "Should NOT move outside");
    }

    @Test
    void testChariotMovement() {
        // Chariot at 5,4. Blocked by friendly at 5,6. Enemy at 5,2.
        // Valid: 5,5 (move); 5,3 (move); 5,2 (capture); 4,4; 3,4...
        String[][] boardConfig = {
            {"·", "·", "·", "·", "將", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "卒", "·", "俥", "·", "兵", "·", "·"}, // Enemy(5,2), Chariot(5,4), Friendly(5,6)
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "帥", "·", "·", "·", "·"},
        };
        Board board = new Board(boardConfig, Colour.RED);
        ChessPiece chariot = board.getPieceAtPosition(5, 4);
        List<Position> dests = chariot.getValidMoves(board.getPosition(5, 4), board.getBoard())
                                      .stream().map(Move::getDestination).toList();
        
        // Horizontal
        assertTrue(dests.stream().anyMatch(p -> p.getX() == 5 && p.getY() == 5));
        assertFalse(dests.stream().anyMatch(p -> p.getX() == 5 && p.getY() == 6)); // Blocked by friendly
        assertFalse(dests.stream().anyMatch(p -> p.getX() == 5 && p.getY() == 7)); // Jump blocked
        
        assertTrue(dests.stream().anyMatch(p -> p.getX() == 5 && p.getY() == 3));
        assertTrue(dests.stream().anyMatch(p -> p.getX() == 5 && p.getY() == 2)); // Capture Enemy
        assertFalse(dests.stream().anyMatch(p -> p.getX() == 5 && p.getY() == 1)); // Cannot jump over captured piece
    }

    @Test
    void testSoldierMovement() {
        // Case 1: Before River (Red at 6,4). Moves: 5,4 (Forward only).
        String[][] boardConfig = {
            {"·", "·", "·", "·", "將", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "兵", "·", "·", "·", "·"}, // Soldier at 6,4
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "帥", "·", "·", "·", "·"},
        };
        Board board = new Board(boardConfig, Colour.RED);
        ChessPiece soldier = board.getPieceAtPosition(6, 4);
        List<Position> dests = soldier.getValidMoves(board.getPosition(6, 4), board.getBoard())
                                      .stream().map(Move::getDestination).toList();
        
        assertTrue(dests.stream().anyMatch(p -> p.getX() == 5 && p.getY() == 4), "Should move forward");
        assertFalse(dests.stream().anyMatch(p -> p.getX() == 6 && p.getY() == 3), "Should NOT move left before river");
        assertFalse(dests.stream().anyMatch(p -> p.getX() == 6 && p.getY() == 5), "Should NOT move right before river");
        assertFalse(dests.stream().anyMatch(p -> p.getX() == 7 && p.getY() == 4), "Should NOT move backward");

        // Case 2: After River (Red at 3,4). Moves: 2,4 (Fwd), 3,3 (Left), 3,5 (Right).
        String[][] riverBoardConfig = {
            {"·", "·", "·", "·", "將", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "兵", "·", "·", "·", "·"}, // Soldier at 3,4 (Crossed)
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "帥", "·", "·", "·", "·"},
        };
        Board riverBoard = new Board(riverBoardConfig, Colour.RED);
        ChessPiece crossedSoldier = riverBoard.getPieceAtPosition(3, 4);
        List<Position> riverDests = crossedSoldier.getValidMoves(riverBoard.getPosition(3, 4), riverBoard.getBoard())
                                      .stream().map(Move::getDestination).toList();
        
        assertTrue(riverDests.stream().anyMatch(p -> p.getX() == 2 && p.getY() == 4), "Should move forward");
        assertTrue(riverDests.stream().anyMatch(p -> p.getX() == 3 && p.getY() == 3), "Should move left after river");
        assertTrue(riverDests.stream().anyMatch(p -> p.getX() == 3 && p.getY() == 5), "Should move right after river");
        assertFalse(riverDests.stream().anyMatch(p -> p.getX() == 4 && p.getY() == 4), "Should NOT move backward");
    }

    // --- OPPOSING PLAYER (BLACK) TESTS ---

    @Test
    void testBlackSoldierMovement() {
         // Black Soldier at 3,4 (Before River). Moves: 4,4 (Down/Forward).
        String[][] boardConfig = {
            {"·", "·", "·", "·", "將", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "卒", "·", "·", "·", "·"}, // Black Soldier at 3,4
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "帥", "·", "·", "·", "·"},
        };
        Board board = new Board(boardConfig, Colour.BLACK);
        ChessPiece soldier = board.getPieceAtPosition(3, 4);
        List<Position> dests = soldier.getValidMoves(board.getPosition(3, 4), board.getBoard())
                                      .stream().map(Move::getDestination).toList();
        
        // Black moves DOWN (Increase X)
        assertTrue(dests.stream().anyMatch(p -> p.getX() == 4 && p.getY() == 4), "Black should move forward (down)");
        assertFalse(dests.stream().anyMatch(p -> p.getX() == 3 && p.getY() == 3), "Black NOT sideways before river");
        assertFalse(dests.stream().anyMatch(p -> p.getX() == 2 && p.getY() == 4), "Black NOT backward (up)");
        
        // Black Crossed River (at 6,4).
        String[][] riverBoardConfig = {
            {"·", "·", "·", "·", "將", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "卒", "·", "·", "·", "·"}, // Black Soldier at 6,4
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "帥", "·", "·", "·", "·"},
        };
        Board riverBoard = new Board(riverBoardConfig, Colour.BLACK);
        ChessPiece crossedSoldier = riverBoard.getPieceAtPosition(6, 4);
        List<Position> riverDests = crossedSoldier.getValidMoves(riverBoard.getPosition(6, 4), riverBoard.getBoard())
                                      .stream().map(Move::getDestination).toList();
        
        assertTrue(riverDests.stream().anyMatch(p -> p.getX() == 7 && p.getY() == 4), "Black should move forward (down)");
        assertTrue(riverDests.stream().anyMatch(p -> p.getX() == 6 && p.getY() == 3), "Black should move left");
        assertTrue(riverDests.stream().anyMatch(p -> p.getX() == 6 && p.getY() == 5), "Black should move right");
    }

    @Test
    void testBlackElephantRestriction() {
        // Black Elephant at 4,2 (River edge). Cannot move to 5,0 or 5,4.
        String[][] boardConfig = {
            {"·", "·", "·", "·", "將", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "象", "·", "·", "·", "·", "·", "·"}, // Black Elephant at 4,2
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "·", "·", "·", "·", "·"},
            {"·", "·", "·", "·", "帥", "·", "·", "·", "·"},
        };
        Board board = new Board(boardConfig, Colour.BLACK);
        ChessPiece elephant = board.getPieceAtPosition(4, 2);
        List<Position> dests = elephant.getValidMoves(board.getPosition(4, 2), board.getBoard())
                                      .stream().map(Move::getDestination).toList();
        
        boolean crossedRiver = dests.stream().anyMatch(p -> p.getX() > 4);
        assertFalse(crossedRiver, "Black Elephant should NOT cross river (row > 4)");
    }
}
