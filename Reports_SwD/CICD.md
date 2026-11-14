# Modifiche alla pipeline di CI/CD

Rispetto alla precedente configurazione, la pipeline è stata rivisitata in modo da migliorarla e renderla più chiara.

Innanzitutto il workflow è stato rinominato in "CI Pipeline" in modo che rifletta bene ciò che deve fare.
Successivamente abbiamo disaccoppiato la parte di CI da quella di CD: sono stati infatti rimossi gli step relativi alla dockerizzazione, che verrà poi inserita in un workflow a parte.

Lo step successivo ha riguardato la pulizia dei trigger, abbiamo di fatto eliminato il vecchio branch CI-Test, limitando l'attivazione della pipeline al solo branch main.

Un miglioramento centrale è stato l'unificazione del ciclo Maven sostituendo i comandi separati della vecchia pipeline (clean compile, checkstyle:check e test) con un unico comando mvn -B -ntp clean verify sfruttando il [Maven Build Lifecycle](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html), e la modifica al pom.xml, inserendo la phase validate al posto di verify in modo da effettuare un'analisi statica del testo, compilare ed infine eseguire i test (evitando ridondanze nella pipeline) 	
