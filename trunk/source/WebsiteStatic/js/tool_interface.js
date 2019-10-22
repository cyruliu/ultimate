let _EDITOR;
let Range = ace.require('ace/range').Range;


/**
 * Load an add the editor template to the DOM.
 */
function load_tool_interface_template() {
  let content = $('#content');
  content.removeClass('p-5');
  const tool_interface_template = Handlebars.compile($("#tool-interface-template").html());
  content.append(tool_interface_template(_CONFIG));
}


/**
 * Initialize the frontend editor.
 */
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
  _EDITOR.on("gutterclick", process_gutter_click);
}


/**
 * Remove all messages from the results.
 */
function clear_messages() {
  let messages_container = $('#messages');
  messages_container.html('');
  _EDITOR.getSession().clearAnnotations();
}


/**
 * Truncate the editor.
 */
function clear_editor() {
  clear_messages();
  _EDITOR.session.setValue(_CONFIG.editor.init_code);
}


/**
 * Bind the user control buttons to process events.
 */
function init_interface_controls () {
  // Changing the tool Language.
  $('.language-selection').on({
    click: function () {
      let language = $( this ).data().language;
      if (language !== get_current_language()) {
        clear_editor();
      }
      choose_language(language);
      refresh_navbar();
    }
  });

  // Handle click on "Execute"
  $('#navbar_execute_interface').on({
    click: function () {
      const settings = get_execute_settings();
      clear_messages();
      run_ultimate_task(settings);
    }
  });

  // Highlight code by message click.
  $(document).on({
    click: function () {
      let data = $( this ).data();
      highlight_code(data.startLine, data.endLine, data.startCol, data.endCol, data.type);
    }
  }, '.toast');

  // Resizable Message container.
  let messages_container = $('#messages');
  interact('#messages')
    .resizable({
      edges: { left: false, right: false, bottom: false, top: true },
      modifiers: [
        // minimum size
        interact.modifiers.restrictSize({
          min: { height: 50 }
        })
      ]
    })
    .on('resizemove', function (event) {
      messages_container.css("flex-basis", event.rect.height + 'px');
      _EDITOR.resize();
    });
}


function refresh_navbar() {
  if ("current_worker" in _CONFIG.context) {
    $('#navbar_language_select_dropdown').html('Language: ' + _CONFIG.context.current_worker.language);

    set_available_code_samples(_CONFIG.context.current_worker.language);
    set_available_frontend_settings(_CONFIG.context.current_worker.language);
    $('#navbar_execute_interface').removeClass('hidden');
  } else {
    $('#navbar_sample_select_dropdown').addClass('hidden');
    $('#navbar_execute_interface').addClass('hidden');
    $('#navbar_settings_select_dropdown').addClass('hidden');
  }
}


function get_annotation_from_message(message) {
  let annotation = {
    row: message.startLNr - 1,
    column: message.startCol,
    text: message.shortDesc,
    type: message.logLvl,
    row_end: message.endLNr,
    col_end: message.endCol
  };

  return annotation
}


/**
 * Process ultimate web bridge results and add them as toasts to the editor interface.
 * @param result
 */
function add_results_to_editor(result) {
  let message;
  let messages_container = $('#messages');
  let annotations = [];
  const editor_message_template = Handlebars.compile($("#editor-message").html());

  for (let key in result.results) {
    message = result.results[key];
    annotations.push(get_annotation_from_message(message))
    switch (message.logLvl) {
      case "error": {
        message.toast_classes = "border border-danger";
        message.oi_icon = "oi-circle-x text-danger";
        break;
      }
      case "warning": {
        message.toast_classes = "border border-warning";
        message.oi_icon = "oi-warning text-warning";
        break;
      }
      case "info": {
        message.toast_classes = "border border-info";
        message.oi_icon = "oi-info text-info";
        break;
      }
    }

    _EDITOR.getSession().setAnnotations(annotations);
    messages_container.append(editor_message_template(result.results[key]));
  }
  $('.toast').toast('show');
}


