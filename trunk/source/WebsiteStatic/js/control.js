"use strict";
window.onload = initControl;

function initControl() {
    // showing all notices, including cookie-info when needed
    initInfo();

    if ($(document.body).hasClass('home')) alignHomeContent();
    if ($(document.body).hasClass('int')) initInterfaceControl();
}


function initInterfaceControl() {
    // restore user specific layout
    moveHandler();

    var actions = $('#messages-actions')[0];

    $('#brand-logo')[0].onclick = function () {
        window.location = './';
    };

    actions.children[0].onclick = function () {
        switchMessageView(false);
    };
    actions.children[1].onclick = switchOrientation;

    $('#show-msg')[0].onclick = function () {
        switchMessageView(true);
    };
    $('#settings')[0].onmouseup = function () {
        setTimeout(alignSettingsDropdownBoxes, 500);
    };

    $('.messages-item .close').each(function () {
        this.onclick = function () {
            removeElement(this.parentElement);
            checkResultsEmpty();
            return false;
        };
    });

    /*$('.button').each(  function()
    { this.onclick = function() { if($(this).hasClass( 'active' )) $(this).removeClass( 'active' ); else  $(this).addClass( 'active' ); }; });
  */
    $('.button').each(function () {
        this.onmousedown = function (e) {
            if ($(this).hasClass('active')) return;
            document.onmousedown();
            $(this).addClass('active');
            e = e || window.event;
            e.stopPropagation();
            e.preventDefault();
            return false;
        };
    });

    document.onmousedown = function () {
        $('.button').each(function () {
            $(this).removeClass('active');
        });
    };

    $('.box').each(function () {
        this.onmousedown = function (e) {
            e = e || window.event;
            e.stopPropagation();
        };
    });

    $('#play')[0].onclick = getResults;

    var o;
    if (o = getCookie('orientation')) switchOrientation(null, o);

    initResizing();
    initEditor();
    initSpinners();
    alignDropdownBoxes();
}


function initInfo() {
    $('#info-bar .close')[0].onclick = function () {
        showNextInfo(false);
    };
    $('#info-bar .hide')[0].onclick = function () {
        showNextInfo(true);
    };

    try {
        if (_SERVER_INFO) Arr.foreach(_SERVER_INFO, function (k, v) {
            _INFO.push(v);
        });
    } catch (e) {
        console.log("transfer.js not found!");
    }

    showNextInfo();
}


// fitting height of toolchain boxes
function alignToolchainBoxes() {
    var container = $('#toolchains')[0];
    var toolchains = container.children;
    var maxHeights = {columns: 0, heights: []};

    maxHeights.columns = Math.floor(container.clientWidth / toolchains[0].clientWidth);

    // read max height per row
    Obj.eachElement(toolchains, function (k, v) {
        var i = Math.floor(k / maxHeights.columns);
        maxHeights.heights[i] = Math.max(maxHeights.heights[i] || 0, v.clientHeight);
    });
    // write new height per row
    Obj.eachElement(toolchains, function (k, v) {
        var i = Math.floor(k / maxHeights.columns);
        v.style.height = (maxHeights.heights[i] - 50) + 'px';
    });
}
