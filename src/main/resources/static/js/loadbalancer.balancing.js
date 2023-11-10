function setServerRole(id, role, dialogType) {
    $.ajax({
        type: "POST",
        url: `/api/endpoints/${id}/role`,
        data: JSON.stringify({
            "role": role
        }),
        success: function () {
            $.confirm({
                title: "Success",
                content: `Successfully made server ${id} ${role}`,
                type: dialogType,
                buttons: {
                    ok: function () {
                        location.reload();
                    }
                }
            })
        },
        error: function (xhr) {
            $.confirm({
                title: "Error",
                content: `Error making server ${id} ${role}: ${xhr.responseText}`,
                type: 'red',
                buttons: {
                    ok: function () {
                        location.reload();
                    }
                }
            })
        },
        contentType: "application/json"
    })
}

$(document).ready(function () {
    $(".btn.make-unassigned").on("click", function () {
        let id = $(this).val();
        setServerRole(id, "UNASSIGNED", "dark")
    });

    $(".btn.make-blue").on("click", function () {
        let id = $(this).val();
        setServerRole(id, "BLUE", "blue")
    });

    $(".btn.make-green").on("click", function () {
        let id = $(this).val();
        setServerRole(id, "GREEN", "green")
    });

    $(".check-ro").on("change", function(event) {
        console.log(`readonly changed to ${event.target.checked}`)
    });
});