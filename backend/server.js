/**
 * Created by manas on 15-09-2015.
 */

var express = require('express');
var morgan = require('morgan');


/**
 * Configuration
 */

// define express app
var app = express();

// use morgan to log HTTP requests to the console
app.use(morgan('dev'));

// define port for the HTTP server (give priority to PORT environment variable)
var port = process.env.PORT || 4000;


/**
 * API routes
 */

// GET test route
var router = express.Router();

router.get("/", function (req, res) {
    res.send("Welcome to our API");
});

// register routes
// all routes will be prefixed with /api
app.use("/api", router);


/**
 * Start server
 */

app.listen(port);