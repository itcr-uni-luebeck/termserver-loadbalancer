// noinspection JSUnresolvedReference

$(document).ready(function () {
    $(".delete-button").click(function () {
        let uuid = $(this).val();
        $.confirm({
            title: "Really delete?",
            content: `Do you really want to delete the configuration ${uuid}?`,
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
                    alert(`Created new endpoint with URL ${created.url} and ID ${created.uuid}`);
                    location.reload()
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



