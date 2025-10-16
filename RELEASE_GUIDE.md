# Guida alla Creazione di Release per Nexum

Questa guida spiega come creare una release del progetto Nexum utilizzando GitHub Actions.

## Prerequisiti

1. **GitHub Repository**: Il progetto deve essere su GitHub
2. **GitHub Actions**: Abilitate nel repository
3. **Token GitHub**: Con permessi per creare release

## Workflow di Release

Il workflow di release è configurato in [`.github/workflows/release.yml`](.github/workflows/release.yml) e viene attivato automaticamente quando viene creato un tag che inizia con `v` (es. `v1.0.0`).

## Passaggi per Creare una Release

### 1. Preparare il Codice

```bash
# Assicurarsi di essere sul branch principale
git checkout main

# Aggiornare il numero di versione in pom.xml
# <version>1.0.0</version> → <version>1.1.0</version>

# Commit delle modifiche
git add pom.xml
git commit -m "Prepare release v1.1.0"
```

### 2. Creare un Tag

```bash
# Creare un tag annotato
git tag -a v1.1.0 -m "Release v1.1.0"

# Spingere il tag su GitHub
git push origin v1.1.0
```

### 3. Attendere il Completamento

GitHub Actions eseguirà automaticamente:
1. Build del progetto
2. Generazione del changelog
3. Creazione della release
4. Upload degli artefatti
5. Pubblicazione su GitHub Packages

## Struttura della Release

La release creata automaticamente includerà:

1. **Descrizione**:
   - Changelog generato automaticamente
   - Istruzioni per l'uso
   - Note sulla compatibilità

2. **Artefatti**:
   - `nexum-{version}.jar` - JAR principale
   - `nexum-{version}-javadoc.jar` - Documentazione Javadoc
   - `nexum-{version}-sources.jar` - Codice sorgente

## Tipi di Release

### Release Stabile

```bash
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0
```

### Release di Pre-rilascio

```bash
git tag -a v1.0.0-rc1 -m "Release Candidate 1"
git push origin v1.0.0-rc1
```

## Personalizzazione

### Changelog

Il changelog viene generato automaticamente basandosi sulle etichette (labels) delle issue/PR:

- `feature`, `enhancement` → 🚀 Features
- `bug`, `fix` → 🐛 Bug Fixes
- `chore`, `dependencies`, `refactor` → 🧰 Maintenance
- `documentation` → 📝 Documentation

### Descrizione della Release

La descrizione può essere personalizzata modificando il template in `.github/workflows/release.yml`.

## Verifica

Dopo la creazione della release:

1. Verificare che tutti gli artefatti siano presenti
2. Controllare che il changelog sia corretto
3. Testare il JAR scaricato

## Esempio di Release

### Tag
```
v1.0.0
```

### Descrizione Generata
```
## Nexum v1.0.0

### 🚀 Features
- Aggiunto supporto per transizioni con guard conditions
- Implementato NexumContext per gestione dati

### 🐛 Bug Fixes
- Corretto problema di thread safety nelle transizioni
- Risolto bug nella gestione degli errori

### Artefatti
- JAR principale: nexum-1.0.0.jar
- Javadoc: nexum-1.0.0-javadoc.jar
- Sources: nexum-1.0.0-sources.jar

### Istruzioni
```xml
<dependency>
    <groupId>it.kyakan</groupId>
    <artifactId>nexum</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Note
- Compatibile con Java 11+
- Nessuna dipendenza esterna
```

## Best Practices

1. **Versioning**: Usare [Semantic Versioning](https://semver.org/)
2. **Changelog**: Mantenerlo aggiornato
3. **Testing**: Eseguire tutti i test prima della release
4. **Documentazione**: Aggiornare README.md se necessario
5. **Artefatti**: Verificare che tutti gli artefatti siano corretti

## Risoluzione Problemi

### Workflow Fallito

1. Controllare i log in GitHub Actions
2. Verificare che il tag sia nel formato corretto (`v*`)
3. Assicurarsi che il pom.xml abbia la versione corretta

### Artefatti Mancanti

1. Verificare che il build Maven sia completato con successo
2. Controllare che i plugin Maven siano configurati correttamente

### Permessi Insuficienti

1. Verificare che il token GitHub abbia i permessi necessari
2. Controllare le impostazioni del repository

## Integrazione con CI/CD

Il workflow può essere esteso per:

1. **Deploy automatico** su repository Maven
2. **Notifiche** su Slack/email
3. **Test aggiuntivi** pre-release
4. **Generazione di documentazione** aggiuntiva

## Esempio di Estensione

Per aggiungere il deploy su Maven Central:

```yaml
- name: Deploy to Maven Central
  if: github.event_name == 'push' && contains(github.ref, 'refs/tags/v')
  run: mvn deploy -P release
  env:
    MAVEN_USERNAME: ${{ secrets.OSSRH_USERNAME }}
    MAVEN_PASSWORD: ${{ secrets.OSSRH_PASSWORD }}
    MAVEN_GPG_PASSPHRASE: ${{ secrets.GPG_PASSPHRASE }}
```

## Note Finali

- Il workflow è configurato per creare release **non draft**
- Le release con suffissi `-rc`, `-beta`, `-alpha` vengono marcate come **pre-release**
- Tutti gli artefatti vengono firmati automaticamente