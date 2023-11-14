function sendAjaxRequest(id, route, data, successMessage, errorMessage, successDialogType) {
    return $.ajax({
        type: "POST",
        url: `/api/endpoints/${id}/${route}`,
        data: JSON.stringify(data),
        contentType: "application/json",
        success: function () {
            $.confirm({
                title: "Success", content: successMessage, type: successDialogType, buttons: {
                    ok: function () {
                        location.reload();
                    }
                }
            })
        },
        error: function (xhr) {
            $.confirm({
                title: "Error", content: `${errorMessage}: ${xhr.responseText}`, type: 'red', buttons: {
                    ok: function () {
                        location.reload();
                    }
                }
            })
        },
    });
}

function setServerRole(id, role, dialogType) {
    let data = {
        role: role
    }
    sendAjaxRequest(id, "role", data, `Successfully made server ${id} ${role}`, `Error making server ${id} ${role}`, dialogType)
}

function handleReadonly(event, id) {
    let setValue = event.target.checked;
    let successMessages = {
        true: `Successfully made server ${id} READONLY.`, false: `Successfully made server ${id} read-write.`
    }
    let errorMessages = {
        true: `Error making server ${id} readonly`, false: `Error making server ${id} read-write`
    }
    let dataToSend = {
        readonly: setValue
    }
    sendAjaxRequest(id, "readonly", dataToSend, successMessages[setValue], errorMessages[setValue], "green")
}

function handleOnOff(event, id) {
    let setValue = event.target.checked;
    let successMessages = {
        true: `Successfully ENABLED server ${id}.`, false: `Successfully DISABLED server ${id}.`
    }
    let errorMessages = {
        true: `Error ENABLING server ${id}`, false: `Error DISABLING server ${id}`
    }
    let dataToSend = {
        enabled: setValue
    }
    sendAjaxRequest(id, "enabled", dataToSend, successMessages[setValue], errorMessages[setValue], "green")
}

$(document).ready(function () {
    $(".btn.make-unassigned").on("click", function () {
        let id = $(this).val();
        setServerRole(id, "UNASSIGNED", "dark");
    });

    $(".btn.make-blue").on("click", function () {
        let id = $(this).val();
        setServerRole(id, "BLUE", "blue");
    });

    $(".btn.make-green").on("click", function () {
        let id = $(this).val();
        setServerRole(id, "GREEN", "green");
    });

    $(".check-ro").on("change", function (event) {
        let id = $(this).val();
        handleReadonly(event, id);
    });

    $(".check-on-off").on("change", function (event) {
        let id = $(this).val();
        handleOnOff(event, id);
    });
});