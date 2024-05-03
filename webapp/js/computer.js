// Define the base URL for the server API.
let apiUrl = "http://localhost:8080/w24-csci2020u-assignment03-hossinzehi-frank-weathers-sharma-1.0-SNAPSHOT/api/TableResource";

// Helper function to introduce a delay (pause) for a specified amount of time (in milliseconds).
function sleep(ms = 0) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

// Event handler for when a cell is clicked on the tic-tac-toe board.
function handleCellClick(event) {
    // Get the clicked cell from the event.
    const clickedCell = event.target;

    // Create an image element for the X mark and set its properties.
    let image = document.createElement("img");
    image.style.width = "100px"; // Set image width to 100 pixels.
    image.style.height = "100px"; // Set image height to 100 pixels.
    image.src = "images/x.png"; // Set the source to the X image.
    // Append the image to the clicked cell.
    clickedCell.appendChild(image);

    // Remove the click event listener from the clicked cell to prevent further clicks.
    clickedCell.removeEventListener('click', handleCellClick);

    // Check if the game has been won.
    checkWin().then(boolValue => {
        if (boolValue) {
            // If the game is won, remove click event listeners from all cells without images.
            const cells = document.getElementById('board').querySelectorAll('.cell');
            cells.forEach(cell => {
                if (!cell.querySelector('img')) {
                    cell.removeEventListener('click', handleCellClick);
                }
            });
        } else {
            // If the game isn't won, send the current game state to the server.
            post_to_server("sendTable");
        }
    });
}

// Function to check if there is a win condition on the board.
async function checkWin() {
    // Define winning combinations for the game (rows, columns, diagonals).
    const winningCombinations = [
        [0, 1, 2], // Row 1
        [3, 4, 5], // Row 2
        [6, 7, 8], // Row 3
        [0, 3, 6], // Column 1
        [1, 4, 7], // Column 2
        [2, 5, 8], // Column 3
        [0, 4, 8], // Diagonal 1
        [2, 4, 6]  // Diagonal 2
    ];

    // Initialize an array to store the game board state.
    const board = [];
    // Get the game board element and all cells.
    const table = document.getElementById('board');
    const cells = table.querySelectorAll('.cell');

    // Extract cell values into a 2D array.
    for (let i = 0; i < cells.length; i += 3) {
        board.push([
            cells[i].querySelector('img') ? cells[i].querySelector('img').src : '',
            cells[i + 1].querySelector('img') ? cells[i + 1].querySelector('img').src : '',
            cells[i + 2].querySelector('img') ? cells[i + 2].querySelector('img').src : ''
        ]);
    }

    // Convert image URLs to 'X', 'O', or empty string.
    for (let x = 0; x < 3; x++) {
        for (let y = 0; y < 3; y++) {
            // Convert image source URL to 'X', 'O', or empty string.
            if (board[x][y] === apiUrl + "/images/x.png") {
                board[x][y] = "X";
            } else if (board[x][y] === apiUrl + "/images/o.png") {
                board[x][y] = "O";
            } else {
                board[x][y] = "";
            }
        }
    }

    // Check each winning combination to see if any player has won.
    for (const combination of winningCombinations) {
        const [a, b, c] = combination; // Destructure the combination.
        const valueA = board[Math.floor(a / 3)][a % 3]; // Get the value of cell a.
        const valueB = board[Math.floor(b / 3)][b % 3]; // Get the value of cell b.
        const valueC = board[Math.floor(c / 3)][c % 3]; // Get the value of cell c.

        // If all three values are the same and not empty, a player has won.
        if (valueA && valueA === valueB && valueB === valueC) {
            // Wait for 100ms (as a short delay) and determine the winner.
            await sleep(100);
            let winner = valueA.split('/').slice(-1)[0][0]; // Determine the winner (X or O).

            // Get the username from the URL query parameters.
            let current_url = new URL(window.location.href);
            let username = current_url.searchParams.get("username");

            // Display an alert indicating the winner.
            if (winner === "X") {
                alert(username + " has won!");
            } else {
                alert("Computer has won!");
            }

            return true; // Return true to indicate that a win has occurred.
        }
    }

    return false; // Return false if no win has occurred.
}

// Event listener that runs when the document is ready.
document.addEventListener("DOMContentLoaded", function() {
    // Get all cells in the game board.
    const cells = document.querySelectorAll('.cell');

    // Add a click event listener to each cell that does not contain an image.
    cells.forEach(cell => {
        if (!cell.querySelector('img')) {
            cell.addEventListener('click', handleCellClick);
        }
    });
});

// Function to send the game state to the server.
function post_to_server(endpoint) {
    // Get the current HTML content (game state).
    const payload = document.getElementById("html").innerHTML;

    // Construct the full API URL with the specified endpoint.
    let newApiUrl = apiUrl + "/" + endpoint;

    // Create a new XMLHttpRequest object to send data to the server.
    const request = new XMLHttpRequest();
    request.open("POST", newApiUrl);
    request.setRequestHeader("Content-Type", "text/html"); // Set content type.
    request.setRequestHeader("Accept", "application/json"); // Expect a JSON response.

    // Define the behavior when the response is received.
    request.onload = () => {
        if (request.status !== 200) {
            console.error("Something went wrong when contacting the server");
            return;
        }

        // Parse the JSON response from the server.
        const jsonResponse = JSON.parse(request.responseText);
        let tempDiv = document.createElement('div');
        tempDiv.innerHTML = jsonResponse.message; // Insert the server response into the temporary div.

        // Extract the new table element from the server response.
        let newTable = tempDiv.querySelector('.game-container table');

        // Check if the new table element exists.
        if (!newTable) {
            console.error("Failed to find the new table in the server response");
            return;
        }

        // Get the game board container and the existing board element.
        let container = document.querySelector('.game-container');
        let existingTable = document.getElementById('board');

        // Check if the existing table exists.
        if (!existingTable) {
            console.error("Failed to find the existing table with id 'board'");
            return;
        }

        // Replace the existing table with the new table from the server response.
        container.replaceChild(newTable, existingTable);

        // Reattach event listeners to cells in the new table.
        const cells = document.querySelectorAll('.cell');
        cells.forEach(cell => {
            if (!cell.querySelector('img')) {
                cell.addEventListener('click', handleCellClick);
            }
        });

        // Check if there is a winner after updating the board.
        checkWin().then(boolValue => {
            if (boolValue) {
                const cells = document.getElementById('board').querySelectorAll('.cell');
                cells.forEach(cell => {
                    if (!cell.querySelector('img')) {
                        cell.removeEventListener('click', handleCellClick);
                    }
                });
            }
        });
    };
    // Send the payload (current HTML) to the server.
    request.send(payload);
}

// Event handler for when the window is about to be closed.
window.onbeforeunload = function(event)
{
    ws.close(); // Close the WebSocket connection if any.
}

// Function to reset the game (refreshes the page).
function reset() {
    location.reload(); // Reload the page to reset the game.
}
