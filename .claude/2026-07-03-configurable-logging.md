# 2026-07-03 — Configurable per-environment logging

## Problem / goal
Login was failing and there was no reliable place to read the stack trace:
`logging.file.name` used ${AQUARIUSWEB_HOME:.}/logs, and under Tomcat "." is
Tomcat's working dir (bin/), so aquarius.log landed in an unpredictable spot.
Goal: a configurable, per-environment log directory that is easy to find and
archive.

## Approach
- Replaced Spring's `logging.file.*` with a dedicated `logback-spring.xml`.
- Log directory comes from property `aquarius.log.dir`, resolved via
  `springProperty` in logback. Precedence: env var AQUARIUS_LOG_DIR >
  -Daquarius.log.dir > per-profile value > ./logs fallback.
- Each versioned profile sets a dedicated dir:
    dev  -> D:/logs/aquariusweb/dev
    sit  -> D:/logs/aquariusweb/sit
    uat  -> D:/logs/aquariusweb/uat
    prod -> D:/logs/aquariusweb/prod
- Rotation: SizeAndTimeBasedRollingPolicy — new file daily AND at 10MB;
  archives gzipped under <dir>/archive/, kept 90 days, total capped (3GB app,
  1GB error). cleanHistoryOnStart prunes on boot.
- Two file appenders: aquarius.log (everything at configured level) and
  aquarius-error.log (WARN+ only, for fast incident triage). Console appender
  kept so catalina.out still mirrors output.

## Files touched
- NEW: src/main/resources/logback-spring.xml
- application.properties: dropped logging.file.name/max-size/max-history;
  added aquarius.log.dir with env/home fallback chain.
- application-{dev,sit,uat,prod}.properties: added aquarius.log.dir per env.

## How to use
- Logs are at the per-env dir above; current file aquarius.log, archives in
  <dir>/archive/*.log.gz. Errors also in aquarius-error.log.
- Override anywhere: set AQUARIUS_LOG_DIR env var (Tomcat setenv) or
  -Daquarius.log.dir=... ; wins over the profile value.
- Logback auto-creates the directory on first write.

## Login error (diagnosis aid)
This change is what makes the failure readable. Likely root cause to confirm
from the fresh logs: config/application-dev.properties not found in Tomcat's
working dir, so tenant DB uses CHANGE_ME placeholders and the first
post-login data access fails. If so, set an absolute path or use env vars in
Tomcat setenv, or place config/ relative to Tomcat's CWD.

## Not verified (confirm on deploy)
- Actual login stack trace now landing in D:/logs/aquariusweb/dev/aquarius-error.log.
- Windows path with forward slashes (Java accepts them) and dir auto-create.
