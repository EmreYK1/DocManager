-- Neuer Status SUMMARY_FAILED. Der Name des Constraints ist der, den PostgreSQL in V1 selbst vergeben hat.
alter table documents drop constraint if exists documents_status_check;
alter table documents add constraint documents_status_check
    check (status in ('UPLOADED', 'OCR_PENDING', 'OCR_DONE', 'SUMMARY_DONE', 'SUMMARY_FAILED', 'FAILED'));
