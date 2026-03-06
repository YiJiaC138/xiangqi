package com.java.xiangqi.model;

import com.java.xiangqi.Board;
import org.springframework.web.socket.WebSocketSession;

public class Room {
    private String id;
    private WebSocketSession redPlayer;
    private WebSocketSession blackPlayer;
    private Board board;
    private String status; // "waiting" or "playing"

    public Room(String id) {
        this.id = id;
        this.board = new Board();
        this.status = "waiting";
    }

    public String getId() {
        return id;
    }

    public WebSocketSession getRedPlayer() {
        return redPlayer;
    }

    public void setRedPlayer(WebSocketSession redPlayer) {
        this.redPlayer = redPlayer;
    }

    public WebSocketSession getBlackPlayer() {
        return blackPlayer;
    }

    public void setBlackPlayer(WebSocketSession blackPlayer) {
        this.blackPlayer = blackPlayer;
    }

    public Board getBoard() {
        return board;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
