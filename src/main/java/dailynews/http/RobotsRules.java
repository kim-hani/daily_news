package dailynews.http;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * robots.txt의 {@code User-agent: *} 그룹 규칙.
 * Allow/Disallow 중 가장 길게 일치하는 규칙을 따르고, 길이가 같으면 Allow가 이긴다(RFC 9309).
 */
final class RobotsRules {

    private static final RobotsRules ALLOW_ALL = new RobotsRules(List.of());
    private static final RobotsRules DISALLOW_ALL = new RobotsRules(List.of(Rule.of("/", false)));

    private final List<Rule> rules;

    private RobotsRules(List<Rule> rules) {
        this.rules = List.copyOf(rules);
    }

    static RobotsRules allowAll() {
        return ALLOW_ALL;
    }

    static RobotsRules disallowAll() {
        return DISALLOW_ALL;
    }

    /**
     * robots.txt를 해석한다. 연속된 User-agent 줄이 한 그룹의 머리이고, 빈 줄은 그룹을 끝내지 않는다.
     * {@code *} 그룹이 여러 개면 규칙을 합친다.
     */
    static RobotsRules parse(String text) {
        List<Rule> rules = new ArrayList<>();
        boolean readingAgents = false;
        boolean groupApplies = false;
        for (String rawLine : text.replace("﻿", "").split("\\R")) {
            String line = stripComment(rawLine);
            int colon = line.indexOf(':');
            if (colon < 0) {
                continue;
            }
            String key = line.substring(0, colon).strip().toLowerCase(Locale.ROOT);
            String value = line.substring(colon + 1).strip();
            switch (key) {
                case "user-agent" -> {
                    if (!readingAgents) {
                        groupApplies = false;
                    }
                    groupApplies |= value.equals("*");
                    readingAgents = true;
                }
                case "allow", "disallow" -> {
                    readingAgents = false;
                    // 값이 빈 Disallow는 "전부 허용"이라 규칙으로 남기지 않는다.
                    if (groupApplies && !value.isEmpty()) {
                        rules.add(Rule.of(value, key.equals("allow")));
                    }
                }
                case "sitemap" -> {
                    // 그룹에 속하지 않는 항목
                }
                default -> readingAgents = false;
            }
        }
        return new RobotsRules(rules);
    }

    /**
     * @param pathAndQuery 경로와 쿼리(예: {@code /view/AKR2026?section=x}). 퍼센트 인코딩은 그대로 비교한다.
     */
    boolean isAllowed(String pathAndQuery) {
        Rule best = null;
        for (Rule rule : rules) {
            if (!rule.matches(pathAndQuery)) {
                continue;
            }
            if (best == null || rule.length() > best.length()
                    || (rule.length() == best.length() && rule.allow() && !best.allow())) {
                best = rule;
            }
        }
        return best == null || best.allow();
    }

    private static String stripComment(String line) {
        int hash = line.indexOf('#');
        return hash < 0 ? line : line.substring(0, hash);
    }

    /**
     * @param path 원래 규칙 문자열. 길이로 우선순위를 정한다.
     * @param regex {@code *}는 임의 문자열, 끝의 {@code $}는 끝 고정으로 바꾼 정규식(앞부분 일치)
     */
    private record Rule(String path, boolean allow, Pattern regex) {

        static Rule of(String path, boolean allow) {
            boolean anchored = path.endsWith("$");
            String body = anchored ? path.substring(0, path.length() - 1) : path;
            String[] parts = body.split("\\*", -1);
            StringBuilder regex = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) {
                    regex.append(".*");
                }
                regex.append(Pattern.quote(parts[i]));
            }
            if (anchored) {
                regex.append('$');
            }
            return new Rule(path, allow, Pattern.compile(regex.toString()));
        }

        boolean matches(String pathAndQuery) {
            return regex.matcher(pathAndQuery).lookingAt();
        }

        int length() {
            return path.length();
        }
    }
}
