package com.example.w24csci2020uassignment03hossinzehifrankweatherssharma;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Random;

@Path("/TableResource")
public class TableResource {
    // API endpoint to handle POST requests for updating the game table
    @Path("/sendTable")
    @POST
    public Response sendTable(String tableData) {
        try {
            // Parse the HTML table data received in the POST request
            Document doc = Jsoup.parse(tableData);
            // Select all table cell elements from the document
            Elements cells = doc.select("td");
            // Create a 3x3 array to represent the game board
            String[][] board = new String[3][3];
            int row = 0;
            int col = 0;

            // Process each cell in the document to populate the board array
            for (Element cell : cells) {
                // If the cell doesn't contain an <img> tag, mark it as empty
                if (cell.select("img").isEmpty()) {
                    board[row][col] = "-";
                } else {
                    // If the cell contains an <img> tag, determine whether it is an "O" or "X"
                    String imgSrc = cell.select("img").attr("src");
                    if (imgSrc.equals("images/o.png")) {
                        board[row][col] = "O";
                    } else {
                        board[row][col] = "X";
                    }
                }

                // Increment the column index
                col++;
                // Move to the next row if the column index reaches 3
                if (col == 3) {
                    col = 0;
                    row++;
                }
                // Break the loop if all rows have been processed
                if (row == 3) {
                    break;
                }
            }

            // Use a random number generator to randomly place an "O" on the board
            Random random = new Random();
            int randomCol, randomRow;
            do {
                randomCol = random.nextInt(3);
                randomRow = random.nextInt(3);
            } while (!board[randomCol][randomRow].equals("-"));

            // Place an "O" on the randomly chosen empty cell
            board[randomCol][randomRow] = "O";

            // Build the HTML for the updated game board
            StringBuilder tableBuilder = new StringBuilder("<table id=\"board\" class=\"board\">");
            int cellIndex = 0;
            // Loop through the board array to construct the HTML table
            for (String[] rowArray : board) {
                tableBuilder.append("<tr>");
                for (String cell : rowArray) {
                    if (cell.equals("O")) {
                        // Add an <img> tag with "O" image to the table cell
                        tableBuilder.append("<td class=\"cell\" id=\"cell-")
                                .append(cellIndex)
                                .append("\"><img src=\"images/o.png\" style=\"width:100px;height:100px;\"></td>");
                    } else if (cell.equals("X")) {
                        // Add an <img> tag with "X" image to the table cell
                        tableBuilder.append("<td class=\"cell\" id=\"cell-")
                                .append(cellIndex)
                                .append("\"><img src=\"images/x.png\" style=\"width:100px;height:100px;\"></td>");
                    } else {
                        // Add an empty table cell
                        tableBuilder.append("<td class=\"cell\" id=\"cell-")
                                .append(cellIndex)
                                .append("\"></td>");
                    }
                    cellIndex++;
                }
                tableBuilder.append("</tr>");
            }
            tableBuilder.append("</table>");

            // Update the document with the new game board HTML
            String newTableHTML = tableBuilder.toString();
            doc.select("table").first().replaceWith(Jsoup.parse(newTableHTML).select("table").first());

            // Create a JSON response containing the updated HTML document
            JSONObject jsonResponse = new JSONObject();
            jsonResponse.put("message", doc.toString());
            return Response.ok()
                    .header("Content-Type", "application/json")
                    .entity(jsonResponse.toString())
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().build(); // Return a server error response if an exception occurs
        }
    }
}
