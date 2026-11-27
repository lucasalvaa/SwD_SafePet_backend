# Strategia di Testing

La baseline del backend di *SafePet* include una test suite contenente unit test suddivisi in 12 classi di test, eseguiti con successo. I casi di test sono stati progettati con approccio black-box (specification based), tramite *Category Partitioning*.

In questa fase i test già presenti sono stati analizzati per verificare l'aderenza ai requisiti di dependability richiesti.

## Struttura della Test Suite

La suite in [`src/test/java`](../src/test/java) segue l'organizzazione per moduli funzionali presente in `src/main/java`: ogni classe si trova nel package `it.safepet.backend.<modulo>.service` corrispondente.

I test si concentrano sullo strato `Service`, il quale contiene la logica principale dell'applicazione, coprendo i flussi d'esecuzione più importanti e più utilizzati: autenticazione, registrazione proprietario, gestione pet e note, associazione pazienti, cartella clinica, recensioni, analisi dermatologica e mappa real-time.

Nella test suite non sono comprese classi di test per lo strato Controller, per le Entity e i DTO, poiché contengono solo routing HTTP o semplici getter e setter, senza nessuna logica complessa. 
Inoltre, i vincoli sui dati in ingresso nei DTO di richiesta (e.g., formato dell'email, requisiti della password, codice microchip a 15 cifre o range del punteggio delle recensioni), vengono verificati tramite annotazioni con `Jakarta Bean Validation` e verificati nei test attraverso un `Validator` in memoria.

## Dipendenze e configurazione di JUnit e Mockito

Nel file `pom.xml` non sono presenti esplicitamente le dipendenze `junit-jupiter` e `mockito-junit-jupiter` poiché vengono ereditate direttamente da `spring-boot-starter-test`. In questo modo vengono utilizzati automaticamente:
`JUnit Jupiter 5.12.2` e `Mockito 5.17`;

Poiché si tratta di versioni recenti, stabili e pienamente compatibili con `Spring Boot 3.5` e `Java 21`, non sono stati effettuati override manuali delle versioni, per evitare possibili conflitti.

Per isolare la logica dei Service nelle classi di test viene utilizzata l'estensione `@ExtendWith(MockitoExtension.class)`, tramite la quale tutte le dipendenze esterne vengono simulate con Mockito, i.e., i repository JPA (evitando l'accesso a un DB reale), il client verso il servizio esterno di analisi dermatologica e il contesto di autenticazione statico (gestito tramite `MockedStatic`).

## Principi FIRST

Analisi dell'aderenza ai principi FIRST della suite:

- **Fast**: i test non avviano il contesto Spring e non richiedono un database reale; completano l'esecuzione in pochi secondi.

- **Independent**: ogni test prepara il proprio stato in `@BeforeEach` e rilascia le risorse statiche in `@AfterEach`, evitando dipendenze tra metodi.  Esempio in `GestionePetServiceImplCreaNotaTest`:

    ```java
    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        authContextMock = Mockito.mockStatic(AuthContext.class);
    }

    @AfterEach
    void tearDown() {
        authContextMock.close();
    }
    ```

- **Repeatable**: i test non dipendondo da servizi esterni e i risultati sono deterministici. Le date richieste dai test vengono calcolate dinamicamente rispetto alla data corrente.

- **Self-validating**: l'esito dei test è verificato automaticamente tramite asserzioni (`assertThat`, `assertThrows`, `assertThatThrownBy`) e verify. L'esempio seguente in `GestionePetServiceImplCreaPetTest#TC_CreaPet_11` è esaustivo per i due punti precedenti:
    
    ```java
    NewPetDTO dto = buildValidRequestForService();
    dto.setDataNascita(LocalDate.now().plusDays(1));
    // ... configurazione AuthContext e mock repository ...
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> service.creaPet(dto));
    assertThat(ex.getMessage()).isEqualTo("La data di nascita non può essere nel futuro");
    verify(petRepository, never()).save(any(Pet.class));
    ```

- **Thorough**: oltre ai casi validi (happy path), vengono testati valori limite, errori di validazione e accessi non autorizzati.  Esempio in `AnalisiDermatologicaServiceImplTest#TC_analisiDermatologica_4`:
    
    ```java
    int size = 10 * 1024 * 1024 + 1; // 10MB + 1 byte
    byte[] image = new byte[size];
    
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            service.avviaAnalisiDermatologica(image));
    assertThat(exception.getMessage()).contains("Immagine troppo grande");
    verifyNoInteractions(skinDetectorAdapter);
    ```

## Integrazione in CI/CD
Tutti i 151 test vengono eseguiti nel workflow `CI Pipeline` su GitHub Actions tramite `mvn -B -ntp clean verify`. Lo stage `test` precede lo stage `verify` nel [Maven Build Lifecycle](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html). I file di test utilizzano esclusivamente dati fittizi, senza alcuna credenziale reale o segreto hardcoded. Lo step successivo del workflow salva il report. 