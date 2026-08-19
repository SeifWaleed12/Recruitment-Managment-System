package com.banquemisr.recruitment.cvparsing.extractor;

import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SkillsExtractor {

    // Controlled skill vocabulary with exact canonical names and matching regex patterns
    private static final List<SkillRule> SKILL_RULES = List.of(
            // Languages
            new SkillRule("Java", "\\bjava\\b(?!script)"),
            new SkillRule("Python", "\\bpython\\b"),
            new SkillRule("C++", "(?:\\bc\\+\\+|\\bcpp\\b)"),
            new SkillRule("C#", "(?:\\bc#|\\bcsharp\\b)"),
            new SkillRule("JavaScript", "(?:\\bjavascript\\b|\\bjs\\b)"),
            new SkillRule("TypeScript", "(?:\\btypescript\\b|\\bts\\b)"),
            new SkillRule("Go", "\\bgolang\\b|\\bgo\\s+language\\b"),
            new SkillRule("Kotlin", "\\bkotlin\\b"),
            new SkillRule("PHP", "\\bphp\\b"),
            new SkillRule("Ruby", "\\bruby\\b"),
            new SkillRule("SQL", "\\bsql\\b"),
            new SkillRule("HTML", "\\bhtml5?\\b"),
            new SkillRule("CSS", "\\bcss3?\\b"),
            new SkillRule("Bash", "(?:\\bbash\\b|\\bshell\\s+scripting\\b)"),

            // Frameworks & Libraries
            new SkillRule("Spring Boot", "\\bspring\\s*boot\\b"),
            new SkillRule("Spring", "\\bspring\\b(?!\\s*boot)"),
            new SkillRule("Hibernate", "\\bhibernate\\b"),
            new SkillRule("JPA", "\\bjpa\\b"),
            new SkillRule("React", "\\breact(?:\\.js|js)?\\b"),
            new SkillRule("Angular", "\\bangular(?:\\.js|js)?\\b"),
            new SkillRule("Vue.js", "\\bvue(?:\\.js|js)?\\b"),
            new SkillRule("Node.js", "\\bnode(?:\\.js|js)?\\b"),
            new SkillRule("Express.js", "\\bexpress(?:\\.js|js)?\\b"),
            new SkillRule("Django", "\\bdjango\\b"),
            new SkillRule("Flask", "\\bflask\\b"),
            new SkillRule("FastAPI", "\\bfastapi\\b"),
            new SkillRule(".NET", "(?:\\.net\\b|\\basp\\.net\\b)"),

            // Databases & Storage
            new SkillRule("PostgreSQL", "(?:\\bpostgresql\\b|\\bpostgres\\b)"),
            new SkillRule("MySQL", "\\bmysql\\b"),
            new SkillRule("Oracle Database", "(?:\\boracle\\s+db\\b|\\boracle\\s+database\\b)"),
            new SkillRule("MongoDB", "(?:\\bmongodb\\b|\\bmongo\\b)"),
            new SkillRule("Redis", "\\bredis\\b"),
            new SkillRule("Elasticsearch", "\\belasticsearch\\b"),
            new SkillRule("Cassandra", "\\bcassandra\\b"),

            // Cloud & DevOps
            new SkillRule("Docker", "\\bdocker\\b"),
            new SkillRule("Kubernetes", "(?:\\bkubernetes\\b|\\bk8s\\b)"),
            new SkillRule("AWS", "(?:\\baws\\b|\\bamazon\\s+web\\s+services\\b)"),
            new SkillRule("Azure", "(?:\\bazure\\b|\\bmicrosoft\\s+azure\\b)"),
            new SkillRule("GCP", "(?:\\bgcp\\b|\\bgoogle\\s+cloud\\b)"),
            new SkillRule("CI/CD", "(?:\\bci/cd\\b|\\bci-cd\\b|\\bcontinuous\\s+integration\\b)"),
            new SkillRule("Jenkins", "\\bjenkins\\b"),
            new SkillRule("Terraform", "\\bterraform\\b"),
            new SkillRule("Git", "\\bgit\\b(?!hub|lab)"),
            new SkillRule("GitHub", "\\bgithub\\b"),
            new SkillRule("GitLab", "\\bgitlab\\b"),
            new SkillRule("Linux", "\\blinux\\b"),
            new SkillRule("Maven", "\\bmaven\\b"),
            new SkillRule("Gradle", "\\bgradle\\b"),

            // Architecture & Messaging
            new SkillRule("Microservices", "\\bmicroservices?\\b"),
            new SkillRule("REST API", "(?:\\brest(?:ful)?\\s*apis?\\b|\\brestful\\b|\\brest\\s+web\\s+services\\b)"),
            new SkillRule("GraphQL", "\\bgraphql\\b"),
            new SkillRule("Kafka", "(?:\\bapache\\s+kafka\\b|\\bkafka\\b)"),
            new SkillRule("RabbitMQ", "\\brabbitmq\\b"),
            new SkillRule("Design Patterns", "\\bdesign\\s+patterns?\\b"),
            new SkillRule("SOLID Principles", "\\bsolid\\s+(?:principles|design)\\b"),

            // Testing & Methodologies
            new SkillRule("JUnit", "\\bjunit\\b"),
            new SkillRule("Mockito", "\\bmockito\\b"),
            new SkillRule("Testcontainers", "\\btestcontainers\\b"),
            new SkillRule("TDD", "(?:\\btdd\\b|\\btest\\s+driven\\s+development\\b)"),
            new SkillRule("Agile", "\\bagile\\b"),
            new SkillRule("Scrum", "\\bscrum\\b"),
            new SkillRule("Jira", "\\bjira\\b")
    );

    public Set<String> extractSkills(String text) {
        Set<String> matchedSkills = new LinkedHashSet<>();
        if (text == null || text.isBlank()) {
            return matchedSkills;
        }

        String normalized = text.toLowerCase(Locale.ROOT);

        for (SkillRule rule : SKILL_RULES) {
            Matcher matcher = rule.pattern.matcher(normalized);
            if (matcher.find()) {
                matchedSkills.add(rule.canonicalName);
            }
        }

        return matchedSkills;
    }

    private static class SkillRule {
        final String canonicalName;
        final Pattern pattern;

        SkillRule(String canonicalName, String regex) {
            this.canonicalName = canonicalName;
            this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        }
    }
}
