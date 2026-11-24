# Pawtection - QR Vaccination Tracker for Street Dogs

A QR code on a dog's ear tag opens a page showing its photo, sterilization status and vaccination history. Vets log in to register dogs and record vaccinations.

## Tech stack
Java 17, Javalin, JDBC, MySQL, HikariCP, ZXing, BCrypt

## Run locally
1. Install JDK 17+, Maven and MySQL
2. Run `schema.sql` in MySQL
3. Set `DB_PASS` (plus `DB_URL`, `DB_USER`, `BASE_URL`, `ADMIN_PASS` if needed)
4. Run `mvn compile exec:java`
5. Open http://localhost:7070/ and log in as `admin`

## Pages
- `/` : vet login
- `/register` : register a dog, add a photo and a vaccination (login required)
- `/d/ID` : public page opened by the QR code
