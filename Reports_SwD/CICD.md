# Modifiche alla pipeline di CI/CD

Rispetto alla precedente configurazione, la pipeline è stata rivisitata in modo da migliorarla e renderla più chiara.

Innanzitutto il workflow è stato rinominato in "CI Pipeline" in modo che rifletta bene ciò che deve fare.
Successivamente abbiamo disaccoppiato la parte di CI da quella di CD: sono stati infatti rimossi gli step relativi alla dockerizzazione, che verrà poi inserita in un workflow a parte.

Lo step successivo ha riguardato la pulizia dei trigger, abbiamo di fatto eliminato il vecchio branch CI-Test, limitando l'attivazione della pipeline al solo branch main.

Un miglioramento centrale è stato l'unificazione del ciclo Maven sostituendo i comandi separati della vecchia pipeline (clean compile, checkstyle:check e test) con un unico comando mvn -B -ntp clean verify sfruttando il [Maven Build Lifecycle](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html), e la modifica al pom.xml, inserendo la phase validate al posto di verify in modo da effettuare un'analisi statica del testo, compilare ed infine eseguire i test (evitando ridondanze nella pipeline) 	

## Aggiornamento delle dipendenze
Tutte le dipendenze all'interno del file `pom.xml` sono state aggiornate all'ultima versione.
Il gruppo `com.openhtmltopdf`, non più mantenuto, è stato sostituito con `io.github.openhtmltopdf`.

Inoltre, nel tag \<properties> sono state aggiunte delle variabili (e.g., `<jjwt.version>0.12.6</jjwt.version>`) affinché, qualora vi fossero
più dipendenze che attingono da uno stesso gruppo, ognuna di esse aggiornati il gruppo alla stessa versione in modo consistente.

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