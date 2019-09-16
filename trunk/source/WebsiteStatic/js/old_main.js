// fetching example asynchronously
function selectExample(ex)
{
    _EDITOR.getSession().setAnnotations([]);

    if (!_SPINNER.task.selected) return;

    $.ajax({
        url: _SERVER,
        data: "example=" + encodeURIComponent(ex),
        success: function (json)
        {
            if (json.exampleContent !== null)
            {
                _EDITOR.session.setValue(json.exampleContent);
                _EDITOR.focus();
                return;
            }
        },
        dataType: 'json'
    });
}

// change language highlighting of editor
function changeMode(mode)
{
    _EDITOR.getSession().setMode('ace/mode/' + mode.replace(' ', '_'));
}

// change font Size of editor
function changeFontSize(pt)
{
    var e = document.getElementById("editor");
    e.style.fontSize = pt + "pt";

    _FONTSIZE = pt;
    setCookie('_FONTSIZE', Math.floor(pt*2)/2, _COOKIE_EX_DAYS);
}

// change font size of content
function changeFontSize(percent)
{
    var e = document.getElementById("editor");
    e.style.fontSize = (percent/2) + "%";
    e.nextElementSibling.style.fontSize = Math.min(150, percent) + '%';

    _FONTSIZE = percent;
    setCookie('_FONTSIZE', Math.floor(percent*2)/2, _COOKIE_EX_DAYS);
}


var Range = require('ace/range').Range;
// set editor preferences