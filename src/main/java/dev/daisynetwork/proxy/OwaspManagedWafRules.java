package dev.daisynetwork.proxy;

import java.util.List;

public final class OwaspManagedWafRules {
    private OwaspManagedWafRules() {
    }

    public static List<WafRule> baseline() {
        return List.of(
                new HeaderValueWafRule(
                        "OWASP-CRS-920160",
                        "Protocol Enforcement: Invalid Content-Length",
                        "PROTOCOL",
                        WafSeverity.CRITICAL,
                        1,
                        "content-length",
                        "\\D",
                        false,
                        "Content-Length must be numeric"),
                new PatternWafRule(
                        "OWASP-CRS-921110",
                        "HTTP Request Smuggling Header",
                        "PROTOCOL",
                        WafSeverity.CRITICAL,
                        1,
                        PatternWafRule.Target.HEADER,
                        "(?:\\btransfer-encoding\\b.*\\bchunked\\b|\\bcontent-length\\b.*\\bcontent-length\\b)",
                        "request contains smuggling-risk header content"),
                new PatternWafRule(
                        "OWASP-CRS-930100",
                        "Path Traversal Attack",
                        "LFI",
                        WafSeverity.CRITICAL,
                        1,
                        PatternWafRule.Target.ANY,
                        "(?:\\.\\./|\\.\\.\\\\|%2e%2e|%252e%252e|/etc/passwd|boot\\.ini)",
                        "path traversal or local file inclusion pattern detected"),
                new PatternWafRule(
                        "OWASP-CRS-932100",
                        "Remote Command Execution",
                        "RCE",
                        WafSeverity.CRITICAL,
                        1,
                        PatternWafRule.Target.ANY,
                        "(?:;|&&|\\|\\|)\\s*(?:cat|curl|wget|bash|sh|powershell|cmd)(?:\\s|$)",
                        "remote command execution pattern detected"),
                new PatternWafRule(
                        "OWASP-CRS-941100",
                        "Cross-Site Scripting Attack",
                        "XSS",
                        WafSeverity.CRITICAL,
                        1,
                        PatternWafRule.Target.ANY,
                        "(?:<\\s*script\\b|javascript\\s*:|onerror\\s*=|onload\\s*=|alert\\s*\\()",
                        "cross-site scripting pattern detected"),
                new PatternWafRule(
                        "OWASP-CRS-942100",
                        "SQL Injection Attack",
                        "SQLI",
                        WafSeverity.CRITICAL,
                        1,
                        PatternWafRule.Target.ANY,
                        "(?:\\bunion\\b\\s+\\bselect\\b|\\bor\\b\\s+1\\s*=\\s*1|--\\s|/\\*|\\bsleep\\s*\\(|\\bdrop\\s+table\\b)",
                        "SQL injection pattern detected"),
                new PatternWafRule(
                        "OWASP-CRS-913100",
                        "Scanner or Bot User Agent",
                        "SCANNER",
                        WafSeverity.WARNING,
                        2,
                        PatternWafRule.Target.HEADER,
                        "(?:sqlmap|nikto|acunetix|nessus|masscan|nmap)",
                        "known scanner user agent detected"),
                new PatternWafRule(
                        "OWASP-CRS-920420",
                        "Restricted File Extension",
                        "PROTOCOL",
                        WafSeverity.ERROR,
                        2,
                        PatternWafRule.Target.PATH,
                        "(?:\\.bak|\\.config|\\.env|\\.git|\\.svn)(?:$|/)",
                        "restricted file extension requested"),
                new PatternWafRule(
                        "OWASP-CRS-933100",
                        "PHP Injection Attack",
                        "PHP",
                        WafSeverity.CRITICAL,
                        3,
                        PatternWafRule.Target.ANY,
                        "(?:<\\?php|php://input|data://text|expect://)",
                        "PHP injection pattern detected"),
                new PatternWafRule(
                        "OWASP-CRS-942430",
                        "SQL Function Abuse",
                        "SQLI",
                        WafSeverity.ERROR,
                        3,
                        PatternWafRule.Target.ANY,
                        "(?:\\bbenchmark\\s*\\(|\\binformation_schema\\b|\\bload_file\\s*\\()",
                        "advanced SQL injection pattern detected"),
                new PatternWafRule(
                        "OWASP-CRS-920500",
                        "High ASCII or Null Byte Evasion",
                        "PROTOCOL",
                        WafSeverity.WARNING,
                        4,
                        PatternWafRule.Target.ANY,
                        "(?:%00|\\\\x00|%u[0-9a-f]{4})",
                        "evasion encoding pattern detected"));
    }
}
