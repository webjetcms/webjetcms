<%@page import="sk.iway.iwcm.system.context.ContextFilter"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.users.*" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%
request.setAttribute("cmpName", "users.passkey");
%>
<%@ include file="layout_top_dialog.jsp" %>

<script type="text/javascript">
    resizeDialog(900, 600);
</script>

<style>
#btnCancel {
    display: none;
}
.passkey-list {
    width: 100%;
    border-collapse: collapse;
}
.passkey-list th, .passkey-list td {
    padding: 8px 12px;
    text-align: left;
    border-bottom: 1px solid #ddd;
}
.passkey-list th {
    background-color: #f5f5f5;
    font-weight: bold;
}
.passkey-list .btn-delete {
    cursor: pointer;
    color: #dc3545;
    border: none;
    background: none;
    font-size: 14px;
}
.passkey-list .btn-delete:hover {
    text-decoration: underline;
}
.passkey-empty {
    padding: 20px;
    text-align: center;
    color: #666;
}
#passkey-register-section {
    margin-top: 15px;
    padding: 15px;
    background: #f8f9fa;
    border-radius: 4px;
}
#passkey-register-section label {
    font-weight: bold;
    display: block;
    margin-bottom: 5px;
}
#passkey-register-section input[type="text"] {
    width: 300px;
    margin-right: 10px;
}
</style>

<div class="padding10" style="min-width: 800px; min-height: 400px;">

    <p><iwcm:text key="passkey.manage.instructions"/></p>

    <div id="passkey-list-container">
        <div class="passkey-empty"><iwcm:text key="passkey.manage.loading"/></div>
    </div>

    <div id="passkey-register-section">
        <label for="passkey-label"><iwcm:text key="passkey.manage.newLabel"/></label>
        <div style="display: flex; align-items: center;">
            <input type="text" id="passkey-label" class="form-control" style="width: 300px; margin-right: 10px;"
                   placeholder="<iwcm:text key="passkey.manage.labelPlaceholder"/>" />
            <button type="button" class="btn btn-primary" onclick="registerPasskey()">
                <i class="ti ti-fingerprint"></i> <iwcm:text key="passkey.manage.register"/>
            </button>
        </div>
    </div>

</div>

<script type="text/javascript">

function loadPasskeys() {
    $.ajax({
        url: "/admin/rest/passkey/list",
        method: "GET",
        dataType: "json"
    }).done(function(passkeys) {
        if (passkeys.length === 0) {
            $("#passkey-list-container").html('<div class="passkey-empty"><iwcm:text key="passkey.manage.noPasskeys"/></div>');
            return;
        }
        var html = '<table class="passkey-list">';
        html += '<tr><th><iwcm:text key="passkey.manage.colLabel"/></th>';
        html += '<th><iwcm:text key="passkey.manage.colCreated"/></th>';
        html += '<th><iwcm:text key="passkey.manage.colLastUsed"/></th>';
        html += '<th><iwcm:text key="passkey.manage.colTransports"/></th>';
        html += '<th></th></tr>';
        for (var i = 0; i < passkeys.length; i++) {
            var p = passkeys[i];
            html += '<tr>';
            html += '<td>' + escapeHtml(p.label || 'PassKey ' + (i + 1)) + '</td>';
            html += '<td>' + formatDate(p.created) + '</td>';
            html += '<td>' + formatDate(p.lastUsed) + '</td>';
            html += '<td>' + escapeHtml(p.transports || '') + '</td>';
            html += '<td><button class="btn-delete" onclick="deletePasskey(\'' + escapeHtml(p.credentialId) + '\')"><i class="ti ti-trash"></i></button></td>';
            html += '</tr>';
        }
        html += '</table>';
        $("#passkey-list-container").html(html);
    }).fail(function() {
        $("#passkey-list-container").html('<div class="passkey-empty" style="color: red;"><iwcm:text key="passkey.manage.loadError"/></div>');
    });
}

function deletePasskey(credentialId) {
    if (!confirm('<iwcm:text key="passkey.manage.confirmDelete"/>')) return;

    $.ajax({
        url: "/admin/rest/passkey/" + encodeURIComponent(credentialId),
        method: "DELETE"
    }).done(function() {
        loadPasskeys();
    }).fail(function() {
        alert('<iwcm:text key="passkey.manage.deleteError"/>');
    });
}

function registerPasskey() {
    var label = $("#passkey-label").val() || "PassKey";

    // Step 1: Get registration options from Spring Security
    fetch('/webauthn/register/options', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({})
    })
    .then(function(response) {
        if (!response.ok) throw new Error('Failed to get registration options');
        return response.json();
    })
    .then(function(options) {
        // Convert base64url to buffers
        options.user.id = base64urlToBuffer(options.user.id);
        options.challenge = base64urlToBuffer(options.challenge);
        if (options.excludeCredentials) {
            options.excludeCredentials = options.excludeCredentials.map(function(cred) {
                cred.id = base64urlToBuffer(cred.id);
                return cred;
            });
        }
        return navigator.credentials.create({ publicKey: options });
    })
    .then(function(credential) {
        // Send the attestation to the server
        var body = {
            publicKey: {
                credential: {
                    id: credential.id,
                    rawId: bufferToBase64url(credential.rawId),
                    response: {
                        attestationObject: bufferToBase64url(credential.response.attestationObject),
                        clientDataJSON: bufferToBase64url(credential.response.clientDataJSON),
                        transports: credential.response.getTransports ? credential.response.getTransports() : []
                    },
                    type: credential.type,
                    clientExtensionResults: credential.getClientExtensionResults(),
                    authenticatorAttachment: credential.authenticatorAttachment
                },
                label: label
            }
        };
        return fetch('/webauthn/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
    })
    .then(function(response) {
        if (!response.ok) throw new Error('Registration failed');
        $("#passkey-label").val("");
        loadPasskeys();
        alert('<iwcm:text key="passkey.manage.registerSuccess"/>');
    })
    .catch(function(error) {
        console.error('PassKey registration error:', error);
        alert('<iwcm:text key="passkey.manage.registerError"/>' + ' ' + error.message);
    });
}

function base64urlToBuffer(base64url) {
    var base64 = base64url.replace(/-/g, '+').replace(/_/g, '/');
    var padding = '='.repeat((4 - base64.length % 4) % 4);
    var binary = atob(base64 + padding);
    var bytes = new Uint8Array(binary.length);
    for (var i = 0; i < binary.length; i++) {
        bytes[i] = binary.charCodeAt(i);
    }
    return bytes.buffer;
}

function bufferToBase64url(buffer) {
    var bytes = new Uint8Array(buffer);
    var binary = '';
    for (var i = 0; i < bytes.length; i++) {
        binary += String.fromCharCode(bytes[i]);
    }
    return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

function escapeHtml(text) {
    if (!text) return '';
    var div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function formatDate(dateStr) {
    if (!dateStr) return '-';
    try {
        var d = new Date(dateStr);
        return d.toLocaleDateString() + ' ' + d.toLocaleTimeString();
    } catch(e) {
        return dateStr;
    }
}

function Ok() {
    window.close();
}

$(document).ready(function() {
    loadPasskeys();
});
</script>

<%@ include file="layout_bottom_dialog.jsp" %>
