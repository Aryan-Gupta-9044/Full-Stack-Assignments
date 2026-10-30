// 1. Select the button and paragraph elements
const textBtn = document.getElementById("text-btn");
const message = document.getElementById("message");

// 2. Listen for a click event on the button
textBtn.addEventListener("click", () => {
    // 3. Update the text content
    message.textContent = "The text has been successfully changed!";
});