/**
 * Initiate a ultimate run and process the result.
 * @param settings
 */
function run_ultimate_task(settings) {
  set_execute_spinner(true);

  if (_CONFIG.meta.debug_mode) {
    $.get('./test/result.json', function (response) {
      add_results_to_editor(response);
    }).fail(function () {
      alert("Could not fetch results. Server error.");
    }).always(function () {
      set_execute_spinner(false);
    });
    return
  }

  $.post(_CONFIG.backend.web_bridge_url, settings, function (response) {
    set_execute_spinner(false);
    add_results_to_editor(response);
  }).fail(function () {
    alert("Could not fetch results. Server error.");
  }).always(function () {
    set_execute_spinner(false);
  });
}


/**
 * Get the current settings Dict to be used as a new job for ultimate.
 * @returns {{user_settings: {}, code: string, action: string, toolchain: {task_id: *, id: *}}}
 */
function get_execute_settings() {
  let settings = {
    action: 'execute',
    code: _EDITOR.getSession().getValue(),
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
  _CONFIG.context.tool.workers.forEach(function (worker) {
    if (worker.language === language) {
      _CONFIG.context.current_worker = worker;
    }
  });
}


/**
 * Highlight (background color pop) a section in the editor.
 * Navigates to the start of the code.
 * @param start_line
 * @param end_line
 * @param start_col
 * @param end_col
 * @param css_type
 */
function highlight_code(start_line, end_line, start_col, end_col, css_type) {
  if (start_line < 0) {
    return
  }
  // Navigate to the start ot the code if not visible.
  if (!_EDITOR.isRowFullyVisible(start_line)) {
    _EDITOR.setAnimatedScroll(false);
    _EDITOR.scrollToLine(start_line, true, true);
    _EDITOR.navigateTo(start_line - 1, start_col > 0 ? start_col : 0);
  }
  // Set marker for given range.
  let maker = _EDITOR.session.addMarker(
    new Range(start_line - 1, start_col, end_line, end_col), "color-pop-animation " + css_type, "line"
  );
  // Remove the maker after 2 seconds
  setTimeout(function (marker) {
    if (marker) _EDITOR.session.removeMarker(marker);
  }, 2000, maker);
}


/**
 * Process the event of a gutter click (the area where the line numbers are).
 * Triggers code highlight for annotation clicks.
 * @param event
 */
function process_gutter_click(event) {
  let target = event.domEvent.target;

  // Check if we clicked on an annotation.
  if (((target.className.indexOf('ace_info') !== -1) ||
    (target.className.indexOf('ace_error') !== -1)  ||
    (target.className.indexOf('ace_warning') !== -1)) &&
    (_EDITOR.isFocused()) &&
    (event.clientX < 20 + target.getBoundingClientRect().left)) {

    // Trigger code highlighting for clicked annotation.
    let current_row = event.getDocumentPosition().row;
    let annotations = _EDITOR.session.getAnnotations();

    annotations.forEach(function (annotation) {
      if (annotation.row === current_row) {
        highlight_code(
          annotation.row + 1,
          annotation.row_end,
          annotation.column,
          annotation.col_end,
          annotation.type
        )
      }
    });
  }
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

  if (example_entries.length > 0) {
    $('#navbar_sample_select_dropdown').removeClass('hidden');
  }
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
    clear_messages();
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

  if (settings_entries.length > 0) {
    $('#navbar_settings_select_dropdown').removeClass('hidden');
  }
  settings_menu.html(settings_entries);
  $('.form-check').on('click', function(e) {
    e.stopPropagation();
  });
}


/**
 * Set (activete == true) or unset the spinner indicating the results are being fetched.
 * @param activate
 */
function set_execute_spinner(activate) {
  let exec_button = $('#navbar_execute_interface');
  if (activate) {
    exec_button.html(
      '<span class="spinner-border spinner-border-sm text-primary" role="status" aria-hidden="true"></span> Executing ...'
    );
  } else {
    exec_button.html('Execute');
  }
}
