CREATE DATABASE IF NOT EXISTS straydog;
USE straydog;

CREATE TABLE IF NOT EXISTS dogs (
  dog_id          VARCHAR(10) PRIMARY KEY,
  name            VARCHAR(50),
  sex             ENUM('M','F'),
  approx_age_months INT,
  color_markings  VARCHAR(200),
  area            VARCHAR(100),
  sterilized      BOOLEAN DEFAULT FALSE,
  sterilized_on   DATE,
  caretaker_phone VARCHAR(15),
  created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vaccinations (
  id           INT AUTO_INCREMENT PRIMARY KEY,
  dog_id       VARCHAR(10) NOT NULL,
  vaccine_type VARCHAR(30) NOT NULL,
  given_on     DATE NOT NULL,
  next_due     DATE,
  batch_no     VARCHAR(30),
  given_by     VARCHAR(80),
  FOREIGN KEY (dog_id) REFERENCES dogs(dog_id)
);

CREATE TABLE IF NOT EXISTS users (
  id            INT AUTO_INCREMENT PRIMARY KEY,
  username      VARCHAR(40) UNIQUE NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  role          ENUM('VET','ADMIN') DEFAULT 'VET'
);
