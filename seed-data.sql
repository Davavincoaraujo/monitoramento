-- Seed data for development/testing
-- Run this after Flyway migrations complete

-- Insert example sites
INSERT INTO sites (name, base_url, enabled, frequency_seconds, email_recipients, created_at, updated_at)
VALUES 
    ('Google', 'https://www.google.com', true, 300, 'dev@example.com', NOW(), NOW()),
    ('GitHub', 'https://github.com', true, 600, 'dev@example.com', NOW(), NOW()),
    ('Example.com', 'https://example.com', true, 300, 'dev@example.com', NOW(), NOW());

-- Insert pages for Google (site_id = 1)
INSERT INTO site_pages (site_id, name, path, enabled, created_at)
VALUES 
    (1, 'Google Home', '/', true, NOW()),
    (1, 'Google Search', '/search?q=test', true, NOW());

-- Insert pages for GitHub (site_id = 2)
INSERT INTO site_pages (site_id, name, path, enabled, created_at)
VALUES 
    (2, 'GitHub Home', '/', true, NOW()),
    (2, 'GitHub Explore', '/explore', true, NOW()),
    (2, 'GitHub Pricing', '/pricing', true, NOW());

-- Insert pages for Example.com (site_id = 3)
INSERT INTO site_pages (site_id, name, path, enabled, created_at)
VALUES 
    (3, 'Example Home', '/', true, NOW());

-- Insert example rules
INSERT INTO rules (site_id, page_id, rule_type, threshold, severity, created_at)
VALUES 
    (1, NULL, 'MAX_LOAD_MS', 3000, 'MAJOR', NOW()),
    (1, NULL, 'MAX_TTFB_MS', 1000, 'MAJOR', NOW()),
    (2, NULL, 'MAX_LOAD_MS', 5000, 'MAJOR', NOW()),
    (3, 1, 'MAX_LOAD_MS', 2000, 'CRITICAL', NOW());

-- Verify data
SELECT 
    s.id, 
    s.name, 
    COUNT(sp.id) as page_count,
    s.frequency_seconds,
    s.enabled
FROM sites s
LEFT JOIN site_pages sp ON sp.site_id = s.id
GROUP BY s.id, s.name, s.frequency_seconds, s.enabled
ORDER BY s.id;
