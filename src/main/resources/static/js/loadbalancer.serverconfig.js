// noinspection JSUnresolvedReference

$(document).ready(function () {
    $(".endpoint-delete-button").click(function () {
        let uuid = $(this).val();
        $.confirm({
            title: "Really delete?",
            content: `Do you really want to delete the configuration ${uuid}?`,
            type: 'purple',
            buttons: {
                confirm: function () {
                    console.log(`Deleting for ${uuid}`);
                    $.ajax({
                        type: "DELETE",
                        url: `/api/endpoints/${uuid}`,
                        success: function () {
                            console.log("Deleted via AJAX");
                            location.reload();
                        }
                    })
                },
                cancel: function () {
                }
            }
        })
    });

    $("#addForm").validate({
        submitHandler: function () {
            let newUrl = $("#inputUrl").val();
            let newName = $("#inputName").val();
            $.ajax({
                type: "POST",
                url: "/api/endpoints",
                data: JSON.stringify({
                    name: newName,
                    url: newUrl
                }),
                success: function (created) {
                    console.log(created);
                    $.confirm({
                        title: "Success",
                        content: `Created new endpoint with URL ${created.url} and ID ${created.id}`,
                        type: 'green',
                        buttons: {
                            ok: function () {
                                location.reload();
                            }
                        }
                    })
                },
                error: function(xhr, textStatus) {
                    let json = JSON.parse(xhr.responseText);
                    let formattedError = `Error '${textStatus}' creating new endpoint: \n${JSON.stringify(json, null, 2)}`
                    $.confirm({
                        title: "Error",
                        content: formattedError,
                        type: 'red',
                        buttons: {
                            ok: function () {
                                location.reload();
                            }
                        }
                    });
                },
                contentType: "application/json"
            });
        }
    });

    $("#inputUrl").on("change", function () {
        let newValue = $(this).val()
        let validator = $("#addForm").validate()
        let urlIsValid = validator.element("#inputUrl")
        if (urlIsValid) {
            $("#newItemMetadata").text("Validating...");
            // noinspection JSUnresolvedReference
            $.ajax({
                type: "POST",
                url: "/api/endpoints/validate",
                data: JSON.stringify({
                    url: newValue
                }),
                success: function (metadata) {
                    let elements = [metadata.fhirVersion, metadata.softwareName, metadata.softwareVersion]
                    let formatted = elements.join(", ")
                    console.log(formatted)
                    $("#newItemMetadata").text(formatted);
                },
                contentType: "application/json"
            })
        } else {
            console.log(`The value ${newValue} isn't valid`)
        }
    });


});



