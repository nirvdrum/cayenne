CREATE TABLE ARTIST (
	DATE_OF_BIRTH TIME NULL, 
	ARTIST_ID INT NOT NULL, 
	ARTIST_NAME CHAR(255) NOT NULL, 
	PRIMARY KEY (ARTIST_ID)
);

CREATE TABLE GALLERY (
	GALLERY_ID INT NOT NULL, 
	GALLERY_NAME VARCHAR(100) NOT NULL, 
	PRIMARY KEY (GALLERY_ID)
);

CREATE TABLE PAINTING (
	PAINTING_TITLE VARCHAR(255) NOT NULL, 
	GALLERY_ID INT NULL, 
	ESTIMATED_PRICE DECIMAL NULL, 
	PAINTING_ID INT NOT NULL, 
	ARTIST_ID INT NULL, 
	PRIMARY KEY (PAINTING_ID)
);
