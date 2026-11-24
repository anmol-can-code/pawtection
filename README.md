# Stray Dog QR Vaccination Tracker
1. Install JDK 17+, Maven, MySQL.
2. Run schema.sql in MySQL.
3. Set env vars if needed: DB_URL, DB_USER, DB_PASS, BASE_URL, ADMIN_PASS
4. `mvn compile exec:java`
5. Open http://localhost:7070/vet.html  (login: admin / admin123)
