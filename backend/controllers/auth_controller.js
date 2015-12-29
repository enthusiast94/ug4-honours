/**
 * Created by manas on 16-09-2015.
 */

var express = require("express");
var router = express.Router();
var User = require("../models/user");
var authTokenService = require("../services/auth_token");
var helpers = require("../utils/helpers");
var config = require("../config");
var userControllerHelpers = require("./user_controller").helpers;


router.post("/authenticate", function (req, res) {
    var email = req.body.email ? req.body.email.trim() : req.body.email;
    var password = req.body.password ? req.body.password.trim() : req.body.password;

    if (!email || !password) return res.sendError(401, "Email and password are required.");

    User.findOneByEmail(email, function (err, user) {
        if (err) return res.sendError(500, err);

        if (!user) return res.sendError(401, "Incorrect email or password.");

        user.comparePassword(password, function (err, match) {
            if (err) return res.sendError(500, err);

            if (!match) return res.sendError(401, "Incorrect email or password.");

            // issue token using user's id as the payload
            authTokenService.issueToken(user.id, function (err, token) {
                if (err) return res.sendError(err.message);

                return res.sendOk({token: token});
            });
        });
    });
});


router.post("/authenticate/google", function (req, res) {
    var idToken = req.body.id_token;

    if (!idToken) return res.sendError(400, "ID token (id_token) is required");

    // Validate token ID using Google's tokeninfo endpoint. If status code is 200, then check if the valid token is
    // intended for the correct server client id.
    helpers.getJson("https://www.googleapis.com/oauth2/v3/tokeninfo?id_token=" + idToken, function (statusCode, json) {
        if (statusCode != 200) return res.sendError(400, "Invalid ID token");

        if (json.aud != config.google_server_client_id) return res.sendError(400, "Invalid ID token");

        createOrAuthenticateThirdPartyUser(json.email, json.name, function (err, token) {
            if (err) return res.sendError(err.statusCode, err.message);

            return res.sendOk({token: token});
        });
    });
});


router.post("/authenticate/facebook", function (req, res) {
    var accessToken = req.body.access_token;

    if (!accessToken) return res.sendError(400, "Access token (access_token) is required");

    // Validate token using Facebook's debug_token endpoint. If status code is 200, then check if the valid token was
    // actually generated by our app.
    helpers.getJson("https://graph.facebook.com/debug_token?access_token=" + accessToken + "&input_token=" +
        accessToken, function (statusCode, json) {
        if (statusCode != 200) return res.sendError(400, "Invalid access token");

        if (json.data.app_id != config.facebook_android_client_id) {
            return res.sendError(400, "Invalid access token");
        }

        // get required user info
        helpers.getJson("https://graph.facebook.com/me/?access_token=" + accessToken + "&fields=name,email",
            function (statusCode2, json2) {
                if (statusCode2 != 200) return res.sendError(500, "Could not retrieve user info via Graph API");

                createOrAuthenticateThirdPartyUser(json2.email, json2.name, function (err, token) {
                    if (err) return res.sendError(err.statusCode, err.message);

                    return res.sendOk({token: token});
                });
            });
    });
});


/**
 * Helpers
 */

function createOrAuthenticateThirdPartyUser(email, name, callback) {
    User.findOneByEmail(email, function (err, user) {
        if (err) return callback(helpers.createErrorMessage(500, err.message));

        // If user does not already exist, create one. Else, simply return authentication token.
        if (!user) {
            userControllerHelpers.createUser(name, email, config.default_user_password, function (err, user) {
                if (err) return callback(err);

                authTokenService.issueToken(user.id, function (err, token) {
                    if (err) return callback(helpers.createErrorMessage(500, err.message));

                    return callback(null, token);
                });
            });
        } else {
            user.comparePassword(config.default_user_password, function (err, match) {
                if (err) return callback(helpers.createErrorMessage(500, err.message));

                // if password is not the same as default password, it implies that user was created manually and
                // not via the endpoint authenticate/<provided>.
                if (!match) return callback(helpers.createErrorMessage(
                    401, "Another user with the same email address already exists."));

                authTokenService.issueToken(user.id, function (err, token) {
                    if (err) return callback(helpers.createErrorMessage(500, err.message));

                    return callback(null, token);
                });
            });
        }
    });
}

module.exports = router;