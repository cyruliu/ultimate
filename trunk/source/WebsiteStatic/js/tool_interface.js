let _EDITOR;


function load_tool_interface_template() {
  let content = $('#content');
  content.removeClass('container-fluid');
  const tool_interface_template = Handlebars.compile($("#tool-interface-template").html());
  content.append(tool_interface_template(_CONFIG));
}


function init_editor() {
  _EDITOR = ace.edit("editor");
  _EDITOR.renderer.setHScrollBarAlwaysVisible(false);
  _EDITOR.setTheme("ace/theme/eclipse");
  _EDITOR.getSession().setMode('ace/mode/c_cpp'); //equv to: changeMode('c_cpp');
  _EDITOR.renderer.setShowGutter(true);
  _EDITOR.setShowPrintMargin(true);
  _EDITOR.setDisplayIndentGuides(true);
  _EDITOR.setHighlightSelectedWord(true);
  _EDITOR.setPrintMarginColumn(80);

  _EDITOR.session.setValue(_CONFIG.editor.init_code);
  _EDITOR.session.setTabSize(4);
  _EDITOR.session.setUseWrapMode(true);
}

function init_interface_controls () {
  $('.language-selection').on({
    click: function () {
      choose_language($( this ).data().language);
    }
  });
  $('#execute-interface').on({
    click: function () {
      const settings = get_execute_settings();
      const result = get_ultimate_results(settings);
    }
  });
}


/**
 * Initiate a ultimate run and process the result.
 * @param settings
 */
function get_ultimate_results(settings) {
  $.post(_CONFIG.backend.web_bridge_url, settings, function (response) {
    console.log(response);
  });
}


/**
 * Get the current settings Dict to be used as a new job for ultimate.
 * @returns {{user_settings: {}, code: string, action: string, toolchain: {task_id: *, id: *}}}
 */
function get_execute_settings() {
  let settings = {
    action: 'execute',
    code: 'here is the code...',
    toolchain: {
      id: _CONFIG.context.current_worker.id,
      task_id: _CONFIG.context.current_worker.task_id,
    },
    user_settings: {}
  };

  _CONFIG.context.current_worker.frontend_settings.forEach(function (setting) {
    settings.user_settings[setting.id] = $('#' + setting.id).is(':checked')
  });

  return settings;
}


/**
 * Process a language selection.
 * @param language
 */
function choose_language(language) {
  console.log('Set current language to ' + language);
  $('#language_select_dropdown').html('Language: ' + language);

  _CONFIG.context.tool.workers.forEach(function (worker) {
    if (worker.language === language) {
      _CONFIG.context.current_worker = worker;
    }
  });

  set_available_code_samples(language);
  set_available_frontend_settings(language);
}


/**
 * Set available code samples to the dropdown.
 * This is adding each example with current language match and current worker id in in the example.assoc_workers list.
 * @param language
 */
function set_available_code_samples(language) {
  let samples_menu = $('#code_sample_dropdown_menu');
  let example_entries = '';

  _CONFIG.code_examples[language].forEach(function (example) {
    if (example.assoc_workers.includes(_CONFIG.context.current_worker.id)) {
      example_entries += '<a class="dropdown-item sample-selection" href="#" data-source="' +  example.source + '">' + example.name + '</a>';
    }
  });

  samples_menu.html(example_entries);
  $('.sample-selection').on({
    click: function () {
      load_sample($( this ).data().source);
    }
  });
}


/**
 * Load an available sample into the editor.
 * @param source
 */
function load_sample(source) {
  $.get('config/code_examples/' + source, function (data) {
    _EDITOR.session.setValue(data);
  })
}



/**
 *
 */
function set_available_frontend_settings(language) {
  let settings_menu = $('#settings_dropdown_menu');
  let settings_entries = '';

  _CONFIG.context.current_worker.frontend_settings.forEach(function (setting) {
    if (setting.type === "bool") {
      settings_entries += '<div class="form-check">' +
        '<input type="checkbox" class="form-check-input" id="' + setting.id + '" ' + (setting.default ? "checked" : "") + '>' +
        '<label class="form-check-label" for="' + setting.id + '">' + setting.name + '</label>' +
        '</div>'
    }
  });

  settings_menu.html(settings_entries);
  $('.form-check').on('click', function(e) {
    e.stopPropagation();
  });
}
