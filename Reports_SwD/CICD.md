# Modifiche alla pipeline di CI/CD

La baseline conteneva già un file chiamato [build-test.yml](https://github.com/lucasalvaa/SwD_SafePet_backend/blob/53af8be989e15ea82b7121ef46eee072f5f2d5d8/.github/workflows/build-test.yml)
con una pipeline monolitica che include nello stesso workflow sia la logica di Continuous Integration che quella di Continuous Delivery.
In altre parole, subito dopo aver eseguito i test e aver salvato il report di checkstyle, il workflow crea un'immagine Docker del progetto e la carica su DockerHub.
Quest'ultima operazione, tutt'altro che rapida, non viene eseguita solo quando una pull request viene integrata nel branch main, ma anche ogni qual volta che viene aperta una pull request.

Inoltre, disaccoppiare la logica di CI da quella di CD migliorerebbe la leggibilità (e quindi la manutenibilità) delle due pipeline.
Sempre a questo scopo, alcuni steps relativi alla build del progetto potrebbero essere unificati sfruttando meglio le potenzialità di Apache Maven
Pertanto, la pipeline è stata rivisitata in modo da migliorarla e renderla più chiara.

## Disaccoppiamento delle responsabilità e pulizia dei trigger
Innanzitutto, il workflow è stato rinominato in `CI Pipeline`, in modo che rifletta bene ciò che deve fare,
e sono stati infatti rimossi gli steps finali relativi alla CD, che verrà poi implementata in un workflow a parte.

Per quanto riguarda i trigger, l'attivazione della pipeline è stata limitata agli eventi di *push* e *pull_request*
sul solo branch `main`, eliminando di fatto il vecchio branch sperimentale `CI-Test`.

## Least Privilege, Checkout e aggiornamento delle Actions
Al fine di integrare il principio del *Least Privilege* è stato inserito il blocco `permissions: contents: read`: limita il token alla sola lettura.
In questo modo vengono evitate modifiche non autorizzate al codice prima dello step di *checkout*, che scarica il codice sorgente del repository all'interno dell'ambiente virtuale Ubuntu (`ubuntu-latest`) su cui eseguire la build.
```yaml
permissions:
  contents: read

jobs:
  build:
    name: Build & Verify Maven Project
    runs-on: ubuntu-latest

    steps:
      # Checkout del codice sorgente
      - name: Checkout repository
        uses: actions/checkout@v7.0.0
```
Inoltre, tutte le actions utilizzate sono state aggiornate alle versioni più recenti.

## Unificazione degli steps di Maven
Un miglioramento centrale è stato l'unificazione di tre steps concernenti le fasi _compile_, _checkstyle:check_ e _test_
del [Maven Build Lifecycle](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html) con un unico step:
```yaml
# prima 
- name: Maven Build
  run: mvn -B clean compile

- name: Run Checkstyle
  run: mvn -B checkstyle:check

- name: Run Tests
  run: mvn -B test
  
# dopo
- name: Build, Checkstyle & Test
  run: mvn -B -ntp clean verify
```
Il comando `mvn verify` esegue anche le fasi precedenti del ciclo, i.e., _validate_, _compile_, _test_ e _package_.
Nel file `pom.xml`, la fase di esecuzione del plugin checkstyle è stata cambiata da _verify_ a _validate_, affinché
l'analisi statica del codice venga eseguita prima della compilazione dello stesso.

Gli steps successivi per generare il report di checkstyle in html e caricarlo nell'ambiente sono rimasti pressoché
invariati rispetto alla baseline.

```yaml
- name: Generate Checkstyle Report
  if: always()
  run: mvn -B -ntp checkstyle:checkstyle

- name: Upload Checkstyle Report
  if: always()
  uses: actions/upload-artifact@v7.0.0
  with:
    name: checkstyle-report
    path: target/site/checkstyle.html
    if-no-files-found: ignore
```

## Aggiornamento delle dipendenze
Tutte le dipendenze all'interno del file `pom.xml` sono state aggiornate all'ultima versione.
Il gruppo `com.openhtmltopdf`, non più mantenuto, è stato sostituito con `io.github.openhtmltopdf`.

Inoltre, nel tag \<properties> sono state aggiunte delle variabili (e.g., `<jjwt.version>0.12.6</jjwt.version>`) affinché, qualora vi fossero
più dipendenze che attingono da uno stesso gruppo, ognuna di esse aggiorni il gruppo alla stessa versione in modo consistente.

## Salvataggio del file .jar
Per evitare di eseguire una seconda volta la build del progetto in fase di Continuous Delivery, si può salvare l'artefatto `target/backend.jar`.
Lo snippet sottostante riporta un nuovo step aggiunto al workflow, con un controllo per effettuare il salvataggio soltanto quando l'evento è un _push_ sul main, i.e., non quando l'evento è una _pull request_.
L'utilizzo di `github.sha` nel nome dell'artefatto da salvare permette di associare inequivocabilmente l'artefatto al commit che ha fatto scattare il workflow.
```yaml
- name: Upload Application JAR
  if: success() && github.event_name == 'push' && github.ref == 'refs/heads/main'
  uses: actions/upload-artifact@v7.0.0
  with:
    name: application-${{ github.sha }}
    path: target/backend.jar
    if-no-files-found: error # Lancia un errore qualora il file non venisse trovato
    retention-days: 7 # Cancella l'artefatto dal cloud dopo sette giorni
```