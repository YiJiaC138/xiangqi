package com.java.xiangqi.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java.xiangqi.Board;
import com.java.xiangqi.model.Room;
import coordinate.Move;
import coordinate.Position;
import helper.Colour;
import helper.PieceType;
import pieces.ChessPiece;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Set<WebSocketSession> allSessions = new CopyOnWriteArraySet<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GameWebSocketHandler() {
        // Initialize 5 rooms
        for (int i = 1; i <= 5; i++) {
            String roomId = String.valueOf(i);
            rooms.put(roomId, new Room(roomId));
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        allSessions.add(session);
        System.out.println("New connection: " + session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        allSessions.remove(session);
        System.out.println("Connection closed: " + session.getId());
        
        boolean needsBroadcast = false;
        // Clean up player from rooms
        for (Room room : rooms.values()) {
            boolean updated = false;
            synchronized (room) {
                if (session.equals(room.getRedPlayer())) {
                    room.setRedPlayer(null);
                    updated = true;
                }
                if (session.equals(room.getBlackPlayer())) {
                    room.setBlackPlayer(null);
                    updated = true;
                }
                
                if (updated) {
                    if (room.getRedPlayer() == null && room.getBlackPlayer() == null) {
                        room.setStatus("waiting");
                        room.getBoard().reset(); 
                    } else {
                         // One player left
                         room.setStatus("waiting");
                    }
                    needsBroadcast = true;
                }
            }
        }
        
        if (needsBroadcast) {
            broadcastRoomUpdate();
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        Map<String, Object> msg = objectMapper.readValue(payload, Map.class);
        String type = (String) msg.get("type");
        
        if (type == null) {
            sendError(session, "Message type is missing");
            return;
        }

        switch (type) {
            case "GET_ROOMS":
                handleGetRooms(session);
                break;
            case "JOIN_GAME":
                handleJoinGame(session, msg);
                break;
            case "GET_GAME_STATE":
                handleGetGameState(session, msg);
                break;
            case "MOVE":
                handleMove(session, msg);
                break;
            case "GET_LEGAL_MOVES":
                handleGetLegalMoves(session, msg);
                break;
            case "RESET":
                handleReset(session, msg);
                break;
            case "UNDO":
                handleUndo(session, msg);
                break;
            default:
                sendError(session, "Unknown message type: " + type);
        }
    }

    private void handleGetRooms(WebSocketSession session) throws IOException {
        sendRoomUpdate(session);
    }

    private void handleJoinGame(WebSocketSession session, Map<String, Object> msg) throws IOException {
        String roomId = (String) msg.get("roomId");
        String color = (String) msg.get("color");
        
        if (roomId == null) {
            Map<String, Object> p = (Map<String, Object>) msg.get("payload");
            if (p != null) {
                roomId = (String) p.get("roomId");
                color = (String) p.get("color");
            }
        }

        Room room = rooms.get(roomId);
        if (room == null) {
            sendError(session, "Room not found");
            return;
        }

        synchronized (room) {
            boolean success = false;
            if ("red".equalsIgnoreCase(color)) {
                if (room.getRedPlayer() == null) {
                    room.setRedPlayer(session);
                    success = true;
                }
            } else if ("black".equalsIgnoreCase(color)) {
                if (room.getBlackPlayer() == null) {
                    room.setBlackPlayer(session);
                    success = true;
                }
            } else {
                sendError(session, "Invalid color");
                return;
            }

            if (success) {
                if (room.getRedPlayer() != null && room.getBlackPlayer() != null) {
                    room.setStatus("playing");
                }
                
                // Send JOIN_SUCCESS
                Map<String, Object> response = new HashMap<>();
                response.put("type", "JOIN_SUCCESS");
                Map<String, Object> respPayload = new HashMap<>();
                respPayload.put("roomId", roomId);
                respPayload.put("color", color);
                // Include gameState as requested
                respPayload.put("gameState", getBoardStatus(room.getBoard()));
                response.put("payload", respPayload);
                sendMessage(session, response);

                // Broadcast ROOM_UPDATE
                broadcastRoomUpdate();

                // Send GAME_STATE (Still keeping this for good measure, but JOIN_SUCCESS now has it)
                sendGameState(session, room);
            } else {
                sendError(session, "Room full or color taken");
            }
        }
    }

    private void handleGetGameState(WebSocketSession session, Map<String, Object> msg) throws IOException {
        String roomId = (String) msg.get("roomId");
         if (roomId == null) {
            Map<String, Object> p = (Map<String, Object>) msg.get("payload");
            if (p != null) {
                roomId = (String) p.get("roomId");
            }
        }

        Room room = rooms.get(roomId);
        if (room == null) return;
        
        sendGameState(session, room);
    }
    
    private void handleMove(WebSocketSession session, Map<String, Object> msg) throws IOException {
        String roomId = (String) msg.get("roomId");
        String moveStr = (String) msg.get("move");
         if (roomId == null) {
            Map<String, Object> p = (Map<String, Object>) msg.get("payload");
            if (p != null) {
                roomId = (String) p.get("roomId");
                moveStr = (String) p.get("move");
            }
        }

        Room room = rooms.get(roomId);
        if (room == null) return;

        synchronized (room) {
            // Validate turn
            Board board = room.getBoard();
            boolean isRedTurn = board.getCurrentTurn() == Colour.RED;
            boolean isPlayerRed = session.equals(room.getRedPlayer());
            boolean isPlayerBlack = session.equals(room.getBlackPlayer());

            if ((isRedTurn && !isPlayerRed) || (!isRedTurn && !isPlayerBlack)) {
                sendError(session, "Not your turn");
                return;
            }

            try {
                board.saveState();
                board.playTurn(moveStr);
                
                // Broadcast GAME_STATE to both
                broadcastGameState(room);
            } catch (Exception e) {
                sendError(session, "Invalid move: " + e.getMessage());
            }
        }
    }

    private void handleGetLegalMoves(WebSocketSession session, Map<String, Object> msg) throws IOException {
        String roomId = (String) msg.get("roomId");
        Integer pieceX = null;
        Integer pieceY = null;

        if (msg.containsKey("pieceX")) {
            pieceX = (Integer) msg.get("pieceX");
            pieceY = (Integer) msg.get("pieceY");
        } else if (msg.containsKey("payload")) {
             Map<String, Object> p = (Map<String, Object>) msg.get("payload");
             if (p != null) {
                 roomId = (String) p.get("roomId");
                 pieceX = (Integer) p.get("pieceX");
                 pieceY = (Integer) p.get("pieceY");
             }
        }
        
        if (roomId == null || pieceX == null || pieceY == null) return;

        Room room = rooms.get(roomId);
        if (room == null) return;

        Board board = room.getBoard();
        List<Map<String, Integer>> legalMoves = new ArrayList<>();
        
        try {
             ChessPiece piece = board.getPieceAtPosition(pieceX, pieceY);
             if (piece != null) {
                 Position pos = board.getPosition(pieceX, pieceY);
                 List<Move> validMoves = board.getValidMoves(piece, pos);
                 for (Move move : validMoves) {
                     Map<String, Integer> moveMap = new HashMap<>();
                     moveMap.put("x", move.getDestination().getX());
                     moveMap.put("y", move.getDestination().getY());
                     legalMoves.add(moveMap);
                 }
             }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("type", "LEGAL_MOVES");
        response.put("payload", legalMoves);
        sendMessage(session, response);
    }

    private void handleReset(WebSocketSession session, Map<String, Object> msg) throws IOException {
        String roomId = (String) msg.get("roomId");
        if (roomId == null && msg.containsKey("payload")) {
            roomId = (String) ((Map) msg.get("payload")).get("roomId");
        }
        
        Room room = rooms.get(roomId);
        if (room == null) return;

        synchronized (room) {
             room.getBoard().clearAllStates(); // Reset game
             broadcastGameState(room);
        }
    }

    private void handleUndo(WebSocketSession session, Map<String, Object> msg) throws IOException {
         String roomId = (String) msg.get("roomId");
        if (roomId == null && msg.containsKey("payload")) {
            roomId = (String) ((Map) msg.get("payload")).get("roomId");
        }

        Room room = rooms.get(roomId);
        if (room == null) return;

        synchronized (room) {
             room.getBoard().restoreBoardState();
             broadcastGameState(room);
        }
    }

    private void broadcastRoomUpdate() {
        try {
            List<Map<String, Object>> roomList = new ArrayList<>();
            for (Room room : rooms.values()) {
                Map<String, Object> r = new HashMap<>();
                r.put("id", room.getId());
                r.put("redPlayer", room.getRedPlayer() != null ? room.getRedPlayer().getId() : null);
                r.put("blackPlayer", room.getBlackPlayer() != null ? room.getBlackPlayer().getId() : null);
                r.put("status", room.getStatus());
                roomList.add(r);
            }
            
            roomList.sort(Comparator.comparing(m -> (String)m.get("id")));
            
            Map<String, Object> response = new HashMap<>();
            response.put("type", "ROOM_UPDATE");
            response.put("payload", roomList);
            
            TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(response));
            
            for (WebSocketSession s : allSessions) {
                if (s.isOpen()) {
                    s.sendMessage(textMessage);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void sendMessage(WebSocketSession session, Map<String, Object> msg) throws IOException {
        if (session.isOpen()) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(msg)));
        }
    }

    private void sendError(WebSocketSession session, String message) throws IOException {
        Map<String, Object> response = new HashMap<>();
        response.put("type", "ERROR");
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", message);
        response.put("payload", payload);
        sendMessage(session, response);
    }

    private void broadcastGameState(Room room) throws IOException {
        Map<String, Object> state = getBoardStatus(room.getBoard());
        Map<String, Object> response = new HashMap<>();
        response.put("type", "GAME_STATE");
        response.put("payload", state);
        
        if (room.getRedPlayer() != null) sendMessage(room.getRedPlayer(), response);
        if (room.getBlackPlayer() != null) sendMessage(room.getBlackPlayer(), response);
    }
    
    private void sendGameState(WebSocketSession session, Room room) throws IOException {
        Map<String, Object> state = getBoardStatus(room.getBoard());
        Map<String, Object> response = new HashMap<>();
        response.put("type", "GAME_STATE");
        response.put("payload", state);
        sendMessage(session, response);
    }
    
    private void sendRoomUpdate(WebSocketSession session) throws IOException {
        List<Map<String, Object>> roomList = new ArrayList<>();
        for (Room room : rooms.values()) {
            Map<String, Object> r = new HashMap<>();
            r.put("id", room.getId());
            r.put("redPlayer", room.getRedPlayer() != null ? room.getRedPlayer().getId() : null);
            r.put("blackPlayer", room.getBlackPlayer() != null ? room.getBlackPlayer().getId() : null);
            r.put("status", room.getStatus());
            roomList.add(r);
        }
        roomList.sort(Comparator.comparing(m -> (String)m.get("id")));
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "ROOM_UPDATE");
        response.put("payload", roomList);
        sendMessage(session, response);
    }

    private Map<String, Object> getBoardStatus(Board board) {
        Map<String, Object> state = new HashMap<>();
        // Use a simplified representation for "board" if needed, 
        // but existing Controller uses board.getBoard() which is List<List<Position>>.
        // I'll trust it works with Jackson.
        state.put("board", board.getBoard()); 
        
        state.put("playerTurn", board.getCurrentTurn().toString().toLowerCase());
        state.put("isGameOver", board.isGameOver());
        state.put("isCheck", board.isInCheck(board.getCurrentTurn()));
        state.put("isCheckmate", board.isInCheckMate(board.getCurrentTurn()));
        state.put("isStalemate", false); 
        
        String result = null;
        if (board.isGameOver()) {
            if (board.isInCheckMate(Colour.RED)) result = "black_wins";
            else if (board.isInCheckMate(Colour.BLACK)) result = "red_wins";
            else result = "draw"; 
        }
        state.put("result", result);
        state.put("pieces", getPiecesList(board));
        state.put("promotionNeeded", false); 

        return state;
    }

    private List<Map<String, Object>> getPiecesList(Board board) {
        List<Map<String, Object>> pieces = new ArrayList<>();
        List<List<Position>> grid = board.getBoard();
        
        for (int i = 0; i < grid.size(); i++) {
            for (int j = 0; j < grid.get(i).size(); j++) {
                Position pos = grid.get(i).get(j);
                if (!pos.isEmpty()) {
                    ChessPiece piece = pos.getPiece();
                    Map<String, Object> pieceMap = new HashMap<>();
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
