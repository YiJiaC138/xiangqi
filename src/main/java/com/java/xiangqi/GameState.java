package com.java.xiangqi;

import java.util.HashMap;
import java.util.List;
import coordinate.Position;
import helper.Colour;

public class GameState {
    List<List<Position>> board;
    Colour currentTurn;
    HashMap<Colour, Boolean> playerInCheck;
    HashMap<Colour, Boolean> playerInCheckMate;
    boolean gameOver;

    public GameState(List<List<Position>> board, Colour currentTurn, HashMap<Colour, Boolean> playerInCheck, HashMap<Colour, Boolean> playerInCheckMate, boolean gameOver) {
        this.board = board;
        this.currentTurn = currentTurn;
        this.playerInCheck = playerInCheck;
        this.playerInCheckMate = playerInCheckMate;
        this.gameOver = gameOver;
    }
}

