function loadStudents() {

    fetch("/students")
        .then(response => response.json())
        .then(students => {

            let table = document.getElementById("studentTable");

            table.innerHTML = "";

            students.forEach(student => {

                table.innerHTML += `
                    <tr>
                        <td>${student.id}</td>
                        <td>${student.name}</td>
                        <td>${student.email}</td>
                        <td>
                            <button onclick="deleteStudent(${student.id})">
                                Delete
                            </button>
                        </td>
                    </tr>
                `;

            });

        });
}


function addStudent() {

    let name =
        document.getElementById("name").value;

    let email =
        document.getElementById("email").value;

    fetch("/students", {

        method: "POST",

        headers: {
            "Content-Type":
                "application/x-www-form-urlencoded"
        },

        body: `name=${name}&email=${email}`

    })
    .then(response => response.text())
    .then(message => {

        alert(message);

        document.getElementById("name").value = "";

        document.getElementById("email").value = "";

        loadStudents();

    });
}


function deleteStudent(id) {

    fetch(`/students?id=${id}`, {

        method: "DELETE"

    })
    .then(response => response.text())
    .then(message => {

        alert(message);

        loadStudents();

    });
}


loadStudents();