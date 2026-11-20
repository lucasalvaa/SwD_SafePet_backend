# Report sulla Code Coverage 

<!-- 
Modifiche effettuate:
4 Scrivere il piano di miglioramento dei test
-->

## Coverage Configuration 
La misurazione della *Code Coverage* è configurata nel file *pom.xml* attraverso il plugin `jacoco-maven-plugin` alla versione `0.8.16`. 
Il ciclo di esecuzione prevede due step principali: l'execution **prepare-agent**, che inizializza l'agente Java prima dell'avvio dei test, e l'execution **report**, che genera automaticamente i report nei formati HTML e XML al termine della suite unitaria. Per concentrare i test sulla reale logica di business, sono state escluse dal calcolo tramite il tag <excludes> le classi seeder, controller, DTO, classi di configurazione, eccezioni custom e la classe principale dell'applicazione.

Per quanto riguarda i *quality gate*, nella configurazione attuale non sono state imposte threshold minime bloccanti, permettendo di monitorare in modo continuo l'andamento della copertura senza interrompere continuamente la pipeline. A livello di CI/CD, il workflow `.github/workflows/ci.yml` esegue il comando `mvn -B -ntp clean verify`, archivia il report HTML come artifact scaricabile su GitHub e invia automaticamente il tracciato XML a Codecov tramite **CODECOV_TOKEN**.

## Coverage Analysis
L'analisi complessiva eseguita sulle 39 classi incluse nel perimetro del progetto mostra una **Instruction Coverage del 51%** (pari a 672 righe coperte su 1.239 totali) e una **Branch Coverage del 42%** (121 branch coperti su 282), a dimostrazione che i test eseguono più della metà delle singole righe di codice, ma esplorano meno della metà dei possibili percorsi logici.

Osservando il dettaglio per singolo package, 11 classi su 39 risultano completamente scoperte, mentre considerando i moduli parzialmente testati, i valori più bassi si registrano in `gestioneUtente.service` (17% di line coverage e 33% di branch coverage) e `gestionePaziente.service` (21% di line coverage e 29% di branch coverage). 
Un caso significativo è rappresentato da `reportCliniche.service`, che raggiunge il 63% di line coverage ma si ferma al 28% di branch coverage.

Considerando invece i metodi, 134 su 342 non risultano testati. Le lacune principali si concentrano nei layer di servizio e sicurezza, in particolare in `gestioneUtente.service` (9 su 11) e `estioneCartellaClinica.service` (9 su 9).
Al contrario, altri moduli come `gestioneCartellaClinica.service.vaccinazione` (100%), `gestioneCartellaClinica.service.terapia` (96%), `gestioneCartellaClinica.service.patologia` (95%) e `gestioneRecensioni.service` (95%) dimostrano un livello di copertura già molto elevato.

## Coverage Improvement Plan