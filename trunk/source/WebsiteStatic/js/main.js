Handlebars.registerHelper('ifCond', function (v1, operator, v2, options) {
    switch (operator) {
        case '==':
            return (v1 == v2) ? options.fn(this) : options.inverse(this);
        case '===':
            return (v1 === v2) ? options.fn(this) : options.inverse(this);
        case '!=':
            return (v1 != v2) ? options.fn(this) : options.inverse(this);
        case '!==':
            return (v1 !== v2) ? options.fn(this) : options.inverse(this);
        case '<':
            return (v1 < v2) ? options.fn(this) : options.inverse(this);
        case '<=':
            return (v1 <= v2) ? options.fn(this) : options.inverse(this);
        case '>':
            return (v1 > v2) ? options.fn(this) : options.inverse(this);
        case '>=':
            return (v1 >= v2) ? options.fn(this) : options.inverse(this);
        case '&&':
            return (v1 && v2) ? options.fn(this) : options.inverse(this);
        case '||':
            return (v1 || v2) ? options.fn(this) : options.inverse(this);
        default:
            return options.inverse(this);
    }
});


/**
 * Check if "key: value" exists in any tool config.
 * @param key
 * @param value
 * @returns {boolean}
 */
function tool_config_key_value_exists(key, value) {
    for (const tool_config of Object.values(_CONFIG.tools)) {
        if (key in tool_config) {
            if (tool_config[key] === value) {
                return true;
            }
        }
    }

    return false;
}


/**
 * Fetch window.location URL parameters
 * @returns {{ui: *, tool: *}}
 */
function get_url_params() {
    let url = new URL(window.location);

    return {
        "ui": url.searchParams.get("ui"),
        "tool": url.searchParams.get("tool")
    };
}


/**
 * Parse the landing page from config.js and add the result to the content container.
 */
function render_landing_page() {
    const landing_page_template = Handlebars.compile($("#landing-page-template").html());
    $('#content').append(landing_page_template(_CONFIG));
}


/**
 * Fetch and parse the tool info page.
 * @param tool_id
 */
function render_tool_page(tool_id) {
    $.get( "./templates/" + tool_id + ".hbs", function( data ) {
        const tool_page_template = Handlebars.compile(data);
        $('#content').append(tool_page_template(_CONFIG));
    });
}


function load_tool_interface(tool_id) {
    load_tool_interface_template();
    alignInterfaceContent();
    // showing all notices, including cookie-info when needed
    initInfo();
    init_tool_interface_control();
}


function render_header() {
    const header_template = Handlebars.compile($("#header-template").html());
    $('#header').append(header_template(_CONFIG));
}


function set_context() {
    const url_params = get_url_params();
    let tool = {};

    // Redirect non existing tools to home page.
    if (!tool_config_key_value_exists("id", url_params.tool)) {
        url_params.ui = "home";
    }

    // Set current tool if active.
    if (url_params.ui !== "home") {
        tool = Object.values(_CONFIG.tools).find(function(e) {e.id === url_params.tool} );
    }

    _CONFIG["context"] = {
        "url": url_params,
        "tool": tool
    }
}


/**
 * Parse URL parameters and load/initialize corresponding content.
 */
function bootstrap() {
    set_context();
    render_header();

    switch (_CONFIG.context.url.ui) {
        case "int":
            // load the interactive mode for the active tool.
            load_tool_interface(_CONFIG.context.tool.id)
            break;
        case "tool":
            // load the tool info page.
            render_tool_page(_CONFIG.context.tool.id);
            break;
        default:
            // load the landing page.
            render_landing_page();
    }
}


$(document).ready(function () {
    bootstrap();
});
