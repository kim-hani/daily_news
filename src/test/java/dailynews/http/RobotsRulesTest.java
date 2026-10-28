package dailynews.http;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RobotsRulesTest {

    @Test
    void Allow가_Disallow보다_길게_일치하면_우선한다() {
        RobotsRules rules = RobotsRules.parse("""
                User-agent: *
                Disallow: /news
                Allow: /news/public
                """);

        assertThat(rules.isAllowed("/news/public/1")).isTrue();
        assertThat(rules.isAllowed("/news/private/1")).isFalse();
        assertThat(rules.isAllowed("/sports")).isTrue();
    }

    @Test
    void Disallow가_더_길게_일치하면_Disallow가_이긴다() {
        RobotsRules rules = RobotsRules.parse("""
                User-agent: *
                Allow: /news
                Disallow: /news/private
                """);

        assertThat(rules.isAllowed("/news/private/1")).isFalse();
        assertThat(rules.isAllowed("/news/1")).isTrue();
    }

    @Test
    void 길이가_같으면_Allow가_이긴다() {
        RobotsRules rules = RobotsRules.parse("""
                User-agent: *
                Disallow: /page
                Allow: /page
                """);

        assertThat(rules.isAllowed("/page")).isTrue();
    }

    @Test
    void 별표는_임의_문자열이고_끝의_달러는_끝_고정이다() {
        RobotsRules rules = RobotsRules.parse("""
                User-agent: *
                Disallow: /*.pdf$
                Disallow: /*?*cp=
                """);

        assertThat(rules.isAllowed("/files/report.pdf")).isFalse();
        assertThat(rules.isAllowed("/files/report.pdf?download=1")).isTrue();
        assertThat(rules.isAllowed("/view/A1?section=x&cp=naver")).isFalse();
        assertThat(rules.isAllowed("/view/A1?section=x")).isTrue();
    }

    @Test
    void 정규식_특수문자는_글자_그대로_비교한다() {
        RobotsRules rules = RobotsRules.parse("""
                User-agent: *
                Disallow: /print.html?art_id=1&type=(basic)
                """);

        assertThat(rules.isAllowed("/print.html?art_id=1&type=(basic)")).isFalse();
        assertThat(rules.isAllowed("/printXhtml?art_id=1&type=(basic)")).isTrue();
    }

    @Test
    void 별표_그룹이_아닌_규칙은_무시한다() {
        RobotsRules rules = RobotsRules.parse("""
                User-agent: GPTBot
                User-agent: ClaudeBot
                Disallow: /

                User-agent: *
                Disallow: /login
                """);

        assertThat(rules.isAllowed("/article/1")).isTrue();
        assertThat(rules.isAllowed("/login")).isFalse();
    }

    @Test
    void 여러_User_agent를_묶은_그룹에_별표가_있으면_적용한다() {
        RobotsRules rules = RobotsRules.parse("""
                User-agent: Googlebot
                User-agent: *
                Disallow: /private
                """);

        assertThat(rules.isAllowed("/private/1")).isFalse();
    }

    @Test
    void 별표_그룹이_여러_개면_규칙을_합친다() {
        RobotsRules rules = RobotsRules.parse("""
                User-agent: *
                Disallow: /a

                User-agent: Bingbot
                Disallow: /b

                User-agent: *
                Disallow: /c
                """);

        assertThat(rules.isAllowed("/a")).isFalse();
        assertThat(rules.isAllowed("/b")).isTrue();
        assertThat(rules.isAllowed("/c")).isFalse();
    }

    @Test
    void 빈_줄은_그룹을_끝내지_않는다() {
        RobotsRules rules = RobotsRules.parse("""
                User-agent: *
                Allow:/

                Disallow: /search/
                """);

        assertThat(rules.isAllowed("/search/x")).isFalse();
    }

    @Test
    void 콜론_앞_공백_대소문자_주석_BOM을_처리한다() {
        RobotsRules rules = RobotsRules.parse("﻿" + """
                # 주석
                USER-AGENT : *   # 모든 봇
                Disallow : /xml/
                DISALLOW:/members/
                Sitemap: https://www.example.com/sitemap.xml
                """);

        assertThat(rules.isAllowed("/xml/rss/news.xml")).isFalse();
        assertThat(rules.isAllowed("/members/login")).isFalse();
        assertThat(rules.isAllowed("/news/1")).isTrue();
    }

    @Test
    void 경로는_대소문자를_구분한다() {
        RobotsRules rules = RobotsRules.parse("""
                User-agent: *
                Disallow: /NEWS
                """);

        assertThat(rules.isAllowed("/NEWS/1")).isFalse();
        assertThat(rules.isAllowed("/news/1")).isTrue();
    }

    @Test
    void 값이_빈_Disallow나_별표_그룹이_없으면_전부_허용한다() {
        assertThat(RobotsRules.parse("User-agent: *\nDisallow:\n").isAllowed("/any")).isTrue();
        assertThat(RobotsRules.parse("User-agent: GPTBot\nDisallow: /\n").isAllowed("/any")).isTrue();
        assertThat(RobotsRules.parse("").isAllowed("/any")).isTrue();
    }

    @Test
    void 전부_허용과_전부_차단() {
        assertThat(RobotsRules.allowAll().isAllowed("/any")).isTrue();
        assertThat(RobotsRules.disallowAll().isAllowed("/")).isFalse();
        assertThat(RobotsRules.disallowAll().isAllowed("/any?x=1")).isFalse();
    }
}
