package kr.reborn.skill.cast;

import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;

import java.util.HashMap;
import java.util.Map;

/**
 * 매우 간이한 수식 평가기.
 * 지원: + - * / ( ), 변수 = 스탯 이름 소문자, 숫자, 함수 없음.
 * 예) "intelligence * 1.5 + mana * 0.1"
 */
public final class Formula {

    public static double eval(String expr, PlayerData d) {
        if (expr == null || expr.isBlank()) return 0;
        Map<String, Double> vars = new HashMap<>();
        for (StatType t : StatType.values()) {
            vars.put(t.name().toLowerCase(), d.getStat(t));
        }
        double total = 0;
        for (StatType t : StatType.COMMON_8) total += d.getStat(t);
        vars.put("total_stats", total);
        return new Parser(expr, vars).parse();
    }

    private static final class Parser {
        final String s; int p; final Map<String, Double> v;
        Parser(String s, Map<String, Double> v) { this.s = s; this.v = v; }

        double parse() { return expr(); }

        double expr() {
            double a = term();
            while (p < s.length()) {
                char c = peek();
                if (c == '+') { p++; a += term(); }
                else if (c == '-') { p++; a -= term(); }
                else break;
            }
            return a;
        }

        double term() {
            double a = factor();
            while (p < s.length()) {
                char c = peek();
                if (c == '*') { p++; a *= factor(); }
                else if (c == '/') { p++; a /= factor(); }
                else break;
            }
            return a;
        }

        double factor() {
            skip();
            char c = peek();
            if (c == '(') { p++; double v0 = expr(); skip(); if (p < s.length() && s.charAt(p) == ')') p++; return v0; }
            if (c == '-') { p++; return -factor(); }
            if (Character.isDigit(c) || c == '.') return num();
            return ident();
        }

        double num() {
            int start = p;
            while (p < s.length() && (Character.isDigit(s.charAt(p)) || s.charAt(p) == '.')) p++;
            return Double.parseDouble(s.substring(start, p));
        }

        double ident() {
            int start = p;
            while (p < s.length() && (Character.isLetterOrDigit(s.charAt(p)) || s.charAt(p) == '_')) p++;
            String n = s.substring(start, p);
            return v.getOrDefault(n.toLowerCase(), 0.0);
        }

        char peek() { skip(); return p < s.length() ? s.charAt(p) : '\0'; }
        void skip() { while (p < s.length() && Character.isWhitespace(s.charAt(p))) p++; }
    }
}
