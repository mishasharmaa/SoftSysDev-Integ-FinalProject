// Function to display a hidden element by setting its display style to "block".
function displayText() {
    let hidden_element = document.getElementById("hidden");
    hidden_element.style.display = "block"; // Change the display style of the hidden element to make it visible.
}

// Function to start the game with a specified room code and username.
function startWithCode() {
    // Get the room code and username from input elements.
    let code = document.getElementById("roomInput").value;
    let username = document.getElementById("usernameInput").value;

    // Define a regex pattern to validate the room code format (5 alphanumeric characters).
    const pattern = /[A-Z0-9]{5}/;
    let isRoom = pattern.test(code); // Test if the room code matches the pattern.

    // If the room code is invalid, show an alert and exit the function.
    if (!isRoom) {
        alert("Please enter a valid room code");
        return;
    }

    // If the username is empty, show an alert and exit the function.
    if (username === "") {
        alert("Please enter a valid username");
        return;
    }

    // Redirect the user to the game page with the room code and username as URL parameters.
    location.href = 'game.html?room=' + code + '&username=' + username;
}

// Function to start the game without specifying a room code (creates a new room).
function startNoCode() {
    // Get the username from the input element.
    let username = document.getElementById("usernameInput").value;

    // If the username is not empty, redirect the user to the game page with the username as a URL parameter.
    if (username !== "") {
        location.href = 'game.html?username=' + username;
    } else {
        // Otherwise, show an alert asking for a valid username.
        alert("Please enter a valid username");
    }
}

// Function to start a game against the computer.
function computer() {
    // Get the username from the input element.
    let username = document.getElementById("usernameInput").value;

    // If the username is empty, show an alert asking for a valid username and exit the function.
    if (username === "") {
        alert("Please enter a valid username");
        return;
    }

    // Redirect the user to the computer game page with the username as a URL parameter.
    location.href = 'computer.html?username=' + username;
}
