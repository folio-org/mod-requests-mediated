CREATE OR REPLACE FUNCTION fn_set_status_text_copy_column()
  RETURNS TRIGGER
  AS
'
BEGIN
  NEW.mediated_request_status := NEW.status::text;
  RETURN NEW;
END;
'
LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_medbatchreq_set_status_text_copy_column ON batch_request;
CREATE TRIGGER trg_medbatchreq_set_status_text_copy_column
  BEFORE INSERT OR UPDATE
  ON batch_request
  FOR EACH ROW EXECUTE FUNCTION fn_set_status_text_copy_column();

DROP TRIGGER IF EXISTS trg_medbatchreqsplit_set_status_text_copy_column ON batch_request_split;
CREATE TRIGGER trg_medbatchreqsplit_set_status_text_copy_column
  BEFORE INSERT OR UPDATE
  ON batch_request_split
  FOR EACH ROW EXECUTE FUNCTION fn_set_status_text_copy_column();
