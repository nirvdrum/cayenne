 CREATE OR REPLACE PROCEDURE cayenne_tst_select_proc
 (a_name IN VARCHAR2, painting_price IN NUMBER) AS
   artists cayenne_types.ref_cursor;
 BEGIN
        SET TRANSACTION READ WRITE;
        UPDATE PAINTING SET ESTIMATED_PRICE = ESTIMATED_PRICE * 2
        WHERE ESTIMATED_PRICE < painting_price;
        COMMIT;
        
        OPEN artists FOR
        SELECT DISTINCT A.ARTIST_ID, A.DATE_OF_BIRTH, A.ARTIST_NAME
        FROM ARTIST A, PAINTING P
        WHERE A.ARTIST_ID = P.ARTIST_ID AND
        A.ARTIST_NAME = a_name;
 END;
