# Generated Java Backend

Three-tier architecture (Controller / Service / Repository).

Entities: Order

## Build & Test
```bash
mvn test
```

Offline verification (no deps):
```bash
javac -d out $(find src/main/java verify -name '*.java')
java -cp out com.example.app.Verification
```
