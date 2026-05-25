CREATE TABLE books (
    book_id NUMBER PRIMARY KEY,
    title VARCHAR2(200),
    author_last_name VARCHAR2(100),
    author_first_name VARCHAR2(100),
    rating NUMBER(2,1)
);

INSERT INTO books (book_id, title, author_last_name, author_first_name, rating)
VALUES (1, '1984', 'Orwell', 'George', 4.8);

INSERT INTO books (book_id, title, author_last_name, author_first_name, rating)
VALUES (2, 'Cien años de soledad', 'Garcia Marquez', 'Gabriel', 4.9);

INSERT INTO books (book_id, title, author_last_name, author_first_name, rating)
VALUES (3, 'The Hobbit', 'Tolkien', 'J.R.R.', 4.7);

INSERT INTO books (book_id, title, author_last_name, author_first_name, rating)
VALUES (4, 'Clean Code', 'Martin', 'Robert', 4.6);

INSERT INTO books (book_id, title, author_last_name, author_first_name, rating)
VALUES (5, 'Don Quijote', 'Cervantes', 'Miguel', 4.5);

COMMIT;
