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
 * Returns the current workers language or "undefined" if none set.
 */
function get_current_language() {
  let result = "undefined";
  if ("current_worker" in _CONFIG.context) {
    result = _CONFIG.context.current_worker.language;
  }
  return result
}
