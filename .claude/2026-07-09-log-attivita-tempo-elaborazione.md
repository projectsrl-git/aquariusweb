# 2026-07-09 — Log di sistema attività + tempo elaborazione + ritocchi bilancio

## (1) Titoli sezione ridondanti rimossi
Tolto l'header-titolo dai fragment sezioneTree/sezioneTreeCompact: le sotto-TAB
(Attività/Passività, Costi/Ricavi) fanno già da titolo. Resta il footer "Totale…".

## (2) Tempo di elaborazione
Il controller misura il tempo di calcolo del bilancio (`elaborazioneMs`) e la
pagina lo mostra dopo Elabora ("Elaborato in X,XX s (N ms)"). Utile per le
performance.

## (3) Log di sistema esclusivo web (attività utenti + tempi richieste/risposte)
- `ActivityLoggingInterceptor` (HandlerInterceptor): per ogni richiesta logga
  user, method, uri (+query), status HTTP, durata ms. Registrato in `WebMvcConfig`
  su `/**` escludendo gli statici (/css,/js,/img,/webjars,/favicon,/actuator,/error).
- `logback-spring.xml`: appender dedicato `ACTIVITY_FILE` →
  `aquarius-web-activity.log` (rotazione giornaliera+10MB, 90gg, gzip) e logger
  `AQUARIUS_ACTIVITY` (additivity=false, non sporca il log applicativo).
- File-based, zero overhead su DB. Un viewer in-app + tabella `aq_web_activity_log`
  (via migration) è il naturale passo successivo se serve consultarlo dall'app.

Versione 0.14.0.
