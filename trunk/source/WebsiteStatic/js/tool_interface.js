var _EDITOR, _CUR_LANG,_LAST_MARKER;
var _COOKIE_EX_DAYS = 365;
var _COOKIE_SKIP    = false;
var _SERVER         = '../WebsiteEclipseBridge/if?callback=?';
var _INIT_CODE      = '// Enter Code here ...';
var _ANIMATE        = !(getCookie('_ANIMATE') == 'false');
var _AUTO_ORIENTATE = !(getCookie('_AUTO_ORIENTATE') == 'false');
var _FONTSIZE       = getCookie('_FONTSIZE') || 100;
var _SPINNER        = {};
var _INFO = new Array();
// an editor event occurs, this is not null for 100ms
var _EVENT;
// false, when messages/annotations added
var _CLEAR;


function load_tool_interface_template() {
    const tool_interface_template = Handlebars.compile($("#tool-interface-template").html());
    $('#content').append(tool_interface_template(_CONFIG));
}

function init_tool_interface_control()
{
    // restore user specific layout
    moveHandler();

    var actions = $('#messages-actions')[0];

    $('#brand-logo')[0].onclick = function() { window.location = './';  };

    actions.children[0].onclick   = function() { switchMessageView(false); };
    actions.children[1].onclick   = switchOrientation;

    $('#show-msg')[0].onclick     = function() { switchMessageView(true); };
    // Todo init the settings div.
    // $('#settings')[0].onmouseup = function() { setTimeout(alignSettingsDropdownBoxes, 500); };

    $('.messages-item .close').each(  function()
    { this.onclick = function() { removeElement(this.parentElement); checkResultsEmpty(); return false; }; });

    $('.button').each(  function()
    { this.onmousedown = function(e) { if($(this).hasClass( 'active' )) return;
        document.onmousedown(); $(this).addClass( 'active' ); e=e||window.event; e.stopPropagation(); e.preventDefault(); return false; }; });

    document.onmousedown = function()
    { $('.button').each( function() { $(this).removeClass( 'active' ); } ); };

    $('.box').each(  function()
    { this.onmousedown = function(e) { e=e||window.event; e.stopPropagation(); }; });

    // $('#play')[0].onclick = getResults;

    var o;
    if(o = getCookie('orientation')) switchOrientation(null, o);

    initResizing();
    initEditor();
    // initSpinners();
    // alignDropdownBoxes();
}

function initEditor()
{
    _EDITOR = ace.edit("editor");
    _EDITOR.renderer.setHScrollBarAlwaysVisible(false);
    _EDITOR.setTheme("ace/theme/eclipse");
    _EDITOR.getSession().setMode('ace/mode/c_cpp'); //equv to: changeMode('c_cpp');
    _EDITOR.renderer.setShowGutter(true);
    _EDITOR.setShowPrintMargin(true);
    _EDITOR.setDisplayIndentGuides(true);
    _EDITOR.setHighlightSelectedWord(true);
    _EDITOR.setPrintMarginColumn(80);

    _EDITOR.session.setValue(_INIT_CODE);
    _EDITOR.session.setTabSize(4);
    _EDITOR.session.setUseWrapMode(true);

    _EDITOR.session.on("change", clearResults);
    $('.ace_text-input').bind('keyup',   clearSampleAndResults);
    $('.ace_text-input').bind('mouseup', clearSampleAndResults);

    _EDITOR.on("gutterclick", highlightCodeByAnnotation);

    _EDITOR.commands.addCommand({
        name: 'execute',
        bindKey: {win: 'Ctrl-D',  mac: 'Command-D'},
        exec: getResults,
        readOnly: true
    });

    _EDITOR.commands.addCommand({
        name: 'execute',
        bindKey: {win: 'Ctrl-S',  mac: 'Command-S'},
        exec: function() { getResults(); toast('Save file not possible!'); },
        readOnly: true
    });
}

