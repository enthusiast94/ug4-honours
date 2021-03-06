/**
 * Created by manas on 15-09-2015.
 */

var express = require("express");
var morgan = require("morgan");
var mongoose = require("mongoose");
var bodyParser = require("body-parser");
var config = require("./config");
var responsesMiddleware = require("./middleware/responses");
var userRouter = require("./controllers/user_controller").router;
var authRouter = require("./controllers/auth_controller");
var stopRouter = require("./controllers/stop_controller");
var serviceRouter = require("./controllers/service_controller");
var directionsRouter = require("./controllers/directions_controller").router;
var waitOrWalkRouter = require("./controllers/wait_or_walk_controller");
var activityRouter = require("./controllers/activity_controller");
var errorHandlerMiddleware = require("./middleware/404_error_handler");


// define express app
var app = express();

var ApiExecutionModeEnum = Object.freeze({
    DEV: "dev",
    TEST: "test"
});

function configure(apiExecutionMode) {
    if (apiExecutionMode !== ApiExecutionModeEnum.DEV && apiExecutionMode !== ApiExecutionModeEnum.TEST) {
        throw new Error("Invalid api execution mode. Allowed values are: " + Object.keys(ApiExecutionModeEnum));
    }

    // use morgan (only in dev execution mode) to log HTTP requests to the console
    if (apiExecutionMode === ApiExecutionModeEnum.DEV) {
        app.use(morgan("dev"));
    }

    // configure body parser, which will let us get data from a POST request
    app.use(bodyParser.urlencoded({extended: true}));
    app.use(bodyParser.json());

    // connect to db depending on the provided execution mode
    mongoose.connect(apiExecutionMode === ApiExecutionModeEnum.DEV ? config.database.dev : config.database.test);

    // add custom methods for sending API responses to res object
    app.use(responsesMiddleware);

    // Register API routes. All routes will be prefixed with /api.
    app.use("/api", userRouter);
    app.use("/api", authRouter);
    app.use("/api", stopRouter);
    app.use("/api", serviceRouter);
    app.use("/api", directionsRouter);
    app.use("/api", waitOrWalkRouter);
    app.use("/api", activityRouter);

    // add 404 error handling middleware in order to send custom message when no route matches client's request
    app.use(errorHandlerMiddleware);
}

function start(apiExecutionMode) {
    configure(apiExecutionMode);
    app.listen(config.apiServerPort);
}

module.exports = {
    app: app,
    start: start,
    ApiExecutionModeEnum: ApiExecutionModeEnum
};
