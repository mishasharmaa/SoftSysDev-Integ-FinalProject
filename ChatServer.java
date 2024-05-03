package com.example.w24csci2020uassignment03hossinzehifrankweatherssharma;

import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;

/**
 * This class represents a WebSocket server, which handles WebSocket connections for a chat and game application.
 * It manages rooms, sessions, and user interactions within chat rooms.
 */
@ServerEndpoint(value = "/ws/{roomID}")
public class ChatServer {

    // A list of chat rooms to manage existing rooms and their users.
    private static ArrayList<ChatRoom> chatRooms = new ArrayList<>();
    // The current chat room the session is connected to.
    private static ChatRoom currentRoom;

    // Represents the tic-tac-toe board as a 2D array.
    private String[][] board = new String[3][3];

    /**
     * Handles the opening of a new WebSocket connection.
     * @param roomID The ID of the chat room.
     * @param session The current session of the client.
     * @throws IOException When there is an error in sending a message.
     * @throws EncodeException When there is an error encoding the message.
     */
    @OnOpen
    public void open(@PathParam("roomID") String roomID, Session session) throws IOException, EncodeException {
        // Check if the room already exists.
        if (!checkRooms(roomID)) {
            // Create a new chat room if it doesn't exist.
            currentRoom = new ChatRoom(roomID, session.getId());
            chatRooms.add(currentRoom);
            session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): Welcome to " + roomID + " chat room.\"}");
        } else {
            // Add user to the existing chat room.
            for (ChatRoom c : chatRooms) {
                if (c.getCode().equals(roomID)) {
                    c.addUser(session.getId());
                    currentRoom = c;
                }
            }
            session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): Welcome to " + roomID + " chat room.\"}");
        }
        // Handle the scenario when there are already 2 players in the room.
        if (currentRoom.getPlayercount() == 2) {
            for (Session peer : session.getOpenSessions()) {
                String user = peer.getId();
                if (currentRoom.inRoom(user) && (currentRoom.getCode().equals(currentRoom.getCode()))) {
                    session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"There are already 2 players in the room. You can type to them but you are unable to play.\"}");
                    session.getBasicRemote().sendText("{\"type\": \"side\", \"message\":\"spectator\"}");
                }
            }
        } else {
            // Manage the addition of players and assign symbols (x or o) to them.
            for (ChatRoom c : chatRooms) {
                if (c.getCode().equals(roomID)) {
                    currentRoom.addPlayercount();
                    String playerSymbol;
                    if (currentRoom.getPlayercount() == 1) {
                        playerSymbol = "x";
                    } else if (currentRoom.getPlayercount() == 2) {
                        playerSymbol = "o";
                    } else {
                        playerSymbol = "spectator";
                    }
                    for (Session peer : session.getOpenSessions()) {
                        String user = peer.getId();
                        if (currentRoom.inRoom(user) && (currentRoom.getCode().equals(currentRoom.getCode()))) {
                            session.getBasicRemote().sendText("{\"type\": \"playerCount\", \"message\":\"" + currentRoom.getPlayercount() + "\"}");
                        }
                    }
                    session.getBasicRemote().sendText("{\"type\": \"side\", \"message\":\"" + playerSymbol + "\"}");
                    session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): You are playing as " + playerSymbol + ".\"}");
                }
            }
        }
    }

    /**
     * Handles the closing of a WebSocket connection.
     * @param session The current session of the client.
     * @throws IOException When there is an error in sending a message.
     * @throws EncodeException When there is an error encoding the message.
     */
    @OnClose
    public void close(Session session) throws IOException, EncodeException {
        currentRoom.minusPlayerCount();
        String userId = session.getId();
        String userName = "";
        for (Session peer : session.getOpenSessions()) {
            String user = peer.getId();
            if (currentRoom.inRoom(user) && (currentRoom.getCode().equals(currentRoom.getCode()))) {
                userName = currentRoom.getUserName(userId);
                currentRoom.removeUser(userId);
                if (userName.equals("")) {
                    userName = "anonymous";
                }
                peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): " + userName + " left the chat room.\"}");
            }
        }
    }

    /**
     * Handles messages sent by the client.
     * @param comm The message received from the client.
     * @param session The current session of the client.
     * @throws IOException When there is an error in sending a message.
     * @throws EncodeException When there is an error encoding the message.
     */
    @OnMessage
    public void handleMessage(String comm, Session session) throws IOException, EncodeException {
        String userID = session.getId();
        JSONObject jsonmsg = new JSONObject(comm);
        String type = (String) jsonmsg.get("type");
        String message = (String) jsonmsg.get("msg");

        // Handle different message types based on the received message type.
        if (type.equals("chat")) {
            // Check if this is the user's first message to set their username.
            if (currentRoom.first(userID)) {
                String username = currentRoom.getUserName(userID);
                System.out.println(username);
                // Broadcast the chat message to other users in the same room.
                for (Session peer : session.getOpenSessions()) {
                    String user = peer.getId();
                    for (ChatRoom c : chatRooms) {
                        if (c.inRoom(user) && (c.getCode().equals(currentRoom.getCode())))
                            peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(" + username + "): " + message + "\"}");
                    }
                }
            } else {
                // Set the user's name if this is the first message from the user.
                currentRoom.setUserName(userID, message);
                for (Session peer : session.getOpenSessions()) {
                    String user = peer.getId();
                    if (currentRoom.inRoom(user))
                        peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): Welcome, " + message + "!\"}");
                }
            }
        } else if (type.equals("playerCount")) {
            // Respond with the current player count in the room.
            session.getBasicRemote().sendText("{\"type\": \"playerCount\", \"message\":\"" + currentRoom.getPlayercount() + "\"}");
        } else if (type.equals("chatRooms")) {
            // Respond with a list of all available chat rooms.
            message = "";
            for (ChatRoom c : chatRooms) {
                message += c.getCode() + " ";
            }
            session.getBasicRemote().sendText("{\"type\": \"chatRooms\", \"message\":\"" + message + "\"}");
        } else if (type.equals("board")) {
            // Update the game board based on the received message.
            updateBoard(message);
            // Check for win conditions and send the results to other sessions.
            if (checkWin(board).equals("x")) {
                for (Session peer : session.getOpenSessions()) {
                    String user = peer.getId();
                    for (ChatRoom c : chatRooms) {
                        if (c.inRoom(user) && (c.getCode().equals(currentRoom.getCode())))
                            peer.getBasicRemote().sendText("{\"type\": \"win\", \"message\":\"" + "x" + "\"}");
                    }
                }
            }
            if (checkWin(board).equals("o")) {
                for (Session peer : session.getOpenSessions()) {
                    String user = peer.getId();
                    for (ChatRoom c : chatRooms) {
                        if (c.inRoom(user) && (c.getCode().equals(currentRoom.getCode())))
                            peer.getBasicRemote().sendText("{\"type\": \"win\", \"message\":\"" + "o" + "\"}");
                    }
                }
            }
            if (checkWin(board).equals("Draw")) {
                for (Session peer : session.getOpenSessions()) {
                    String user = peer.getId();
                    for (ChatRoom c : chatRooms) {
                        if (c.inRoom(user) && (c.getCode().equals(currentRoom.getCode())))
                            peer.getBasicRemote().sendText("{\"type\": \"win\", \"message\":\"" + "Draw" + "\"}");
                    }
                }
            } else {
                // Send the updated board state to other sessions.
                String m = sendBoard(message);
                m = m.replace("\n", "");
                m = m.replace("\"", "\\\"");
                String playersSession = session.getId();
                for (Session peer : session.getOpenSessions()) {
                    String user = peer.getId();
                    for (ChatRoom c : chatRooms) {
                        if (c.inRoom(user) && (c.getCode().equals(currentRoom.getCode())) && !playersSession.equals(user)) {
                            peer.getBasicRemote().sendText("{\"type\": \"board\", \"message\":\"" + m + "\"}");
                        }
                    }
                }
            }
        }
    }

    /**
     * Convert a 2D array to a single string for transmission.
     * @param array The 2D array to convert.
     * @return A single string representing the array.
     */
    public static String arrayToString(String[][] array) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[i].length; j++) {
                stringBuilder.append(array[i][j]).append(" ");
            }
        }
        return stringBuilder.toString();
    }

    /**
     * Converts HTML string of a table with tic-tac-toe board data to a modified HTML string based on the board state.
     * @param tableData The HTML string containing the tic-tac-toe board.
     * @return A modified HTML string with the board state.
     */
    public String sendBoard(String tableData) {
        Document doc = Jsoup.parse(tableData);
        StringBuilder tableBuilder = new StringBuilder("<table id=\"board\" class=\"board\">");
        int cellIndex = 0;
        // Iterate through each row of the board to build the HTML.
        for (String[] rowArray : board) {
            tableBuilder.append("<tr>");
            for (String cell : rowArray) {
                if (cell.equals("O")) {
                    tableBuilder.append("<td class=\"cell\" id=\"cell-").append(cellIndex).append("\"><img src=\"images/o.png\" style=\"width:100px;height:100px;\"></td>");
                } else if (cell.equals("X")) {
                    tableBuilder.append("<td class=\"cell\" id=\"cell-").append(cellIndex).append("\"><img src=\"images/x.png\" style=\"width:100px;height:100px;\"></td>");
                } else {
                    tableBuilder.append("<td class=\"cell\" id=\"cell-").append(cellIndex).append("\"></td>");
                }
                cellIndex++;
            }
            tableBuilder.append("</tr>");
        }
        tableBuilder.append("</table>");
        String newTableHTML = tableBuilder.toString();
        doc.select("table").first().replaceWith(Jsoup.parse(newTableHTML).select("table").first());
        return doc.toString();
    }

    /**
     * Updates the board state based on HTML table data.
     * @param tableData The HTML string containing the tic-tac-toe board.
     */
    public void updateBoard(String tableData) {
        Document doc = Jsoup.parse(tableData);
        Elements cells = doc.select("td");
        int row = 0;
        int col = 0;
        // Check each cell in the document and update the board.
        for (Element cell : cells) {
            if (cell.select("img").isEmpty()) {
                board[row][col] = "";
            } else {
                // Check the img src to determine 'O' or 'X'.
                String imgSrc = cell.select("img").attr("src");
                if (imgSrc.equals("images/o.png")) {
                    board[row][col] = "O";
                } else {
                    board[row][col] = "X";
                }
            }
            col++;
            if (col == 3) {
                col = 0;
                row++;
            }
            if (row == 3) {
                break;
            }
        }
    }

    /**
     * Checks the game board to determine if there is a winner or if the game is a draw.
     * @param board The tic-tac-toe board represented as a 2D array.
     * @return A string representing the game state ("x" for X win, "o" for O win, "Draw" for draw, "noWinner" otherwise).
     */
    public String checkWin(String[][] board) {
        String[] winningCombinations = {
                // Rows
                board[0][0] + board[0][1] + board[0][2],
                board[1][0] + board[1][1] + board[1][2],
                board[2][0] + board[2][1] + board[2][2],
                // Columns
                board[0][0] + board[1][0] + board[2][0],
                board[0][1] + board[1][1] + board[2][1],
                board[0][2] + board[1][2] + board[2][2],
                // Diagonals
                board[0][0] + board[1][1] + board[2][2],
                board[0][2] + board[1][1] + board[2][0]
        };

        // Check for winning combinations.
        for (String combination : winningCombinations) {
            if (combination.equals("OOO")) {
                return "o";
            }
            if (combination.equals("XXX")) {
                return "x";
            }
        }

        // Check if there are any empty cells.
        for (String[] row : board) {
            for (String cell : row) {
                if (cell.isEmpty()) {
                    return "noWinner";
                }
            }
        }
        // If all cells are filled, it is a draw.
        return "Draw";
    }

    /**
     * Checks if a chat room with the given ID already exists.
     * @param roomID The ID of the chat room.
     * @return True if the room exists, false otherwise.
     */
    public boolean checkRooms(String roomID) {
        for (ChatRoom c : chatRooms) {
            if (c.getCode().equals(roomID)) {
                return true;
            }
        }
        return false;
    }
}