function getResults()
{
    _CLEAR = false;
    clearResults();

    // get trimmed code from editor
    var trimmedCode = $.trim(_EDITOR.getSession().getValue());
    if (!editorHasCode(trimmedCode))
    {
        toast('No code to ' + _SPINNER.tool.selected.evalText);
        _EDITOR.focus();
        return;
    }

    // show ajax-loader on #play
    loading('play', true);
    toast('Fetching Ultimate results...');

    var tc = _SPINNER.task.selected;

    var values = "action=execute";
    values += "&code=" + encodeURIComponent(trimmedCode);
    values += getSerializedSettings();
    values += '&taskID='+tc.taskID+'&tcID='+tc.id;

    $.ajax({
        type: "POST",
        url: _SERVER,
        data: values,
        success: function (json) {
            loading('play', false);
            _EDITOR.focus();

            if (json.error) { toast("Error:\n\n"+json.error); } else {

                if(json.status == "success") { addResults(json.results); }
                else { alert("Unexpected response from server!"); } }
        },
        contentType: 'application/x-www-form-urlencoded;charset=UTF-8',
        dataType: 'json',
        error: function(e) { loading('play', false); toast(e.statusText+' ('+e.status+')'); }
    });
    /*
    var res = [
                {
                  endCol: -1,
                  endLNr: 9,
                  logLvl: "error",
                  longDesc: "Variable is neither declared globally nor locally! ID=lock",
                  shortDesc: "Incorrect Syntax",
                  startCol: -1,
                  startLNr: 7
                },
                {
                    endCol: 10,
                    endLNr: 14,
                    logLvl: "warning",
                    longDesc: "Found wrong description in current line",
                    shortDesc: "Loop invariant",
                    startCol: 3,
                    startLNr: 13
                },
                {
                    endCol: 6,
                    endLNr: 16,
                    logLvl: "info",
                    longDesc: "Have a look on several letters again",
                    shortDesc: "Information found",
                    startCol: 2,
                    startLNr: 12
                },
                {
                    endCol: 6,
                    endLNr: 16,
                    logLvl: "info",
                    longDesc: "Have a look on several letters again",
                    shortDesc: "Information found",
                    startCol: 6,
                    startLNr: 16
                },
                {
                    endCol: -1,
                    endLNr: 56,
                    logLvl: "info",
                    longDesc: "Have a look on several letters again",
                    shortDesc: "Information found",
                    startCol: 0,
                    startLNr: 42
                }
              ];
    setTimeout( function()
            {
              loading('play', false);
              addResults(res);
              _EDITOR.focus();
            }, 1000); */
}


function initResizing()
{
    $('.resize-v')[0].onmousedown = function(e) { moveHandler(e, true, 'h'); dragResizer(e); };
    $('.resize-h')[0].onmousedown = function(e) { moveHandler(e, true, 'v'); dragResizer(e); };

    document.onmouseup   = function(e) { moveHandler(e, false); };
}

function initInfo()
{
    $('#info-bar .close')[0].onclick = function() { showNextInfo(false); };
    $('#info-bar .hide' )[0].onclick = function() { showNextInfo(true);  };

    //if(_SERVER_INFO) Arr.foreach(_SERVER_INFO, function(k,v){ _INFO.push(v); } );

    // showNextInfo();
}

function alignContentHeight(h)
{
    if(!h) h = $('#header')[0].clientHeight;
    // padding top of content must be as high as #header
    $('#content')[0].style.paddingTop = h + 'px';
}

function alignPageContent()
{
    fitHeight();
}

// automatically changing orientation
function alignInterfaceContent()
{
    var w = document.getElementById('messages').clientWidth;

    var h = window.innerHeight
        || document.documentElement.clientHeight
        || document.body.clientHeight;

    if(_AUTO_ORIENTATE && w < 400 && $(document.body).hasClass(  'vertical' ))
    {
        switchOrientation('horizontal');
    }
    else if(_AUTO_ORIENTATE && w > 1200 && h < 800 && $(document.body).hasClass(  'horizontal' ))
    {
        switchOrientation(false);
    }

    if(_EVENT) window.clearTimeout(_EVENT);
    _EVENT = setTimeout(alignHeaderWidth, 50);

    alignDropdownBoxes();
    setTimeout(alignDropdownBoxes, 500);
}

