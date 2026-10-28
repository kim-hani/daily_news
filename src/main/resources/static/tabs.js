// 카테고리 탭 전환(점진적 향상). 스크립트가 없으면 탭은 섹션으로 이동하는 앵커로 동작하고 모든 섹션이 보인다.
(function () {
    "use strict";
    var tabs = Array.prototype.slice.call(document.querySelectorAll('[role="tab"]'));
    if (tabs.length === 0) {
        return;
    }
    var panels = tabs.map(function (tab) {
        return document.getElementById(tab.getAttribute("aria-controls"));
    });

    function select(index, focus) {
        tabs.forEach(function (tab, i) {
            var selected = i === index;
            tab.setAttribute("aria-selected", selected ? "true" : "false");
            tab.tabIndex = selected ? 0 : -1;
            panels[i].hidden = !selected;
        });
        if (focus) {
            tabs[index].focus();
        }
    }

    tabs.forEach(function (tab, index) {
        tab.addEventListener("click", function (event) {
            event.preventDefault();
            select(index, false);
            history.replaceState(null, "", "#" + tab.getAttribute("aria-controls"));
        });
        tab.addEventListener("keydown", function (event) {
            var last = tabs.length - 1;
            var next = {ArrowRight: index === last ? 0 : index + 1, ArrowLeft: index === 0 ? last : index - 1,
                Home: 0, End: last}[event.key];
            if (next === undefined) {
                return;
            }
            event.preventDefault();
            select(next, true);
        });
    });

    var initial = tabs.findIndex(function (tab) {
        return "#" + tab.getAttribute("aria-controls") === location.hash;
    });
    select(initial < 0 ? 0 : initial, false);
})();
