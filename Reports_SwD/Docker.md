# Dockerfile, compose.yaml e workflow docker-build.yml

## Dockerfile
La base image scelta è `gcr.io/distroless/java21-debian13:nonroot`: si tratta di un'immagine [_distroless_](https://docs.docker.com/dhi/explore/security-concepts/distroless/), che contiene solo il runtime necessario per Java 21, l'applicazione e le sue dipendenze, senza shell né package manager. I vantaggi sono un'immagine più leggera da scaricare e avviare, una superficie d'attacco ridotta, meno vulnerabilità da gestire e una maggiore sicurezza in quanto utenti malintenzionati non hanno accesso alla shell; quest'ultimo è purtroppo anche uno svantaggio per il debugging. 
Il tag `nonroot` fa girare il processo con un utente senza privilegi, secondo il principio del Least Privilege: se l'applicazione fosse compromessa, l'attaccante avrebbe permessi molto limitati. Anche il JAR è copiato con `--chown=nonroot:nonroot`.
L'entrypoint della base image è già `java -jar`, quindi il Dockerfile indica con `CMD` solo il percorso del JAR da eseguire.


## _compose.yaml_
Il file è stato chiamato [compose.yaml]() per seguire il [nuovo nome canonico di Docker Compose](https://docs.docker.com/compose/intro/compose-application-model/#the-compose-file). La network è composta da tre services: `mysql`, `skin-detector` (un'API per utilizzare una CNN da noi addestrata), e `backend`. Quest'ultimo dipende dall'healthcheck di `mysql` e dalla creazione di `skin-detector`, per il quale non è previsto healthcheck in quanto la sua API espone solo l'endpoint `/predict`, i.e., nessun endpoint come `/ping` o `/health`. Non è possibile eseguire un healthcheck sul servizio `backend` per via della natura _distroless_ della base image. L'healthcheck di `mysql` invia un ping al servizio per un massimo di 5 volte, un timer di timeout di cinque secondi e un'attesa di tre secondi per il ping successivo in caso di timeout. Il database viene reso persistente anche all'esterno del container su un volume chiamato `mysql_data`.
Tutti e tre i servizi hanno l'attributo `restart: on-failure:3`, se il container termina per un errore, viene riavviato in automatico fino a un massimo di tre volte. Sono stati definiti tre `secrets` per le credenziali di accesso al database all'interno di file di testo situati nella directory `/secrets`, la quale è stata inserita nel `.gitignore` per non poter essere caricata su GitHub.

## Workflow GitHub Actions

### Setup del workflow e download del JAR 
Il workflow si avvia solo quando termina il precedente `CI Pipeline` (evento `workflow_run`, tipo `completed`, branch `main`).
Inoltre, è presente un controllo di robustezza per eseguire i jobs solo se la CI è terminata con successo e se l'evento d'origine è un `push`. In altre parole, riteniamo utile creare l'immagine Docker solo quando le pull request vengono integrate sul branch principale.

È stato aggiunto un controllo sulla concorrenza per impedire che, nel caso in cui due push ravvicinati su `main` terminino in ordine inverso, venga caricata su DockerHub un'immagine più vecchia con il tag `:latest`. Con `cancel-in-progress: false` l'esecuzione già avviata non viene interrotta e la successiva attende il suo turno.
```yaml
concurrency:
  group: docker-publish
  cancel-in-progress: false
```

Per la build dell'immagine Docker occorre recuperare il JAR prodotto in precedenza dalla CI e salvato con il nome `application-<sha>`, dove `<sha>` è l'hash del commit che ha innescato la CI. Il JAR viene scaricato nello step `Download Application JAR`, indicando l'id della run di CI (`run-id`) e un token con permesso di lettura sulle Actions (`github-token`).

### _Build_ dell'immagine e _push_ su DockerHub 
Lo username di DockerHub è stato definito come variabile (`vars.DOCKERHUB_USERNAME`) piuttosto che come secret perché, altrimenti, nei log di GitHub Actions il nome dell'immagine comparirebbe come `***/safepet-backend`, rendendo il log incomprensibile per un eventuale debugging.

Due attributi degni di nota dello step _Build and Push on Dockerhub_ sono `provenance: true` e `sbom: true`. Il primo allega all'immagine una raccolta di metadati per documentare come, dove e da quali sorgenti è stata creata. Il secondo allega l'elenco dei componenti presenti che gli scanner di vulnerabilità come Docker Scout usano per segnalare le librerie a rischio.

Nella build non viene sfruttata la cache perché occuperebbe quota di GitHub senza alcun vantaggio: l'immagine, essendo minimale, non prevede passaggi costosi e, inoltre, il layer che contiene il JAR dovrebbe cambiare in ogni build, rendendo superfluo il suo salvataggio.