function alignHeaderWidth(stop)
{
    $(document.body ).removeClass( 'animate' );

    var toolLabel  = document.getElementById('tool').firstElementChild;
    var taskLabel  = document.getElementById('task').firstElementChild;
    var header     = document.getElementById('header');
    var leftWidth  = header.children[0].clientWidth;
    var rightWidth = header.children[1].clientWidth;
    var list = $('.right .int.button');

    if(!stop && header.clientWidth > leftWidth + rightWidth + 50)
    {
        $('#tool')[0].firstElementChild.style.maxWidth = '';
        $('#task')[0].firstElementChild.style.maxWidth = '';
    }

    if(header.clientWidth < leftWidth + rightWidth + 100)
    {
        var last = null;
        if(!stop) list.each(  function() { $(this).addClass( 'show' ); });
        // header is smaller than a gap of 150px between left and right
        list.each(  function() {
            if($(this).hasClass( 'show' )) { last = this; }
            if(last) { $(last).removeClass( 'show' ); }
            else { $('#brand-label').addClass( 'away' ); }
        });
    }

    if(header.clientWidth > leftWidth + rightWidth + 255 && $('#brand-label').hasClass( 'away' ))
    {
        // header is wider than a gap of 250px between left and right
        $('#brand-label').removeClass( 'away' );
        setTimeout(alignHeaderWidth, 3, ++stop);
        return;
    }

    if(header.clientWidth > leftWidth + rightWidth + 300)
    {
        // header is wider than a gap of 250px between left and right
        list.each(  function() {
            if(!$(this).hasClass( 'show' ) && !isHidden(this))
            { $(this).addClass( 'show' ); return false; }
        });
    }

    if(!stop) stop = 0;
    if(stop == 4 && header.clientWidth < leftWidth + rightWidth + 30)
    {
        var diff = -header.clientWidth + leftWidth + rightWidth + 40;
        toolLabel.style.maxWidth = Math.max(100, toolLabel.clientWidth - diff) + 'px';
    }
    if(stop == 5 && header.clientWidth < leftWidth + rightWidth + 30)
    {
        var diff = -header.clientWidth + leftWidth + rightWidth + 40;
        taskLabel.style.maxWidth = Math.max(100, taskLabel.clientWidth - diff) + 'px';
    }
    if(stop == 5) { $(document.body ).addClass( _ANIMATE ? 'animate' : '' ); return; }

    setTimeout(alignHeaderWidth, 3, ++stop);
    _EVENT = null;
}

function alignDropdownBoxes()
{
    // if button.spinner.visible max-width -= label.offsetLeft

    var distance = 12;
    var header   = document.getElementById('header'  );
    var editor   = document.getElementById('editor'  );
    var docWidth = window.innerWidth
        || document.documentElement.clientWidth
        || document.body.clientWidth;

    $('.button .box').each(  function()
    {
        this.style.top       = header.clientHeight +   distance - 1 + 'px';
        this.style.maxHeight = editor.clientHeight - 2*distance     + 'px';
        this.style.maxWidth  =            docWidth - 2*distance     + 'px';
    });

    if(_EDITOR) _EDITOR.resize();
}

function alignSettingsDropdownBoxes()
{
    var settingsBox = document.getElementById('settings').lastElementChild;

    var distance = 12;
    var docWidth = window.innerWidth
        || document.documentElement.clientWidth
        || document.body.clientWidth;
    var side = Math.min(docWidth-settingsBox.offsetLeft, settingsBox.offsetLeft+settingsBox.clientWidth);

    $('.box', settingsBox).each(  function()
    {
        this.style.left = this.style.right = '';
        /* see basics.css:520 .vertical .button > .box
        if($(document.body).hasClass( 'vertical' ))
          this.style.left    = side + distance + 'px';
        else */
        this.style.right   = side + distance + 'px';
        this.style.maxWidth  = docWidth - side - 4*distance + 'px';
    });
}

function switchOrientation(e, o)
{
    var el = $(document.body);
    el.removeClass( 'animate' );

    if (o == 'horizontal' || (el.hasClass(  'vertical' ) && o == null))
    {
        el.removeClass( 'vertical' );
        el.addClass(    'horizontal' );
        if(o == null) setCookie('orientation', 'horizontal', _COOKIE_EX_DAYS);
    }
    else
    {
        el.removeClass( 'horizontal' );
        el.addClass(    'vertical' );
        if(o == null) setCookie('orientation', 'vertical', _COOKIE_EX_DAYS);
    }

    setTimeout(function() { $(document.body).addClass( _ANIMATE ? 'animate' : '' ); }, 100);
    setTimeout(alignDropdownBoxes, 500);

    try{ _EDITOR.resize(); } catch (e) {}
}

