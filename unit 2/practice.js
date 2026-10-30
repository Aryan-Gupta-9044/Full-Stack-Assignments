
const textElement = document.getElementById('text-element');
const actionButton = document.getElementById('action-btn');

actionButton.addEventListener('click', () => {
   
    textElement.textContent = "New Text";
});

const bgButton = document.getElementById('bg-btn');

bgButton.addEventListener('click', () => {
    document.body.style.backgroundColor = "lightblue";
});
const clockElement = document.getElementById('clock');
function updateClock() {
    const now = new Date();
    
    const currentDate = now.toLocaleDateString(); // Formats as MM/DD/YYYY or DD/MM/YYYY
    const currentTime = now.toLocaleTimeString(); // Formats as HH:MM:SS AM/PM

    // Combine them and update the element
    clockElement.textContent = `Date: ${currentDate} | Time: ${currentTime}`;
}
updateClock();
setInterval(updateClock, 1000);