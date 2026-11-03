// 첫 화면 전에 배경을 정한다(FR-7.5). <head>에서 동기 실행. 저장값 dn-theme = light | dark | system
(function () {
    var root = document.documentElement, theme = "system";
    try {
        var saved = localStorage.getItem("dn-theme");
        if (saved === "light" || saved === "dark") theme = saved;
    } catch (e) {
        // 저장소를 쓸 수 없으면 기기 설정을 따른다
    }
    root.classList.remove("no-js");
    root.classList.add("js");
    if (theme === "dark" || (theme === "system" && window.matchMedia
        && window.matchMedia("(prefers-color-scheme: dark)").matches)) {
        root.classList.add("dark");
    }
})();
