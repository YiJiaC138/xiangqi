# Xiangqi Backend (Chinese Chess)

This is the backend application for a Xiangqi (Chinese Chess) game, built with Java and Spring Boot. It provides a RESTful API to manage game logic, state persistence, and rule enforcement for the frontend application.

## Features Checklist

- [x] **Game State Management**:
  - [x] REST API endpoints for retrieving board state (`/api/game/board`).
  - [x] Manages turn-based play (Red vs Black).
  - [x] Handles game initialization and reset (`/api/game/start`, `/api/game/reset`).

- [x] **Move Validation & Logic**:
  - [x] Calculates valid moves for all Xiangqi pieces (General, Advisor, Elephant, Horse, Chariot, Cannon, Soldier).
  - [x] Exposes legal moves via API (`/api/game/legal_moves/{x}/{y}`) to support frontend highlighting.
  - [x] Enforces movement rules and prevents illegal moves.

- [x] **Game Status Detection**:
  - [x] **Check** detection logic.
  - [x] **Checkmate** detection logic.
  - [x] **Game Over** state handling.
  - [ ] **Stalemate** detection (Currently a placeholder).

- [x] **Game Controls**:
  - [x] **Undo Move**: API support for reverting the last move (`/api/game/undo`).
  - [x] **State Persistence**: Maintains history for undo operations.

- [x] **Multiplayer & Networking**:
  - [x] Game Lobby / Room management.
  - [x] WebSocket support for real-time multiplayer.

  Front Logic can be found [here](https://github.com/YiJiaC138/xiangqi-front)

## [DEPLOYED HERE](https://xiangqi-front.onrender.com) (Takes awhile upon bootup)