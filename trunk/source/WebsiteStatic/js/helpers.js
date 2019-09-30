function fitHeight(h) {

    if ($('#header').hasClass('fixed')) {
        alignContentHeight(h);
        setTimeout(alignContentHeight, _ANIMSec);
    }
    if (_CONFIG.context.url === 'int') {
        alignDropdownBoxes();
        setTimeout(alignDropdownBoxes, _ANIMSec);
    }
}

function alignPageContent() {
    fitHeight();
}

function alignContentHeight(h) {
    if (!h) h = $('#header')[0].clientHeight;
    // padding top of content must be as high as #header
    $('#content')[0].style.paddingTop = h + 'px';
}

function alignHomeContent() {
    alignToolchainBoxes();
    alignPageContent();
}

// automatically changing orientation
function alignInterfaceContent() {
    var w = document.getElementById('messages').clientWidth;

    var h = window.innerHeight
        || document.documentElement.clientHeight
        || document.body.clientHeight;

    if (_AUTO_ORIENTATE && w < 400 && $(document.body).hasClass('vertical')) {
        switchOrientation('horizontal');
    } else if (_AUTO_ORIENTATE && w > 1200 && h < 800 && $(document.body).hasClass('horizontal')) {
        switchOrientation(false);
    }

    if (_EVENT) window.clearTimeout(_EVENT);
    _EVENT = setTimeout(alignHeaderWidth, 50);

    alignDropdownBoxes();
    setTimeout(alignDropdownBoxes, 500);
}

function alignHeaderWidth(stop) {
    $(document.body).removeClass('animate');

    var toolLabel = document.getElementById('tool').firstElementChild;
    var taskLabel = document.getElementById('task').firstElementChild;
    var header = document.getElementById('header');
    var leftWidth = header.children[0].clientWidth;
    var rightWidth = header.children[1].clientWidth;
    var list = $('.right .int.button');

    if (!stop && header.clientWidth > leftWidth + rightWidth + 50) {
        $('#tool')[0].firstElementChild.style.maxWidth = '';
        $('#task')[0].firstElementChild.style.maxWidth = '';
    }

    if (header.clientWidth < leftWidth + rightWidth + 100) {
        var last = null;
        if (!stop) list.each(function () {
            $(this).addClass('show');
        });
        // header is smaller than a gap of 150px between left and right
        list.each(function () {
            if ($(this).hasClass('show')) {
                last = this;
            }
            if (last) {
                $(last).removeClass('show');
            } else {
                $('#brand-label').addClass('away');
            }
        });
    }

    if (header.clientWidth > leftWidth + rightWidth + 255 && $('#brand-label').hasClass('away')) {
        // header is wider than a gap of 250px between left and right
        $('#brand-label').removeClass('away');
        setTimeout(alignHeaderWidth, 3, ++stop);
        return;
    }

    if (header.clientWidth > leftWidth + rightWidth + 300) {
        // header is wider than a gap of 250px between left and right
        list.each(function () {
            if (!$(this).hasClass('show') && !isHidden(this)) {
                $(this).addClass('show');
                return false;
            }
        });
    }

    if (!stop) stop = 0;
    if (stop == 4 && header.clientWidth < leftWidth + rightWidth + 30) {
        var diff = -header.clientWidth + leftWidth + rightWidth + 40;
        toolLabel.style.maxWidth = Math.max(100, toolLabel.clientWidth - diff) + 'px';
    }
    if (stop == 5 && header.clientWidth < leftWidth + rightWidth + 30) {
        var diff = -header.clientWidth + leftWidth + rightWidth + 40;
        taskLabel.style.maxWidth = Math.max(100, taskLabel.clientWidth - diff) + 'px';
    }
    if (stop == 5) {
        $(document.body).addClass(_ANIMATE ? 'animate' : '');
        return;
    }

    setTimeout(alignHeaderWidth, 3, ++stop);
    _EVENT = null;
}

function alignDropdownBoxes() {
    // if button.spinner.visible max-width -= label.offsetLeft

    var distance = 12;
    var header = document.getElementById('header');
    var editor = document.getElementById('editor');
    var docWidth = window.innerWidth
        || document.documentElement.clientWidth
        || document.body.clientWidth;

    $('.button .box').each(function () {
        this.style.top = header.clientHeight + distance - 1 + 'px';
        this.style.maxHeight = editor.clientHeight - 2 * distance + 'px';
        this.style.maxWidth = docWidth - 2 * distance + 'px';
    });

    if (_EDITOR) _EDITOR.resize();
}

function alignSettingsDropdownBoxes() {
    var settingsBox = document.getElementById('settings').lastElementChild;

    var distance = 12;
    var docWidth = window.innerWidth
        || document.documentElement.clientWidth
        || document.body.clientWidth;
    var side = Math.min(docWidth - settingsBox.offsetLeft, settingsBox.offsetLeft + settingsBox.clientWidth);

    $('.box', settingsBox).each(function () {
        this.style.left = this.style.right = '';
        /* see basics.css:520 .vertical .button > .box
        if($(document.body).hasClass( 'vertical' ))
          this.style.left    = side + distance + 'px';
        else */
        this.style.right = side + distance + 'px';
        this.style.maxWidth = docWidth - side - 4 * distance + 'px';
    });
}

function alignContent() {
    if (_CONFIG.context.url === 'home') alignHomeContent();
    if (_CONFIG.context.url === 'tool') alignPageContent();
    if (_CONFIG.context.url === 'int') alignInterfaceContent();
}