// Daily News 상호작용(plan §5.2). 모든 콘텐츠와 링크는 HTML에 있고, 이 파일은 전환·이동·꾸밈만 더한다(NFR-7).
// 스타일은 클래스와 CSS 변수(CSSOM)로만 바꾼다. style 속성 문자열은 쓰지 않는다(CSP).
(() => {
    "use strict";
    const doc = document;
    const root = doc.documentElement;
    const media = (query) => window.matchMedia(query);
    const reducedMotion = () => media("(prefers-reduced-motion: reduce)").matches;
    const canHover = () => media("(hover: hover) and (pointer: fine)").matches;
    const all = (selector, scope = doc) => Array.from(scope.querySelectorAll(selector));
    const DATE = /^\d{4}-\d{2}-\d{2}$/;

    // ── 배경 전환(FR-7): light | dark | system. 저장할 수 없으면 메모리 값으로만 동작한다 ──
    const THEME_KEY = "dn-theme";
    const darkQuery = media("(prefers-color-scheme: dark)");
    let theme = "system";

    function initTheme() {
        try {
            const saved = localStorage.getItem(THEME_KEY);
            theme = saved === "light" || saved === "dark" ? saved : "system";
        } catch (e) {
            theme = "system";
        }
        const apply = () => root.classList.toggle("dark", theme === "dark" || (theme === "system" && darkQuery.matches));
        root.classList.replace("no-js", "js");
        apply();
        darkQuery.addEventListener("change", () => {
            if (theme === "system") {
                apply();
            }
        });
        all("[data-theme-switcher]").forEach((switcher) => {
            all("input[type=radio]", switcher).forEach((radio) => {
                radio.checked = radio.value === theme;
                radio.addEventListener("change", () => {
                    if (!radio.checked) {
                        return;
                    }
                    theme = radio.value === "light" || radio.value === "dark" ? radio.value : "system";
                    try {
                        localStorage.setItem(THEME_KEY, theme);
                    } catch (e) {
                        // 저장하지 못해도 이 페이지에서는 바뀐 배경을 유지한다.
                    }
                    apply();
                });
            });
        });
    }

    // ── 카테고리 탭(animated-navigation-tabs): 클릭, ←/→/Home/End, 해시 동기화, 활성 막대 이동 ──
    function initTabs() {
        const list = doc.querySelector("[role=tablist]");
        if (!list) {
            return;
        }
        const tabs = all("[role=tab]", list);
        const panels = tabs.map((tab) => doc.getElementById(tab.getAttribute("aria-controls")));
        const indicator = list.querySelector("[data-tabs-indicator]");
        let current = 0;

        const moveIndicator = () => {
            if (indicator) {
                list.style.setProperty("--indicator-x", tabs[current].offsetLeft + "px");
                list.style.setProperty("--indicator-w", tabs[current].offsetWidth + "px");
            }
        };
        const select = (index, focus) => {
            current = index;
            tabs.forEach((tab, i) => {
                const selected = i === index;
                tab.setAttribute("aria-selected", String(selected));
                tab.tabIndex = selected ? 0 : -1;
                panels[i].hidden = !selected;
            });
            moveIndicator();
            if (focus) {
                tabs[index].focus();
                tabs[index].scrollIntoView({block: "nearest", inline: "nearest"});
            }
        };
        const fromHash = () => tabs.findIndex((tab) => "#" + tab.getAttribute("aria-controls") === location.hash);

        tabs.forEach((tab, index) => {
            tab.addEventListener("click", (event) => {
                event.preventDefault();
                select(index, false);
                history.replaceState(null, "", "#" + tab.getAttribute("aria-controls"));
                // 탭 막대가 위에 붙은 상태에서 바꾸면 새 패널의 처음으로 올라간다.
                if (panels[index].getBoundingClientRect().top < 0) {
                    panels[index].scrollIntoView({block: "start"});
                }
            });
            tab.addEventListener("keydown", (event) => {
                const last = tabs.length - 1;
                const next = {
                    ArrowRight: index === last ? 0 : index + 1,
                    ArrowLeft: index === 0 ? last : index - 1,
                    Home: 0,
                    End: last
                }[event.key];
                if (next !== undefined) {
                    event.preventDefault();
                    select(next, true);
                }
            });
        });
        window.addEventListener("hashchange", () => {
            const index = fromHash();
            if (index >= 0) {
                select(index, false);
            }
        });
        window.addEventListener("resize", moveIndicator);
        if (doc.fonts) {
            doc.fonts.ready.then(moveIndicator);
        }
        list.classList.toggle("has-indicator", !!indicator);
        const initial = fromHash();
        select(initial < 0 ? 0 : initial, false);
    }

    // ── 통계 숫자(counting-number): 화면에 들어오면 0부터 최종 값까지 1초 안에 한 번 센다 ──
    function initCountUp() {
        const numbers = all("[data-count-up]");
        if (!numbers.length || reducedMotion() || !("IntersectionObserver" in window)) {
            return;
        }
        const format = new Intl.NumberFormat("en-US");
        const run = (el) => {
            const target = Number(el.dataset.value) || 0;
            const start = performance.now();
            const step = (now) => {
                const progress = Math.min(1, (now - start) / 900);
                el.textContent = format.format(Math.round(target * (1 - Math.pow(1 - progress, 3))));
                if (progress < 1) {
                    requestAnimationFrame(step);
                }
            };
            requestAnimationFrame(step);
        };
        const observer = new IntersectionObserver((entries) => entries.forEach((entry) => {
            if (entry.isIntersecting) {
                observer.unobserve(entry.target);
                run(entry.target);
            }
        }), {threshold: 0.5});
        numbers.forEach((el) => {
            el.textContent = "0";
            observer.observe(el);
        });
    }

    // ── 기사 카드(spotlight-card): 마우스를 쓰는 기기에서만 포인터 위치를 CSS 변수로 넘긴다 ──
    function initSpotlight() {
        if (!doc.querySelector("[data-spotlight]") || !canHover() || reducedMotion()) {
            return;
        }
        doc.addEventListener("pointermove", (event) => {
            const card = event.target instanceof Element ? event.target.closest("[data-spotlight]") : null;
            if (card) {
                const rect = card.getBoundingClientRect();
                card.style.setProperty("--x", event.clientX - rect.left + "px");
                card.style.setProperty("--y", event.clientY - rect.top + "px");
            }
        }, {passive: true});
    }

    // ── 등장 효과(FR-3.8): 첫 화면 밖의 요소만 숨겼다가 들어올 때 보여 준다 ──
    function initReveal() {
        const items = all("[data-reveal]");
        if (!items.length || reducedMotion() || !("IntersectionObserver" in window)) {
            return;
        }
        const seen = new WeakSet();
        const observer = new IntersectionObserver((entries) => entries.forEach((entry) => {
            const el = entry.target;
            if (entry.isIntersecting) {
                el.setAttribute("data-reveal", "shown");
                observer.unobserve(el);
            } else if (!seen.has(el)) {
                el.setAttribute("data-reveal", "pending");
            }
            seen.add(el);
        }), {rootMargin: "0px 0px -40px 0px"});
        items.forEach((el) => observer.observe(el));
    }

    // ── 히어로(m-variable-font-hover-2): 글자별 지연 순서만 넣고 효과는 CSS가 맡는다 ──
    function initHero() {
        all("[data-variable-hover] .hero-line").forEach((line) => {
            const letters = all(":scope > span", line);
            const fromLast = line.dataset.stagger === "last";
            letters.forEach((letter, i) => letter.style.setProperty("--i", String(fromLast ? letters.length - 1 - i : i)));
            if (!media("(hover: hover)").matches) {
                let timer = 0;
                line.addEventListener("click", () => {
                    line.classList.add("is-active");
                    clearTimeout(timer);
                    timer = setTimeout(() => line.classList.remove("is-active"), 1200);
                });
            }
        });
    }

    // ── 캘린더(new-calendar): 한 달씩 보여 주고 이전/다음 달로 이동한다. JS가 없으면 모든 달이 쌓여 보인다 ──
    function initCalendar() {
        const calendar = doc.querySelector("[data-calendar]");
        const months = calendar ? all("[data-calendar-month]", calendar) : [];
        const bar = calendar && calendar.querySelector("[data-calendar-nav]");
        if (!months.length || !bar) {
            return;
        }
        const prev = bar.querySelector("[data-calendar-prev]");
        const next = bar.querySelector("[data-calendar-next]");
        const label = bar.querySelector("[data-calendar-label]");
        const ids = months.map((month) => month.dataset.calendarMonth);
        let index = ids.indexOf(location.hash.slice(1));
        if (index < 0) {
            index = Math.max(0, ids.indexOf(calendar.dataset.latest));
        }
        const enable = (button, enabled, other) => {
            if (!enabled && doc.activeElement === button) {
                other.focus();
            }
            button.disabled = !enabled;
            button.setAttribute("aria-disabled", String(!enabled));
        };
        const show = (i, updateHash) => {
            index = i;
            months.forEach((month, k) => {
                month.hidden = k !== i;
            });
            label.textContent = months[i].dataset.label;
            // 켜지는 버튼을 먼저 켜야 꺼지는 버튼에 있던 포커스를 그쪽으로 옮길 수 있다.
            const states = [[prev, i > 0, next], [next, i < ids.length - 1, prev]];
            states.sort((a, b) => Number(b[1]) - Number(a[1]))
                .forEach(([button, enabled, other]) => enable(button, enabled, other));
            if (updateHash) {
                // 처음 그릴 때는 움직이지 않고, 사용자가 달을 넘길 때만 등장 효과를 준다.
                calendar.classList.add("is-navigated");
                history.replaceState(null, "", "#" + ids[i]);
            }
        };
        prev.addEventListener("click", () => index > 0 && show(index - 1, true));
        next.addEventListener("click", () => index < ids.length - 1 && show(index + 1, true));
        window.addEventListener("hashchange", () => {
            const i = ids.indexOf(location.hash.slice(1));
            if (i >= 0) {
                show(i, false);
            }
        });
        show(index, false);
        calendar.classList.add("is-paged");
    }

    // ── 날짜별 뉴스피드(FR-3.6): archive.json으로 보관 중인 이전/다음 날짜만 링크한다 ──
    function initDayNav() {
        const nav = doc.querySelector("[data-day-nav]");
        if (!nav || !window.fetch) {
            return;
        }
        const base = nav.dataset.base || "";
        const link = (selector, date) => {
            const anchor = nav.querySelector(selector);
            if (anchor && DATE.test(date || "")) {
                anchor.href = base + "archive/" + date + ".html";
                anchor.querySelector("[data-day-label]").textContent = date;
                anchor.hidden = false;
            }
        };
        fetch(base + "data/archive.json", {cache: "no-cache"})
            .then((response) => (response.ok ? response.json() : Promise.reject(response.status)))
            .then((archive) => {
                const dates = (Array.isArray(archive.dates) ? archive.dates : []).filter((d) => DATE.test(d)).sort();
                const i = dates.indexOf(nav.dataset.date);
                if (i >= 0) {
                    link("[data-day-prev]", dates[i - 1]);
                    link("[data-day-next]", dates[i + 1]);
                }
            })
            .catch(() => {
                // 읽지 못하면 캘린더로 돌아가는 링크만 남긴다.
            });
    }

    const start = () => [initTheme, initTabs, initCountUp, initSpotlight, initReveal, initHero, initCalendar, initDayNav]
        .forEach((init) => init());
    if (doc.readyState === "loading") {
        doc.addEventListener("DOMContentLoaded", start);
    } else {
        start();
    }
})();
