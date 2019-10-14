/**
 * Parse the landing page from config.js and add the result to the content container.
 */
function render_landing_page() {
  const content = $('#content');
  content.addClass('p-5');
  const landing_page_template = Handlebars.compile(
    $("#landing-page-template").html()
  );
  content.append(landing_page_template(_CONFIG));
}


/**
 * Fetch and parse the tool info page.
 * @param tool_id
 */
function render_tool_page(tool_id) {
  const content = $('#content');
  content.addClass('p-5');
  $.get("./config/tool_pages/" + tool_id + ".hbs", function (data) {
    const tool_page_template = Handlebars.compile(data);
    content.append(tool_page_template(_CONFIG));
  });
}


/**
 * Load the interactive tool interface.
 * @param tool_id
 */
function load_tool_interface(tool_id) {
  load_tool_interface_template();
  init_editor();
  init_interface_controls();
  refresh_navbar();
}


/**
 * Render the header/navigation-bar.
 */
function render_navbar() {
  const navbar_template = Handlebars.compile($("#navbar-template").html());
  $('#navbar_content').append(navbar_template(_CONFIG));
}


/**
 * Inject current context to _CONFIG.context s.t:
 *
 * _CONFIG.context = {
 *     url: {
 *         ui: <URL ui param | home by default.>
 *         tool: <URL tool param>
 *     },
 *     tool: <CONFIG for tool with corresponding tool.id>
 * }
 */
function set_context() {
  const url_params = get_url_params();
  let tool = {};

  // Redirect non existing tools to home page.
  if (!tool_config_key_value_exists("id", url_params.tool)) {
    url_params.ui = "home";
  }

  // Set current tool if active.
  if (url_params.ui !== "home") {
    tool = Object.values(_CONFIG.tools).find(function (tool) {
      return tool.id === url_params.tool
    });
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
  render_navbar();

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