function switchMessageView(show)
{
    if (show)
        $('#content').removeClass( 'hide' );
    else
        $('#content').addClass( 'hide' );

    setTimeout(alignDropdownBoxes, _ANIMATE*500);
}

var _newHeight = getCookie('_EDITOR_H') || null;;
var _newWidth  = getCookie('_EDITOR_W') || null;
var _direction = _newHeight || _newWidth;
var _hidden;

function moveHandler(e, start, direction)
{
    if(start)
    {
        $(document.body ).removeClass( 'animate' );
        $('#content'    ).removeClass( 'hide'    );
        $('#content'    ).addClass   ( 'drag'    );

        e = e || window.event;

        _direction = direction;

        // tell our code to start moving the element with the mouse
        document.onmousemove = dragResizer;

        // cancel out any text selections
        document.body.focus();

        // prevent text selection in IE
        document.onselectstart = function () { return false; };
        // prevent IE from trying to drag an image
        if(e.srcElement) e.srcElement.ondragstart = function() { return false; };
        // prevent text selection (except IE)
        return false;
    }

    // mouse is up
    if(!_direction) return false;

    var editor   = document.getElementById('editor'  );
    var messages = document.getElementById('messages');

    document.onmousemove = null;
    document.onselectstart = null;
    _direction = null;

    $(document.body).addClass   ( _ANIMATE ? 'animate' : '' );
    $('#content'   ).removeClass( 'drag'    );

    if (_hidden)
    {
        $('#content'   ).addClass( 'hide' );
    }

    if (_newHeight)
    {
        editor.style.height   = (100-_newHeight) + '%';
        messages.style.height =      _newHeight  + '%';
        setCookie('_EDITOR_H', _newHeight, _COOKIE_EX_DAYS);
    }

    if (_newWidth)
    {
        editor.style.width   = (100-_newWidth) + '%';
        messages.style.width =      _newWidth  + '%';
        setCookie('_EDITOR_W', _newWidth, _COOKIE_EX_DAYS);
    }

    _newHeight = null;
    _newWidth  = null;

    setTimeout(alignDropdownBoxes, _ANIMATE*500);

    return false;
}

function dragResizer(e)
{
    e = e || window.event;

    e.preventDefault();

    var wPx = window.innerWidth
        || document.documentElement.clientWidth
        || document.body.clientWidth;
    var hPx = window.innerHeight
        || document.documentElement.clientHeight
        || document.body.clientHeight;

    var editor   = document.getElementById('editor'  );
    var messages = document.getElementById('messages');

    if(_direction == 'v')
    {
        // minimal width of messages AND editor*2
        var minW = 400;
        // mouse
        var mY   = e.clientX + 5;

        _hidden = e.clientX > wPx-80;

        var w = getInInterval(0, ((wPx-mY)/wPx)*100, 95);

        // minimal pixel value
        mY        = getInInterval(minW/2, mY, wPx-minW);
        // min-max percentage value
        _newWidth = getInInterval(20, ((wPx-mY)/wPx)*100, 80);

        editor.style.width   = (100-w) + '%';
        messages.style.width =      w  + '%';
    }
    else
    {
        // minimal height of editor AND messages
        var minH = 100;
        var hH   = document.getElementById('header').clientHeight;
        hPx     -= hH;
        // mouse
        var mX   = e.clientY-hH + 5;

        _hidden = mX > hPx-40;

        var h = getInInterval(0, ((hPx-(mX))/hPx)*100, 99);
        // minimal pixel value
        mX         = getInInterval(minH, mX, hPx-minH);
        // min-max percentage value
        _newHeight = getInInterval(10, ((hPx-(mX))/hPx)*100, 90);

        editor.style.height   = (100-h) + '%';
        messages.style.height =      h  + '%';
    }

    _EDITOR.resize();

    return false;
}
