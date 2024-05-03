let side; // Variable to store which side the player is ("x", "o", or "spectator").
let playerCount = 0; // Variable to store the number of players in the game.

// Function to pause the execution for a specified number of milliseconds.
function sleep(ms = 0) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

// Event handler function for cell click events on the game board.
function handleCellClick(event) {
    // If the player is a spectator, do not handle cell clicks.
    if (side === "spectator") {
        return;
    } else {
        const clickedCell = event.target; // Get the clicked cell element.
        // Create an img element to represent the player's move (x or o).
        let image = document.createElement("img");
        image.style.width = "100px";
        image.style.height = "100px";
        image.src = "images/" + side + ".png"; // Set the image source based on the player's side.
        clickedCell.appendChild(image); // Append the image to the clicked cell.

        // Disable click events on all cells that do not contain an image.
        const cells = document.getElementById('board').querySelectorAll('.cell');
        cells.forEach(cell => {
            if (!cell.querySelector('img')) {
                cell.removeEventListener('click', handleCellClick);
            }
        });

        // Send the updated board state to the server.
        let payload = document.getElementById("html").innerHTML;
        let request = {"type": "board", "msg": payload};
        ws.send(JSON.stringify(request));
    }
}

// Function to set up event listeners and other initialization tasks when the document is loaded.
document.addEventListener("DOMContentLoaded", function() {
    const cells = document.querySelectorAll('.cell');
    // Attach click event listeners to cells without images.
    cells.forEach(cell => {
        if (!cell.querySelector('img')) {
            cell.addEventListener('click', handleCellClick);
        }
    });
    // Add an event listener to handle Enter key presses in the chat input.
    document.getElementById('messageInput').addEventListener('keydown', function(event) {
        if (event.key === 'Enter') {
            sendMessage();
        }
    });
});

let ws; // WebSocket connection variable.
window.onload = function() {
    // Get the room code and username from URL parameters.
    let current_url = new URL(window.location.href);
    let code = current_url.searchParams.get("room");
    let username = current_url.searchParams.get("username");

    // If both code and username are provided, enter the specified room.
    if (code && username) {
        enterRoom(code, username);
    } else {
        newRoom(username); // Otherwise, create a new room.
    }

    // Log a waiting message to the chat log.
    document.getElementById("log").value += "[" + timestamp() + "] Waiting for player.\n";
};

// Function to create a new room on the server.
function newRoom(username) {
    let callURL = "http://localhost:8080/w24-csci2020u-assignment03-hossinzehi-frank-weathers-sharma-1.0-SNAPSHOT/chat-servlet";
    fetch(callURL, {
        method: 'GET',
        headers: {
            'Accept': 'text/plain',
        },
    })
        .then(response => response.text())
        .then(response => enterRoom(response, username));
}

// Function to enter a specified room.
function enterRoom(code, username) {
    ws = new WebSocket("ws://localhost:8080/w24-csci2020u-assignment03-hossinzehi-frank-weathers-sharma-1.0-SNAPSHOT/ws/" + code);
    // Update the room code display in the HTML.
    document.querySelector("#roomCode tbody").innerHTML = "<tr><td>" + code + "</td></tr>";
    document.getElementById("log").value += "[" + timestamp() + "] Player Found!\n";
    // WebSocket event handlers.
    ws.onopen = function (event) {
        if (username) {
            let request = {"type": "chat", "msg": username};
            ws.send(JSON.stringify(request));
        }
        refreshRooms(); // Refresh the list of chat rooms.
        waitingForPlayer(); // Wait for another player to join.
    }
    ws.onmessage = function(event) {
        let message = JSON.parse(event.data);
        // Handle different types of messages from the server.
        if (message.type === "chatRooms") {
            // Update the chat room display.
            document.getElementById("chatRoom").value = "";
            document.getElementById("chatRoom").value += message.message;
        } else if (message.type === "board") {
            // Update the game board based on the server response.
            const jsonResponse = JSON.parse(event.data);
            let tempDiv = document.createElement('div');
            tempDiv.innerHTML = jsonResponse.message;
            let newTable = tempDiv.querySelector('.game-container table');

            if (!newTable) {
                console.error("Failed to find the new table in the server response");
                return;
            }

            let container = document.querySelector('.game-container');
            let existingTable = document.getElementById('board');

            if (!existingTable) {
                console.error("Failed to find the existing table with id 'board'");
                return;
            }

            container.replaceChild(newTable, existingTable);
            // Reattach event listeners to the cells of the new table.
            if (side !== "spectator") {
                const cells = document.querySelectorAll('.cell');
                cells.forEach(cell => {
                    if (!cell.querySelector('img')) {
                        cell.addEventListener('click', handleCellClick);
                    }
                });
            }
        } else if (message.type === "side") {
            side = message.message; // Update the player's side.
        } else if (message.type === "playerCount") {
            playerCount = message.message;
            // Enable or disable cell click events based on the number of players.
            if (playerCount < 2) {
                const cells = document.getElementById('board').querySelectorAll('.cell');
                cells.forEach(cell => {
                    if (!cell.querySelector('img')) {
                        cell.removeEventListener('click', handleCellClick);
                    }
                });
            } else {
                document.getElementById("log").value += "[" + timestamp() + "] Player Found!\n";
                if (side === "x") {
                    const cells = document.getElementById('board').querySelectorAll('.cell');
                    cells.forEach(cell => {
                        if (!cell.querySelector('img')) {
                            cell.addEventListener('click', handleCellClick);
                        }
                    });
                } else if (side === "o") {
                    const cells = document.getElementById('board').querySelectorAll('.cell');
                    cells.forEach(cell => {
                        if (!cell.querySelector('img')) {
                            cell.removeEventListener('click', handleCellClick);
                        }
                    });
                } else {
                    side = "spectator";
                }
            }
        } else if (message.type === "chat") {
            // Log chat messages.
            document.getElementById("log").value += "[" + timestamp() + "] " + message.message + "\n";
        } else if (message.type === "win") {
            // Handle the end of the game based on the win condition.
            let winCondition = message.message;
            if (winCondition === "x") {
                alert("X has won the game!");
                disableCellClicks();
            } else if (winCondition === "o") {
                alert("O has won the game!");
                disableCellClicks();
            } else {
                alert("There was a draw!");
                disableCellClicks();
            }
        }
    }
}

// Function to disable cell click events.
function disableCellClicks() {
    const cells = document.getElementById('board').querySelectorAll('.cell');
    cells.forEach(cell => {
        if (!cell.querySelector('img')) {
            cell.removeEventListener('click', handleCellClick);
        }
    });
}

// Function to refresh the list of chat rooms.
function refreshRooms() {
    let request = {"type": "chatRooms", "msg": ""};
    ws.send(JSON.stringify(request));
}

// Function to get the current timestamp for logging.
function timestamp() {
    let d = new Date(), minutes = d.getMinutes();
    if (minutes < 10) minutes = '0' + minutes;
    return d.getHours() + ':' + minutes;
}

// Event handler for before the window unloads.
window.onbeforeunload = function(event) {
    ws.close(); // Close the WebSocket connection.
}

// Function to send chat messages.
function sendMessage() {
    let message_data = document.getElementById("messageInput");
    let request = {"type": "chat", "msg": message_data.value};
    ws.send(JSON.stringify(request));
    message_data.value = "";
}

// Function to wait for another player to join the game.
async function waitingForPlayer() {
    while (playerCount < 2) {
        let request = {"type": "playerCount", "msg": ""};
        ws.send(JSON.stringify(request));
        console.log(playerCount);
        await sleep(200);
    }
}
