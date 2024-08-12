DROP INDEX IF EXISTS idx_medreq_item_barcode;
DROP INDEX IF EXISTS idx_medreq_instance_title;
DROP INDEX IF EXISTS idx_medreq_requester_barcode;
DROP INDEX IF EXISTS idx_medreq_call_number;
DROP INDEX IF EXISTS idx_medreq_full_call_number;

CREATE INDEX IF NOT EXISTS idx_medreq_item_barcode ON mediated_request(lower(item_barcode));
CREATE INDEX IF NOT EXISTS idx_medreq_instance_title ON mediated_request(lower(instance_title));
CREATE INDEX IF NOT EXISTS idx_medreq_requester_barcode ON mediated_request(lower(requester_barcode));
CREATE INDEX IF NOT EXISTS idx_medreq_call_number ON mediated_request(lower(call_number));
CREATE INDEX IF NOT EXISTS idx_medreq_full_call_number ON mediated_request(lower(full_call_number));